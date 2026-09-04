package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.upgrade.UpgradeComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Laser Quarry block entity: a component-hosted machine composed of an energy buffer, an upgrade
 * holder, the {@link QuarryComponent} (all mining behavior), and a chunk loader. The block entity
 * itself is mostly lifecycle + capability + facade plumbing; most public methods delegate to the
 * quarry component for the renderer / HUD / frame block / markers.
 */
public class LaserQuarryBlockEntity extends MachineEntity implements PipeConnection {

    private EnergyStorageComponent energy;
    private QuarryComponent quarry;

    public LaserQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void configure(MachineBuilder machine) {
        // Order is load-bearing: energy resets its received-counter before the quarry consumes; the
        // chunk loader ticks after the quarry so a freshly-finished quarry stops loading the same tick.
        UpgradeComponent upgrades = machine.upgrades("upgrades");
        energy = machine.energy("energy")
                .capacity(QuarryEnergy.energyCapacity())
                .maxInput(QuarryEnergy.maxEnergyInput())
                .build();
        quarry = machine.add(new QuarryComponent("quarry", getBlockPos(), energy, upgrades.modifiers()));
        machine.chunkLoader("chunks")
                .tickets(LogisticsAutomation.TICKET_TYPE.QUARRY, LogisticsAutomation.TICKET_TYPE.QUARRY_BOUNDARY)
                .area(quarry::chunkArea)
                .build();
    }

    public static void tick(Level world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        // No client-side animation to run (the renderer interpolates the arm itself).
        MachineEntity.tick(world, pos, state, entity);
    }

    @Override
    @Nullable
    public MenuProvider createMenuProvider() {
        // The quarry has no GUI; the block never opens a menu.
        return null;
    }

    // ==================== Marker / bounds API ====================

    public void setCustomBounds(int minX, int minZ, int maxX, int maxZ) {
        quarry.setCustomBounds(minX, minZ, maxX, maxZ);
        setChanged();
    }

    // ==================== Energy diagnostics ====================

    public double getEnergyLevel() {
        long capacity = QuarryEnergy.energyCapacity();
        return capacity > 0 ? (double) energy.amount() / capacity : 0.0;
    }

    public long getEnergyReceivedLastTick() {
        return energy.receivedLastTick();
    }

    // ==================== NBT (legacy fallbacks) ====================

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);

        // Ancient pre-"Energy" save: a bare root StoredEnergy long, before the component format.
        if (!nbt.contains("components") && !nbt.contains("Energy") && nbt.contains("StoredEnergy")) {
            energy.setAmount(NbtCompat.getLong(nbt, "StoredEnergy", 0L));
        }
    }

    // ==================== Lifecycle ====================

    /**
     * Real removal of the quarry block: tears down the frame and clears any break progress.
     * Driven by {@code LaserQuarryBlock#onRemove}, not {@link #setRemoved()}, which also fires
     * on chunk unload.
     */
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        if (level != null && level.isClientSide()) {
            ClientRenderCacheHooks.clearQuarryInterpolationCache(pos);
        }

        if (level != null && !level.isClientSide()) {
            quarry.removeFrame((ServerLevel) level, pos, oldState);
            BlockPos currentTarget = quarry.currentTarget();
            if (currentTarget != null) {
                ((ServerLevel) level).destroyBlockProgress(quarry.breakingEntityIdValue(), currentTarget, -1);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) {
            ClientRenderCacheHooks.clearQuarryInterpolationCache(worldPosition);
        }
    }

    // ==================== Public getters (renderer / frame block / HUD) ====================

    public QuarryPhase getCurrentPhase() {
        return quarry.phase();
    }

    public float getArmX() {
        return quarry.armX();
    }

    public float getArmY() {
        return quarry.armY();
    }

    public float getArmZ() {
        return quarry.armZ();
    }

    public QuarryArmState getArmState() {
        return quarry.armState();
    }

    public float getSyncedArmSpeed() {
        return quarry.syncedArmSpeed();
    }

    public boolean isArmInitialized() {
        return quarry.armInitialized();
    }

    public boolean isFinished() {
        return quarry.isFinished();
    }

    public boolean consumedEnergyThisTick() {
        return quarry.consumedEnergyThisTick();
    }

    public boolean hasCustomBounds() {
        return quarry.hasCustomBounds();
    }

    public int getCustomMinX() {
        return quarry.customMinX();
    }

    public int getCustomMinZ() {
        return quarry.customMinZ();
    }

    public int getCustomMaxX() {
        return quarry.customMaxX();
    }

    public int getCustomMaxZ() {
        return quarry.customMaxZ();
    }

    /** True if the quarry has been placed but hasn't started any clearing yet. */
    public boolean isFreshlyPlaced() {
        return quarry.isFreshlyPlaced();
    }

    // ==================== PipeConnection ====================

    /**
     * Quarry accepts pipe connections from above so pipes render arms to it.
     * Other directions report no connection.
     */
    @Override
    public PipeConnection.Type getConnectionType(Direction direction) {
        return direction == Direction.UP ? PipeConnection.Type.PIPE : PipeConnection.Type.NONE;
    }

    /** Quarry never accepts items from pipes — it only pushes them out. */
    @Override
    public boolean addItem(Direction from, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canAcceptFrom(Direction from, ItemStack stack) {
        return false;
    }
}
