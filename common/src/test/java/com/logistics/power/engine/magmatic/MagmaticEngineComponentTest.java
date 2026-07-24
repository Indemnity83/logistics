package com.logistics.power.engine.magmatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.machine.FakeMachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Behavior of the Magmatic Engine simulation. Uses vanilla lava via an injected validity predicate (so
 * tests don't need the lava tag bound), an {@link EngineEnergyOutputComponent} buffer, and a
 * {@link FluidStoreComponent} lava tank seeded directly. Config-default profile: base 10 RF/t, 40k buffer,
 * 4,000 mB tank, 20,000-tick bucket → 2,000-tick batch, 5/10/15 RF/t, 30,000 RF admission.
 */
class MagmaticEngineComponentTest extends MinecraftTestEnvironment {

    private static final MagmaticEngineProfile PROFILE = MagmaticEngineProfile.of(10, 40_000, 4_000, 20_000);

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final Predicate<Fluid> isLava = f -> f == Fluids.LAVA;

    private EngineEnergyOutputComponent energy(long capacity, long amount) {
        EngineEnergyOutputComponent e = new EngineEnergyOutputComponent("energy", () -> capacity, () -> {});
        e.setAmount(amount);
        return e;
    }

    private FluidStoreComponent tank(@Nullable Fluid fluid, long mb) {
        FluidStoreComponent s = new FluidStoreComponent("lava", FluidUnits.mb(4000), () -> {});
        if (fluid != null && mb > 0) {
            s.tank().setContents(SimpleFluidKey.of(fluid), FluidUnits.mb(mb));
        }
        return s;
    }

    private MagmaticEngineComponent engine(EngineEnergyOutputComponent e, FluidStoreComponent lava, BooleanSupplier powered) {
        return new MagmaticEngineComponent("magma", e, lava, isLava, powered, PROFILE, () -> {});
    }

    private void seed(MagmaticEngineComponent m, int burnTicks, double temperature) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("RemainingBurnTicks", burnTicks);
        tag.putDouble("Temperature", temperature);
        m.load(tag, registries);
    }

    private void tick(MagmaticEngineComponent m) {
        m.serverTick(new FakeMachineContext());
    }

    private void tick(MagmaticEngineComponent m, int n) {
        for (int i = 0; i < n; i++) {
            tick(m);
        }
    }

    private double temp(MagmaticEngineComponent m) {
        return m.temperature() / 100.0;
    }

    private long mb(long millibuckets) {
        return FluidUnits.mb(millibuckets);
    }

    // ==================== Heat-soak ====================

    @Test
    void heatSoakRaisesTemperatureNonlinearlyAndClampsAtOne() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 2000, 0.0);
        tick(m);
        double afterOne = temp(m);
        tick(m);
        double afterTwo = temp(m);
        assertThat(afterOne).isGreaterThan(0.0);
        assertThat(afterTwo).isGreaterThan(afterOne);
        // first increment > second increment (approach slows) — nonlinear
        assertThat(afterOne - 0.0).isGreaterThan(afterTwo - afterOne);
        tick(m, 5000);
        assertThat(temp(m)).isLessThanOrEqualTo(1.0).isGreaterThan(0.95);
    }

    @Test
    void coolingLowersTemperatureNonlinearlyAndClampsAtZero() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(null, 0), () -> false);
        seed(m, 0, 0.8);
        tick(m);
        double afterOne = temp(m);
        tick(m);
        double afterTwo = temp(m);
        assertThat(afterOne).isLessThan(0.8);
        assertThat(afterTwo).isLessThan(afterOne);
        assertThat(0.8 - afterOne).isGreaterThan(afterOne - afterTwo); // cooling slows as it approaches ambient
        tick(m, 20000);
        assertThat(temp(m)).isGreaterThanOrEqualTo(0.0).isLessThan(0.05);
    }

    @Test
    void shortPausePreservesHeatLongPauseReturnsNearCold() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(null, 0), () -> false);
        seed(m, 0, 0.9);
        tick(m, 100);
        assertThat(temp(m)).isGreaterThan(0.7); // brief pause keeps most heat
        tick(m, 4000);
        assertThat(temp(m)).isLessThan(0.05); // long pause → near ambient
    }

    // ==================== Stage / hysteresis ====================

    private int tickUntilStage(MagmaticEngineComponent m, HeatStage target, int max) {
        for (int i = 0; i < max; i++) {
            if (m.stage() == target) {
                return i;
            }
            tick(m);
        }
        return -1;
    }

    @Test
    void startsCold() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 2000, 0.0);
        assertThat(m.stage()).isEqualTo(HeatStage.COLD);
    }

    @Test
    void warmsThroughStagesAtHysteresisEnterThresholds() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 2000, 0.0);
        assertThat(tickUntilStage(m, HeatStage.COOL, 5000)).isPositive();
        assertThat(temp(m)).isGreaterThanOrEqualTo(0.27); // Cold→Warm only at ≥0.27
        assertThat(tickUntilStage(m, HeatStage.WARM, 5000)).isPositive();
        assertThat(temp(m)).isGreaterThanOrEqualTo(0.77); // Warm→Hot only at ≥0.77
    }

    @Test
    void coolingHysteresisHoldsHotUntilLowExitThresholds() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(null, 0), () -> false);
        // Seed hot; stageFor(0.9) = WARM (magmatic "Hot").
        seed(m, 0, 0.9);
        assertThat(m.stage()).isEqualTo(HeatStage.WARM);
        assertThat(tickUntilStage(m, HeatStage.COOL, 5000)).isPositive();
        assertThat(temp(m)).isLessThanOrEqualTo(0.73); // Hot→Warm only at ≤0.73
        assertThat(tickUntilStage(m, HeatStage.COLD, 5000)).isPositive();
        assertThat(temp(m)).isLessThanOrEqualTo(0.23); // Warm→Cold only at ≤0.23
    }

    @Test
    void neverEntersHotOrOverheatStage() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 2000, 0.0);
        for (int i = 0; i < 3000; i++) {
            tick(m);
            assertThat(m.stage()).isIn(HeatStage.COLD, HeatStage.COOL, HeatStage.WARM);
        }
        assertThat(temp(m)).isGreaterThan(0.95); // fully soaked, still only WARM
    }

    // ==================== Burn timer ====================

    @Test
    void ignitionConsumesOneHundredMbAndSetsBatchDuration() {
        FluidStoreComponent lava = tank(Fluids.LAVA, 4000);
        MagmaticEngineComponent m = engine(energy(40_000, 0), lava, () -> true);
        seed(m, 0, 0.0);
        tick(m);
        assertThat(m.remainingBurnTicks()).isEqualTo(1_999); // 2,000 committed, minus this tick's burn
        assertThat(lava.tank().getAmount()).isEqualTo(mb(3_900)); // exactly 100 mB consumed
        assertThat(m.lit()).isTrue();
    }

    @Test
    void poweredBurnDecrementsOncePerTick() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 2000, 0.5);
        tick(m, 10);
        assertThat(m.remainingBurnTicks()).isEqualTo(1_990);
    }

    @Test
    void fullBufferDoesNotPauseBurnAndWastesOutput() {
        EngineEnergyOutputComponent e = energy(40_000, 40_000); // full
        MagmaticEngineComponent m = engine(e, tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 2000, 1.0);
        tick(m, 10);
        assertThat(m.remainingBurnTicks()).isEqualTo(1_990); // burned regardless of demand
        assertThat(e.getAmount()).isEqualTo(40_000); // nothing accepted
        assertThat(m.lastAccepted()).isZero();
        assertThat(m.lastWasted()).isEqualTo(15); // full-hot attempt, all wasted
    }

    @Test
    void partialBufferLimitsAcceptedAndWastesRemainder() {
        EngineEnergyOutputComponent e = energy(40_000, 40_000 - 7); // 7 free
        MagmaticEngineComponent m = engine(e, tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 2000, 1.0); // attempts 15
        tick(m);
        assertThat(m.lastAccepted()).isEqualTo(7);
        assertThat(m.lastWasted()).isEqualTo(8);
        assertThat(e.getAmount()).isEqualTo(40_000);
        assertThat(m.remainingBurnTicks()).isEqualTo(1_999);
    }

    @Test
    void noSecondBatchCommittedWhileLit() {
        FluidStoreComponent lava = tank(Fluids.LAVA, 4000);
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), lava, () -> true);
        seed(m, 2000, 0.5);
        tick(m, 100);
        assertThat(lava.tank().getAmount()).isEqualTo(mb(4000)); // no new commit while a batch burns
    }

    @Test
    void burnDurationIndependentOfAcceptedRf() {
        MagmaticEngineComponent full = engine(energy(40_000, 40_000), tank(Fluids.LAVA, 4000), () -> true);
        MagmaticEngineComponent empty = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(full, 2000, 1.0);
        seed(empty, 2000, 1.0);
        tick(full, 100);
        tick(empty, 100);
        assertThat(full.remainingBurnTicks()).isEqualTo(empty.remainingBurnTicks()).isEqualTo(1_900);
    }

    @Test
    void wastedRfIsNotStoredOrConvertedToHeat() {
        EngineEnergyOutputComponent e = energy(40_000, 40_000); // full → all wasted
        MagmaticEngineComponent m = engine(e, tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 2000, 0.5);
        tick(m);
        assertThat(e.getAmount()).isEqualTo(40_000); // not stored
        // temperature changed by heat-soak ONLY (no waste→heat): 0.5 + (1-0.5)*0.0025 = 0.50125
        assertThat(temp(m)).isEqualTo(0.5 + 0.5 * 0.0025, within(1e-9));
    }

    // ==================== Continuous generation ====================

    @Test
    void firstColdTickGeneratesFromJustWarmedTemperature() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 0, 0.0); // cold, unlit
        tick(m); // ignites, heat-soaks to 0.0025, then generates
        assertThat(m.lastAttempted()).isEqualTo(5); // round(5 + 0.0025*10)
        assertThat(m.remainingBurnTicks()).isEqualTo(1_999); // generated same tick as ignition
    }

    @Test
    void attemptedOutputRisesWithTemperature() {
        MagmaticEngineComponent cold = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        MagmaticEngineComponent warm = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        MagmaticEngineComponent hot = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(cold, 2000, 0.0);
        seed(warm, 2000, 0.5);
        seed(hot, 2000, 1.0);
        tick(cold);
        tick(warm);
        tick(hot);
        assertThat(cold.lastAttempted()).isEqualTo(5);
        assertThat(warm.lastAttempted()).isEqualTo(10);
        assertThat(hot.lastAttempted()).isEqualTo(15);
    }

    // ==================== Ignition gating ====================

    @Test
    void ignitionRequiresMaximumBatchPotentialFree() {
        FluidStoreComponent lavaA = tank(Fluids.LAVA, 4000);
        MagmaticEngineComponent tooFull = engine(energy(40_000, 40_000 - 29_999), lavaA, () -> true); // 29,999 free
        seed(tooFull, 0, 0.0);
        tick(tooFull);
        assertThat(tooFull.lit()).isFalse();
        assertThat(lavaA.tank().getAmount()).isEqualTo(mb(4000)); // no lava consumed

        FluidStoreComponent lavaB = tank(Fluids.LAVA, 4000);
        MagmaticEngineComponent justEnough = engine(energy(40_000, 40_000 - 30_000), lavaB, () -> true); // 30,000 free
        seed(justEnough, 0, 0.0);
        tick(justEnough);
        assertThat(justEnough.lit()).isTrue();
        assertThat(lavaB.tank().getAmount()).isEqualTo(mb(3_900));
    }

    @Test
    void temperatureDoesNotLowerRequiredAdmissionSpace() {
        FluidStoreComponent lava = tank(Fluids.LAVA, 4000);
        MagmaticEngineComponent m = engine(energy(40_000, 40_000 - 29_999), lava, () -> true); // 29,999 free
        seed(m, 0, 0.9); // hot but unlit
        tick(m);
        assertThat(m.lit()).isFalse(); // temperature doesn't reduce the requirement
        assertThat(lava.tank().getAmount()).isEqualTo(mb(4000));
    }

    @Test
    void nextBatchWaitsForSpaceAfterBatchEnds() {
        FluidStoreComponent lava = tank(Fluids.LAVA, 4000);
        EngineEnergyOutputComponent e = energy(40_000, 40_000); // full
        MagmaticEngineComponent m = engine(e, lava, () -> true);
        seed(m, 1, 1.0); // one tick of burn left, buffer full
        tick(m); // burns the last tick → unlit
        assertThat(m.lit()).isFalse();
        assertThat(lava.tank().getAmount()).isEqualTo(mb(4000)); // no re-ignition (no space)
        tick(m, 5);
        assertThat(lava.tank().getAmount()).isEqualTo(mb(4000)); // still waiting
        e.setAmount(0); // free the buffer
        tick(m);
        assertThat(m.lit()).isTrue();
        assertThat(lava.tank().getAmount()).isEqualTo(mb(3_900)); // ignites exactly one batch
    }

    // ==================== Redstone ====================

    @Test
    void redstoneOffBlocksInitialIgnition() {
        FluidStoreComponent lava = tank(Fluids.LAVA, 4000);
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), lava, () -> false);
        seed(m, 0, 0.0);
        tick(m);
        assertThat(m.lit()).isFalse();
        assertThat(lava.tank().getAmount()).isEqualTo(mb(4000));
    }

    @Test
    void redstoneOffPausesBurnAndPreservesReserveWhileCooling() {
        boolean[] powered = {true};
        FluidStoreComponent lava = tank(Fluids.LAVA, 4000);
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), lava, () -> powered[0]);
        seed(m, 2000, 0.5);
        powered[0] = false;
        tick(m, 10);
        assertThat(m.remainingBurnTicks()).isEqualTo(2000); // timer paused, not decremented
        assertThat(temp(m)).isLessThan(0.5); // still cooling
        assertThat(m.lastAccepted()).isZero();

        powered[0] = true;
        tick(m);
        assertThat(m.remainingBurnTicks()).isEqualTo(1_999); // resumes the same batch
        assertThat(lava.tank().getAmount()).isEqualTo(mb(4000)); // no new batch consumed
    }

    // ==================== Lit vs running / piston ====================

    @Test
    void litAndRunningAreDistinct() {
        boolean[] powered = {true};
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> powered[0]);
        seed(m, 2000, 0.5);
        assertThat(m.lit()).isTrue();
        assertThat(m.isRunning()).isTrue();
        assertThat(m.isRunning(new FakeMachineContext())).isTrue(); // RunningGate = lit (host ANDs powered)
        assertThat(m.pistonSpeed()).isGreaterThan(0f);

        powered[0] = false;
        assertThat(m.lit()).isTrue();
        assertThat(m.isRunning()).isFalse(); // paused
        assertThat(m.pistonSpeed()).isZero();

        seed(m, 0, 0.5); // unlit
        powered[0] = true;
        assertThat(m.lit()).isFalse();
        assertThat(m.isRunning()).isFalse();
        assertThat(m.pistonSpeed()).isZero();
    }

    @Test
    void pistonSpeedRisesWithThermalStage() {
        MagmaticEngineComponent cold = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        MagmaticEngineComponent warm = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        MagmaticEngineComponent hot = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(cold, 2000, 0.10); // COLD
        seed(warm, 2000, 0.50); // COOL ("Warm")
        seed(hot, 2000, 0.90); // WARM ("Hot")
        // Speed tracks the visible stage, and accelerates as it heat-soaks.
        assertThat(cold.pistonSpeed()).isLessThan(warm.pistonSpeed());
        assertThat(warm.pistonSpeed()).isLessThan(hot.pistonSpeed());
        assertThat(cold.pistonSpeed()).isGreaterThan(0f);
    }

    // ==================== Persistence ====================

    @Test
    void savePreservesBurnAndTemperatureAndDerivesStage() {
        MagmaticEngineComponent writer = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(writer, 1500, 0.6);
        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);
        // only the two authoritative fields are written
        assertThat(tag.contains("RemainingBurnTicks")).isTrue();
        assertThat(tag.contains("Temperature")).isTrue();
        assertThat(tag.contains("ThermalStage")).isFalse();
        assertThat(tag.contains("Stage")).isFalse();

        MagmaticEngineComponent reader = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        reader.load(tag, registries);
        assertThat(reader.remainingBurnTicks()).isEqualTo(1500);
        assertThat(temp(reader)).isEqualTo(0.6, within(1e-9));
        assertThat(reader.stage()).isEqualTo(HeatStage.COOL); // stageFor(0.6)
    }

    @Test
    void stageDerivedFromTemperatureOnLoad() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 0, 0.10);
        assertThat(m.stage()).isEqualTo(HeatStage.COLD);
        seed(m, 0, 0.50);
        assertThat(m.stage()).isEqualTo(HeatStage.COOL);
        seed(m, 0, 0.90);
        assertThat(m.stage()).isEqualTo(HeatStage.WARM);
    }

    @Test
    void transientOutputResetsOnLoad() {
        MagmaticEngineComponent m = engine(energy(1_000_000, 0), tank(Fluids.LAVA, 4000), () -> true);
        seed(m, 2000, 1.0);
        tick(m);
        assertThat(m.lastAccepted()).isPositive();
        CompoundTag tag = new CompoundTag();
        m.save(tag, registries);
        m.load(tag, registries);
        assertThat(m.lastAttempted()).isZero();
        assertThat(m.lastAccepted()).isZero();
        assertThat(m.lastWasted()).isZero();
    }
}
