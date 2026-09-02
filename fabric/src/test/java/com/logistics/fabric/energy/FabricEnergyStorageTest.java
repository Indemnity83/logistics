package com.logistics.fabric.energy;

import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.reborn.energy.api.EnergyStorage;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fabric energy storage adapters")
class FabricEnergyStorageTest {

    private static EnergyComponent battery(long capacity, long amount) {
        EnergyComponent component = new EnergyComponent(capacity, capacity, capacity, () -> {});
        component.setAmount(amount);
        return component;
    }

    private static EnergyStorageAccess.SimulateBooleanAdapter adapter(IEnergyStorage storage) {
        return EnergyStorageAccess.adapterFor(new Object(), null, storage);
    }

    @Test
    @DisplayName("SimulateBooleanAdapter forwards energy through a zero-capacity conduit (cable parity)")
    void simulateBooleanAdapter_forwardsThroughZeroCapacityConduit() {
        // Mirrors the Fabric engine-push path: target.insert(amount, tx); tx.commit().
        // A cable reports capacity 0 but forwards inserts into its network, and the
        // Fabric bridge delegates without a capacity gate — so this works on Fabric
        // (unlike NeoForge before its fix).
        FakeConduit conduit = new FakeConduit(80);
        EnergyStorageAccess.SimulateBooleanAdapter adapter = adapter(conduit);

        try (Transaction tx = Transaction.openOuter()) {
            assertThat(adapter.insert(200L, tx)).isEqualTo(80);
            assertThat(conduit.forwarded).isZero(); // not committed yet
            tx.commit();
        }

        assertThat(conduit.forwarded).isEqualTo(80);
    }

    @Test
    @DisplayName("A nested commit inside an aborted transaction must not reach the storage")
    void nestedCommit_thenOuterAbort_leavesStorageUntouched() {
        // EnergyStorageUtil.move() always wraps its work in a nested transaction and commits it.
        // If that nested commit writes through, a later abort of the enclosing transaction cannot
        // undo it and the energy is duplicated.
        EnergyComponent storage = battery(10_000, 0);
        EnergyStorageAccess.SimulateBooleanAdapter adapter = adapter(storage);

        try (Transaction outer = Transaction.openOuter()) {
            try (Transaction nested = outer.openNested()) {
                assertThat(adapter.insert(1000, nested)).isEqualTo(1000);
                nested.commit();
            }
            assertThat(storage.getAmount()).isZero(); // staged, not applied
            outer.abort();
        }

        assertThat(storage.getAmount()).isZero();
    }

    @Test
    @DisplayName("A nested commit is applied once, when the outermost transaction commits")
    void nestedCommit_thenOuterCommit_appliesOnceAtOuterClose() {
        EnergyComponent storage = battery(10_000, 0);
        EnergyStorageAccess.SimulateBooleanAdapter adapter = adapter(storage);

        try (Transaction outer = Transaction.openOuter()) {
            try (Transaction nested = outer.openNested()) {
                assertThat(adapter.insert(1000, nested)).isEqualTo(1000);
                nested.commit();
            }
            assertThat(storage.getAmount()).isZero(); // deferred to the outer close
            outer.commit();
        }

        assertThat(storage.getAmount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("An aborted nested transaction rolls back even when the outer one commits")
    void nestedAbort_thenOuterCommit_appliesNothing() {
        EnergyComponent storage = battery(10_000, 500);
        EnergyStorageAccess.SimulateBooleanAdapter adapter = adapter(storage);

        try (Transaction outer = Transaction.openOuter()) {
            try (Transaction nested = outer.openNested()) {
                assertThat(adapter.extract(500, nested)).isEqualTo(500);
                nested.abort();
            }
            outer.commit();
        }

        assertThat(storage.getAmount()).isEqualTo(500);
    }

    @Test
    @DisplayName("Repeated inserts in one transaction must not offer more room than exists")
    void repeatedInserts_inOneTransaction_accountForStagedEnergy() {
        EnergyComponent storage = battery(150, 0);
        EnergyStorageAccess.SimulateBooleanAdapter adapter = adapter(storage);

        try (Transaction tx = Transaction.openOuter()) {
            assertThat(adapter.insert(100, tx)).isEqualTo(100);
            assertThat(adapter.insert(100, tx)).isEqualTo(50); // only 50 RF of room is left
            tx.commit();
        }

        assertThat(storage.getAmount()).isEqualTo(150);
    }

    @Test
    @DisplayName("Repeated extracts in one transaction must not offer more energy than exists")
    void repeatedExtracts_inOneTransaction_accountForStagedEnergy() {
        EnergyComponent storage = battery(1000, 150);
        EnergyStorageAccess.SimulateBooleanAdapter adapter = adapter(storage);

        try (Transaction tx = Transaction.openOuter()) {
            assertThat(adapter.extract(100, tx)).isEqualTo(100);
            assertThat(adapter.extract(100, tx)).isEqualTo(50); // only 50 RF is left
            tx.commit();
        }

        assertThat(storage.getAmount()).isZero();
    }

    @Test
    @DisplayName("Two lookups of the same block and side share one staging participant")
    void twoLookups_ofSameBlockAndSide_doNotDoubleAccept() {
        // A network visiting one machine from two faces looks the capability up twice; two
        // independent participants would each simulate against unmodified state and over-accept.
        Object blockEntity = new Object();
        EnergyComponent storage = battery(150, 0);

        EnergyStorage first = EnergyStorageAccess.adapterFor(blockEntity, Direction.NORTH, storage);
        EnergyStorage second = EnergyStorageAccess.adapterFor(blockEntity, Direction.NORTH, storage);
        assertThat(second).isSameAs(first);

        try (Transaction tx = Transaction.openOuter()) {
            assertThat(first.insert(100, tx)).isEqualTo(100);
            assertThat(second.insert(100, tx)).isEqualTo(50);
            tx.commit();
        }

        assertThat(storage.getAmount()).isEqualTo(150);
    }

    @Test
    @DisplayName("Different blocks and different sides get their own staging participants")
    void lookups_ofDifferentBlocksOrSides_areNotShared() {
        Object blockEntity = new Object();
        Object otherBlockEntity = new Object();
        EnergyComponent storage = battery(150, 0);

        EnergyStorage north = EnergyStorageAccess.adapterFor(blockEntity, Direction.NORTH, storage);
        EnergyStorage south = EnergyStorageAccess.adapterFor(blockEntity, Direction.SOUTH, storage);
        EnergyStorage other = EnergyStorageAccess.adapterFor(otherBlockEntity, Direction.NORTH, storage);

        assertThat(south).isNotSameAs(north);
        assertThat(other).isNotSameAs(north);
    }

    @Test
    @DisplayName("Non-positive transaction transfers are ignored")
    void simulateBooleanAdapter_nonPositiveTransactionTransfers_returnZero() {
        EnergyComponent storage = battery(1000, 500);
        EnergyStorageAccess.SimulateBooleanAdapter adapter = adapter(storage);

        try (Transaction tx = Transaction.openOuter()) {
            assertThat(adapter.insert(0, tx)).isZero();
            assertThat(adapter.insert(-10, tx)).isZero();
            assertThat(adapter.extract(0, tx)).isZero();
            assertThat(adapter.extract(-10, tx)).isZero();
            tx.commit();
        }

        assertThat(storage.getAmount()).isEqualTo(500);
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
}
