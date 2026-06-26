package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.Test;

class EnergyStorageComponentTest extends MinecraftTestEnvironment {

    private EnergyStorageComponent buffer() {
        return new EnergyStorageComponent("energy", 1_000, 128, 0, () -> {});
    }

    @Test
    void demandReflectsRoomAndRemainingInput() {
        EnergyStorageComponent energy = buffer();

        // Empty buffer: demand is limited by max input (128), not the 1000 of room.
        assertThat(energy.networkDemandPerTick()).isEqualTo(128);
    }

    @Test
    void receivedEnergyReducesDemandUntilTickReset() {
        EnergyStorageComponent energy = buffer();
        FakeMachineContext ctx = new FakeMachineContext();

        energy.energy(null).insert(50, false);
        assertThat(energy.amount()).isEqualTo(50);
        assertThat(energy.networkDemandPerTick()).isEqualTo(128 - 50);

        // Tick resets the received-this-tick counter; demand returns to the input cap.
        energy.serverTick(ctx);
        assertThat(energy.networkDemandPerTick()).isEqualTo(128);
    }

    @Test
    void demandClampedByRemainingRoom() {
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100, 128, 0, () -> {});

        energy.energy(null).insert(100, false); // fill to capacity
        energy.serverTick(new FakeMachineContext()); // clear received counter
        assertThat(energy.networkDemandPerTick()).isZero();
    }

    @Test
    void demandIsZeroWhenInputDisabled() {
        // A buffer that accepts no input (maxInsert == 0) reports no demand — the natural opt-out.
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 1_000, 0, 0, () -> {});
        assertThat(energy.networkDemandPerTick()).isZero();
    }

    @Test
    void consumeReducesStoredEnergy() {
        EnergyStorageComponent energy = buffer();
        energy.energy(null).insert(100, false);
        energy.consume(30);
        assertThat(energy.amount()).isEqualTo(70);
    }
}
