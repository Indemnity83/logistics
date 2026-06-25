package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.EnergyDemandProvider;
import com.logistics.core.machine.upgrade.MachineModifiers;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LaserQuarryBlockEntity extends BaseBlockEntity
        implements PipeConnection, HasEnergyStorage, EnergyDemandProvider, QuarryContext {

    // Energy storage — kept on the BE because it's part of the capability surface.
    private final EnergyComponent energy = new EnergyComponent(
            LogisticsConfig.get().quarry.energyCapacity(),
            LogisticsConfig.get().quarry.maxEnergyInput(),
            0,
            this::setChanged);
    private long energyReceivedLastTick = 0;
    private long energyReceivedThisTick = 0;
    private final IEnergyStorage trackingStorage = new IEnergyStorage() {
        @Override
        public long insert(long maxAmount, boolean simulate) {
            long inserted = energy.insert(maxAmount, simulate);
            if (!simulate && inserted > 0) energyReceivedThisTick += inserted;
            return inserted;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return energy.extract(maxAmount, simulate);
        }

        @Override
        public long getAmount() {
            return energy.getAmount();
        }

        @Override
        public long getCapacity() {
            return energy.getCapacity();
        }

        @Override
        public boolean canInsert() {
            return energy.canInsert();
        }

        @Override
        public boolean canExtract() {
            return energy.canExtract();
        }
    };
    private long lastSyncedEnergy = 0;

    // Quarry-specific energy behavior (action costs + spend); modifiers are identity until upgrades land.
    private final QuarryEnergyPolicy energyPolicy =
            new QuarryEnergyPolicy(energy, MachineModifiers.identity(), this::setChanged);

    // Extracted collaborators — see ArmController/QuarryPhaseRunner/QuarryBounds.
    private final QuarryBounds bounds = new QuarryBounds();
    private final ArmController armController = new ArmController();
    private final QuarryPhaseRunner phaseRunner = new QuarryPhaseRunner();

    // Block breaking animation entity ID (use position hash for uniqueness).
    private final int breakingEntityId;

    public LaserQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, pos, state);
        this.breakingEntityId = pos.hashCode();
    }

    // ==================== HasEnergyStorage ====================

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return trackingStorage;
    }

    // ==================== Tick ====================

    public static void tick(Level world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        if (world.isClientSide()) {
            return;
        }

        ActiveQuarryRegistry.register((ServerLevel) world, pos);

        entity.energyReceivedLastTick = entity.energyReceivedThisTick;
        entity.energyReceivedThisTick = 0;

        boolean wasConsumedEnergy = entity.energyPolicy.consumedThisTick();
        entity.energyPolicy.resetConsumedThisTick();

        if (entity.phaseRunner.isFinished()) {
            entity.updateActiveState(world, pos, false);
            if (wasConsumedEnergy) {
                entity.syncToClients();
            }
            return;
        }

        entity.phaseRunner.tick(entity);

        // Idle power consumption: 1 RF every 4 ticks (5 RF/second) to slowly drain buffer.
        if (world.getGameTime() % 4 == 0 && entity.energy.getAmount() > 0) {
            entity.energy.setAmount(Math.max(0, entity.energy.getAmount() - 1));
            entity.setChanged();
        }

        entity.updateActiveState(world, pos, entity.energy.getAmount() > 0);

        boolean needsSync = entity.energy.getAmount() != entity.lastSyncedEnergy
                || entity.energyPolicy.consumedThisTick() != wasConsumedEnergy;

        if (needsSync) {
            entity.lastSyncedEnergy = entity.energy.getAmount();
            entity.armController.setSyncedSpeed(entity.effectiveArmSpeed());
            entity.syncToClients();
        }
    }

    /**
     * Sync arm state to clients. Called on arm state transitions.
     *
     * <p>Does not call {@code setChanged()} — chunk dirty is managed separately by
     * {@link #tick}, which marks dirty when energy is consumed or mining advances.
     */
    void syncToClients() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    /** Drives {@link LaserQuarryBlock#ACTIVE} so the front lights up while powered and mining. */
    private void updateActiveState(Level level, BlockPos pos, boolean active) {
        BlockState state = getBlockState();
        if (state.getValue(LaserQuarryBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(LaserQuarryBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    // ==================== QuarryContext (collaborator boundary) ====================

    @Override
    public ServerLevel level() {
        return (ServerLevel) this.level;
    }

    @Override
    public BlockPos pos() {
        return worldPosition;
    }

    @Override
    public BlockState quarryState() {
        return getBlockState();
    }

    @Override
    public QuarryBounds bounds() {
        return bounds;
    }

    @Override
    public ArmController arm() {
        return armController;
    }

    @Override
    public int breakingEntityId() {
        return breakingEntityId;
    }

    @Override
    public int levelMinY() {
        return level.getMinY();
    }

    @Override
    public long energyStored() {
        return energyPolicy.stored();
    }

    @Override
    public boolean hasEnergy(long rf) {
        return energyPolicy.has(rf);
    }

    @Override
    public void consumeEnergy(long rf) {
        energyPolicy.consume(rf);
    }

    @Override
    public long moveCost() {
        return energyPolicy.moveCost();
    }

    @Override
    public float effectiveArmSpeed() {
        return energyPolicy.effectiveArmSpeed(level, worldPosition);
    }

    @Override
    public long frameBuildCost() {
        return energyPolicy.frameBuildCost();
    }

    @Override
    public float breakCost(float hardness) {
        return energyPolicy.breakCost(hardness);
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public void sync() {
        syncToClients();
    }

    // ==================== Marker / bounds API ====================

    public void setCustomBounds(int minX, int minZ, int maxX, int maxZ) {
        bounds.setCustom(minX, minZ, maxX, maxZ);
        phaseRunner.onCustomBoundsSet();
        setChanged();
    }

    // ==================== Energy diagnostics ====================

    public double getEnergyLevel() {
        return (double) energy.getAmount() / LogisticsConfig.get().quarry.energyCapacity();
    }

    @Override
    public long networkDemandPerTick() {
        long storageRoom = Math.max(0, LogisticsConfig.get().quarry.energyCapacity() - energy.getAmount());
        long remainingInput = Math.max(0, LogisticsConfig.get().quarry.maxEnergyInput() - energyReceivedThisTick);
        return Math.min(remainingInput, storageRoom);
    }

    public long getEnergyReceivedLastTick() {
        return energyReceivedLastTick;
    }

    // ==================== NBT ====================

    @Override
    protected void saveLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveLogisticsData(nbt, registries);

        energy.writeNbt(nbt, "Energy");
        phaseRunner.save(nbt);
        armController.save(nbt);
        bounds.save(nbt);
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);

        energy.readNbt(nbt, "Energy");

        phaseRunner.load(nbt);
        armController.load(nbt);
        bounds.load(nbt);
    }

    // ==================== Lifecycle ====================

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);

        if (level != null && level.isClientSide()) {
            ClientRenderCacheHooks.clearQuarryInterpolationCache(pos);
        }

        if (level != null && !level.isClientSide()) {
            ActiveQuarryRegistry.unregister((ServerLevel) level, pos);
            BlockPos currentTarget = phaseRunner.getCurrentTarget();
            if (currentTarget != null) {
                ((ServerLevel) level).destroyBlockProgress(breakingEntityId, currentTarget, -1);
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

    // ==================== Public getters (renderer / frame block) ====================

    public QuarryPhase getCurrentPhase() {
        return phaseRunner.getPhase();
    }

    public float getArmX() {
        return armController.getX();
    }

    public float getArmY() {
        return armController.getY();
    }

    public float getArmZ() {
        return armController.getZ();
    }

    public QuarryArmState getArmState() {
        return armController.getState();
    }

    public float getSyncedArmSpeed() {
        return armController.getSyncedSpeed();
    }

    public boolean isArmInitialized() {
        return armController.isInitialized();
    }

    public boolean isFinished() {
        return phaseRunner.isFinished();
    }

    public boolean consumedEnergyThisTick() {
        return energyPolicy.consumedThisTick();
    }

    public boolean hasCustomBounds() {
        return bounds.isCustom();
    }

    public int getCustomMinX() {
        return bounds.getMinX();
    }

    public int getCustomMinZ() {
        return bounds.getMinZ();
    }

    public int getCustomMaxX() {
        return bounds.getMaxX();
    }

    public int getCustomMaxZ() {
        return bounds.getMaxZ();
    }

    /** True if the quarry has been placed but hasn't started any clearing yet. */
    public boolean isFreshlyPlaced() {
        return phaseRunner.isFreshlyPlaced();
    }

    public static List<BlockPos> getActiveQuarries(ServerLevel world) {
        return ActiveQuarryRegistry.getAll(world);
    }

    public static void clearActiveQuarries(ServerLevel world) {
        ActiveQuarryRegistry.clear(world);
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
