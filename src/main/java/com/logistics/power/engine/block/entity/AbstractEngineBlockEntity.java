package com.logistics.power.engine.block.entity;

import com.logistics.api.EnergyStorage;
import com.logistics.core.lib.support.ProbeResult;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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
 */
public abstract class AbstractEngineBlockEntity extends BlockEntity implements EnergyStorage {

    // ==================== Heat Stage Enum ====================

    /** Represents the heat stages of an engine. */
    public enum HeatStage implements StringIdentifiable {
        COLD,
        COOL,
        WARM,
        HOT,
        OVERHEAT;

        private static final HeatStage[] VALUES = values();

        @Override
        public String asString() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static HeatStage fromOrdinal(int ordinal) {
            return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : COLD;
        }
    }

    /** Block state property for engine heat stage. */
    public static final EnumProperty<HeatStage> STAGE = EnumProperty.of("stage", HeatStage.class);

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
    protected long energy = 0;
    protected float progress = 0;
    protected CyclePhase cyclePhase = CyclePhase.IDLE;
    protected HeatStage heatStage = HeatStage.COLD;
    private boolean wasRunning = false;

    protected AbstractEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
     *   <li>isOverheated check - handle overheat state (early exit)</li>
     *   <li>isShutdown check - apply decay when not running, trigger onShutdown on transition</li>
     *   <li>syncStage - update visual stage based on heat</li>
     *   <li>produceEnergy - generate energy from fuel/redstone</li>
     *   <li>advanceCycle - move the piston cycle forward</li>
     * </ol>
     */
    public void tickEngine(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) {
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
        markDirty();
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
        if (energy >= decay) {
            energy -= decay;
        } else {
            energy = 0;
        }
    }

