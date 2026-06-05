package com.logistics.fabric.energy;

import com.logistics.core.lib.energy.IEnergyStorage;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.reborn.energy.api.EnergyStorage;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fabric energy storage adapters")
class FabricEnergyStorageTest {

    @Test
    @DisplayName("SimulateBooleanAdapter forwards energy through a zero-capacity conduit (cable parity)")
    void simulateBooleanAdapter_forwardsThroughZeroCapacityConduit() {
        // Mirrors the Fabric engine-push path: target.insert(amount, tx); tx.commit().
        // A cable reports capacity 0 but forwards inserts into its network, and the
        // Fabric bridge delegates without a capacity gate — so this works on Fabric
        // (unlike NeoForge before its fix).
        FakeConduit conduit = new FakeConduit(80);
        EnergyStorageAccess.SimulateBooleanAdapter adapter = new EnergyStorageAccess.SimulateBooleanAdapter(conduit);

        FakeTransactionContext tx = new FakeTransactionContext();
        long accepted = adapter.insert(200L, tx);

        assertThat(accepted).isEqualTo(80);
        assertThat(conduit.forwarded).isZero(); // not committed yet

        tx.commit();
        assertThat(conduit.forwarded).isEqualTo(80);
    }

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

    /**
     * Models a cable: a bufferless conduit that reports zero capacity/amount but
     * forwards inserted energy into its network (capped at {@code networkRoom}).
     */
    private static final class FakeConduit implements IEnergyStorage {
        private final long networkRoom;
        private long forwarded;

        private FakeConduit(long networkRoom) {
            this.networkRoom = networkRoom;
        }

        @Override
        public long insert(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) return 0;
            long accepted = Math.min(maxAmount, networkRoom);
            if (!simulate) forwarded += accepted;
            return accepted;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return 0;
        }

        @Override
        public long getAmount() {
            return 0;
        }

        @Override
        public long getCapacity() {
            return 0;
        }

        @Override
        public boolean canInsert() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return false;
        }
    }

    /**
     * Minimal {@link TransactionContext} that captures close callbacks so a unit
     * test can drive Team Reborn's deferred-commit insert without bootstrapping the
     * Fabric environment. {@link #commit()} fires the callbacks as a committed root.
     */
    private static final class FakeTransactionContext implements TransactionContext {
        private final List<CloseCallback> callbacks = new ArrayList<>();

        void commit() {
            for (CloseCallback callback : callbacks) {
                callback.onClose(this, Result.COMMITTED);
            }
            callbacks.clear();
        }

        @Override
        public void addCloseCallback(CloseCallback callback) {
            callbacks.add(callback);
        }

        @Override
        public void addOuterCloseCallback(OuterCloseCallback callback) {}

        @Override
        public int nestingDepth() {
            return 0;
        }

        @Override
        public Transaction openNested() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Transaction getOpenTransaction(int nestingDepth) {
            throw new UnsupportedOperationException();
        }
    }
}
