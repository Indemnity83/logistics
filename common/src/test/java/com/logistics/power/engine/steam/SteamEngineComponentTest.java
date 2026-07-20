package com.logistics.power.engine.steam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.lib.power.fuel.FuelSource;
import com.logistics.core.machine.FakeMachineContext;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

/**
 * Behavior of the Steam Engine pressure simulation. Uses vanilla water and an injected fake {@link
 * FuelSource} so tests don't need a live level or the mod's custom fluids.
 * Profile: maxOutput 40, maxPressure 1000, operating 400, relight 650, target 800, steam 12/t,
 * pressurePerRf 0.25, water 6 pressure/mB, coolingDecay 0.05, stokedInterval 20.
 */
class SteamEngineComponentTest extends MinecraftTestEnvironment {

    private static final SteamEngineProfile PROFILE =
            new SteamEngineProfile(40, 1000, 400, 650, 800, 12, 0.25, 6, 0.05, 20);

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    /** A fake fuel source: hands out {@code ticksPerItem} per ignition until {@code available} runs out. */
    private static final class FakeFuel implements FuelSource {
        int available;
        final int ticksPerItem;
        int ignitions;

        FakeFuel(int available, int ticksPerItem) {
            this.available = available;
            this.ticksPerItem = ticksPerItem;
        }

        @Override
        public int ignite(MachineContext ctx) {
            if (available <= 0) {
                return 0;
            }
            available--;
            ignitions++;
            return ticksPerItem;
        }
    }

    private EngineEnergyOutputComponent energy(long capacity, long amount) {
        EngineEnergyOutputComponent e = new EngineEnergyOutputComponent("energy", () -> capacity, () -> {});
        e.setAmount(amount);
        return e;
    }

