package com.logistics.core.lib.power;

import com.logistics.core.lib.power.component.ConstantFillComponent;
import com.logistics.core.lib.power.component.EngineBurnComponent;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.lib.power.component.EngineHeatComponent;
import com.logistics.core.lib.power.component.EnginePistonCycleComponent;
import com.logistics.core.lib.power.component.IntervalGenerationComponent;
import com.logistics.core.lib.power.fuel.CoolantSource;
import com.logistics.core.lib.power.fuel.FuelSource;
import com.logistics.core.lib.power.gen.Generation;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineComponentContainer;
import com.logistics.core.machine.MachineContext;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.Direction;

/**
 * Fluent assembler for an {@link EngineEntity}'s components, the source-side counterpart to
 * {@link MachineBuilder}. Wraps a {@code MachineBuilder} internally so engines can reuse
 * {@code items(...)}/{@code fluids(...)} (for fuel and coolant tanks), and adds the engine-specific
 * builders.
 *
 * <p>Registration order is load-bearing — build in the order energy → (items/fluids) → heat →
 * production → cycle so heat can decay before production tops the buffer up and the cycle pushes
 * last.
 */
public final class EngineBuilder {

    private final MachineComponentContainer components;
    private final Runnable onChanged;
    private final MachineBuilder machine;

    public EngineBuilder(MachineComponentContainer components, Runnable onChanged) {
        this.components = components;
        this.onChanged = onChanged;
        this.machine = new MachineBuilder(components, onChanged);
    }

    public EnergyOutputBuilder energyOutput(String id) {
        return new EnergyOutputBuilder(id);
    }

    public HeatBuilder heat(String id) {
        return new HeatBuilder(id);
    }

    public PistonCycleBuilder pistonCycle(String id) {
        return new PistonCycleBuilder(id);
    }

    public BurnBuilder burn(String id) {
        return new BurnBuilder(id);
    }

    public IntervalGenBuilder intervalGen(String id) {
        return new IntervalGenBuilder(id);
    }

    public ConstantFillBuilder constantFill(String id) {
        return new ConstantFillBuilder(id);
    }

    /** Reuse the machine item-store builder (e.g. a solid-fuel slot). */
    public MachineBuilder.ItemsBuilder items(String id) {
        return machine.items(id);
    }

    /** Reuse the machine fluid-tank builder (e.g. a liquid-fuel or coolant tank). */
    public MachineBuilder.FluidBuilder fluids(String id) {
        return machine.fluids(id);
    }

    /** Escape hatch for a component with no fluent builder. */
    public <T extends MachineComponent> T add(T component) {
        return components.add(component);
    }

    /** Builds an {@link EngineEnergyOutputComponent}. */
    public final class EnergyOutputBuilder {
        private final String id;
        private LongSupplier capacity = () -> 0L;

        private EnergyOutputBuilder(String id) {
            this.id = id;
        }

        public EnergyOutputBuilder capacity(LongSupplier capacity) {
            this.capacity = capacity;
            return this;
        }

        public EngineEnergyOutputComponent build() {
            return components.add(new EngineEnergyOutputComponent(id, capacity, onChanged));
        }
    }

    /** Builds an {@link EngineHeatComponent}. */
    public final class HeatBuilder {
        private final String id;
        private EngineEnergyOutputComponent energy;
        private double floor = 20;
        private double max = 250;
        private boolean canOverheat = true;
        private long decayRate = 10;
        private HeatStage stageOverride;
        private BooleanSupplier running = () -> false;
        private BooleanSupplier compression = () -> false;

        private HeatBuilder(String id) {
            this.id = id;
        }

        public HeatBuilder energy(EngineEnergyOutputComponent energy) {
            this.energy = energy;
            return this;
        }

        public HeatBuilder floor(double floor) {
            this.floor = floor;
            return this;
        }

        public HeatBuilder max(double max) {
            this.max = max;
            return this;
        }

        public HeatBuilder canOverheat(boolean canOverheat) {
            this.canOverheat = canOverheat;
            return this;
        }

        public HeatBuilder decayRate(long decayRate) {
            this.decayRate = decayRate;
            return this;
        }

        /** Pins the reported stage (e.g. the Creative Engine stays COLD). */
        public HeatBuilder stageOverride(HeatStage stage) {
            this.stageOverride = stage;
            return this;
        }

        public HeatBuilder running(BooleanSupplier running) {
            this.running = running;
            return this;
        }

        public HeatBuilder compression(BooleanSupplier compression) {
            this.compression = compression;
            return this;
        }

        public EngineHeatComponent build() {
            java.util.Objects.requireNonNull(energy, "heat(" + id + ") requires an energy component");
            return components.add(new EngineHeatComponent(
                    id, energy, floor, max, canOverheat, decayRate, stageOverride, running, compression, onChanged));
        }
    }

    /** Builds an {@link EnginePistonCycleComponent}. */
    public final class PistonCycleBuilder {
        private final String id;
        private EngineEnergyOutputComponent energy;
        private BooleanSupplier overheated = () -> false;
        private BooleanSupplier powered = () -> false;
        private LongSupplier outputPower = () -> 0L;
        private Supplier<Direction> outputFace = () -> Direction.NORTH;
        private DoubleSupplier pistonSpeed = () -> 0.0;
        private boolean sendsEnergyContinuously;

        private PistonCycleBuilder(String id) {
            this.id = id;
        }

        public PistonCycleBuilder energy(EngineEnergyOutputComponent energy) {
            this.energy = energy;
            return this;
        }

        /** Whether the engine is overheated (skips the cycle); typically the heat component's {@code isOverheated}. */
        public PistonCycleBuilder overheated(BooleanSupplier overheated) {
            this.overheated = overheated;
            return this;
        }

