package com.logistics.core.lib.power.component;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.lib.power.EngineHeatModel;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Derives engine heat from buffer fullness and drives the {@code STAGE} block-state property.
 *
 * <p>Reproduces the heat half of the classic engine tick: temperature tracks how full the buffer is
 * (a blocked output naturally overheats), OVERHEAT is sticky and bleeds energy until reset, and the
 * engine decays energy while not running. Ticks before the production and cycle components so those
 * can self-gate on {@link #isOverheated()}.
 */
public final class EngineHeatComponent implements MachineComponent, EngineComponent.HeatState {

    private final String id;
    private final EngineEnergyOutputComponent energy;
    private final double floor;
    private final double max;
    private final boolean canOverheat;
    private final long decayRate;
    @Nullable
    private final HeatStage stageOverride;
    private final BooleanSupplier running;
    private final BooleanSupplier compression;
    private final Runnable onChanged;

    private double temperature;
    private HeatStage heatStage = HeatStage.COLD;

    public EngineHeatComponent(
            String id,
            EngineEnergyOutputComponent energy,
            double floor,
            double max,
            boolean canOverheat,
            long decayRate,
            @Nullable HeatStage stageOverride,
            BooleanSupplier running,
            BooleanSupplier compression,
            Runnable onChanged) {
        this.id = id;
        this.energy = energy;
        this.floor = floor;
        this.max = max;
        this.canOverheat = canOverheat;
        this.decayRate = decayRate;
        this.stageOverride = stageOverride;
        this.running = running;
        this.compression = compression;
        this.onChanged = onChanged;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        temperature = EngineHeatModel.temperature(energy.getAmount(), energy.getCapacity(), floor, max);

        if (isOverheated()) {
            // Sticky overheat: bleed energy and smoke until manually reset; nothing else runs.
            energy.setAmount(Math.max(0, energy.getAmount() - 50));
            emitSmoke(ctx, ParticleTypes.LARGE_SMOKE);
            return;
        }

        if (!running.getAsBoolean()) {
            energy.setAmount(Math.max(0, energy.getAmount() - decayRate));
        }

        HeatStage newStage = stageOverride != null
                ? stageOverride
                : EngineHeatModel.stage(temperature, max, canOverheat, compression.getAsBoolean());
        if (newStage != heatStage) {
            heatStage = newStage;
            writeStage(ctx);
            onChanged.run();
        }

        if (canOverheat && newStage == HeatStage.HOT) {
            emitSmoke(ctx, ParticleTypes.SMOKE);
        }
    }

    /** Piston speed derived from current heat (used by heat-driven engines). */
    public float getPistonSpeed() {
        return EngineHeatModel.pistonSpeed(temperature, max, canOverheat);
    }

    @Override
    public double temperature() {
        return temperature;
    }

    @Override
    public HeatStage stage() {
        return heatStage;
    }

    @Override
    public boolean isOverheated() {
        return heatStage == HeatStage.OVERHEAT;
    }

    @Override
    public boolean resetOverheat(MachineContext ctx) {
        if (!isOverheated()) {
            return false;
        }
        energy.setAmount(0);
        temperature = floor;
        heatStage = HeatStage.COLD;
        writeStage(ctx);
        onChanged.run();
        return true;
    }

    private void writeStage(MachineContext ctx) {
        BlockState state = ctx.blockState();
        if (state.hasProperty(HeatStage.STAGE)) {
            ctx.setBlockState(state.setValue(HeatStage.STAGE, heatStage), Block.UPDATE_ALL);
        }
    }

    private void emitSmoke(MachineContext ctx, ParticleOptions particle) {
        if (ctx.level() instanceof ServerLevel serverLevel && serverLevel.getRandom().nextInt(4) == 0) {
            double x = ctx.pos().getX() + 0.5 + (serverLevel.getRandom().nextDouble() - 0.5) * 0.5;
            double y = ctx.pos().getY() + 1.0;
            double z = ctx.pos().getZ() + 0.5 + (serverLevel.getRandom().nextDouble() - 0.5) * 0.5;
            serverLevel.sendParticles(particle, x, y, z, 1, 0, 0.05, 0, 0.01);
        }
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("Temperature", temperature);
        tag.putInt("HeatStage", heatStage.ordinal());
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        readState(tag);
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        readState(root);
    }

    private void readState(CompoundTag tag) {
        temperature = NbtCompat.getDouble(tag, "Temperature", 0.0);
        heatStage = HeatStage.fromOrdinal(NbtCompat.getInt(tag, "HeatStage", 0));
    }
}
