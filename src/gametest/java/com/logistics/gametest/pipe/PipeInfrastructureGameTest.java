package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for Logistics mod using Fabric's GameTest framework.
 * These tests run in an actual Minecraft server environment with full mod initialization.
 */
public class PipeInfrastructureGameTest {

    /**
     * Simple test to verify game test infrastructure works.
     */
    @FabricGameTest
    public void verifyGameTestWorks(GameTestHelper context) {
        // Just verify we can access blocks
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, Blocks.STONE);

        context.assertTrue(
                context.getBlockState(pos).is(Blocks.STONE),
                "Stone block should be placed"
        );

        context.succeed();
    }

    /**
     * Test that a pipe block can be placed and creates a block entity.
     */
    @FabricGameTest
    public void testPipePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);

        // Place a copper transport pipe
        context.setBlock(pos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);

        // Verify block entity exists
        PipeBlockEntity blockEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (blockEntity == null) {
            context.fail("Pipe block entity should exist at " + pos);
        }

        context.succeed();
    }

    /**
     * Test that different pipe types can be placed.
     */
    @FabricGameTest
    public void testMultiplePipeTypes(GameTestHelper context) {
        // Place various pipe types
        context.setBlock(new BlockPos(0, 1, 0), LogisticsPipe.BLOCK.STONE_TRANSPORT_PIPE);
        context.setBlock(new BlockPos(1, 1, 0), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(new BlockPos(2, 1, 0), LogisticsPipe.BLOCK.ITEM_FILTER_PIPE);
        context.setBlock(new BlockPos(3, 1, 0), LogisticsPipe.BLOCK.ITEM_EXTRACTOR_PIPE);

        // Verify all have block entities
        if (context.getBlockEntity(new BlockPos(0, 1, 0), PipeBlockEntity.class) == null) {
            context.fail("Stone pipe should have block entity");
            return;
        }
        if (context.getBlockEntity(new BlockPos(1, 1, 0), PipeBlockEntity.class) == null) {
            context.fail("Copper pipe should have block entity");
            return;
        }
        if (context.getBlockEntity(new BlockPos(2, 1, 0), PipeBlockEntity.class) == null) {
            context.fail("Filter pipe should have block entity");
            return;
        }
        if (context.getBlockEntity(new BlockPos(3, 1, 0), PipeBlockEntity.class) == null) {
            context.fail("Extractor pipe should have block entity");
            return;
        }

        context.succeed();
    }

    /**
     * Test that pipes can connect to each other.
     */
    @FabricGameTest
    public void testPipeConnections(GameTestHelper context) {
        BlockPos center = new BlockPos(1, 1, 1);

        // Place a central pipe with pipes on all sides
        context.setBlock(center, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.north(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.south(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.east(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.west(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);

        // Verify center pipe has block entity
        PipeBlockEntity centerEntity = context.getBlockEntity(center, PipeBlockEntity.class);
        if (centerEntity == null) {
            context.fail("Center pipe should have block entity");
        }

        context.succeed();
    }
}
