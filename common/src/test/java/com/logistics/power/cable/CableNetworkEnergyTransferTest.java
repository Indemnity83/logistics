package com.logistics.power.cable;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cable network energy transfer")
class CableNetworkEnergyTransferTest {

    @Test
    @DisplayName("conserves energy when the target's simulate overstates what it accepts")
    void moveEnergy_optimisticSimulate_conservesEnergy() {
        FakeStorage source = FakeStorage.full(10_000);
        // Models a rate-limited machine behind a dishonest simulate.
        RateLimitedStorage target = new RateLimitedStorage(100_000, 32);

        long moved = CableNetwork.moveEnergy(source, target, 1_000);

        assertThat(moved).isEqualTo(32);
        assertThat(target.getAmount()).isEqualTo(32);
        assertThat(source.getAmount()).isEqualTo(10_000 - 32);
        assertConserved(source, target, 10_000, 0, moved);
    }

    @Test
    @DisplayName("moves energy normally when the target reports honestly")
    void moveEnergy_honestTarget_movesUpToTheLimit() {
        FakeStorage source = FakeStorage.full(10_000);
        FakeStorage target = FakeStorage.empty(500);

        long moved = CableNetwork.moveEnergy(source, target, 1_000);

        assertThat(moved).isEqualTo(500);
        assertConserved(source, target, 10_000, 0, moved);
    }

    @Test
    @DisplayName("refunds the source when the target accepts nothing at commit time")
    void moveEnergy_targetAcceptsNothingOnCommit_leavesSourceIntact() {
        FakeStorage source = FakeStorage.full(10_000);
        RateLimitedStorage target = new RateLimitedStorage(100_000, 0);

        long moved = CableNetwork.moveEnergy(source, target, 1_000);

        assertThat(moved).isZero();
        assertThat(source.getAmount()).isEqualTo(10_000);
        assertConserved(source, target, 10_000, 0, moved);
    }

    @Test
    @DisplayName("returns every stranded unit to a source that rate-limits its own refund")
    void moveEnergy_rateLimitedSource_refundsInFull() {
        // A Power Junction outruns its own input rate: 1000 out per operation, 128 back in.
        RateLimitedStorage source = RateLimitedStorage.full(10_000, 128);
        RateLimitedStorage target = new RateLimitedStorage(100_000, 32);

        long moved = CableNetwork.moveEnergy(source, target, 1_000);

        assertThat(moved).isEqualTo(32);
        assertConserved(source, target, 10_000, 0, moved);
    }

    @Test
    @DisplayName("ignores non-positive requests")
    void moveEnergy_nonPositiveRequest_isANoOp() {
        FakeStorage source = FakeStorage.full(10_000);
        FakeStorage target = FakeStorage.empty(500);

        assertThat(CableNetwork.moveEnergy(source, target, 0)).isZero();
        assertThat(CableNetwork.moveEnergy(source, target, -5)).isZero();
        assertThat(source.getAmount()).isEqualTo(10_000);
        assertThat(target.getAmount()).isZero();
    }

    /** Energy leaving the source must equal energy the target accepted, and the reported move. */
    private static void assertConserved(
            IEnergyStorage source, IEnergyStorage target, long sourceBefore, long targetBefore, long reported) {
        long lost = sourceBefore - source.getAmount();
        long gained = target.getAmount() - targetBefore;
        assertThat(lost).as("energy drained from the source").isEqualTo(gained);
        assertThat(reported).as("energy billed to the network").isEqualTo(gained);
    }

    /** Plain buffer: honest simulate, no per-operation rate limit. */
    // ===== self-transfer guard =====

    private static CableNetwork.DeviceConnection connection(BlockPos devicePos, Direction side) {
        // A fresh storage wrapper per face, which is what both loaders hand back per lookup.
        return new CableNetwork.DeviceConnection(
                devicePos.relative(side), devicePos, side, FakeStorage.full(1_000), null);
    }

    @Test
    @DisplayName("a device touching the network on two faces is not a source for itself")
    void isSameDevice_twoFacesOfOneBlock() {
        BlockPos battery = new BlockPos(4, 5, 6);

        assertThat(CableNetwork.isSameDevice(
                        connection(battery, Direction.NORTH), connection(battery, Direction.SOUTH)))
                .as("the same block reached from two faces holds two separately allocated wrappers")
                .isTrue();
    }

    @Test
    @DisplayName("two different devices still transfer between each other")
    void isSameDevice_distinctDevices() {
        assertThat(CableNetwork.isSameDevice(
                        connection(new BlockPos(4, 5, 6), Direction.NORTH),
                        connection(new BlockPos(9, 5, 6), Direction.NORTH)))
                .isFalse();
    }

    @Test
    @DisplayName("the reference check still catches a single-face device")
    void isSameDevice_sameWrapper() {
        BlockPos pos = new BlockPos(1, 2, 3);
        IEnergyStorage shared = FakeStorage.full(1_000);
        CableNetwork.DeviceConnection one =
                new CableNetwork.DeviceConnection(pos.above(), pos, Direction.UP, shared, null);

        assertThat(CableNetwork.isSameDevice(one, one)).isTrue();
    }

    private static final class FakeStorage implements IEnergyStorage {
        private final long capacity;
        private long amount;

        private FakeStorage(long capacity, long amount) {
            this.capacity = capacity;
            this.amount = amount;
        }

        static FakeStorage full(long capacity) {
            return new FakeStorage(capacity, capacity);
        }

        static FakeStorage empty(long capacity) {
            return new FakeStorage(capacity, 0);
        }

        @Override
        public long insert(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) return 0;
            long accepted = Math.min(maxAmount, capacity - amount);
            if (!simulate) amount += accepted;
            return accepted;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) return 0;
            long extracted = Math.min(maxAmount, amount);
            if (!simulate) amount -= extracted;
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
     * Large buffer with a small per-operation input rate whose simulate reports free
     * room instead of the rate — an optimistic simulate the network must not trust.
     */
    private static final class RateLimitedStorage implements IEnergyStorage {
        private final long capacity;
        private final long maxInsert;
        private long amount;

        private RateLimitedStorage(long capacity, long maxInsert) {
            this.capacity = capacity;
            this.maxInsert = maxInsert;
        }

        /** Starts full, so the storage can act as a source as well as a target. */
        static RateLimitedStorage full(long capacity, long maxInsert) {
            RateLimitedStorage storage = new RateLimitedStorage(capacity, maxInsert);
            storage.amount = capacity;
            return storage;
        }

        @Override
        public long insert(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) return 0;
            long room = capacity - amount;
            if (simulate) return Math.min(maxAmount, room);
            long accepted = Math.min(Math.min(maxAmount, maxInsert), room);
            amount += accepted;
            return accepted;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) return 0;
            long extracted = Math.min(maxAmount, amount);
            if (!simulate) amount -= extracted;
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
