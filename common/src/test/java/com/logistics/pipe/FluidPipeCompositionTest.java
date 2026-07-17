package com.logistics.pipe;

import com.logistics.core.lib.pipe.DestinationPriority;
import com.logistics.core.lib.pipe.FluidPipeBehavior;
import com.logistics.core.lib.pipe.Module;
import com.logistics.pipe.modules.FluidBypassModule;
import com.logistics.pipe.modules.FluidInsertionModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FluidPipe transport-policy composition")
class FluidPipeCompositionTest {

    /** A behavior facet with a fixed connect vote, for exercising the fold in isolation. */
    private record Connects(boolean value) implements Module, FluidPipeBehavior {
        @Override
        public boolean canConnectToFluidHandler() {
            return value;
        }
    }

    /** A behavior facet that forces a destination priority, ignoring the running value. */
    private record Priority(DestinationPriority forced) implements Module, FluidPipeBehavior {
        @Override
        public DestinationPriority destinationPriority(DestinationPriority priority) {
            return forced;
        }
    }

    // ==================== canConnectToFluidHandler ====================

    @Test
    @DisplayName("a bare fluid pipe connects to external handlers")
    void connect_defaultsToTrue() {
        assertThat(new FluidPipe().canConnectToFluidHandler()).isTrue();
    }

    @Test
    @DisplayName("the bypass module vetoes external handler connections")
    void connect_bypassVetoes() {
        assertThat(new FluidPipe(new FluidBypassModule()).canConnectToFluidHandler()).isFalse();
    }

    @Test
    @DisplayName("a single veto wins over any number of allowing behaviors")
    void connect_anyVetoWins() {
        assertThat(new FluidPipe(new Connects(true), new FluidBypassModule(), new Connects(true))
                .canConnectToFluidHandler())
                .isFalse();
    }

    @Test
    @DisplayName("behaviors that do not veto leave the connection allowed")
    void connect_allowedWhenNoVeto() {
        assertThat(new FluidPipe(new Connects(true), new FluidInsertionModule()).canConnectToFluidHandler())
                .isTrue();
    }

    // ==================== destinationPriority ====================

    @Test
    @DisplayName("a bare fluid pipe uses NORMAL destination priority")
    void priority_defaultsToNormal() {
        assertThat(new FluidPipe().destinationPriority()).isEqualTo(DestinationPriority.NORMAL);
    }

    @Test
    @DisplayName("the insertion module prefers filling handlers first")
    void priority_insertionForcesHandlersFirst() {
        assertThat(new FluidPipe(new FluidInsertionModule()).destinationPriority())
                .isEqualTo(DestinationPriority.HANDLERS_FIRST);
    }

    @Test
    @DisplayName("priority folds left-to-right, so a later behavior overrides an earlier one")
    void priority_laterBehaviorWins() {
        assertThat(new FluidPipe(
                        new Priority(DestinationPriority.HANDLERS_FIRST),
                        new Priority(DestinationPriority.NORMAL))
                .destinationPriority())
                .isEqualTo(DestinationPriority.NORMAL);
    }

    @Test
    @DisplayName("a module that does not override priority leaves the running value untouched")
    void priority_nonOverridingModuleIsTransparent() {
        assertThat(new FluidPipe(new Priority(DestinationPriority.HANDLERS_FIRST), new FluidBypassModule())
                .destinationPriority())
                .isEqualTo(DestinationPriority.HANDLERS_FIRST);
    }
}