    private FluidStoreComponent water(long mb) {
        FluidStoreComponent s = new FluidStoreComponent("water", FluidUnits.mb(100_000), () -> {});
        if (mb > 0) {
            s.tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(mb));
        }
        return s;
    }

    private SteamEngineComponent engine(
            EngineEnergyOutputComponent e, FluidStoreComponent w, FuelSource fuel, BooleanSupplier powered) {
        return engine(e, w, fuel, powered, PROFILE, (ctx, lit) -> {});
    }

    private SteamEngineComponent engine(
            EngineEnergyOutputComponent e,
            FluidStoreComponent w,
            FuelSource fuel,
            BooleanSupplier powered,
            SteamEngineProfile profile,
            EngineComponent.LitController lit) {
        return new SteamEngineComponent("steam", e, w, fuel, profile, powered, lit, () -> {});
    }

    /** Seed pressure + burn reserve directly through the persistence path. */
    private void seed(SteamEngineComponent s, double pressure, int burn, int total, int stoked, double debt) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Pressure", pressure);
        tag.putInt("CommittedBurnTicks", burn);
        tag.putInt("TotalFuelTicks", total);
        tag.putInt("StokedTickAccumulator", stoked);
        tag.putDouble("WaterDebt", debt);
        s.load(tag, registries);
    }

    private void tick(SteamEngineComponent s) {
        s.serverTick(new FakeMachineContext());
    }

    private void tick(SteamEngineComponent s, int n) {
        for (int i = 0; i < n; i++) {
            tick(s);
        }
    }

    // ==================== Firebox + burn reserve ====================

    @Test
    void boilingSpendsOneBurnTickPerTick() {
        SteamEngineComponent s = engine(energy(10_000, 0), water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 100, 100, 0, 0);
        tick(s);
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.BOILING);
        assertThat(s.committedBurnTicks()).isEqualTo(99);
    }

    @Test
    void dryCommittedFireIsStokedNotOff() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 100, 100, 0, 0);
        tick(s);
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.STOKED);
        assertThat(s.committedBurnTicks()).isEqualTo(100); // not spent this tick
    }

    @Test
    void stokedFireSpendsBurnTickOnlyAfterInterval() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 100, 100, 0, 0);
        tick(s, 19);
        assertThat(s.committedBurnTicks()).isEqualTo(100); // 19 stoked ticks < interval of 20
        tick(s);
        assertThat(s.committedBurnTicks()).isEqualTo(99); // 20th stoked tick spends one
    }

    @Test
    void stokedFirePreventsPressureDecay() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 100, 100, 0, 0);
        tick(s); // buffer full → no turbine draw; dry → stoked → no decay
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.STOKED);
        assertThat(s.pressure()).isEqualTo(700.0, within(1e-9));
    }

    @Test
    void reachingTargetFlipsBoilingToStoked() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 799, 100, 100, 0, 0);
        tick(s); // boils the last point up to the target
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.BOILING);
        assertThat(s.pressure()).isEqualTo(800.0, within(1e-9));
        tick(s); // at target now → cannot boil → stoked
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.STOKED);
    }

    @Test
    void fallingBelowTargetResumesBoilingWithoutConsumingNewFuel() {
        FakeFuel fuel = new FakeFuel(1, 1600);
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(10_000), fuel, () -> true);
        seed(s, 800, 100, 100, 0, 0);
        tick(s); // at target → stoked
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.STOKED);
        seed(s, 790, s.committedBurnTicks(), 100, 0, 0); // drop below target, keep the reserve
        tick(s);
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.BOILING);
        assertThat(fuel.ignitions).isZero(); // never lit a new item — resumed the committed reserve
    }

    @Test
    void waterArrivingFlipsStokedToBoiling() {
        FluidStoreComponent w = water(0);
        SteamEngineComponent s = engine(energy(10_000, 10_000), w, new FakeFuel(0, 0), () -> true);
        seed(s, 700, 100, 100, 0, 0);
        tick(s);
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.STOKED);
        w.tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(1000));
        tick(s);
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.BOILING);
    }

    @Test
    void exhaustingReserveWhileStokedGoesOffThenDecays() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 1, 1, 19, 0); // one burn tick left, one stoked tick from spending it
        tick(s); // stoked, 20th accumulator tick spends the last burn tick → reserve exhausted
        assertThat(s.committedBurnTicks()).isZero();
        assertThat(s.pressure()).isEqualTo(700.0, within(1e-9)); // fire still counted as maintained this tick
        tick(s); // now OFF → cooling decay kicks in
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.OFF);
        assertThat(s.pressure()).isLessThan(700.0);
    }

    // ==================== Pressure / target / clamp ====================

    @Test
    void boilerStopsAtTargetNotMax() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(100_000), new FakeFuel(0, 0), () -> true);
        seed(s, 799, 1_000_000, 1_000_000, 0, 0);
        tick(s, 200);
        assertThat(s.pressure()).isEqualTo(800.0, within(1e-6)); // holds at target, never climbs to max
    }

    @Test
    void safetyMaxClampsPressureWithoutFailure() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 1200, 0, 0, 0, 0); // above the safety ceiling (e.g. a config change)
        tick(s);
        assertThat(s.pressure()).isEqualTo(1000.0, within(1e-9));
    }

    @Test
    void waterLimitsSteamAndConsumesTransactionally() {
        FluidStoreComponent w = water(1); // only 1 mB → at most 6 pressure of steam
        SteamEngineComponent s = engine(energy(10_000, 10_000), w, new FakeFuel(0, 0), () -> true);
        seed(s, 100, 100, 100, 0, 0);
        tick(s); // buffer full → no turbine; steam limited by 1 mB water = 6 pressure
        assertThat(s.pressure()).isEqualTo(106.0, within(1e-9));
        assertThat(w.tank().getAmount()).isZero(); // exactly the 1 mB consumed, never over-drawn
    }

    @Test
    void noSteamWithoutWater() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 100, 100, 100, 0, 0);
        tick(s);
        assertThat(s.pressure()).isEqualTo(100.0, within(1e-9)); // no water → no steam
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.STOKED);
    }

    @Test
    void outputLimitedByAvailablePressure() {
        // pressurePerRf 100 makes the pressure-limit bind before the ramp.
        SteamEngineProfile p = new SteamEngineProfile(40, 1000, 400, 650, 800, 12, 100, 6, 0.05, 20);
        SteamEngineComponent s = engine(energy(10_000, 0), water(0), new FakeFuel(0, 0), () -> true, p, (c, l) -> {});
        seed(s, 250, 0, 0, 0, 0);
        tick(s);
        assertThat(s.lastGenerationRate()).isEqualTo(2); // floor(250 / 100) caps it at 2, below the ramp of 25
        assertThat(s.pressure()).isEqualTo(49.95, within(1e-6)); // 250 - 2*100 = 50, minus 0.05 leak (fire off)
    }

    @Test
    void bufferFullDrawsNoPressureButBoilerRuns() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 500, 100, 100, 0, 0);
        tick(s);
        assertThat(s.lastGenerationRate()).isZero(); // buffer full → no turbine draw
        assertThat(s.pressure()).isEqualTo(512.0, within(1e-9)); // boiler still added 12
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.BOILING);
    }

    // ==================== Redstone / leak ====================

    @Test
    void redstoneDisabledFreezesReserveAndDecays() {
        FakeFuel fuel = new FakeFuel(5, 1600);
        boolean[] powered = {false};
        SteamEngineComponent s = engine(energy(10_000, 0), water(10_000), fuel, () -> powered[0]);
        seed(s, 700, 100, 100, 0, 0);
        tick(s);
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.OFF);
        assertThat(s.committedBurnTicks()).isEqualTo(100); // frozen, not consumed
        assertThat(s.lastGenerationRate()).isZero();
        assertThat(s.pressure()).isLessThan(700.0); // cooling decay applies
        assertThat(s.status()).isEqualTo(SteamEngineStatus.REDSTONE_DISABLED);
        assertThat(fuel.ignitions).isZero();

        powered[0] = true;
        tick(s);
        assertThat(s.committedBurnTicks()).isEqualTo(99); // resumed the same reserve (boiled one tick)
        assertThat(fuel.ignitions).isZero(); // no new fuel item consumed
    }

    @Test
    void coolingDecayAppliesOnlyWhileOff() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 1000, 0, 0, 0, 0); // no reserve → OFF
        tick(s, 100);
        assertThat(s.pressure()).isEqualTo(1000.0 - 100 * 0.05, within(1e-6)); // 995
    }

    // ==================== LIT ====================

    @Test
    void litTrueForStokedAndBoilingFalseForOff() {
        boolean[] lit = {false};
        EngineComponent.LitController rec = (ctx, l) -> lit[0] = l;

        SteamEngineComponent boiling = engine(energy(10_000, 0), water(10_000), new FakeFuel(0, 0), () -> true, PROFILE, rec);
        seed(boiling, 700, 100, 100, 0, 0);
        tick(boiling);
        assertThat(lit[0]).isTrue(); // boiling

        SteamEngineComponent stoked = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true, PROFILE, rec);
        seed(stoked, 700, 100, 100, 0, 0);
        tick(stoked);
        assertThat(lit[0]).isTrue(); // stoked

        SteamEngineComponent off = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true, PROFILE, rec);
        seed(off, 700, 0, 0, 0, 0);
        tick(off);
        assertThat(lit[0]).isFalse(); // off
    }

    // ==================== Persistence ====================

    @Test
    void lastGenerationRateResetsAfterLoadAndRecomputes() {
        SteamEngineComponent writer = engine(energy(10_000, 0), water(10_000), new FakeFuel(0, 0), () -> true);
        seed(writer, 700, 100, 100, 0, 0);
        tick(writer);
        assertThat(writer.lastGenerationRate()).isEqualTo(40);

        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);

        SteamEngineComponent reader = engine(energy(10_000, 0), water(10_000), new FakeFuel(0, 0), () -> true);
        reader.load(tag, registries);
        assertThat(reader.lastGenerationRate()).isZero(); // not persisted → starts at 0
        tick(reader);
        assertThat(reader.lastGenerationRate()).isEqualTo(40); // recomputed
    }

    @Test
    void persistedStateRoundTrips() {
        SteamEngineComponent writer = engine(energy(10_000, 0), water(10_000), new FakeFuel(0, 0), () -> true);
        seed(writer, 512.5, 77, 100, 5, 0.5);

        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);
        assertThat(tag.contains("LastGenerationRate")).isFalse(); // transient render/derived state

        SteamEngineComponent reader = engine(energy(10_000, 0), water(10_000), new FakeFuel(0, 0), () -> true);
        reader.load(tag, registries);
        assertThat(reader.pressure()).isEqualTo(512.5, within(1e-9));
        assertThat(reader.committedBurnTicks()).isEqualTo(77);
        assertThat(reader.totalFuelTicks()).isEqualTo(100);
        assertThat(reader.burnFraction()).isEqualTo(0.77, within(1e-9));
    }

    // ==================== Status derivation ====================

    @Test
    void statusRedstoneDisabled() {
        SteamEngineComponent s = engine(energy(10_000, 0), water(10_000), new FakeFuel(1, 1600), () -> false);
        seed(s, 700, 100, 100, 0, 0);
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.REDSTONE_DISABLED);
    }

    @Test
    void statusEmpty() {
        SteamEngineComponent s = engine(energy(10_000, 0), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 0, 0, 0, 0, 0);
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.EMPTY);
    }

    @Test
    void statusNoFuel() {
        SteamEngineComponent s = engine(energy(10_000, 0), water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 100, 0, 0, 0, 0); // below relight, water present, but no fuel item
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.NO_FUEL);
    }

    @Test
    void statusNoWater() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 100, 100, 0, 0); // lit reserve but dry
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.NO_WATER);
    }

    @Test
    void statusBuildingPressure() {
        SteamEngineComponent s = engine(energy(10_000, 0), water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 100, 100, 100, 0, 0); // boiling below operating
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.BUILDING_PRESSURE);
    }

    @Test
    void statusGenerating() {
        SteamEngineComponent s = engine(energy(10_000, 0), water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 100, 100, 0, 0); // boiling at/above operating, generating
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.GENERATING);
    }

    @Test
    void statusCoasting() {
        SteamEngineComponent s = engine(energy(10_000, 0), water(10_000), new FakeFuel(1, 1600), () -> true);
        seed(s, 700, 0, 0, 0, 0); // no reserve, pressure above relight → coasting on stored pressure
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.COASTING);
    }

    @Test
    void statusOutputFull() {
        SteamEngineComponent s = engine(energy(10_000, 10_000), water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 100, 100, 0, 0); // buffer full, pressurized
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.OUTPUT_FULL);
    }
}
