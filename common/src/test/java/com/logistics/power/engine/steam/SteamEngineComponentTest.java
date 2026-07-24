package com.logistics.power.engine.steam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.EngineComponent;
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
 * Behavior of the Steam Engine thermal-mass simulation (fuel -> heat -> steam -> pressure -> RF). Uses
 * vanilla water, an injected fake {@link FuelSource}, and an injected {@link TurbineOutput} so tests need
 * no live level. Test profile scales heat down for readability: maxHeat 2000, boiling 800, refuel 1400,
 * target 1600, heatPerBurnTick 1, firingRate 16, passiveLoss 0.1, latentHeat 0.5, steamRate 12,
 * condensation 0.5; pressure side matches the real defaults (maxOutput 40, operating 400, target 800,
 * pressurePerRf 0.25, water 6 pressure/mB).
 */
class SteamEngineComponentTest extends MinecraftTestEnvironment {

    private static final SteamEngineProfile PROFILE = new SteamEngineProfile(
            40, 1000, 400, 800, 0.25, 6, 12, 0.5, 2000, 800, 1400, 1600, 1, 16, 0.1, 0.5);

    // Injected output "consumers": nothing accepts / everything accepts / a fixed cap accepts.
    private static final TurbineOutput NO_CONSUMER = (ctx, rf) -> 0;
    private static final TurbineOutput FULL_CONSUMER = (ctx, rf) -> rf;

    private static TurbineOutput accepts(long cap) {
        return (ctx, rf) -> Math.min(rf, cap);
    }

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

