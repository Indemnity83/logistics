package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.behavior.ProbeResult;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.EnergyDemandProvider;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LaserQuarryBlockEntity extends BaseBlockEntity
        implements PipeConnection, HasEnergyStorage, EnergyDemandProvider {
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
        public long extract(long maxAmount, boolean simulate) { return energy.extract(maxAmount, simulate); }

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
    private boolean consumedEnergyThisTick = false;

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

        if (entity.phaseRunner.isFinished()) {
            return;
        }

        ServerLevel serverWorld = (ServerLevel) world;

        boolean wasConsumedEnergy = entity.consumedEnergyThisTick;
        entity.consumedEnergyThisTick = false;

        entity.phaseRunner.tick(entity, serverWorld, pos, state);

        // Idle power consumption: 1 RF every 4 ticks (5 RF/second) to slowly drain buffer.
        if (world.getGameTime() % 4 == 0 && entity.energy.getAmount() > 0) {
            entity.energy.setAmount(Math.max(0, entity.energy.getAmount() - 1));
            entity.setChanged();
        }

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
        return level.getMinBuildHeight();
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

    private long getCurrentBufferDraw() {
        if (phaseRunner.isFinished()) {
            return 0;
        }

        return switch (phaseRunner.getPhase()) {
            case CLEARING -> phaseRunner.getCurrentBreakTime() < 0
                    ? LogisticsConfig.get().quarry.maxEnergyInput()
                    : phaseRunner.getCurrentBreakTime() > phaseRunner.getBreakProgress()
                            ? (long) Math.ceil(phaseRunner.getCurrentBreakTime() - phaseRunner.getBreakProgress())
                            : 0;
            case BUILDING_FRAME -> FRAME_BUILD_COST;
            case MINING -> switch (armController.getState()) {
                case MOVING -> getMoveCost();
                case SETTLING -> 0;
                case BREAKING -> phaseRunner.getCurrentBreakTime() < 0
                        ? LogisticsConfig.get().quarry.maxEnergyInput()
                        : phaseRunner.getCurrentBreakTime() > phaseRunner.getBreakProgress()
                                ? (long) Math.ceil(phaseRunner.getCurrentBreakTime() - phaseRunner.getBreakProgress())
                                : 0;
            };
        };
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

    // ==================== Probe ====================

    public ProbeResult getProbeResult() {
        ProbeResult.Builder builder = ProbeResult.builder("Laser Quarry Status");

        String phaseName = switch (phaseRunner.getPhase()) {
            case CLEARING -> "Clearing";
            case BUILDING_FRAME -> "Building Frame";
            case MINING -> "Mining";
        };
        if (phaseRunner.isFinished()) {
            builder.entry("Phase", "Finished", ChatFormatting.AQUA);
        } else if (phaseRunner.getPhase() == Phase.BUILDING_FRAME) {
            builder.entry("Phase", phaseName + " (need 240 RF)", ChatFormatting.AQUA);
        } else {
            builder.entry("Phase", phaseName, ChatFormatting.AQUA);
        }

        double energyPercent = getEnergyLevel() * 100;
        ChatFormatting energyColor = energyPercent > 50
                ? ChatFormatting.GREEN
                : energyPercent > 20 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        builder.entry(
                "Energy",
                String.format(
                        "%,d / %,d RF (%.1f%%)",
                        energy.getAmount(),
                        LogisticsConfig.get().quarry.energyCapacity(),
                        energyPercent),
                energyColor);

        if (!phaseRunner.isFinished()) {
            builder.entry("Power In", String.format("%,d RF/t", energyReceivedLastTick), ChatFormatting.GREEN);
            builder.entry("Buffer Draw", String.format("%,d RF/t", getCurrentBufferDraw()), ChatFormatting.GOLD);

            if (phaseRunner.getPhase() == Phase.MINING) {
                float speed = getEffectiveArmSpeed();
                String speedText = String.format("%.2f blocks/tick", speed);
                if (level != null && level.isRainingAt(worldPosition.above())) {
                    speedText += " (rain)";
                }
                builder.entry("Arm Speed", speedText, ChatFormatting.LIGHT_PURPLE);
            }
        }

        if (energy.getAmount() == 0 && !phaseRunner.isFinished()) {
            builder.warning("No power!");
        }

        return builder.build();
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

        if (nbt.contains("Energy")) {
            energy.readNbt(nbt, "Energy");
        } else {
            energy.setAmount(NbtCompat.getLong(nbt, "StoredEnergy", 0L));
        }

        phaseRunner.load(nbt);
        armController.load(nbt);
        bounds.load(nbt);
    }

    @Override
    protected void loadLegacyData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLegacyData(nbt, registries);

        bounds.clear();

        if (nbt.contains("Energy")) {
            CompoundTag energyState = nbt.getCompound("Energy");
            energy.setAmount(NbtCompat.getLong(energyState, "Amount", 0L));
        }

        if (nbt.contains("MiningState")) {
            CompoundTag miningState = nbt.getCompound("MiningState");
            phaseRunner.loadLegacy(miningState);
            armController.loadLegacy(miningState);
        }

        if (nbt.contains("CustomBounds")) {
            CompoundTag customBoundsNbt = nbt.getCompound("CustomBounds");
            bounds.loadLegacy(
                    NbtCompat.getInt(customBoundsNbt, "MinX", 0),
                    NbtCompat.getInt(customBoundsNbt, "MinZ", 0),
                    NbtCompat.getInt(customBoundsNbt, "MaxX", 0),
                    NbtCompat.getInt(customBoundsNbt, "MaxZ", 0));
        }
    }

    // ==================== Lifecycle ====================

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (level != null && level.isClientSide()) {
            ClientRenderCacheHooks.clearQuarryInterpolationCache(worldPosition);
        }

        if (level != null && !level.isClientSide()) {
            ActiveQuarryRegistry.unregister((ServerLevel) level, worldPosition);
            BlockPos currentTarget = phaseRunner.getCurrentTarget();
            if (currentTarget != null) {
                ((ServerLevel) level).destroyBlockProgress(breakingEntityId, currentTarget, -1);
            }
        }
    }

    // ==================== Public getters (renderer / frame block / probe) ====================

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
