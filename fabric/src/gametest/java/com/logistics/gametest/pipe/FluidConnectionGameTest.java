package com.logistics.gametest.pipe;

import com.logistics.LogisticsFluid;
import com.logistics.pipe.block.FluidConnection;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class FluidConnectionGameTest {

    /**
     * Two fluid extractors placed side by side must not connect to each other — each extractor is an
     * independent endpoint that only reaches its source handler and downstream transport pipes.
     * Regression test for #676.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void fluidExtractorsDoNotConnectToEachOther(GameTestHelper context) {
        BlockPos a = new BlockPos(0, 1, 0);
        BlockPos b = a.east();
        context.setBlock(a, LogisticsFluid.BLOCK.FLUID_EXTRACTOR_PIPE);
        context.setBlock(b, LogisticsFluid.BLOCK.FLUID_EXTRACTOR_PIPE);

        context.succeedWhen(() -> {
            FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) context.getBlockEntity(a);
            context.assertTrue(pipe != null, "Fluid extractor should have a block entity");
            context.assertTrue(
                    pipe.connection(Direction.EAST) == FluidConnection.NONE,
                    "Fluid extractors must not connect to each other");
        });
    }

    /**
     * A fluid extractor still connects to a downstream transport pipe (positive control for the
     * connection veto above).
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void fluidExtractorConnectsToTransportPipe(GameTestHelper context) {
        BlockPos a = new BlockPos(0, 1, 0);
        BlockPos b = a.east();
        context.setBlock(a, LogisticsFluid.BLOCK.FLUID_EXTRACTOR_PIPE);
        context.setBlock(b, LogisticsFluid.BLOCK.STONE_FLUID_PIPE);

        context.succeedWhen(() -> {
            FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) context.getBlockEntity(a);
            context.assertTrue(pipe != null, "Fluid extractor should have a block entity");
            context.assertTrue(
                    pipe.connection(Direction.EAST) == FluidConnection.PIPE,
                    "Fluid extractor should connect to a transport pipe");
        });
    }
}
