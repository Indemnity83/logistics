package com.logistics.core.lib.power;

import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.EnergyPushService;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.IPipeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for energy buffer blocks that can supply power to adjacent pipe networks.
 *
 * <p>Concrete subclasses provide capacity/rate constants via the constructor.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Accepts energy on all sides (loader energy API + low-tier from Redstone Engines)</li>
 *   <li>Each tick: pushes energy into adjacent non-pipe blocks that accept it (machines, etc.)</li>
 *   <li>Each tick: registers this battery with any adjacent logistics pipe networks so that
 *       modules can {@link ILogisticsNetwork#consumeEnergy draw from it}</li>
 *   <li>On removal: unregisters from all tracked networks</li>
 * </ul>
 *
 * <p>Multiple batteries adjacent to the same network are all registered; the network
 * draws from them in registration order until the requested amount is satisfied.
 */
public abstract class AbstractBatteryBlockEntity extends BaseBlockEntity
        implements HasEnergyStorage, AcceptsLowTierEnergy {

    /** Max RF to push into a single adjacent machine per tick. */
    private static final long MAX_OUTPUT_PER_SIDE = 200L;

    /** How often (in ticks) to refresh pipe-network registrations. */
    private static final int NETWORK_SCAN_INTERVAL = 20;

    /**
     * Discrete charge level (0–10) exposed as a block state property so the battery's fill bar is
     * rendered by the vanilla multipart blockstate — no block entity renderer required.
     */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 10);

    protected final EnergyComponent energy;

    /** Networks this battery is currently registered with. */
    private final Set<ILogisticsNetwork> registeredNetworks = new HashSet<>();

    // Start at NETWORK_SCAN_INTERVAL - 1 so the first tick triggers an immediate scan.
    // Ensures the battery registers with adjacent networks as soon as it is placed.
    private int networkScanTick = NETWORK_SCAN_INTERVAL - 1;

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
        entity.refreshNetworkRegistrations(level, pos);
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
     * loader-agnostic {@link EnergyPushService}. Pipe neighbors are intentionally skipped — they
     * draw from the battery through the network energy path, not direct insertion.
     */
    private void pushEnergyToMachines(Level level, BlockPos pos) {
        if (energy.getAmount() <= 0) return;
        EnergyPushService pushService = EnergyPushService.get();
        if (pushService == null) return;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (level.getBlockEntity(neighborPos) instanceof IPipeAccess) continue;
            long maxSend = Math.min(MAX_OUTPUT_PER_SIDE, energy.getAmount());
            if (maxSend <= 0) break;
            long sent = pushService.push(level, neighborPos, dir.getOpposite(), energy, maxSend);
            if (sent > 0) {
                energy.consume(sent);
                setChanged();
            }
        }
    }

    /** Scan adjacent pipes to keep network registrations up to date. */
    private void refreshNetworkRegistrations(Level level, BlockPos pos) {
        networkScanTick++;
        // Scan every tick until registered with a network (so a freshly placed battery powers the
        // network as soon as it forms), then back off to the interval to keep periodic scans cheap.
        if (!registeredNetworks.isEmpty() && networkScanTick < NETWORK_SCAN_INTERVAL) return;
        networkScanTick = 0;

        Set<ILogisticsNetwork> currentNetworks = new HashSet<>();
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof IPipeAccess pipe) {
                ILogisticsNetwork net = pipe.getNetwork();
                if (net != null) currentNetworks.add(net);
            }
        }

        // Register with newly found networks
        for (ILogisticsNetwork net : currentNetworks) {
            if (registeredNetworks.add(net)) {
                net.registerEnergySource(worldPosition);
            }
        }

        // Unregister from networks no longer adjacent
        Set<ILogisticsNetwork> toRemove = new HashSet<>(registeredNetworks);
        toRemove.removeAll(currentNetworks);
        for (ILogisticsNetwork net : toRemove) {
            net.unregisterEnergySource(worldPosition);
            registeredNetworks.remove(net);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        for (ILogisticsNetwork net : registeredNetworks) {
            net.unregisterEnergySource(worldPosition);
        }
        registeredNetworks.clear();
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
        tag.put("LogisticsData", logisticsData);
        builder.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(getType(), tag));
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