    private FluidStoreComponent water(long mb) {
        FluidStoreComponent s = new FluidStoreComponent("water", FluidUnits.mb(1_000_000), () -> {});
        if (mb > 0) {
            s.tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(mb));
        }
        return s;
    }

    private SteamEngineComponent engine(
            TurbineOutput out, FluidStoreComponent w, FuelSource fuel, BooleanSupplier powered) {
        return engine(out, w, fuel, powered, PROFILE, (ctx, lit) -> {});
    }

    private SteamEngineComponent engine(
            TurbineOutput out,
            FluidStoreComponent w,
            FuelSource fuel,
            BooleanSupplier powered,
            SteamEngineProfile profile,
            EngineComponent.LitController lit) {
        return new SteamEngineComponent("steam", out, w, fuel, profile, powered, lit, () -> {});
    }

    /** Seed pressure + boiler heat + burn reserve directly through the persistence path. */
    private void seed(SteamEngineComponent s, double pressure, double heat, int burn, int total, double debt) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Pressure", pressure);
        tag.putDouble("BoilerHeat", heat);
        tag.putInt("CommittedBurnTicks", burn);
        tag.putInt("TotalFuelTicks", total);
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

    /** Tick until boiler heat reaches {@code threshold} (cap at {@code maxTicks}); returns the tick count. */
    private int ticksUntilHeat(SteamEngineComponent s, double threshold, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            if (s.boilerHeat() >= threshold) {
                return i;
            }
            tick(s);
        }
        return maxTicks;
    }

    // ==================== Direct output (turbine spends pressure, never heat) ====================

    @Test
    void noConsumerDrawsNoPressure() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 0, 0, 0, 0); // cold, dry, no reserve: isolate the turbine
        tick(s);
        assertThat(s.lastGenerationRate()).isZero();
        assertThat(s.pressure()).isEqualTo(700.0 - 0.5, within(1e-9)); // cold boiler condenses 0.5
    }

    @Test
    void partialConsumerDrawsOnlyAcceptedPressure() {
        SteamEngineComponent s = engine(accepts(15), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 0, 0, 0, 0);
        tick(s);
        assertThat(s.lastGenerationRate()).isEqualTo(15);
        assertThat(s.pressure()).isEqualTo(700.0 - 15 * 0.25 - 0.5, within(1e-9)); // 695.75
    }

    @Test
    void fullConsumerDrawsFullPressure() {
        SteamEngineComponent s = engine(FULL_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 0, 0, 0, 0);
        tick(s);
        assertThat(s.lastGenerationRate()).isEqualTo(40);
        assertThat(s.pressure()).isEqualTo(700.0 - 40 * 0.25 - 0.5, within(1e-9)); // 689.5
    }

    @Test
    void outputNeverExceedsMaxOutput() {
        SteamEngineComponent s = engine((ctx, rf) -> 1_000_000, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 950, 0, 0, 0, 0);
        tick(s);
        assertThat(s.lastGenerationRate()).isEqualTo(40);
    }

    @Test
    void outputLimitedByAvailablePressure() {
        // pressurePerRf 100 makes the pressure-limit bind before the ramp; full consumer.
        SteamEngineProfile p = new SteamEngineProfile(
                40, 1000, 400, 800, 100, 6, 12, 0.5, 2000, 800, 1400, 1600, 1, 16, 0.1, 0.5);
        SteamEngineComponent s = engine(FULL_CONSUMER, water(0), new FakeFuel(0, 0), () -> true, p, (c, l) -> {});
        seed(s, 250, 0, 0, 0, 0);
        tick(s);
        assertThat(s.lastGenerationRate()).isEqualTo(2); // floor(250 / 100) caps it at 2, below the ramp of 25
        assertThat(s.pressure()).isEqualTo(49.5, within(1e-6)); // 250 - 2*100 = 50, minus 0.5 cold condensation
    }

    @Test
    void pressureNeverGoesNegative() {
        SteamEngineComponent s = engine(FULL_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 5, 0, 0, 0, 0);
        tick(s, 50);
        assertThat(s.pressure()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void turbineSpendsPressureNotHeat() {
        // Two identical hot engines, no water/steam and no fuel: the consumer draws pressure but heat only
        // ever changes by passive loss — the turbine never touches boiler heat.
        SteamEngineComponent idle = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        SteamEngineComponent loaded = engine(FULL_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(idle, 700, 1500, 0, 0, 0);
        seed(loaded, 700, 1500, 0, 0, 0);
        tick(idle);
        tick(loaded);
        assertThat(idle.boilerHeat()).isEqualTo(1499.9, within(1e-9));
        assertThat(loaded.boilerHeat()).isEqualTo(1499.9, within(1e-9)); // same heat despite the draw
        assertThat(idle.pressure()).isEqualTo(700.0, within(1e-9)); // hot boiler: no condensation
        assertThat(loaded.pressure()).isEqualTo(690.0, within(1e-9)); // only the turbine spent pressure
    }

    @Test
    void redstoneDisabledDoesNoOutput() {
        SteamEngineComponent s = engine(FULL_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> false);
        seed(s, 700, 1500, 100, 100, 0);
        tick(s);
        assertThat(s.lastGenerationRate()).isZero();
        assertThat(s.pressure()).isEqualTo(700.0, within(1e-9)); // hot: no draw, no condensation
    }

    @Test
    void pistonSpeedTracksAcceptedRf() {
        SteamEngineComponent s = engine(accepts(20), water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 0, 0, 0, 0);
        tick(s);
        // 20/40 -> 0.02 + 0.5*0.06 = 0.05
        assertThat(s.pistonSpeed()).isEqualTo(0.05f, within(1e-6f));
        assertThat(s.isRunning(new FakeMachineContext())).isTrue();

        SteamEngineComponent idle = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(idle, 700, 0, 0, 0, 0);
        tick(idle);
        assertThat(idle.pistonSpeed()).isZero();
        assertThat(idle.isRunning(new FakeMachineContext())).isFalse();
    }

    // ==================== Continuous burn + item-boundary thermostat ====================

    @Test
    void committedFuelBurnsFiringRatePerTick() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 1000, 100, 100, 0); // hot enough to fire, mid-band
        tick(s);
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.FIRING);
        assertThat(s.committedBurnTicks()).isEqualTo(84); // 100 - firingRate(16)
    }

    @Test
    void reachingTargetDoesNotPauseCommittedFuel() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 800, 1800, 100, 100, 0); // above targetHeat, pressure at target (no steam headroom)
        tick(s);
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.FIRING); // still burning above target
        assertThat(s.committedBurnTicks()).isEqualTo(84);
        assertThat(s.boilerHeat()).isEqualTo(1815.9, within(1e-9)); // 1800 - 0.1 + 16, still climbing
    }

    @Test
    void noNewItemCommittedWhileAboveRefuel() {
        FakeFuel fuel = new FakeFuel(5, 1600);
        SteamEngineComponent s = engine(NO_CONSUMER, water(10_000), fuel, () -> true);
        seed(s, 700, 1500, 0, 0, 0); // reserve empty, but heat above refuelHeat
        tick(s);
        assertThat(fuel.ignitions).isZero();
        assertThat(s.committedBurnTicks()).isZero();
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.STOKED); // hot & ready, no new item
    }

    @Test
    void newItemCommittedOnceAtOrBelowRefuel() {
        FakeFuel fuel = new FakeFuel(5, 1600);
        SteamEngineComponent s = engine(NO_CONSUMER, water(10_000), fuel, () -> true);
        seed(s, 700, 1400, 0, 0, 0); // reserve empty, heat at refuelHeat
        tick(s);
        assertThat(fuel.ignitions).isEqualTo(1);
        assertThat(s.committedBurnTicks()).isEqualTo(1600); // committed this tick, burns from next
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.FIRING);
    }

    @Test
    void ignitionAndCombustionAreNotWaterGated() {
        FakeFuel fuel = new FakeFuel(5, 1600);
        SteamEngineComponent s = engine(NO_CONSUMER, water(0), fuel, () -> true); // bone dry
        seed(s, 0, 0, 0, 0, 0);
        tick(s, 5);
        assertThat(fuel.ignitions).isEqualTo(1); // committed despite no water
        assertThat(s.boilerHeat()).isGreaterThan(0.0); // and heated (dry-firing permitted)
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.FIRING);
    }

    @Test
    void passiveLossAppliesWhileFiring() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 0, 1000, 100, 100, 0); // dry so no steam removes heat; isolate fire + passive loss
        tick(s);
        assertThat(s.boilerHeat()).isEqualTo(1015.9, within(1e-9)); // 1000 - 0.1 passive + 16 fire
    }

    // ==================== Saturation: clamp, discard, no failure ====================

    @Test
    void heatClampsAtMaxWithoutShutdown() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 0, 1990, 1000, 1000, 0); // hot, huge reserve, no load, no water
        tick(s);
        assertThat(s.boilerHeat()).isEqualTo(2000.0, within(1e-9)); // clamped, overflow discarded
        assertThat(s.isSafetyValveActive()).isTrue();
        assertThat(s.committedBurnTicks()).isEqualTo(984); // keeps burning while saturated
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.FIRING);
    }

    @Test
    void smallFuelOvershootsLessThanLarge() {
        SteamEngineComponent small = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        SteamEngineComponent large = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(small, 0, 1400, 16, 16, 0); // one firing tick of fuel
        seed(large, 0, 1400, 1600, 1600, 0); // a full item
        // Check the large item at its peak (before its reserve exhausts and passive loss cools it back).
        tick(large, 60);
        assertThat(large.boilerHeat()).isEqualTo(2000.0, within(1e-9)); // large saturates the boiler
        assertThat(large.isSafetyValveActive()).isTrue();
        tick(small, 60);
        assertThat(small.boilerHeat()).isLessThan(large.boilerHeat()); // small overshoots far less
        assertThat(small.isSafetyValveActive()).isFalse();
    }

    // ==================== Steam / latent heat ====================

    @Test
    void noSteamBelowBoiling() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 0, 700, 100, 100, 0); // below boilingHeat
        tick(s);
        assertThat(s.pressure()).isEqualTo(0.0, within(1e-9)); // no steam while heating
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.FIRING);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.HEATING);
    }

    @Test
    void heatFactorRampsSteam() {
        SteamEngineComponent half = engine(NO_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        SteamEngineComponent full = engine(NO_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(half, 0, 1200, 0, 0, 0); // heatFactor ~0.5
        seed(full, 0, 1700, 0, 0, 0); // heatFactor clamps to 1
        tick(half);
        tick(full);
        assertThat(half.pressure()).isEqualTo(12 * (1199.9 - 800) / 800.0, within(1e-6)); // ~5.9985
        assertThat(full.pressure()).isEqualTo(12.0, within(1e-9)); // full rate
        assertThat(full.pressure()).isGreaterThan(half.pressure());
    }

    @Test
    void steamConsumesLatentHeat() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 0, 1700, 0, 0, 0); // hot, no reserve so no fuel heat added this tick
        tick(s);
        assertThat(s.pressure()).isEqualTo(12.0, within(1e-9)); // full-rate steam
        assertThat(s.boilerHeat()).isEqualTo(1700.0 - 0.1 - 12 * 0.5, within(1e-9)); // 1693.9: latent 0.5/pressure
    }

    @Test
    void efficiencyChainIsEightRfPerBurnTick() {
        double rfPerBurnTick =
                PROFILE.heatPerBurnTick() / (PROFILE.latentHeat() * PROFILE.pressurePerRf());
        assertThat(rfPerBurnTick).isEqualTo(8.0, within(1e-9));
    }

    // ==================== Condensation ====================

    @Test
    void pressureHeldWhileHot() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 1500, 0, 0, 0); // hot, no water/steam, no reserve
        tick(s, 10);
        assertThat(s.pressure()).isEqualTo(700.0, within(1e-9)); // no condensation above boiling
    }

    @Test
    void pressureCondensesOnlyWhenCold() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 700, 0, 0, 0); // below boilingHeat
        tick(s);
        assertThat(s.pressure()).isEqualTo(699.5, within(1e-9)); // condensationRate 0.5
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.OFF);
    }

    // ==================== Redstone disable preserves + resumes the reserve ====================

    @Test
    void redstoneDisablePreservesReserveThenResumes() {
        FakeFuel fuel = new FakeFuel(5, 1600);
        boolean[] powered = {false};
        SteamEngineComponent s = engine(FULL_CONSUMER, water(0), fuel, () -> powered[0]);
        seed(s, 700, 1500, 100, 100, 0);
        tick(s);
        assertThat(s.fireboxState()).isEqualTo(SteamFireboxState.OFF);
        assertThat(s.committedBurnTicks()).isEqualTo(100); // preserved, not consumed
        assertThat(s.lastGenerationRate()).isZero();
        assertThat(s.status()).isEqualTo(SteamEngineStatus.REDSTONE_DISABLED);
        assertThat(fuel.ignitions).isZero();

        powered[0] = true;
        tick(s);
        assertThat(s.committedBurnTicks()).isEqualTo(84); // resumed the same reserve (burned firingRate)
        assertThat(fuel.ignitions).isZero(); // no new fuel item committed
    }

    // ==================== LIT ====================

    @Test
    void litTrueOnlyWhileFiring() {
        boolean[] lit = {false};
        EngineComponent.LitController rec = (ctx, l) -> lit[0] = l;

        SteamEngineComponent firing = engine(NO_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true, PROFILE, rec);
        seed(firing, 700, 1000, 100, 100, 0);
        tick(firing);
        assertThat(lit[0]).isTrue(); // firing

        SteamEngineComponent stoked = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true, PROFILE, rec);
        seed(stoked, 700, 1500, 0, 0, 0); // no reserve, above refuel
        tick(stoked);
        assertThat(lit[0]).isFalse(); // stoked is not an active flame

        SteamEngineComponent off = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true, PROFILE, rec);
        seed(off, 700, 0, 0, 0, 0);
        tick(off);
        assertThat(lit[0]).isFalse(); // off
    }

    // ==================== Persistence / migration ====================

    @Test
    void boilerHeatRoundTrips() {
        SteamEngineComponent writer = engine(FULL_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(writer, 512.5, 1234.5, 77, 100, 0.5);

        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);
        assertThat(tag.contains("StokedTickAccumulator")).isFalse(); // dropped
        assertThat(tag.contains("StoredEnergy")).isFalse(); // no RF buffer

        SteamEngineComponent reader = engine(FULL_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        reader.load(tag, registries);
        assertThat(reader.pressure()).isEqualTo(512.5, within(1e-9));
        assertThat(reader.boilerHeat()).isEqualTo(1234.5, within(1e-9));
        assertThat(reader.committedBurnTicks()).isEqualTo(77);
        assertThat(reader.totalFuelTicks()).isEqualTo(100);
        assertThat(reader.burnFraction()).isEqualTo(0.77, within(1e-9));
    }

    @Test
    void loadsColdWhenNoBoilerHeatAndIgnoresObsoleteTags() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Pressure", 300);
        tag.putInt("CommittedBurnTicks", 50);
        tag.putInt("TotalFuelTicks", 100);
        tag.putLong("StoredEnergy", 9999); // obsolete RF buffer
        tag.putInt("StokedTickAccumulator", 7); // obsolete stoked timer
        SteamEngineComponent s = engine(FULL_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        s.load(tag, registries);
        assertThat(s.boilerHeat()).isEqualTo(0.0, within(1e-9)); // reloads cold
        assertThat(s.pressure()).isEqualTo(300.0, within(1e-9));
        assertThat(s.committedBurnTicks()).isEqualTo(50);
    }

    @Test
    void lastGenerationRateIsSyncedForClientAndRecomputed() {
        SteamEngineComponent writer = engine(FULL_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(writer, 700, 1700, 100, 100, 0);
        tick(writer);
        assertThat(writer.lastGenerationRate()).isEqualTo(40);

        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);
        assertThat(tag.contains("LastGenerationRate")).isTrue();

        SteamEngineComponent reader = engine(FULL_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        reader.load(tag, registries);
        assertThat(reader.lastGenerationRate()).isEqualTo(40);
        tick(reader);
        assertThat(reader.lastGenerationRate()).isEqualTo(40); // recomputed
    }

    // ==================== Status derivation ====================

    @Test
    void statusRedstoneDisabled() {
        SteamEngineComponent s = engine(FULL_CONSUMER, water(10_000), new FakeFuel(1, 1600), () -> false);
        seed(s, 700, 1500, 100, 100, 0);
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.REDSTONE_DISABLED);
    }

    @Test
    void statusNoFuel() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 0, 0, 0, 0, 0); // cold, no pressure, no fuel to commit
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.NO_FUEL);
    }

    @Test
    void statusHeating() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 0, 500, 100, 100, 0); // firing but below boiling
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.HEATING);
    }

    @Test
    void statusNoWater() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(0), new FakeFuel(0, 0), () -> true);
        seed(s, 500, 1500, 100, 100, 0); // hot enough to boil but dry
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.NO_WATER);
    }

    @Test
    void statusBuildingPressure() {
        SteamEngineComponent s = engine(FULL_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 100, 1700, 100, 100, 0); // steaming, pressure below operating
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.BUILDING_PRESSURE);
    }

    @Test
    void statusGenerating() {
        SteamEngineComponent s = engine(FULL_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 1700, 100, 100, 0); // steaming and delivering above operating
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.GENERATING);
    }

    @Test
    void statusCoasting() {
        SteamEngineComponent s = engine(FULL_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 700, 700, 0, 0, 0); // cold boiler, no steam, delivering stored pressure
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.COASTING);
    }

    @Test
    void statusOutputBlocked() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(10_000), new FakeFuel(0, 0), () -> true);
        seed(s, 800, 1500, 100, 100, 0); // at target (no steam headroom), pressure available, nothing accepts
        tick(s);
        assertThat(s.status()).isEqualTo(SteamEngineStatus.OUTPUT_BLOCKED);
    }

    // ==================== Balance relationships (tolerances, not brittle counts) ====================

    @Test
    void coldStartPassesBoilingBeforeTarget() {
        SteamEngineComponent s = engine(NO_CONSUMER, water(1_000_000), new FakeFuel(0, 0), () -> true);
        seed(s, 0, 0, 1_000_000, 1_000_000, 0);
        int toBoiling = ticksUntilHeat(s, PROFILE.boilingHeat(), 5000);
        int toTarget = ticksUntilHeat(s, PROFILE.targetHeat(), 5000);
        assertThat(toBoiling).isLessThan(toTarget); // boiling reached first
        assertThat(toTarget).isLessThan(5000); // and the target is eventually reached
    }

    @Test
    void heavyLoadCommitsFuelMoreOftenThanLightLoad() {
        FakeFuel heavyFuel = new FakeFuel(1_000_000, 100);
        FakeFuel lightFuel = new FakeFuel(1_000_000, 100);
        SteamEngineComponent heavy = engine(FULL_CONSUMER, water(1_000_000), heavyFuel, () -> true);
        SteamEngineComponent light = engine(NO_CONSUMER, water(1_000_000), lightFuel, () -> true);
        seed(heavy, 0, 1600, 0, 0, 0);
        seed(light, 0, 1600, 0, 0, 0);
        tick(heavy, 3000);
        tick(light, 3000);
        assertThat(heavyFuel.ignitions).isGreaterThan(lightFuel.ignitions);
    }

    @Test
    void fullLoadWithHotBoilerHoldsPressureAndDelivers() {
        // A well-fed, fully-heated boiler: full-rate steam (12/t) comfortably covers the turbine draw
        // (10/t at 40 RF), so pressure holds near the target and full output is delivered indefinitely.
        SteamEngineComponent s = engine(FULL_CONSUMER, water(1_000_000), new FakeFuel(0, 0), () -> true);
        seed(s, 800, 1700, 1_000_000, 1_000_000, 0);
        tick(s, 400);
        assertThat(s.lastGenerationRate()).isEqualTo(40);
        assertThat(s.pressure()).isBetween(760.0, 800.0); // steam covers draw; hovers just below target
        assertThat(s.boilerHeat()).isGreaterThanOrEqualTo(PROFILE.targetHeat()); // stays fully hot
    }
}
