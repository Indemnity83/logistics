package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.FluidConnection;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Shared fluid connection GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each loader's
 * own registration mechanism: Fabric's {@code @GameTest}-annotated {@code FluidConnectionGameTest}
 * delegates to these methods, and NeoForge's {@code FluidConnectionGameTestRegistration} references
 * them directly as {@code Consumer<GameTestHelper>} method references.
 */
public class FluidConnectionGameTestBody {

    /**
     * Two fluid extractors placed side by side must not connect to each other — each extractor is an
     * independent endpoint that only reaches its source handler and downstream transport pipes.
     * Regression test for #676.
     */
    public static void fluidExtractorsDoNotConnectToEachOther(GameTestHelper context) {
        BlockPos a = new BlockPos(0, 1, 0);
        BlockPos b = a.east();
        context.setBlock(a, LogisticsPipe.BLOCK.FLUID_EXTRACTOR_PIPE);
        context.setBlock(b, LogisticsPipe.BLOCK.FLUID_EXTRACTOR_PIPE);

        context.succeedWhen(() -> {
            FluidPipeBlockEntity pipe = context.getBlockEntity(a, FluidPipeBlockEntity.class);
            if (pipe == null) {
                throw context.assertionException("Fluid extractor should have a block entity");
            }
            if (pipe.connection(Direction.EAST) != FluidConnection.NONE) {
                throw context.assertionException("Fluid extractors must not connect to each other");
            }
        });
    }

    /**
     * A fluid extractor still connects to a downstream transport pipe (positive control for the
     * connection veto above).
     */
    public static void fluidExtractorConnectsToTransportPipe(GameTestHelper context) {
        BlockPos a = new BlockPos(0, 1, 0);
        BlockPos b = a.east();
        context.setBlock(a, LogisticsPipe.BLOCK.FLUID_EXTRACTOR_PIPE);
        context.setBlock(b, LogisticsPipe.BLOCK.STONE_FLUID_PIPE);

        context.succeedWhen(() -> {
            FluidPipeBlockEntity pipe = context.getBlockEntity(a, FluidPipeBlockEntity.class);
            if (pipe == null) {
                throw context.assertionException("Fluid extractor should have a block entity");
            }
            if (pipe.connection(Direction.EAST) != FluidConnection.PIPE) {
                throw context.assertionException("Fluid extractor should connect to a transport pipe");
            }
        });
    }
}
