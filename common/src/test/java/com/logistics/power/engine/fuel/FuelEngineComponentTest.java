package com.logistics.power.engine.fuel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.component.EngineEnergyOutputComponent;
import com.logistics.core.machine.FakeMachineContext;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Behavior of the Fuel Engine simulation. Uses vanilla lava as a stand-in "fuel" and water as coolant
 * via injected lookups, so tests don't depend on the mod's custom fluids being registered.
 */
class FuelEngineComponentTest extends MinecraftTestEnvironment {

    private static final FuelEngineProfile PROFILE = FuelEngineProfile.of(10, 40, 250.0, 0.25);

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final Function<Fluid, FuelEngineFuel> fuelLookup =
            f -> f == Fluids.LAVA ? new FuelEngineFuel(40_000, 3.0) : null;
    private final Function<Fluid, FuelEngineCoolant> coolantLookup =
            f -> f == Fluids.WATER ? new FuelEngineCoolant(4.0, 6.0) : null;

    private EngineEnergyOutputComponent energy(long capacity, long amount) {
        EngineEnergyOutputComponent e = new EngineEnergyOutputComponent("energy", () -> capacity, () -> {});
        e.setAmount(amount);
        return e;
    }

    private FluidStoreComponent tank(String id, @Nullable Fluid fluid, long mb) {
        FluidStoreComponent s = new FluidStoreComponent(id, FluidUnits.mb(4000), () -> {});
        if (fluid != null && mb > 0) {
            s.tank().setContents(SimpleFluidKey.of(fluid), FluidUnits.mb(mb));
        }
        return s;
    }

    private FuelEngineComponent engine(
            EngineEnergyOutputComponent e,
            FluidStoreComponent fuel,
            FluidStoreComponent coolant,
            BooleanSupplier powered,
            FuelEngineProfile profile) {
        return new FuelEngineComponent(
                "fuel", e, fuel, coolant, fuelLookup, coolantLookup, powered, profile, () -> {});
    }

    private long mb(long millibuckets) {
        return FluidUnits.mb(millibuckets);
    }

    @Test
    void generatesUpToMaxWhenBufferHasRoom() {
        EngineEnergyOutputComponent e = energy(100_000, 0);
        FuelEngineComponent fuel = engine(e, tank("fuel", Fluids.LAVA, 300), tank("cool", null, 0), () -> true, PROFILE);

        fuel.serverTick(new FakeMachineContext());

        assertThat(fuel.lastGenerationRate()).isEqualTo(40);
        assertThat(e.getAmount()).isEqualTo(40);
        assertThat(fuel.committedFuelEnergy()).isEqualTo(4_000 - 40);
        assertThat(fuel.temperature()).isEqualTo(3.0); // base heat only (nothing wasted)
    }

    @Test
    void throttlesToBufferFreeSpaceBetweenMinAndMax() {
        EngineEnergyOutputComponent e = energy(100, 75); // 25 free
        FuelEngineComponent fuel = engine(e, tank("fuel", Fluids.LAVA, 300), tank("cool", null, 0), () -> true, PROFILE);

        fuel.serverTick(new FakeMachineContext());

        assertThat(fuel.lastGenerationRate()).isEqualTo(25);
        assertThat(e.getAmount()).isEqualTo(100);
    }

    @Test
    void fullBufferReleasesMinWastesRemainderAndReducesReserveByReleased() {
        EngineEnergyOutputComponent e = energy(100, 100); // full
        FuelEngineComponent fuel = engine(e, tank("fuel", Fluids.LAVA, 300), tank("cool", null, 0), () -> true, PROFILE);

        fuel.serverTick(new FakeMachineContext());

        assertThat(fuel.lastGenerationRate()).isEqualTo(10); // still attempts MIN
        assertThat(e.getAmount()).isEqualTo(100); // nothing accepted (rejected RF is not buffered)
        assertThat(fuel.committedFuelEnergy()).isEqualTo(4_000 - 10); // reduced by RELEASED, not accepted
        assertThat(fuel.temperature()).isEqualTo(5.5); // 3.0 base + 10 wasted * 0.25
    }

