package com.logistics.core.lib.power;

import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.EnergyPushService;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.compat.NbtCompat;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for all engine block entities.
 * Implements BuildCraft-style heat mechanics with a two-stroke cycle.
 *
 * <p>Heat System:
 * <ul>
 *   <li>Heat is tied to energy level (buffer fullness)</li>
 *   <li>Full buffer = hot, empty buffer = cool</li>
 *   <li>This naturally handles "blocked output = overheat"</li>
 * </ul>
 *
 * <p>Two-Stroke Cycle:
 * <ul>
 *   <li>Progress goes from 0 to 1, speed varies by heat stage</li>
 *   <li>Expansion stroke (0-0.5): energy accumulates in buffer</li>
 *   <li>Compression stroke (0.5-1): energy is pushed to output</li>
 * </ul>
 *
 * <p>Note: Implements {@link HasEnergyStorage} with custom sided storage.
 * Uses {@link EnergyComponent} for internal energy buffering with direction-based output control.
 */
public abstract class AbstractEngineBlockEntity extends BaseBlockEntity implements HasEnergyStorage {

    // ==================== Heat Stage Enum ====================

    /** Represents the heat stages of an engine. */
    public enum HeatStage implements StringRepresentable {
        COLD,
        COOL,
        WARM,
        HOT,
        OVERHEAT;

        private static final HeatStage[] VALUES = values();

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static HeatStage fromOrdinal(int ordinal) {
            return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : COLD;
        }
    }

    /** Block state property for engine heat stage. */
    public static final EnumProperty<HeatStage> STAGE = EnumProperty.create("stage", HeatStage.class);

    /** Client-side callback for cleanup when an engine is removed. Set by client bootstrap. */
    private static java.util.function.Consumer<BlockPos> onRemovedCallback;

    /** Two-stroke engine cycle phases. */
    protected enum CyclePhase {
        IDLE,
        EXPANSION,
        COMPRESSION;

        private static final CyclePhase[] VALUES = values();

        static CyclePhase fromOrdinal(int ordinal) {
            return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : IDLE;
        }
    }

    // State tracking
    protected double temperature = 0;
    protected float progress = 0;
    protected CyclePhase cyclePhase = CyclePhase.IDLE;
    protected HeatStage heatStage = HeatStage.COLD;
    private boolean wasRunning = false;

    // Energy buffer — engines never accept energy (maxInsert=0); extraction is managed via sendEnergy()
    protected final EnergyComponent energyBuffer = new EnergyComponent(
            /* capacity    */ this::getEnergyBufferCapacity,
            /* maxInsert   */ 0L,
            /* maxExtract  */ Long.MAX_VALUE,
            /* onChanged   */ this::setChanged);

