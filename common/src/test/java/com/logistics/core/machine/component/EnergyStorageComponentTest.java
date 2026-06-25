package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.FakeMachineContext;
import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.Test;

class EnergyStorageComponentTest extends MinecraftTestEnvironment {

    private EnergyStorageComponent withDemand() {
        return new EnergyStorageComponent("energy", 1_000, 128, 0, true, true, () -> {});
    }

    @Test
    void demandReflectsRoomAndRemainingInput() {
        EnergyStorageComponent energy = withDemand();

        // Empty buffer: demand is limited by max input (128), not the 1000 of room.
        assertThat(energy.networkDemandPerTick()).isEqualTo(128);
    }

    @Test
    void receivedEnergyReducesDemandUntilTickReset() {
        EnergyStorageComponent energy = withDemand();
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
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100, 128, 0, true, true, () -> {});

        energy.energy(null).insert(100, false); // fill to capacity
        energy.serverTick(new FakeMachineContext()); // clear received counter
        assertThat(energy.networkDemandPerTick()).isZero();
    }

    @Test
    void demandIsZeroWhenNotExposed() {
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 1_000, 128, 0, true, false, () -> {});
        assertThat(energy.networkDemandPerTick()).isZero();
    }

    @Test
    void consumeReducesStoredEnergy() {
        EnergyStorageComponent energy = withDemand();
        energy.energy(null).insert(100, false);
        energy.consume(30);
        assertThat(energy.amount()).isEqualTo(70);
    }
}