    @Test
    void missingWaterKeepsBurningAndHeatsUp() {
        EngineEnergyOutputComponent e = energy(100, 100); // full → wastes → heats
        FuelEngineComponent fuel = engine(e, tank("fuel", Fluids.LAVA, 300), tank("cool", null, 0), () -> true, PROFILE);

        for (int i = 0; i < 5; i++) {
            fuel.serverTick(new FakeMachineContext());
        }

        assertThat(fuel.isRunning(new FakeMachineContext())).isTrue();
        assertThat(fuel.temperature()).isGreaterThan(0);
        assertThat(fuel.committedFuelEnergy()).isLessThan(4_000);
    }

    @Test
    void waterCoolingRemovesHeatUpToRateAndSpendsCapacity() {
        // A hot engine (seeded via load) with water present: commits a 400-unit batch, removes up to 6/t.
        EngineEnergyOutputComponent e = energy(100_000, 0);
        FuelEngineComponent fuel = engine(e, tank("fuel", null, 0), tank("cool", Fluids.WATER, 500), () -> true, PROFILE);
        CompoundTag seed = new CompoundTag();
        seed.putDouble("Temperature", 200.0);
        fuel.load(seed, registries);

        fuel.serverTick(new FakeMachineContext()); // no fuel → idle cooling of the seeded heat

        assertThat(fuel.committedCoolingCapacity()).isEqualTo(400 - 6, within(1e-6)); // 4/mB*100 = 400, spent 6/t
        assertThat(fuel.temperature()).isEqualTo(200 - 6 - 0.5, within(1e-6)); // active 6 + passive 0.5
    }

    @Test
    void overheatShutsDownDiscardsReserveButKeepsTankFuel() {
        FuelEngineProfile low = FuelEngineProfile.of(10, 40, 20.0, 0.25);
        EngineEnergyOutputComponent e = energy(100, 100); // full → 5.5 heat/t
        FluidStoreComponent fuelTank = tank("fuel", Fluids.LAVA, 300);
        FuelEngineComponent fuel = engine(e, fuelTank, tank("cool", null, 0), () -> true, low);

        for (int i = 0; i < 10 && !fuel.thermallyShutDown(); i++) {
            fuel.serverTick(new FakeMachineContext());
        }

        assertThat(fuel.thermallyShutDown()).isTrue();
        assertThat(fuel.committedFuelEnergy()).isZero();
        assertThat(fuelTank.tank().getAmount()).isEqualTo(mb(200)); // one 100 mB batch was committed, rest preserved
    }

    @Test
    void wrenchResetCapsTemperatureAndDoesNotRestoreReserve() {
        FuelEngineProfile low = FuelEngineProfile.of(10, 40, 20.0, 0.25);
        EngineEnergyOutputComponent e = energy(100, 100);
        FuelEngineComponent fuel = engine(e, tank("fuel", Fluids.LAVA, 300), tank("cool", null, 0), () -> true, low);
        MachineContext ctx = new FakeMachineContext();
        for (int i = 0; i < 10 && !fuel.thermallyShutDown(); i++) {
            fuel.serverTick(ctx);
        }

        assertThat(fuel.resetOverheat(ctx)).isTrue();

        assertThat(fuel.thermallyShutDown()).isFalse();
        assertThat(fuel.temperature()).isEqualTo(15.0); // 20 * 0.75, not zero
        assertThat(fuel.committedFuelEnergy()).isZero(); // discarded reserve is not restored
    }

    @Test
    void wrenchCannotBypassCooling_reOverheatsWithoutCoolant() {
        FuelEngineProfile low = FuelEngineProfile.of(10, 40, 20.0, 0.25);
        EngineEnergyOutputComponent e = energy(100, 100);
        FuelEngineComponent fuel = engine(e, tank("fuel", Fluids.LAVA, 400), tank("cool", null, 0), () -> true, low);
        MachineContext ctx = new FakeMachineContext();
        for (int i = 0; i < 10 && !fuel.thermallyShutDown(); i++) {
            fuel.serverTick(ctx);
        }
        fuel.resetOverheat(ctx);

        for (int i = 0; i < 10 && !fuel.thermallyShutDown(); i++) {
            fuel.serverTick(ctx);
        }
        assertThat(fuel.thermallyShutDown()).isTrue(); // re-trips quickly without coolant
    }