    /** Handles overheat state: drains energy and emits smoke particles. */
    private void tickOverheat() {
        energy = Math.max(energy - 50, 0);

        if (world instanceof ServerWorld serverWorld && world.random.nextInt(4) == 0) {
            double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0, 0.05, 0, 0.01);
        }
    }

    /** Syncs engine stage to block state if changed. */
    private void syncStage() {
        HeatStage newStage = computeStage();
        if (newStage != heatStage) {
            heatStage = newStage;
            syncStageToBlock();
        }
    }

    private void syncStageToBlock() {
        if (world == null) return;
        BlockState newState = getCachedState().with(STAGE, heatStage);
        world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS);
    }

    // ==================== Heat System ====================

    /** Computes temperature from energy level. Hotter when buffer is fuller. */
    protected void computeTemperature() {
        temperature = (getMaxTemperature() - getTemperatureFloor()) * getEnergyLevel() + getTemperatureFloor();
    }

    /** Computes the engine stage based on current heat level. */
    protected HeatStage computeStage() {
        double level = getHeatLevel();

        if (level < 0.25) return HeatStage.COLD;
        if (level < 0.50) return HeatStage.COOL;
        if (level < 0.75) return HeatStage.WARM;
        if (level < 1.0) return HeatStage.HOT;

        return HeatStage.OVERHEAT;
    }

    /** Compute the piston speed based on current heat level. */
    public float getPistonSpeed() {
        double level = getHeatLevel();

        if (level < 0.25) return 0.01f;
        if (level < 0.50) return 0.02f;
        if (level < 0.75) return 0.04f;
        if (level < 1.0) return 0.08f;

        return 0.0f;
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
        if (cyclePhase == CyclePhase.IDLE && isRedstonePowered()) {
            cyclePhase = CyclePhase.EXPANSION;
            return;
        }

        if (cyclePhase == CyclePhase.IDLE) {
            return; // Don't advance progress while idle
        }

        progress += getPistonSpeed();

        boolean justTransitioned = cyclePhase == CyclePhase.EXPANSION && progress > 0.5f;
        if (justTransitioned) {
            cyclePhase = CyclePhase.COMPRESSION;
        }

        if (sendsEnergyContinuously() || justTransitioned) {
            sendEnergy();
        }

        if (progress >= 1.0f) {
            progress = 0;
            cyclePhase = CyclePhase.IDLE;
        }
    }

    /** Sends energy to the block this engine is facing via Team Reborn Energy API. */
    protected void sendEnergy() {
        if (world == null || energy <= 0 || !isRedstonePowered()) return;

        Direction outputDir = getOutputDirection();
        BlockPos targetPos = pos.offset(outputDir);

        team.reborn.energy.api.EnergyStorage target =
                team.reborn.energy.api.EnergyStorage.SIDED.find(world, targetPos, outputDir.getOpposite());

        if (target != null && target.supportsInsertion()) {
            long maxSend = getOutputPower();
            long toSend = Math.min(maxSend, energy);

            if (toSend > 0) {
                try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                    long accepted = target.insert(toSend, transaction);
                    if (accepted > 0) {
                        energy -= accepted;
                        transaction.commit();
                    }
                }
            }
        }
    }

    /** Adds energy to the buffer, capped at max capacity. */
    protected void addEnergy(long amount) {
        energy += amount;
        if (energy > getEnergyBufferCapacity()) {
            energy = getEnergyBufferCapacity();
        }
    }

    // ==================== Public API ====================

    /** Checks if the engine is currently in the overheat state. */
    public boolean isOverheated() {
        return heatStage == HeatStage.OVERHEAT;
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
        return energy;
    }

    public float getProgress() {
        return progress;
    }

    public HeatStage getHeatStage() {
        return heatStage;
    }

    /** Gets the heat level as a ratio from 0.0 to 1.0. */
    public double getHeatLevel() {
        double maxTemp = getMaxTemperature();
        if (maxTemp <= 0) {
            return 0.0;
        }
        return temperature / maxTemp;
    }

    /** Gets the energy level as a ratio from 0.0 to 1.0. */
    public double getEnergyLevel() {
        long capacity = getEnergyBufferCapacity();
        if (capacity <= 0) {
            return 0.0;
        }
        return energy / (double) capacity;
    }

    public long getCurrentOutputPower() {
        return getOutputPower();
    }

    // ==================== Probe Support ====================

    /**
     * Creates probe result with engine diagnostic information.
     * Override in subclasses to add engine-specific entries.
     *
     * @return the probe result
     */
    public ProbeResult getProbeResult() {
        ProbeResult.Builder builder = ProbeResult.builder("Engine Stats");
        addProbeEntries(builder);
        return builder.build();
    }

    /**
     * Adds probe entries to the builder.
     * Subclasses should call super and then add their own entries.
     */
    protected void addProbeEntries(ProbeResult.Builder builder) {
        // Stage with color coding
        HeatStage stage = getHeatStage();
        builder.entry("Stage", stage.name(), getStageColor(stage));

        // Temperature info
        double temp = getTemperature();
        double maxTemp = getMaxTemperature();
        double heatLevel = getHeatLevel();
        Formatting tempColor =
                heatLevel >= 1.0 ? Formatting.RED : heatLevel >= 0.75 ? Formatting.YELLOW : Formatting.GREEN;
        builder.entry("Temperature", String.format("%.0f\u00B0C (%.0f Max)", temp, maxTemp), tempColor);

        // Energy info (buffer)
        long storedEnergy = getEnergy();
        double energyLevel = getEnergyLevel();
        builder.entry(
                "Energy",
                String.format("%,d / %,d RF (%.1f%%)", storedEnergy, getCapacity(), energyLevel * 100),
                Formatting.AQUA);

        // Output power
        builder.entry("Output Power", String.format("%d RF/t", getCurrentOutputPower()), Formatting.LIGHT_PURPLE);

        // Running state
        builder.entry("Running", isRunning() ? "Yes" : "No", isRunning() ? Formatting.GREEN : Formatting.GRAY);

        // Overheat warning
        if (isOverheated()) {
            builder.warning("OVERHEATED!");
        }
    }

    private static Formatting getStageColor(HeatStage stage) {
        return switch (stage) {
            case COLD -> Formatting.BLUE;
            case COOL -> Formatting.GREEN;
            case WARM -> Formatting.YELLOW;
            case HOT -> Formatting.RED;
            case OVERHEAT -> Formatting.DARK_RED;
        };
    }

    // ==================== EnergyStorage Implementation ====================

    @Override
    public long getAmount() {
        return energy;
    }

    @Override
    public long getCapacity() {
        return getEnergyBufferCapacity();
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        return 0; // Engines don't accept external energy
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        long extracted = Math.min(maxAmount, energy);
        if (!simulate && extracted > 0) {
            energy -= extracted;
            markDirty();
        }
        return extracted;
    }

    @Override
    public boolean canInsert() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        NbtCompound engineData = new NbtCompound();
        engineData.putLong("energy", energy);
        engineData.putDouble("heat", temperature);
        engineData.putFloat("progress", progress);
        engineData.putInt("cyclePhase", cyclePhase.ordinal());
        engineData.putInt("stage", heatStage.ordinal());
        view.put("Engine", NbtCompound.CODEC, engineData);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        view.read("Engine", NbtCompound.CODEC).ifPresent(engineData -> {
            energy = engineData.getLong("energy").orElse(0L);
            temperature = engineData.getDouble("heat").orElse(0.0);
            progress = engineData.getFloat("progress").orElse(0f);
            cyclePhase = CyclePhase.fromOrdinal(engineData.getInt("cyclePhase").orElse(0));
            heatStage = HeatStage.fromOrdinal(engineData.getInt("stage").orElse(0));
        });
    }

    @Nullable @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
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
    public void markRemoved() {
        super.markRemoved();
        if (onRemovedCallback != null && world != null && world.isClient()) {
            onRemovedCallback.accept(pos);
        }
    }
}
