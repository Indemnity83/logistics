package com.logistics.fabric.energy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.reborn.energy.api.base.SimpleEnergyStorage;

@DisplayName("Team Reborn energy adapter")
class TrToIEnergyStorageAdapterTest {

    /** Big buffer, small per-operation transfer rate. */
    private static SimpleEnergyStorage rateLimited(long amount) {
        SimpleEnergyStorage storage = new SimpleEnergyStorage(100_000, 32, 32);
        storage.amount = amount;
        return storage;
    }

    @Test
    @DisplayName("simulated insert reports the machine's input rate, not its free room")
    void insertSimulate_respectsPerOperationInputRate() {
        SimpleEnergyStorage storage = rateLimited(0);
        TrToIEnergyStorageAdapter adapter = new TrToIEnergyStorageAdapter(storage);

        assertThat(adapter.insert(1_000, true)).isEqualTo(32);
        assertThat(storage.amount).as("simulate must not mutate the storage").isZero();
    }

    @Test
    @DisplayName("simulated insert matches what a committed insert accepts")
    void insertSimulate_matchesCommit() {
        SimpleEnergyStorage storage = rateLimited(0);
        TrToIEnergyStorageAdapter adapter = new TrToIEnergyStorageAdapter(storage);

        long simulated = adapter.insert(1_000, true);
        long committed = adapter.insert(1_000, false);

        assertThat(simulated).isEqualTo(committed);
        assertThat(storage.amount).isEqualTo(committed);
    }

    @Test
    @DisplayName("simulated extract reports the machine's output rate, not its stored amount")
    void extractSimulate_respectsPerOperationOutputRate() {
        SimpleEnergyStorage storage = rateLimited(10_000);
        TrToIEnergyStorageAdapter adapter = new TrToIEnergyStorageAdapter(storage);

        assertThat(adapter.extract(1_000, true)).isEqualTo(32);
        assertThat(storage.amount).as("simulate must not mutate the storage").isEqualTo(10_000);

        assertThat(adapter.extract(1_000, false)).isEqualTo(32);
        assertThat(storage.amount).isEqualTo(10_000 - 32);
    }

    @Test
    @DisplayName("simulated insert still reports free room once the storage is full")
    void insertSimulate_fullStorageAcceptsNothing() {
        SimpleEnergyStorage storage = rateLimited(100_000);
        TrToIEnergyStorageAdapter adapter = new TrToIEnergyStorageAdapter(storage);

        assertThat(adapter.insert(1_000, true)).isZero();
        assertThat(adapter.insert(1_000, false)).isZero();
    }
}
