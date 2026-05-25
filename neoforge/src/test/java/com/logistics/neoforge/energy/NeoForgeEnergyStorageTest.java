package com.logistics.neoforge.energy;

import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NeoForgeEnergyStorage adapter (NeoForge 21.1)")
class NeoForgeEnergyStorageTest {

    private static EnergyComponent component(long capacity) {
        return new EnergyComponent(capacity, capacity, capacity, () -> {});
    }

    private static net.neoforged.neoforge.energy.IEnergyStorage fakeStorage(int capacity, int stored) {
        int[] energy = {stored};
        return new net.neoforged.neoforge.energy.IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int toReceive = Math.min(maxReceive, capacity - energy[0]);
                if (!simulate) energy[0] += toReceive;
                return toReceive;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int toExtract = Math.min(maxExtract, energy[0]);
                if (!simulate) energy[0] -= toExtract;
                return toExtract;
            }

            @Override
            public int getEnergyStored() {
                return energy[0];
            }

            @Override
            public int getMaxEnergyStored() {
                return capacity;
            }

            @Override
            public boolean canExtract() {
                return true;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
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
    @DisplayName("CommonEnergyHandler (common → NeoForge 21.1)")
    class CommonEnergyHandlerTests {

        @Test
        @DisplayName("receiveEnergy commits energy to backing storage")
        void receiveEnergy_commitsEnergy() {
            EnergyComponent backing = component(100);
            var handler = NeoForgeEnergyStorage.asNeoForge(backing);

            int received = handler.receiveEnergy(60, false);
            assertThat(received).isEqualTo(60);
            assertThat(backing.getAmount()).isEqualTo(60);
        }

        @Test
        @DisplayName("receiveEnergy simulates without committing")
        void receiveEnergy_simulate_doesNotCommit() {
            EnergyComponent backing = component(100);
            var handler = NeoForgeEnergyStorage.asNeoForge(backing);

            int simulated = handler.receiveEnergy(60, true);
            assertThat(simulated).isEqualTo(60);
            assertThat(backing.getAmount()).isZero();
        }

        @Test
        @DisplayName("receiveEnergy clamps to remaining capacity")
        void receiveEnergy_clampsToCapacity() {
            EnergyComponent backing = component(100);
            NeoForgeEnergyStorage.asNeoForge(backing).receiveEnergy(80, false);
            var handler = NeoForgeEnergyStorage.asNeoForge(backing);

            int received = handler.receiveEnergy(100, false);
            assertThat(received).isEqualTo(20);
        }

        @Test
        @DisplayName("extractEnergy removes energy from backing storage")
        void extractEnergy_removesEnergy() {
            EnergyComponent backing = component(100);
            backing.insert(80, false);
            var handler = NeoForgeEnergyStorage.asNeoForge(backing);

            int extracted = handler.extractEnergy(50, false);
            assertThat(extracted).isEqualTo(50);
            assertThat(backing.getAmount()).isEqualTo(30);
        }

        @Test
        @DisplayName("extractEnergy simulates without committing")
        void extractEnergy_simulate_doesNotCommit() {
            EnergyComponent backing = component(100);
            backing.insert(80, false);
            var handler = NeoForgeEnergyStorage.asNeoForge(backing);

            int simulated = handler.extractEnergy(50, true);
            assertThat(simulated).isEqualTo(50);
            assertThat(backing.getAmount()).isEqualTo(80);
        }

        @Test
        @DisplayName("getEnergyStored reflects backing storage amount")
        void getEnergyStored_reflectsAmount() {
            EnergyComponent backing = component(100);
            backing.insert(42, false);
            var handler = NeoForgeEnergyStorage.asNeoForge(backing);
            assertThat(handler.getEnergyStored()).isEqualTo(42);
        }

        @Test
        @DisplayName("getMaxEnergyStored reflects backing capacity")
        void getMaxEnergyStored_reflectsCapacity() {
            EnergyComponent backing = component(500);
            var handler = NeoForgeEnergyStorage.asNeoForge(backing);
            assertThat(handler.getMaxEnergyStored()).isEqualTo(500);
        }

        @Test
        @DisplayName("canReceive reflects canInsert on backing storage")
        void canReceive_reflectsCanInsert() {
            assertThat(NeoForgeEnergyStorage.asNeoForge(component(100)).canReceive()).isTrue();
        }

        @Test
        @DisplayName("canExtract reflects canExtract on backing storage")
        void canExtract_reflectsCanExtract() {
            assertThat(NeoForgeEnergyStorage.asNeoForge(component(100)).canExtract()).isTrue();
        }
    }

    @Nested
    @DisplayName("NeoForgeEnergyStorage (NeoForge 21.1 → common)")
    class WrapTests {

        @Test
        @DisplayName("insert delegates to receiveEnergy with simulate=false")
        void insert_delegatesToReceiveEnergy() {
            var neoStorage = fakeStorage(100, 0);
            IEnergyStorage wrapped = NeoForgeEnergyStorage.wrap(neoStorage);

            long inserted = wrapped.insert(60, false);
            assertThat(inserted).isEqualTo(60);
            assertThat(wrapped.getAmount()).isEqualTo(60);
        }

        @Test
        @DisplayName("insert simulates without changing storage")
        void insert_simulate_doesNotChange() {
            var neoStorage = fakeStorage(100, 0);
            IEnergyStorage wrapped = NeoForgeEnergyStorage.wrap(neoStorage);

            long simulated = wrapped.insert(60, true);
            assertThat(simulated).isEqualTo(60);
            assertThat(wrapped.getAmount()).isZero();
        }

        @Test
        @DisplayName("extract delegates to extractEnergy with simulate=false")
        void extract_delegatesToExtractEnergy() {
            var neoStorage = fakeStorage(100, 80);
            IEnergyStorage wrapped = NeoForgeEnergyStorage.wrap(neoStorage);

            long extracted = wrapped.extract(50, false);
            assertThat(extracted).isEqualTo(50);
            assertThat(wrapped.getAmount()).isEqualTo(30);
        }

        @Test
        @DisplayName("getAmount returns NeoForge getEnergyStored")
        void getAmount_returnsEnergyStored() {
            var neoStorage = fakeStorage(100, 42);
            assertThat(NeoForgeEnergyStorage.wrap(neoStorage).getAmount()).isEqualTo(42);
        }

        @Test
        @DisplayName("getCapacity returns NeoForge getMaxEnergyStored")
        void getCapacity_returnsMaxEnergyStored() {
            var neoStorage = fakeStorage(200, 0);
            assertThat(NeoForgeEnergyStorage.wrap(neoStorage).getCapacity()).isEqualTo(200);
        }
    }
}
