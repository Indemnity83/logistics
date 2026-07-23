package com.logistics.core.lib.power.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.lib.power.fuel.CoolantSource;
import com.logistics.core.lib.power.fuel.FuelSource;
import com.logistics.core.lib.power.gen.FixedGeneration;
import com.logistics.core.lib.power.gen.Generation;
import com.logistics.core.machine.FakeMachineContext;
import com.logistics.core.machine.MachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.Test;

class EngineBurnComponentTest extends MinecraftTestEnvironment {

    /** A fuel source that yields one 3-tick burn, then nothing. */
    private static final class SingleShotFuel implements FuelSource {
        int ignitions;

        @Override
        public int ignite(MachineContext ctx) {
            return ignitions++ == 0 ? 3 : 0;
        }
    }

    private EngineEnergyOutputComponent energy() {
        return new EngineEnergyOutputComponent("energy", () -> 10_000L, () -> {});
    }

    private EngineHeatComponent heat(EngineEnergyOutputComponent energy) {
        return new EngineHeatComponent("heat", energy, 20, 250, true, 10, null, () -> false, () -> false, () -> {});
    }

    @Test
    void burnsFuelAndGeneratesEnergyWhilePowered() {
        EngineEnergyOutputComponent energy = energy();
        EngineBurnComponent burn = new EngineBurnComponent(
                "burn", energy, heat(energy), () -> true, new SingleShotFuel(), new FixedGeneration(5),
                CoolantSource.NONE, (ctx, lit) -> {}, () -> {});
        FakeMachineContext ctx = new FakeMachineContext();

        burn.serverTick(ctx); // ignites (burnTime 3), generates 5
        assertThat(burn.burnTime()).isEqualTo(3);
        assertThat(burn.fuelTime()).isEqualTo(3);
        assertThat(energy.getAmount()).isEqualTo(5);

        burn.serverTick(ctx); // burnTime 2, +5
        burn.serverTick(ctx); // burnTime 1, +5
        assertThat(energy.getAmount()).isEqualTo(15);
    }

    @Test
    void extinguishesWhenUnpowered() {
        EngineEnergyOutputComponent energy = energy();
        EngineBurnComponent burn = new EngineBurnComponent(
                "burn", energy, heat(energy), () -> false, new SingleShotFuel(), new FixedGeneration(5),
                CoolantSource.NONE, (ctx, lit) -> {}, () -> {});

        burn.serverTick(new FakeMachineContext());
        assertThat(burn.burnTime()).isEqualTo(0);
        assertThat(energy.getAmount()).isEqualTo(0);
    }

    @Test
    void togglesLitWithBurnState() {
        EngineEnergyOutputComponent energy = energy();
        boolean[] lit = {false};
        EngineComponent.LitController controller = (ctx, on) -> lit[0] = on;
        EngineBurnComponent burn = new EngineBurnComponent(
                "burn", energy, heat(energy), () -> true, new SingleShotFuel(), new FixedGeneration(5),
                CoolantSource.NONE, controller, () -> {});
        FakeMachineContext ctx = new FakeMachineContext();

        burn.serverTick(ctx);
        assertThat(lit[0]).isTrue();

        // Burn out (3 ticks total), then a dry tick with no fuel left → lit off.
        burn.serverTick(ctx);
        burn.serverTick(ctx);
        burn.serverTick(ctx);
        assertThat(lit[0]).isFalse();
    }

    @Test
    void resetsGenerationOnShutdown() {
        EngineEnergyOutputComponent energy = energy();
        boolean[] reset = {false};
        Generation generation = new Generation() {
            @Override
            public long generate(MachineContext ctx, double temperature, long stored, long capacity) {
                return 5;
            }

            @Override
            public void reset() {
                reset[0] = true;
            }
        };
        EngineBurnComponent burn = new EngineBurnComponent(
                "burn", energy, heat(energy), () -> true, new SingleShotFuel(), generation,
                CoolantSource.NONE, (ctx, lit) -> {}, () -> {});
        FakeMachineContext ctx = new FakeMachineContext();

        // Run until fuel exhausts; the running→stopped transition resets generation.
        for (int i = 0; i < 4; i++) {
            burn.serverTick(ctx);
        }
        assertThat(reset[0]).isTrue();
    }
}
