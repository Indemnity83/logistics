package com.logistics.core.lib.power;

import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.EnergyPushService;
import com.logistics.core.lib.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for energy buffer blocks (batteries).
 *
 * <p>Concrete subclasses provide capacity/rate constants via the constructor.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Accepts energy on all sides (loader energy API + low-tier from Redstone Engines)</li>
 *   <li>Each tick: pushes energy into adjacent non-pipe blocks that accept it (machines, etc.)</li>
 * </ul>
 *
 * <p>Batteries do not feed the logistics pipe network directly — power enters a network through a
 * Power Junction (which the network draws from); batteries supply that junction over cables.
 */
public abstract class AbstractBatteryBlockEntity extends BaseBlockEntity
        implements HasEnergyStorage, AcceptsLowTierEnergy {

    /** Max RF to push into a single adjacent machine per tick. */
    private static final long MAX_OUTPUT_PER_SIDE = 200L;

    /**
     * Discrete charge level (0–10) exposed as a block state property so the battery's fill bar is
     * rendered by the vanilla multipart blockstate — no block entity renderer required.
     */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 10);

    protected final EnergyComponent energy;

    protected AbstractBatteryBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state,
            long capacity, long maxInsert, long maxExtract) {
        super(type, pos, state);
        this.energy = new EnergyComponent(capacity, maxInsert, maxExtract, this::setChanged);
    }

    // ==================== Server Tick ====================

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractBatteryBlockEntity entity) {
        if (level.isClientSide()) return;
        entity.pushEnergyToMachines(level, pos);
        entity.updateChargeBlockState(level, pos, state);
    }

    /** Update the {@link #CHARGE} block state property when the discrete charge level changes. */
    private void updateChargeBlockState(Level level, BlockPos pos, BlockState state) {
        int chargeLevel = getChargeLevel();
        if (state.hasProperty(CHARGE) && state.getValue(CHARGE) != chargeLevel) {
            level.setBlock(pos, state.setValue(CHARGE, chargeLevel), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Push energy into adjacent blocks that have energy storage (skipping pipe blocks), using the
     * loader-agnostic {@link EnergyPushService}. Pipe neighbors are intentionally skipped — pipes are
     * not energy receivers (power enters a logistics network through a Power Junction).
     */
    private void pushEnergyToMachines(Level level, BlockPos pos) {
        if (energy.getAmount() <= 0) return;
        EnergyPushService pushService = EnergyPushService.get();
        if (pushService == null) return;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (level.getBlockEntity(neighborPos) instanceof DirectEnergyReceiver) continue;
            long maxSend = Math.min(MAX_OUTPUT_PER_SIDE, energy.getAmount());
            if (maxSend <= 0) break;
            long sent = pushService.push(level, neighborPos, dir.getOpposite(), energy, maxSend);
            if (sent > 0) {
                energy.consume(sent);
                setChanged();
            }
        }
    }

    // ==================== Item Drop Components ====================

    /**
     * Exposes the stored energy as {@code minecraft:block_entity_data} so that the
     * {@code copy_components} loot function can copy it onto the dropped item, and so
     * that {@link net.minecraft.world.item.BlockItem} can restore it when the item is placed.
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        CompoundTag logisticsData = new CompoundTag();
        energy.writeNbt(logisticsData, "Energy");
        CompoundTag tag = new CompoundTag();
        // block_entity_data must carry the block-entity id, or encoding the stack on save throws
        // "Missing id for entity". TypedEntityData supplies this automatically on newer versions.
        tag.putString("id", BlockEntityType.getKey(getType()).toString());
        tag.put("LogisticsData", logisticsData);
        builder.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
    }

    // ==================== HasEnergyStorage ====================

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return energy;
    }

    // ==================== AcceptsLowTierEnergy ====================

    @Override
    public boolean acceptsLowTierEnergyFrom(Direction from) {
        return true;
    }

    // ==================== NBT ====================

    @Override
    protected void saveLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveLogisticsData(nbt, registries);
        energy.writeNbt(nbt, "Energy");
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);
        energy.readNbt(nbt, "Energy");
    }

    // ==================== Info ====================

    public long getEnergyStored() {
        return energy.getAmount();
    }

    public long getEnergyCapacity() {
        return energy.getCapacity();
    }

    /** Discrete charge level from 0 (empty) to 10 (full), used to select the rendered overlay. */
    public int getChargeLevel() {
        long cap = energy.getCapacity();
        float charge = cap > 0 ? (float) energy.getAmount() / cap : 0f;
        return charge <= 0f ? 0 : Math.max(1, Math.round(charge * 10));
    }
}
