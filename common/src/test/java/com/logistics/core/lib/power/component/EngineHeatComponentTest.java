package com.logistics.core.lib.power.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.Test;

class EngineHeatComponentTest extends MinecraftTestEnvironment {

    private EngineEnergyOutputComponent energy(long capacity) {
        return new EngineEnergyOutputComponent("energy", () -> capacity, () -> {});
    }

    private EngineHeatComponent heat(
            EngineEnergyOutputComponent energy, boolean canOverheat, boolean running) {
        return new EngineHeatComponent(
                "heat", energy, 20, 250, canOverheat, 10, null, () -> running, () -> false, () -> {});
    }

    @Test
    void temperature_tracksBufferFullness() {
        EngineEnergyOutputComponent energy = energy(1000);
        energy.setAmount(500);
        EngineHeatComponent heat = heat(energy, true, true);

        heat.serverTick(new FakeMachineContext());

        // (250 - 20) * 0.5 + 20
        assertThat(heat.temperature()).isEqualTo(135.0);
        assertThat(heat.stage()).isEqualTo(HeatStage.WARM);
    }

    @Test
    void decaysEnergyWhenNotRunning() {
        EngineEnergyOutputComponent energy = energy(1000);
        energy.setAmount(500);
        EngineHeatComponent heat = heat(energy, true, /* running */ false);

        heat.serverTick(new FakeMachineContext());

        assertThat(energy.getAmount()).isEqualTo(490);
    }

    @Test
    void doesNotDecayWhenRunning() {
        EngineEnergyOutputComponent energy = energy(1000);
        energy.setAmount(500);
        EngineHeatComponent heat = heat(energy, true, /* running */ true);

        heat.serverTick(new FakeMachineContext());

        assertThat(energy.getAmount()).isEqualTo(500);
    }

    @Test
    void overheatsOnFullBufferThenBleedsEnergy() {
        EngineEnergyOutputComponent energy = energy(1000);
        energy.setAmount(1000);
        EngineHeatComponent heat = heat(energy, /* canOverheat */ true, /* running */ true);
        FakeMachineContext ctx = new FakeMachineContext();

        // First tick reaches the OVERHEAT stage (isOverheated() lags one tick, as in the classic tick).
        heat.serverTick(ctx);
        assertThat(heat.stage()).isEqualTo(HeatStage.OVERHEAT);
        assertThat(heat.isOverheated()).isTrue();

        // Next tick bleeds 50 RF and does nothing else.
        heat.serverTick(ctx);
        assertThat(energy.getAmount()).isEqualTo(950);
    }

    @Test
    void nonOverheatingEngineStaysHotAtFullBuffer() {
        EngineEnergyOutputComponent energy = energy(1000);
        energy.setAmount(1000);
        EngineHeatComponent heat = heat(energy, /* canOverheat */ false, /* running */ true);

        heat.serverTick(new FakeMachineContext());

        assertThat(heat.stage()).isEqualTo(HeatStage.HOT);
        assertThat(heat.isOverheated()).isFalse();
    }

    @Test
    void resetOverheat_clearsStateWhenOverheated() {
        EngineEnergyOutputComponent energy = energy(1000);
        energy.setAmount(1000);
        EngineHeatComponent heat = heat(energy, true, true);
        FakeMachineContext ctx = new FakeMachineContext();
        heat.serverTick(ctx);

        assertThat(heat.resetOverheat(ctx)).isTrue();
        assertThat(heat.stage()).isEqualTo(HeatStage.COLD);
        assertThat(energy.getAmount()).isEqualTo(0);
        assertThat(heat.resetOverheat(ctx)).isFalse();
    }
}
