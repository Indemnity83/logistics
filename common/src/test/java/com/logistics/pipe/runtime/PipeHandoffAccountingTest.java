package com.logistics.pipe.runtime;

import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Item conservation across a pipe-to-pipe hand-off.
 *
 * <p>The source pipe drops the traveling item from its list as soon as the hand-off runs, so every
 * item the target pipe did not accept has to be accounted for by {@code unacceptedCount} — otherwise
 * it is silently destroyed.
 */
@DisplayName("Pipe hand-off accounting")
class PipeHandoffAccountingTest extends MinecraftTestEnvironment {

    private TravelingItem travelingItem(int count) {
        return new TravelingItem(new ItemStack(Items.DIAMOND, count), Direction.NORTH, 0.05f);
    }

    @Test
    @DisplayName("should report nothing left behind when the target accepts the whole stack")
    void fullAcceptLeavesNothing() {
        assertThat(PipeRuntime.unacceptedCount(travelingItem(64), 64)).isZero();
    }

    @Test
    @DisplayName("should report the whole stack when the target accepts nothing")
    void rejectedHandoffLeavesEverything() {
        assertThat(PipeRuntime.unacceptedCount(travelingItem(64), 0)).isEqualTo(64);
    }

    @Test
    @DisplayName("should report the shortfall when a congested target accepts only part of the stack")
    void partialAcceptLeavesTheShortfall() {
        // A pipe holding 300 of its 320 virtual capacity accepts 20 of an incoming 64 stack.
        assertThat(PipeRuntime.unacceptedCount(travelingItem(64), 20)).isEqualTo(44);
    }

    @Test
    @DisplayName("should conserve every item at any accepted amount")
    void handoffConservesItems() {
        int count = 64;
        for (int inserted = 0; inserted <= count; inserted++) {
            long unaccepted = PipeRuntime.unacceptedCount(travelingItem(count), inserted);
            assertThat(inserted + unaccepted)
                    .as("items conserved when the target accepts %d of %d", inserted, count)
                    .isEqualTo(count);
        }
    }
}
