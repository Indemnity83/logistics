package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.DestinationPriority;
import com.logistics.core.lib.pipe.FluidPipeBehavior;
import com.logistics.pipe.FluidPipe;
import com.logistics.pipe.PipeTypes;
import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the four fluid-routing modules and the registered pipes they back. The routing itself
 * (handler-first splitting, the merger's check valve, voiding) runs in {@code FluidPipeBlockEntity} against a
 * live level, so it is covered in-world by {@code FluidRoutingGameTestBody}; these tests pin the policy each
 * module contributes and the composition the block entity reads it from.
 */
@DisplayName("Fluid routing modules")
class FluidRoutingModulesTest extends MinecraftTestEnvironment {

    @Nested
    @DisplayName("FluidInsertionModule")
    class Insertion {

        private final FluidInsertionModule module = new FluidInsertionModule();

        @ParameterizedTest(name = "from {0}")
        @EnumSource(DestinationPriority.class)
        @DisplayName("forces handlers-first priority whatever came before it")
        void forcesHandlersFirst(DestinationPriority incoming) {
            assertThat(module.destinationPriority(incoming)).isEqualTo(DestinationPriority.HANDLERS_FIRST);
        }

        @Test
        @DisplayName("still connects to the tanks it fills")
        void connectsToHandlers() {
            assertThat(module.canConnectToFluidHandler()).isTrue();
        }

        /** The buffer is not widened to hold a cauldron level; see WIKI_DISCREPANCIES.md § Insertion Fluid Pipe. */
        @ParameterizedTest(name = "{0}")
        @ValueSource(longs = {0, 1, 250, 1_000})
        @DisplayName("leaves transfer rate and buffer capacity at the shared default")
        void leavesRateAndCapacityAlone(long value) {
            assertThat(module.modifyTransferRate(value)).isEqualTo(value);
            assertThat(module.modifyCapacity(value)).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("FluidBypassModule")
    class Bypass {

        private final FluidBypassModule module = new FluidBypassModule();

        @Test
        @DisplayName("never connects to an external fluid handler")
        void vetoesHandlers() {
            assertThat(module.canConnectToFluidHandler()).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(DestinationPriority.class)
        @DisplayName("passes destination priority through unchanged")
        void priorityPassesThrough(DestinationPriority incoming) {
            assertThat(module.destinationPriority(incoming)).isEqualTo(incoming);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(longs = {0, 1, 250, 1_000})
        @DisplayName("leaves transfer rate and buffer capacity at the shared default")
        void leavesRateAndCapacityAlone(long value) {
            assertThat(module.modifyTransferRate(value)).isEqualTo(value);
            assertThat(module.modifyCapacity(value)).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("FluidMergerModule")
    class Merger {

        @Test
        @DisplayName("is a pure marker that contributes no transport policy of its own")
        void isNotABehavior() {
            assertThat(new FluidMergerModule()).isNotInstanceOf(FluidPipeBehavior.class);
        }

        @Test
        @DisplayName("marks its pipe as a merger with a wrench-set feature face")
        void marksMerger() {
            FluidPipe pipe = new FluidPipe(new FluidMergerModule());

            assertThat(pipe.isMerger()).isTrue();
            assertThat(pipe.usesFeatureFace()).isTrue();
            assertThat(pipe.isVoid()).isFalse();
            assertThat(pipe.isExtractor()).isFalse();
        }

        @Test
        @DisplayName("leaves the pipe free to connect to handlers, at normal priority")
        void leavesPolicyAlone() {
            FluidPipe pipe = new FluidPipe(new FluidMergerModule());

            assertThat(pipe.canConnectToFluidHandler()).isTrue();
            assertThat(pipe.destinationPriority()).isEqualTo(DestinationPriority.NORMAL);
        }
    }

    @Nested
    @DisplayName("FluidVoidModule")
    class VoidSink {

        @Test
        @DisplayName("is a pure marker that contributes no transport policy of its own")
        void isNotABehavior() {
            assertThat(new FluidVoidModule()).isNotInstanceOf(FluidPipeBehavior.class);
        }

        @Test
        @DisplayName("marks its pipe as a void sink with no feature face")
        void marksVoid() {
            FluidPipe pipe = new FluidPipe(new FluidVoidModule());

            assertThat(pipe.isVoid()).isTrue();
            assertThat(pipe.isMerger()).isFalse();
            assertThat(pipe.usesFeatureFace()).isFalse();
        }
    }

    /** The registered pipes, as {@link PipeTypes} composes them. */
    @Nested
    @DisplayName("registered pipes")
    class Registered {

        @Test
        @DisplayName("Insertion Fluid Pipe fills handlers first and connects to them")
        void insertionPipe() {
            FluidPipe pipe = PipeTypes.INSERTION_FLUID_PIPE;

            assertThat(pipe.destinationPriority()).isEqualTo(DestinationPriority.HANDLERS_FIRST);
            assertThat(pipe.canConnectToFluidHandler()).isTrue();
            assertThat(pipe.isMerger()).isFalse();
            assertThat(pipe.isVoid()).isFalse();
        }

        @Test
        @DisplayName("Bypass Fluid Pipe connects to pipes only")
        void bypassPipe() {
            FluidPipe pipe = PipeTypes.BYPASS_FLUID_PIPE;

            assertThat(pipe.canConnectToFluidHandler()).isFalse();
            assertThat(pipe.destinationPriority()).isEqualTo(DestinationPriority.NORMAL);
            assertThat(pipe.isVoid()).isFalse();
        }

        @Test
        @DisplayName("Merger Fluid Pipe is a check valve that still connects to handlers")
        void mergerPipe() {
            FluidPipe pipe = PipeTypes.MERGER_FLUID_PIPE;

            assertThat(pipe.isMerger()).isTrue();
            assertThat(pipe.usesFeatureFace()).isTrue();
            assertThat(pipe.canConnectToFluidHandler()).isTrue();
        }

        @Test
        @DisplayName("Void Fluid Pipe destroys fluid and connects to pipes only")
        void voidPipe() {
            FluidPipe pipe = PipeTypes.VOID_FLUID_PIPE;

            assertThat(pipe.isVoid()).isTrue();
            assertThat(pipe.canConnectToFluidHandler()).isFalse();
            assertThat(pipe.usesFeatureFace()).isFalse();
        }

        /**
         * Wiki claim (Fluid Pipes § Usage): "Passive transport is free — the stone, copper, golden, insertion,
         * merger, bypass, and void pipes move fluid with no power."
         */
        @Test
        @DisplayName("none of the four routing pipes needs power")
        void routingPipesAreUnpowered() {
            assertThat(PipeTypes.INSERTION_FLUID_PIPE.hasEnergy()).isFalse();
            assertThat(PipeTypes.BYPASS_FLUID_PIPE.hasEnergy()).isFalse();
            assertThat(PipeTypes.MERGER_FLUID_PIPE.hasEnergy()).isFalse();
            assertThat(PipeTypes.VOID_FLUID_PIPE.hasEnergy()).isFalse();
        }
    }
}
