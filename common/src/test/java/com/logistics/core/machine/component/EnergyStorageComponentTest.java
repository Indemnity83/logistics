package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void worldFacingViewNeverExtracts() {
        // A host with a non-zero maxOutput -- the Power Junction is the only one -- still exposes an
        // insert-only capability, so a cable network cannot list it as a source and drain it.
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 1_000, 128, 1_000, () -> {});
        energy.energy(null).insert(128, false);

        assertThat(energy.energy(null).canExtract()).isFalse();
        assertThat(energy.energy(null).extract(128, true)).isZero();
        assertThat(energy.energy(null).extract(128, false)).isZero();
        assertThat(energy.amount()).isEqualTo(128);
    }

    @Test
    void networkViewExtractsUpToMaxOutput() {
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 1_000, 128, 100, () -> {});
        energy.energy(null).insert(128, false);

        assertThat(energy.networkEnergyStorage().extract(128, false)).isEqualTo(100);
        assertThat(energy.amount()).isEqualTo(28);
    }

    @Test
    void consumeReducesStoredEnergy() {
        EnergyStorageComponent energy = buffer();
        energy.energy(null).insert(100, false);
        energy.consume(30);
        assertThat(energy.amount()).isEqualTo(70);
    }

    @Test
    void resizeGrowKeepsStoredEnergyAndRaisesTheCeiling() {
        int[] changes = {0};
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100, 1_000, 0, () -> changes[0]++);
        energy.energy(null).insert(100, false);
        changes[0] = 0;

        energy.resize(500, 1_000);

        assertThat(energy.capacity()).isEqualTo(500);
        assertThat(energy.energy(null).getCapacity()).isEqualTo(500);
        assertThat(energy.amount()).isEqualTo(100);
        assertThat(energy.energy(null).insert(1_000, false)).isEqualTo(400);
        assertThat(changes[0]).isGreaterThanOrEqualTo(1);
    }

    @Test
    void resizeShrinkClampsStoredEnergy() {
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 1_000, 1_000, 0, () -> {});
        energy.energy(null).insert(800, false);

        energy.resize(300, 1_000);

        assertThat(energy.amount()).isEqualTo(300);
        assertThat(energy.networkDemandPerTick()).isZero();
    }

    @Test
    void resizeMaxInsertChangesInsertLimitAndDemand() {
        EnergyStorageComponent energy = buffer();
        FakeMachineContext ctx = new FakeMachineContext();

        energy.resize(1_000, 40);

        assertThat(energy.maxInsert()).isEqualTo(40);
        assertThat(energy.networkDemandPerTick()).isEqualTo(40);
        assertThat(energy.energy(null).insert(100, false)).isEqualTo(40);
        assertThat(energy.networkDemandPerTick()).isZero();

        energy.serverTick(ctx);
        energy.resize(1_000, 300);
        assertThat(energy.networkDemandPerTick()).isEqualTo(300);
        assertThat(energy.energy(null).insert(1_000, false)).isEqualTo(300);
    }

    @Test
    void resizeToZeroCapacityEmptiesAndRefusesInput() {
        EnergyStorageComponent energy = buffer();
        energy.energy(null).insert(100, false);

        energy.resize(0, 128);

        assertThat(energy.amount()).isZero();
        assertThat(energy.networkDemandPerTick()).isZero();
        assertThat(energy.energy(null).insert(50, false)).isZero();
    }

    @Test
    void resizeToZeroMaxInsertStopsInputAndDemand() {
        EnergyStorageComponent energy = buffer();

        energy.resize(1_000, 0);

        assertThat(energy.networkDemandPerTick()).isZero();
        assertThat(energy.energy(null).canInsert()).isFalse();
        assertThat(energy.energy(null).insert(50, false)).isZero();
    }

    @Test
    void resizeToTheSameSizeDoesNotMarkDirty() {
        int[] changes = {0};
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 1_000, 128, 0, () -> changes[0]++);

        energy.resize(1_000, 128);

        assertThat(changes[0]).isZero();
    }

    @Test
    void resizeRejectsNegativeValues() {
        EnergyStorageComponent energy = buffer();

        assertThatThrownBy(() -> energy.resize(-1, 128)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> energy.resize(1_000, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(energy.capacity()).isEqualTo(1_000);
    }
}