        public PistonCycleBuilder powered(BooleanSupplier powered) {
            this.powered = powered;
            return this;
        }

        public PistonCycleBuilder outputPower(LongSupplier outputPower) {
            this.outputPower = outputPower;
            return this;
        }

        public PistonCycleBuilder outputFace(Supplier<Direction> outputFace) {
            this.outputFace = outputFace;
            return this;
        }

        public PistonCycleBuilder pistonSpeed(DoubleSupplier pistonSpeed) {
            this.pistonSpeed = pistonSpeed;
            return this;
        }

        public PistonCycleBuilder sendsEnergyContinuously(boolean sendsEnergyContinuously) {
            this.sendsEnergyContinuously = sendsEnergyContinuously;
            return this;
        }

        public EnginePistonCycleComponent build() {
            java.util.Objects.requireNonNull(energy, "pistonCycle(" + id + ") requires an energy component");
            return components.add(new EnginePistonCycleComponent(
                    id, energy, overheated, powered, outputPower, outputFace, pistonSpeed, sendsEnergyContinuously,
                    onChanged));
        }
    }

    /** Builds an {@link EngineBurnComponent}. */
    public final class BurnBuilder {
        private final String id;
        private EngineEnergyOutputComponent energy;
        private EngineHeatComponent heat;
        private BooleanSupplier powered = () -> false;
        private FuelSource fuel;
        private Generation generation;
        private CoolantSource coolant = CoolantSource.NONE;
        private EngineComponent.LitController lit = (ctx, on) -> {};

        private BurnBuilder(String id) {
            this.id = id;
        }

        public BurnBuilder energy(EngineEnergyOutputComponent energy) {
            this.energy = energy;
            return this;
        }

        public BurnBuilder heat(EngineHeatComponent heat) {
            this.heat = heat;
            return this;
        }

        public BurnBuilder powered(BooleanSupplier powered) {
            this.powered = powered;
            return this;
        }

        public BurnBuilder fuel(FuelSource fuel) {
            this.fuel = fuel;
            return this;
        }

        public BurnBuilder generation(Generation generation) {
            this.generation = generation;
            return this;
        }

        public BurnBuilder coolant(CoolantSource coolant) {
            this.coolant = coolant;
            return this;
        }

        public BurnBuilder lit(EngineComponent.LitController lit) {
            this.lit = lit;
            return this;
        }

        public EngineBurnComponent build() {
            java.util.Objects.requireNonNull(energy, "burn(" + id + ") requires an energy component");
            java.util.Objects.requireNonNull(heat, "burn(" + id + ") requires a heat component");
            java.util.Objects.requireNonNull(fuel, "burn(" + id + ") requires a fuel source");
            java.util.Objects.requireNonNull(generation, "burn(" + id + ") requires a generation strategy");
            return components.add(
                    new EngineBurnComponent(id, energy, heat, powered, fuel, generation, coolant, lit, onChanged));
        }
    }

    /** Builds an {@link IntervalGenerationComponent}. */
    public final class IntervalGenBuilder {
        private final String id;
        private EngineEnergyOutputComponent energy;
        private EngineHeatComponent heat;
        private BooleanSupplier powered = () -> false;
        private LongSupplier output = () -> 0L;
        private LongSupplier interval = () -> 1L;
        private Predicate<MachineContext> runningGate = ctx -> true;

        private IntervalGenBuilder(String id) {
            this.id = id;
        }

        public IntervalGenBuilder energy(EngineEnergyOutputComponent energy) {
            this.energy = energy;
            return this;
        }

        public IntervalGenBuilder heat(EngineHeatComponent heat) {
            this.heat = heat;
            return this;
        }

        public IntervalGenBuilder powered(BooleanSupplier powered) {
            this.powered = powered;
            return this;
        }

        public IntervalGenBuilder output(LongSupplier output) {
            this.output = output;
            return this;
        }

        public IntervalGenBuilder interval(LongSupplier interval) {
            this.interval = interval;
            return this;
        }

        /** The extra running condition beyond powered/!overheated (e.g. target accepts low-tier energy). */
        public IntervalGenBuilder runningGate(Predicate<MachineContext> runningGate) {
            this.runningGate = runningGate;
            return this;
        }

        public IntervalGenerationComponent build() {
            java.util.Objects.requireNonNull(energy, "intervalGen(" + id + ") requires an energy component");
            java.util.Objects.requireNonNull(heat, "intervalGen(" + id + ") requires a heat component");
            return components.add(
                    new IntervalGenerationComponent(id, energy, heat, powered, output, interval, runningGate));
        }
    }

    /** Builds a {@link ConstantFillComponent}. */
    public final class ConstantFillBuilder {
        private final String id;
        private EngineEnergyOutputComponent energy;
        private BooleanSupplier powered = () -> false;
        private IntSupplier levelIndexGetter;
        private IntConsumer levelIndexSetter;

        private ConstantFillBuilder(String id) {
            this.id = id;
        }

        public ConstantFillBuilder energy(EngineEnergyOutputComponent energy) {
            this.energy = energy;
            return this;
        }

        public ConstantFillBuilder powered(BooleanSupplier powered) {
            this.powered = powered;
            return this;
        }

        /** Persist an output-level index (the sneak-wrench-cycled rate). */
        public ConstantFillBuilder levelIndex(IntSupplier getter, IntConsumer setter) {
            this.levelIndexGetter = getter;
            this.levelIndexSetter = setter;
            return this;
        }

        public ConstantFillComponent build() {
            java.util.Objects.requireNonNull(energy, "constantFill(" + id + ") requires an energy component");
            return components.add(
                    new ConstantFillComponent(id, energy, powered, levelIndexGetter, levelIndexSetter));
        }
    }
}
