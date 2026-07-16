package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;import com.logistics.pipe.block.FluidConnection;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

public class FluidConnectionGameTest {

    /**
     * Two fluid extractors placed side by side must not connect to each other — each extractor is an
     * independent endpoint that only reaches its source handler and downstream transport pipes.
     * Regression test for #676.
     */
    @GameTest
    public void fluidExtractorsDoNotConnectToEachOther(GameTestHelper context) {
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
    @GameTest
    public void fluidExtractorConnectsToTransportPipe(GameTestHelper context) {
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
