package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.block.MachineBlockEntity;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.compat.NbtCompat;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class LaserQuarryBlockEntity extends MachineBlockEntity
        implements PipeConnection {
    static final long FRAME_BUILD_COST = 240L;
    private static final long MOVE_COST_BUFFER_DIVISOR = 10L;

    /** Quarry operation phases. */
    public enum Phase {
        CLEARING,
        BUILDING_FRAME,
        MINING
    }

    /** Arm movement sub-states during mining phase. */
    public enum ArmState {
        MOVING,
        SETTLING,
        BREAKING
    }

    private long energyReceivedLastTick = 0;
    private long lastSyncedEnergy = 0;
    private boolean consumedEnergyThisTick = false;

    // Extracted collaborators — see ArmController/QuarryPhaseRunner/QuarryBounds.
    private final QuarryBounds bounds = new QuarryBounds();
    private final ArmController armController = new ArmController();
    private final QuarryPhaseRunner phaseRunner = new QuarryPhaseRunner();

    // Block breaking animation entity ID (use position hash for uniqueness).
    private final int breakingEntityId;

    public LaserQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, pos, state,
                () -> LogisticsConfig.get().quarry.energyCapacity(),
                () -> LogisticsConfig.get().quarry.maxEnergyInput());
        this.breakingEntityId = pos.hashCode();
    }

    // ==================== Tick ====================

    public static void tick(Level world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        if (world.isClientSide()) {
            return;
        }

        ActiveQuarryRegistry.register((ServerLevel) world, pos);

        entity.energyReceivedLastTick = entity.energyReceivedThisTick();
        entity.resetEnergyReceived();

        boolean wasConsumedEnergy = entity.consumedEnergyThisTick;
        entity.consumedEnergyThisTick = false;

        if (entity.phaseRunner.isFinished()) {
            entity.updateActiveState(world, pos, false);
            if (wasConsumedEnergy) {
                entity.syncToClients();
            }
            return;
        }

        ServerLevel serverWorld = (ServerLevel) world;

        entity.phaseRunner.tick(entity, serverWorld, pos, state);

        // Idle power consumption: 1 RF every 4 ticks (5 RF/second) to slowly drain buffer.
        if (world.getGameTime() % 4 == 0 && entity.energy.getAmount() > 0) {
            entity.energy.setAmount(Math.max(0, entity.energy.getAmount() - 1));
            entity.setChanged();
        }

        entity.updateActiveState(world, pos, entity.energy.getAmount() > 0);

        boolean needsSync = entity.energy.getAmount() != entity.lastSyncedEnergy
                || entity.consumedEnergyThisTick != wasConsumedEnergy;

        if (needsSync) {
            entity.lastSyncedEnergy = entity.energy.getAmount();
            entity.armController.setSyncedSpeed(entity.getEffectiveArmSpeed());
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

    // ==================== Package-private accessors for collaborators ====================

    QuarryBounds getBounds() {
        return bounds;
    }

    ArmController getArmController() {
        return armController;
    }

    int getBreakingEntityId() {
        return breakingEntityId;
    }

    int getLevelMinY() {
        return level.getMinY();
    }

    long getEnergyAmount() {
        return energy.getAmount();
    }

    boolean hasEnergy(long amount) {
        return energy.getAmount() >= amount;
    }

    /**
     * Consumes energy from the buffer if available. Sets the consumed-this-tick flag
     * so the LED ring renders correctly.
     */
    void consumeEnergy(long amount) {
        if (energy.getAmount() >= amount) {
            energy.consume(amount);
            consumedEnergyThisTick = true;
            setChanged();
        }
    }

    /** Move cost per tick — scales with current buffer to drain excess energy faster. */
    long getMoveCost() {
        return ArmController.moveCost(energy.getAmount(), MOVE_COST_BUFFER_DIVISOR);
    }

    /** Effective arm speed in blocks/tick, including the rain penalty when applicable. */
    float getEffectiveArmSpeed() {
        return ArmController.effectiveSpeed(energy.getAmount(), MOVE_COST_BUFFER_DIVISOR, level, worldPosition);
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

    public long getEnergyReceivedLastTick() {
        return energyReceivedLastTick;
    }

    // ==================== NBT ====================

    @Override
    protected void saveLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveLogisticsData(nbt, registries);

        phaseRunner.save(nbt);
        armController.save(nbt);
        bounds.save(nbt);
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);

        // Legacy fallback — pre-"Energy" saves stored the buffer under "StoredEnergy".
        if (!nbt.contains("Energy")) {
            energy.setAmount(NbtCompat.getLong(nbt, "StoredEnergy", 0L));
        }

        phaseRunner.load(nbt);
        armController.load(nbt);
        bounds.load(nbt);
    }

    @Override
    protected void loadLegacyData(net.minecraft.world.level.storage.ValueInput view) {
        super.loadLegacyData(view);

        bounds.clear();

        view.read("Energy", CompoundTag.CODEC)
                .ifPresent(energyState -> energy.setAmount(NbtCompat.getLong(energyState, "Amount", 0L)));

        view.read("MiningState", CompoundTag.CODEC).ifPresent(miningState -> {
            phaseRunner.loadLegacy(miningState);
            armController.loadLegacy(miningState);
        });

        view.read("CustomBounds", CompoundTag.CODEC)
                .ifPresent(customBoundsNbt -> bounds.loadLegacy(
                        NbtCompat.getInt(customBoundsNbt, "MinX", 0),
                        NbtCompat.getInt(customBoundsNbt, "MinZ", 0),
                        NbtCompat.getInt(customBoundsNbt, "MaxX", 0),
                        NbtCompat.getInt(customBoundsNbt, "MaxZ", 0)));
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

    public Phase getCurrentPhase() {
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

    public ArmState getArmState() {
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
        return consumedEnergyThisTick;
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
