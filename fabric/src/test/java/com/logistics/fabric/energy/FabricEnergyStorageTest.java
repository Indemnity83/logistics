package com.logistics.fabric.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.reborn.energy.api.EnergyStorage;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fabric energy storage adapters")
class FabricEnergyStorageTest {

    @Test
    @DisplayName("FabricEnergyStorage should ignore non-positive transfer requests")
    void fabricEnergyStorage_nonPositiveTransfers_returnZeroWithoutMutation() {
        FabricEnergyStorage storage = new FabricEnergyStorage(100, 50, 50);
        storage.insert(40, false);

        assertThat(storage.insert(-10, false)).isZero();
        assertThat(storage.insert(0, false)).isZero();
        assertThat(storage.extract(-10, false)).isZero();
        assertThat(storage.extract(0, false)).isZero();
        assertThat(storage.getAmount()).isEqualTo(40);
    }

    @Test
    @DisplayName("TR adapter should ignore non-positive transfer requests")
    void trAdapter_nonPositiveTransfers_returnZeroWithoutCallingDelegate() {
        FakeEnergyStorage delegate = new FakeEnergyStorage(100, 40);
        TrToIEnergyStorageAdapter adapter = new TrToIEnergyStorageAdapter(delegate);

        assertThat(adapter.insert(-10, false)).isZero();
        assertThat(adapter.insert(0, false)).isZero();
        assertThat(adapter.extract(-10, false)).isZero();
        assertThat(adapter.extract(0, false)).isZero();

        assertThat(delegate.insertCalls).isZero();
        assertThat(delegate.extractCalls).isZero();
        assertThat(delegate.amount).isEqualTo(40);
    }

    @Test
    @DisplayName("TR adapter simulate paths should clamp invalid delegate state to non-negative")
    void trAdapter_simulateClampsInvalidDelegateState() {
        FakeEnergyStorage delegate = new FakeEnergyStorage(-100, -40);
        TrToIEnergyStorageAdapter adapter = new TrToIEnergyStorageAdapter(delegate);

        assertThat(adapter.insert(10, true)).isZero();
        assertThat(adapter.extract(10, true)).isZero();
    }

    private static final class FakeEnergyStorage implements EnergyStorage {
        private final long capacity;
        private long amount;
        private int insertCalls;
        private int extractCalls;

        private FakeEnergyStorage(long capacity, long amount) {
            this.capacity = capacity;
            this.amount = amount;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            insertCalls++;
            long inserted = Math.min(maxAmount, Math.max(0, capacity - amount));
            amount += inserted;
            return inserted;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            extractCalls++;
            long extracted = Math.min(maxAmount, Math.max(0, amount));
            amount -= extracted;
            return extracted;
        }

        @Override
        public long getAmount() {
            return amount;
        }

        @Override
        public long getCapacity() {
            return capacity;
        }
    }
}
