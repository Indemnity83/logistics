package com.logistics.neoforge.energy;

import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NeoForgeEnergyStorage adapter")
class NeoForgeEnergyStorageTest {

    private static EnergyComponent component(long capacity) {
        return new EnergyComponent(capacity, capacity, capacity, () -> {});
    }

    @Nested
    @DisplayName("null safety")
    class NullSafety {

        @Test
        @DisplayName("wrap(null) returns null")
        void wrap_null_returnsNull() {
            assertThat(NeoForgeEnergyStorage.wrap(null)).isNull();
        }

        @Test
        @DisplayName("asNeoForge(null) returns null")
        void asNeoForge_null_returnsNull() {
            assertThat(NeoForgeEnergyStorage.asNeoForge(null)).isNull();
        }
    }

    @Nested
    @DisplayName("CommonEnergyHandler (common → NeoForge)")
    class CommonEnergyHandlerTests {

        @Test
        @DisplayName("insert commits energy to backing storage on root commit")
        void insert_commitsOnRootCommit() {
            EnergyComponent backing = component(100);
            EnergyHandler handler = NeoForgeEnergyStorage.asNeoForge(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.insert(60, tx)).isEqualTo(60);
                tx.commit();
            }

            assertThat(backing.getAmount()).isEqualTo(60);
        }

        @Test
        @DisplayName("insert respects pendingDelta within a transaction (regression: insert ignored staged amount)")
        void insert_respectsPendingDelta() {
            EnergyComponent backing = component(100);
            EnergyHandler handler = NeoForgeEnergyStorage.asNeoForge(backing);

            try (Transaction tx = Transaction.openRoot()) {
                int first = handler.insert(60, tx);
                // Before the fix, second insert returned 100 (ignored the pending 60).
                // After the fix, only 40 capacity remains.
                int second = handler.insert(100, tx);
                assertThat(first).isEqualTo(60);
                assertThat(second).isEqualTo(40);
                tx.commit();
            }

            assertThat(backing.getAmount()).isEqualTo(100);
        }

        @Test
        @DisplayName("insert rollback does not mutate backing storage")
        void insert_rollback_doesNotMutate() {
            EnergyComponent backing = component(100);
            EnergyHandler handler = NeoForgeEnergyStorage.asNeoForge(backing);

            try (Transaction tx = Transaction.openRoot()) {
                handler.insert(60, tx);
                // tx closes without commit → rollback
            }

            assertThat(backing.getAmount()).isEqualTo(0);
        }

        @Test
        @DisplayName("getAmountAsLong reflects pending inserts within a transaction")
        void getAmountAsLong_reflectsPendingInserts() {
            EnergyComponent backing = component(100);
            EnergyHandler handler = NeoForgeEnergyStorage.asNeoForge(backing);

            try (Transaction tx = Transaction.openRoot()) {
                handler.insert(40, tx);
                assertThat(handler.getAmountAsLong()).isEqualTo(40);
                // rollback
            }

            assertThat(handler.getAmountAsLong()).isEqualTo(0);
        }

        @Test
        @DisplayName("extract commits energy removal on root commit")
        void extract_commitsOnRootCommit() {
            EnergyComponent backing = component(100);
            backing.insert(80, false);
            EnergyHandler handler = NeoForgeEnergyStorage.asNeoForge(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.extract(50, tx)).isEqualTo(50);
                tx.commit();
            }

