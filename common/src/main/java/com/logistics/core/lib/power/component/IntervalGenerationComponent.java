package com.logistics.core.lib.power.component;

import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/**
 * Fuel-less generation on a fixed interval: while powered and not overheated, adds a fixed amount of
 * RF every {@code interval} game ticks. Used by the Redstone Engine. Its running state is an
 * engine-supplied gate (e.g. "the target accepts low-tier energy"). Stateless — nothing to persist.
 */
public final class IntervalGenerationComponent implements MachineComponent, EngineComponent.RunningGate {

    private final String id;
    private final EngineEnergyOutputComponent energy;
    private final EngineHeatComponent heat;
    private final BooleanSupplier powered;
    private final LongSupplier output;
    private final LongSupplier interval;
    private final Predicate<MachineContext> runningGate;

    public IntervalGenerationComponent(
            String id,
            EngineEnergyOutputComponent energy,
            EngineHeatComponent heat,
            BooleanSupplier powered,
            LongSupplier output,
            LongSupplier interval,
            Predicate<MachineContext> runningGate) {
        this.id = id;
        this.energy = energy;
        this.heat = heat;
        this.powered = powered;
        this.output = output;
        this.interval = interval;
        this.runningGate = runningGate;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        if (!powered.getAsBoolean() || heat.isOverheated() || ctx.level() == null) {
            return;
        }
        long ticks = interval.getAsLong();
        if (ticks > 0 && ctx.level().getGameTime() % ticks == 0) {
            energy.addEnergy(output.getAsLong());
        }
    }

    @Override
    public boolean isRunning(MachineContext ctx) {
        return runningGate.test(ctx);
    }
}
