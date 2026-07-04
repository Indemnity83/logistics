package com.logistics.pipe.network;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.test.MinecraftTestEnvironment;
import com.logistics.test.TestItemKey;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NetworkJob")
class NetworkJobTest extends MinecraftTestEnvironment {

    private static NetworkJob job(long requested, long planned) {
        return new NetworkJob(
                new UUID(0L, 1L),
                new TestItemKey(Items.COBBLESTONE),
                requested,
                planned,
                FulfillmentMode.PARTIAL,
                BlockPos.ZERO);
    }

    @Test
    @DisplayName("starts PLANNED with the full planned amount outstanding")
    void initialState() {
        NetworkJob job = job(64, 64);
        assertThat(job.state()).isEqualTo(JobState.PLANNED);
        assertThat(job.outstanding()).isEqualTo(64);
        assertThat(job.deliveredAmount()).isZero();
        assertThat(job.invalidatedAmount()).isZero();
    }

    @Nested
    @DisplayName("outstanding")
    class Outstanding {

        @Test
        @DisplayName("is planned minus delivered minus invalidated")
        void subtractsDeliveredAndInvalidated() {
            NetworkJob job = job(100, 100);
            job.recordDelivery(30);
            job.recordInvalidation(20);
            assertThat(job.outstanding()).isEqualTo(50);
        }

        @Test
        @DisplayName("never goes negative")
        void floorsAtZero() {
            NetworkJob job = job(10, 10);
            job.recordDelivery(25);
            assertThat(job.outstanding()).isZero();
        }
    }

    @Nested
    @DisplayName("recordDelivery")
    class RecordDelivery {

        @Test
        @DisplayName("a partial delivery moves the job to DELIVERING")
        void partialDeliveryIsDelivering() {
            NetworkJob job = job(64, 64);
            job.recordDelivery(20);
            assertThat(job.deliveredAmount()).isEqualTo(20);
            assertThat(job.state()).isEqualTo(JobState.DELIVERING);
        }

        @Test
        @DisplayName("completing the planned amount moves the job to COMPLETE")
        void fullDeliveryCompletes() {
            NetworkJob job = job(64, 64);
            job.recordDelivery(40);
            job.recordDelivery(24);
            assertThat(job.state()).isEqualTo(JobState.COMPLETE);
        }

        @Test
        @DisplayName("ignores non-positive amounts")
        void ignoresNonPositive() {
            NetworkJob job = job(64, 64);
            job.recordDelivery(0);
            job.recordDelivery(-5);
            assertThat(job.deliveredAmount()).isZero();
            assertThat(job.state()).isEqualTo(JobState.PLANNED);
        }
    }

    @Nested
    @DisplayName("recordInvalidation")
    class RecordInvalidation {

        @Test
        @DisplayName("invalidating everything with no delivery fails the job")
        void allInvalidatedNoDeliveryFails() {
            NetworkJob job = job(64, 64);
            job.recordInvalidation(64);
            assertThat(job.state()).isEqualTo(JobState.FAILED);
        }

        @Test
        @DisplayName("invalidating the remainder after a partial delivery completes the job")
        void invalidatedRemainderAfterDeliveryCompletes() {
            NetworkJob job = job(64, 64);
            job.recordDelivery(40);
            job.recordInvalidation(24);
            assertThat(job.state()).isEqualTo(JobState.COMPLETE);
        }

        @Test
        @DisplayName("a partial invalidation with work still outstanding leaves the state unchanged")
        void partialInvalidationKeepsState() {
            NetworkJob job = job(100, 100);
            job.recordInvalidation(30);
            assertThat(job.invalidatedAmount()).isEqualTo(30);
            assertThat(job.outstanding()).isEqualTo(70);
            assertThat(job.state()).isEqualTo(JobState.PLANNED);
        }
    }

    @Nested
    @DisplayName("transitionTo")
    class TransitionTo {

        @Test
        @DisplayName("moves to the requested state")
        void moves() {
            NetworkJob job = job(64, 64);
            job.transitionTo(JobState.ACTIVE);
            assertThat(job.state()).isEqualTo(JobState.ACTIVE);
        }

        @Test
        @DisplayName("is a no-op once the job has reached a terminal state")
        void noOpWhenTerminal() {
            NetworkJob job = job(64, 64);
            job.transitionTo(JobState.FAILED);

            job.transitionTo(JobState.ACTIVE);

            assertThat(job.state()).isEqualTo(JobState.FAILED);
        }
    }
}