            assertThat(backing.getAmount()).isEqualTo(30);
        }

        @Test
        @DisplayName("extract rollback does not mutate backing storage")
        void extract_rollback_doesNotMutate() {
            EnergyComponent backing = component(100);
            backing.insert(80, false);
            EnergyHandler handler = NeoForgeEnergyStorage.asNeoForge(backing);

            try (Transaction tx = Transaction.openRoot()) {
                handler.extract(50, tx);
                // rollback
            }

            assertThat(backing.getAmount()).isEqualTo(80);
        }

        @Test
        @DisplayName("getCapacityAsLong returns backing capacity")
        void getCapacityAsLong_returnsBackingCapacity() {
            EnergyHandler handler = NeoForgeEnergyStorage.asNeoForge(component(500));
            assertThat(handler.getCapacityAsLong()).isEqualTo(500);
        }

        @Test
        @DisplayName("non-positive transaction transfers return zero without mutating backing storage")
        void nonPositiveTransfers_returnZeroWithoutMutation() {
            EnergyComponent backing = component(100);
            backing.insert(40, false);
            EnergyHandler handler = NeoForgeEnergyStorage.asNeoForge(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.insert(-10, tx)).isZero();
                assertThat(handler.insert(0, tx)).isZero();
                assertThat(handler.extract(-10, tx)).isZero();
                assertThat(handler.extract(0, tx)).isZero();
                tx.commit();
            }

            assertThat(backing.getAmount()).isEqualTo(40);
        }

        @Test
        @DisplayName("getCapacityAsLong clamps negative backing capacity to zero")
        void getCapacityAsLong_clampsNegativeBackingCapacity() {
            EnergyHandler handler = NeoForgeEnergyStorage.asNeoForge(new EnergyComponent(() -> -100L, 50, 50, () -> {}));

            assertThat(handler.getCapacityAsLong()).isZero();
        }
    }

    @Nested
    @DisplayName("NeoForgeEnergyStorage (NeoForge → common wrapper)")
    class WrapperTests {

        @Test
        @DisplayName("insert(simulate=false) commits to backing storage")
        void insert_simulate_false_commits() {
            EnergyComponent backing = component(100);
            IEnergyStorage wrapped = NeoForgeEnergyStorage.wrap(NeoForgeEnergyStorage.asNeoForge(backing));

            assertThat(wrapped.insert(70, false)).isEqualTo(70);
            assertThat(backing.getAmount()).isEqualTo(70);
        }

        @Test
        @DisplayName("insert(simulate=true) does not mutate backing storage")
        void insert_simulate_true_doesNotMutate() {
            EnergyComponent backing = component(100);
            IEnergyStorage wrapped = NeoForgeEnergyStorage.wrap(NeoForgeEnergyStorage.asNeoForge(backing));

            assertThat(wrapped.insert(70, true)).isEqualTo(70);
            assertThat(backing.getAmount()).isEqualTo(0);
        }

        @Test
        @DisplayName("extract(simulate=false) commits removal from backing storage")
        void extract_simulate_false_commits() {
            EnergyComponent backing = component(100);
            backing.insert(80, false);
            IEnergyStorage wrapped = NeoForgeEnergyStorage.wrap(NeoForgeEnergyStorage.asNeoForge(backing));

            assertThat(wrapped.extract(50, false)).isEqualTo(50);
            assertThat(backing.getAmount()).isEqualTo(30);
        }

        @Test
        @DisplayName("extract(simulate=true) does not mutate backing storage")
        void extract_simulate_true_doesNotMutate() {
            EnergyComponent backing = component(100);
            backing.insert(80, false);
            IEnergyStorage wrapped = NeoForgeEnergyStorage.wrap(NeoForgeEnergyStorage.asNeoForge(backing));

            assertThat(wrapped.extract(50, true)).isEqualTo(50);
            assertThat(backing.getAmount()).isEqualTo(80);
        }

        @Test
        @DisplayName("non-positive wrapper transfers return zero without mutating backing storage")
        void nonPositiveTransfers_returnZeroWithoutMutation() {
            EnergyComponent backing = component(100);
            backing.insert(40, false);
            IEnergyStorage wrapped = NeoForgeEnergyStorage.wrap(NeoForgeEnergyStorage.asNeoForge(backing));

            assertThat(wrapped.insert(-10, false)).isZero();
            assertThat(wrapped.insert(0, false)).isZero();
            assertThat(wrapped.extract(-10, false)).isZero();
            assertThat(wrapped.extract(0, false)).isZero();
            assertThat(backing.getAmount()).isEqualTo(40);
        }
    }
}