    protected AbstractEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== HasEnergyStorage ====================

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        // Engines only expose energy storage on their output face
        if (side != null && !isOutputDirection(side)) {
            return null;
        }
        return energyBuffer;
    }

    // ==================== Subclass Configuration ====================
    // Abstract methods that subclasses must implement, plus overridable defaults.

    /** Gets the maximum energy buffer capacity in RF. */
    protected abstract long getEnergyBufferCapacity();

    /** Gets the power output rate in RF/t. */
    protected abstract long getOutputPower();

    /** Gets the output direction for this engine from the block state. */
    protected abstract Direction getOutputDirection();

    /** Checks if the engine has redstone power. Implementations should check block state. */
    protected abstract boolean isRedstonePowered();

    /** Gets the maximum temperature. At 100% of this value, engine enters OVERHEAT. */
    public double getMaxTemperature() {
        return 250;
    }

    /** Gets the minimum temperature (at 0% energy). */
    protected double getTemperatureFloor() {
        return 20;
    }

    /** Gets the energy decay rate in RF per tick when engine is off. */
    protected long getEnergyDecayRate() {
        return 10L;
    }

    /** Whether this engine sends energy continuously or only during compression stroke. */
    protected boolean sendsEnergyContinuously() {
        return false;
    }

    /** Whether this engine overheats, or just continues running after reaching max temperature. */
    public boolean canOverheat() {
        return true;
    }

    // ==================== Lifecycle Hooks ====================
    // Override these to customize engine behavior.

    /**
     * Called each tick to produce energy from fuel or redstone signal.
     * Override to implement engine-specific energy generation.
     */
    protected void produceEnergy() {
        // Default: no energy production
    }

    /**
     * Called once when the engine transitions from running to not running.
     * Override to perform cleanup like resetting controllers.
     */
    protected void onShutdown() {
        // Default: no action
    }

    // ==================== Main Tick ====================

    /**
     * Main tick method to be called from the block's ticker.
     *
     * <p>Orchestrates the engine update sequence:
     * <ol>
     *   <li>computeTemperature - derive heat from energy level</li>
     *   <li>isOverheated check - handle overheat state (early exit, keeps overheat sticky)</li>
     *   <li>isShutdown check - apply decay when not running, trigger onShutdown on transition</li>
     *   <li>syncStage - update visual stage based on heat</li>
     *   <li>produceEnergy - generate energy from fuel/redstone</li>
     *   <li>advanceCycle - move the piston cycle forward</li>
     * </ol>
     */
    public void tickEngine(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        computeTemperature();

        if (isOverheated()) {
            tickOverheat();
            return;
        }

        if (isShutdown()) {
            applyDecay();
        }

        syncStage();
        produceEnergy();
        advanceCycle();
        setChanged();
    }

    /**
     * Checks if the engine is currently shut down (not running).
     * Also triggers onShutdown() once when transitioning from running to stopped.
     */
    private boolean isShutdown() {
        boolean running = isRunning();
        if (wasRunning && !running) {
            onShutdown();
        }
        wasRunning = running;
        return !running;
    }

    /** Applies energy decay when engine is idle. */
    private void applyDecay() {
        long decay = getEnergyDecayRate();
        energyBuffer.setAmount(Math.max(energyBuffer.getAmount() - decay, 0));
        setChanged();
    }

    /** Handles overheat state: drains energy and emits smoke particles. */
    private void tickOverheat() {
        energyBuffer.setAmount(Math.max(energyBuffer.getAmount() - 50, 0));
        setChanged();

        if (level instanceof ServerLevel serverLevel && level.getRandom().nextInt(4) == 0) {
            double x = getBlockPos().getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.5;
            double y = getBlockPos().getY() + 1.0;
            double z = getBlockPos().getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.5;
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0, 0.05, 0, 0.01);
        }
    }

    /** Syncs engine stage to block state if changed. */
    private void syncStage() {
        HeatStage newStage = computeStage();

        if (newStage != heatStage) {
            heatStage = newStage;
            syncStageToBlock();
        }

        if (canOverheat()
                && newStage == HeatStage.HOT
                && level instanceof ServerLevel serverLevel
                && level.getRandom().nextInt(4) == 0) {
            double x = getBlockPos().getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.5;
            double y = getBlockPos().getY() + 1.0;
            double z = getBlockPos().getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.5;
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0.05, 0, 0.01);
        }
    }

    private void syncStageToBlock() {
        if (level == null) return;
        BlockState newState = getBlockState().setValue(STAGE, heatStage);
        level.setBlock(getBlockPos(), newState, Block.UPDATE_ALL);
    }

    // ==================== Heat System ====================

    /** Computes temperature from energy level. Hotter when buffer is fuller. */
    protected void computeTemperature() {
        temperature = EngineHeatModel.temperature(energyBuffer.getAmount(), getEnergyBufferCapacity(), getTemperatureFloor(), getMaxTemperature());
    }

    /** Computes the engine stage based on current heat level. */
    protected HeatStage computeStage() {
        return EngineHeatModel.stage(temperature, getMaxTemperature(), canOverheat(), cyclePhase == CyclePhase.COMPRESSION);
    }

    /** Compute the piston speed based on current heat level. */
    public float getPistonSpeed() {
        return EngineHeatModel.pistonSpeed(temperature, getMaxTemperature(), canOverheat());
    }

    // ==================== Cycle System ====================

    /**
     * Advances the two-stroke engine cycle.
     *
     * <p>The cycle has two phases:
     * <ul>
     *   <li>Expansion (0 to 0.5): Energy accumulates in the buffer</li>
     *   <li>Compression (0.5 to 1): Energy is pushed to the output</li>
     * </ul>
     *
     * <p>When idle, the engine waits for redstone power to start a new cycle.
     */
    protected void advanceCycle() {
        EngineCyclePlanner.Result result = EngineCyclePlanner.advance(
                cyclePhase, progress, isRedstonePowered(), getPistonSpeed(), sendsEnergyContinuously());
        cyclePhase = result.phase();
        progress = result.progress();

        if (result.shouldSendEnergy()) {
            sendEnergy();
        }
    }

    /**
     * Sends energy to the block this engine is facing. Extraction pipes and the Fluid Pump are
     * {@link DirectEnergyReceiver}s kept off the loader energy grid, so they are fed directly through
     * their own energy buffer; everything else goes through the loader-specific energy push service.
     */
    protected void sendEnergy() {
        if (level == null || !isRedstonePowered()) return;

        Direction outputDir = getOutputDirection();
        BlockPos targetPos = getBlockPos().relative(outputDir);
        long maxSend = Math.min(getOutputPower(), energyBuffer.getAmount());
        if (maxSend <= 0) return;

        long sent = sendEnergyTo(targetPos, outputDir.getOpposite(), maxSend);
        if (sent > 0) {
            energyBuffer.consume(Math.min(sent, energyBuffer.getAmount()));
            setChanged();
        }
    }

    private long sendEnergyTo(BlockPos targetPos, Direction fromDirection, long maxSend) {
        if (level.getBlockEntity(targetPos) instanceof DirectEnergyReceiver receiver
                && receiver instanceof HasEnergyStorage hasStorage) {
            IEnergyStorage storage = hasStorage.energyStorage(fromDirection);
            return storage == null ? 0L : storage.insert(maxSend, false);
        }
        EnergyPushService pushService = EnergyPushService.get();
        return pushService == null ? 0L : pushService.push(level, targetPos, fromDirection, energyBuffer, maxSend);
    }

    /** Adds energy to the buffer, capped at max capacity. */
    protected void addEnergy(long amount) {
        energyBuffer.setAmount(Math.min(energyBuffer.getAmount() + amount, getEnergyBufferCapacity()));
        setChanged();
    }

    // ==================== Public API ====================

    /** Checks if the engine is currently in the overheat state. */
    public boolean isOverheated() {
        return heatStage == HeatStage.OVERHEAT;
    }

    /**
     * Resets the engine from an overheated state.
     * Drains all energy and resets the heat stage to COLD.
     *
     * @return true if the engine was overheated and was reset, false otherwise
     */
    public boolean resetOverheat() {
        if (!isOverheated()) {
            return false;
        }
        energyBuffer.setAmount(0);
        temperature = getTemperatureFloor();
        heatStage = HeatStage.COLD;
        syncStageToBlock();
        setChanged();
        return true;
    }

    /** Checks if the engine is currently running (powered and not overheated). */
    public boolean isRunning() {
        return isRedstonePowered() && !isOverheated();
    }

    /** Checks if the given direction is this engine's output face. */
    public boolean isOutputDirection(Direction direction) {
        return direction == getOutputDirection();
    }

    public double getTemperature() {
        return temperature;
    }

    public long getEnergy() {
        return energyBuffer.getAmount();
    }

    public long getMaxEnergy() {
        return getEnergyBufferCapacity();
    }

    public float getProgress() {
        return progress;
    }

    // ==================== Public Getters ====================

    public HeatStage getHeatStage() {
        return heatStage;
    }

    /** Gets the heat level as a ratio from 0.0 to 1.0. */
    public double getHeatLevel() {
        return EngineHeatModel.heatLevel(temperature, getMaxTemperature());
    }

    /** Gets the energy level as a ratio from 0.0 to 1.0. */
    public double getEnergyLevel() {
        return EngineHeatModel.energyLevel(energyBuffer.getAmount(), getEnergyBufferCapacity());
    }

    public long getCurrentOutputPower() {
        return getOutputPower();
    }

    // ==================== Energy Storage Access ====================

    // ==================== NBT Serialization ====================

    @Override
    protected void saveLogisticsData(CompoundTag engineData, HolderLookup.Provider registries) {
        engineData.putLong("StoredEnergy", energyBuffer.getAmount());
        engineData.putDouble("Temperature", temperature);
        engineData.putFloat("CycleProgress", progress);
        engineData.putInt("CyclePhase", cyclePhase.ordinal());
        engineData.putInt("HeatStage", heatStage.ordinal());
    }

    @Override
    protected void loadLogisticsData(CompoundTag engineData, HolderLookup.Provider registries) {
        energyBuffer.setAmount(NbtCompat.getLong(engineData, "StoredEnergy", 0L));
        temperature = NbtCompat.getDouble(engineData, "Temperature", 0.0);
        progress = NbtCompat.getFloat(engineData, "CycleProgress", 0f);
        cyclePhase = CyclePhase.fromOrdinal(NbtCompat.getInt(engineData, "CyclePhase", 0));
        heatStage = HeatStage.fromOrdinal(NbtCompat.getInt(engineData, "HeatStage", 0));
    }

    // ==================== Lifecycle ====================

    /**
     * Sets a callback to be invoked when an engine block entity is removed.
     * Used by client-side code to clean up render caches.
     */
    public static void setOnRemovedCallback(java.util.function.Consumer<BlockPos> callback) {
        onRemovedCallback = callback;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (onRemovedCallback != null && level != null && level.isClientSide()) {
            onRemovedCallback.accept(getBlockPos());
        }
    }
}