    @Test
    void redstoneOffPausesPreservingCommittedFuel() {
        EngineEnergyOutputComponent e = energy(100_000, 0);
        boolean[] powered = {true};
        FuelEngineComponent fuel =
                engine(e, tank("fuel", Fluids.LAVA, 300), tank("cool", null, 0), () -> powered[0], PROFILE);
        fuel.serverTick(new FakeMachineContext());
        long committedAfterBurn = fuel.committedFuelEnergy();
        long energyAfterBurn = e.getAmount();

        powered[0] = false;
        fuel.serverTick(new FakeMachineContext());

        assertThat(fuel.committedFuelEnergy()).isEqualTo(committedAfterBurn); // reserve preserved
        assertThat(e.getAmount()).isEqualTo(energyAfterBurn); // no RF generated
        assertThat(fuel.lastGenerationRate()).isZero();
        assertThat(fuel.temperature()).isEqualTo(3.0 - 0.5, within(1e-6)); // only passive cooling
    }

    @Test
    void idleDoesNotCommitCoolantBelowThreshold() {
        EngineEnergyOutputComponent e = energy(100_000, 0);
        FluidStoreComponent cool = tank("cool", Fluids.WATER, 500);
        FuelEngineComponent fuel = engine(e, tank("fuel", null, 0), cool, () -> true, PROFILE);
        CompoundTag seed = new CompoundTag();
        seed.putDouble("Temperature", 50.0); // below PASSIVE_COOLING_TARGET (125)
        fuel.load(seed, registries);

        fuel.serverTick(new FakeMachineContext());

        assertThat(fuel.committedCoolingCapacity()).isZero(); // no fresh batch committed
        assertThat(cool.tank().getAmount()).isEqualTo(mb(500)); // water untouched
        assertThat(fuel.temperature()).isEqualTo(49.5, within(1e-6)); // passive cooling only
    }

    @Test
    void committedCoolantIdentityRetainedWhenTankFluidChanges() {
        // Water rate 6; a hypothetical faster coolant rate 30. Once water is committed, the reserve
        // keeps water's rate even if the external tank is swapped to the faster coolant.
        Function<Fluid, FuelEngineCoolant> lookup = f -> f == Fluids.WATER
                ? new FuelEngineCoolant(4.0, 6.0)
                : f == Fluids.LAVA ? new FuelEngineCoolant(4.0, 30.0) : null;
        EngineEnergyOutputComponent e = energy(100_000, 0);
        FluidStoreComponent cool = tank("cool", Fluids.WATER, 500);
        FuelEngineComponent fuel = new FuelEngineComponent(
                "fuel", e, tank("fuel", null, 0), cool, fuelLookup, lookup, () -> true, PROFILE, () -> {});
        CompoundTag seed = new CompoundTag();
        seed.putDouble("Temperature", 200.0);
        fuel.load(seed, registries);

        fuel.serverTick(new FakeMachineContext()); // commits water (rate 6), spends 6
        double afterFirst = fuel.committedCoolingCapacity();
        cool.tank().setContents(SimpleFluidKey.of(Fluids.LAVA), mb(500)); // swap tank to the faster coolant

        fuel.serverTick(new FakeMachineContext());

        // Still spent at water's rate (6), not lava's (30).
        assertThat(afterFirst - fuel.committedCoolingCapacity()).isEqualTo(6.0, within(1e-6));
    }

    @Test
    void persistenceRoundTripsAllState() {
        EngineEnergyOutputComponent e = energy(100_000, 0);
        FuelEngineComponent writer =
                engine(e, tank("fuel", Fluids.LAVA, 300), tank("cool", Fluids.WATER, 500), () -> true, PROFILE);
        for (int i = 0; i < 3; i++) {
            writer.serverTick(new FakeMachineContext());
        }

        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);

        FuelEngineComponent reader =
                engine(energy(100_000, 0), tank("fuel", null, 0), tank("cool", null, 0), () -> true, PROFILE);
        reader.load(tag, registries);

        assertThat(reader.temperature()).isEqualTo(writer.temperature(), within(1e-6));
        assertThat(reader.committedFuelEnergy()).isEqualTo(writer.committedFuelEnergy());
        assertThat(reader.committedCoolingCapacity()).isEqualTo(writer.committedCoolingCapacity(), within(1e-6));
        assertThat(reader.thermallyShutDown()).isEqualTo(writer.thermallyShutDown());
        // Synced to the client to drive piston-speed rendering.
        assertThat(reader.lastGenerationRate()).isEqualTo(writer.lastGenerationRate());
        assertThat(writer.lastGenerationRate()).isGreaterThan(0);
    }
}
