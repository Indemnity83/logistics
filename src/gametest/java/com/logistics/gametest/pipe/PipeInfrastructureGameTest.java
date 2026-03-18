package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.gametest.framework.GameTest;
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
    @GameTest
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
    @GameTest
    public void testPipePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);

        // Place a copper transport pipe
        context.setBlock(pos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);

        // Verify block entity exists
        PipeBlockEntity blockEntity = (PipeBlockEntity) context.getBlockEntity(pos);
        if (blockEntity == null) {
            context.fail("Pipe block entity should exist at " + pos);
        }

        context.succeed();
    }

    /**
     * Test that different pipe types can be placed.
     */
    @GameTest
    public void testMultiplePipeTypes(GameTestHelper context) {
        // Place various pipe types
        context.setBlock(new BlockPos(0, 1, 0), LogisticsPipe.BLOCK.STONE_TRANSPORT_PIPE);
        context.setBlock(new BlockPos(1, 1, 0), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(new BlockPos(2, 1, 0), LogisticsPipe.BLOCK.ITEM_FILTER_PIPE);
        context.setBlock(new BlockPos(3, 1, 0), LogisticsPipe.BLOCK.ITEM_EXTRACTOR_PIPE);

        // Verify all have block entities
        if (!(context.getBlockEntity(new BlockPos(0, 1, 0)) instanceof PipeBlockEntity)) {
            context.fail("Stone pipe should have block entity");
            return;
        }
        if (!(context.getBlockEntity(new BlockPos(1, 1, 0)) instanceof PipeBlockEntity)) {
            context.fail("Copper pipe should have block entity");
            return;
        }
        if (!(context.getBlockEntity(new BlockPos(2, 1, 0)) instanceof PipeBlockEntity)) {
            context.fail("Filter pipe should have block entity");
            return;
        }
        if (!(context.getBlockEntity(new BlockPos(3, 1, 0)) instanceof PipeBlockEntity)) {
            context.fail("Extractor pipe should have block entity");
            return;
        }

        context.succeed();
    }

    /**
     * Test that pipes can connect to each other.
     */
    @GameTest
    public void testPipeConnections(GameTestHelper context) {
        BlockPos center = new BlockPos(1, 1, 1);

        // Place a central pipe with pipes on all sides
        context.setBlock(center, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.north(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.south(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.east(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.west(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);

        // Verify center pipe has block entity
        PipeBlockEntity centerEntity = (PipeBlockEntity) context.getBlockEntity(center);
        if (centerEntity == null) {
            context.fail("Center pipe should have block entity");
        }

        context.succeed();
    }

    /**
     * Test that connection cache is only recalculated when neighbors change.
     * This verifies the performance optimization that avoids per-tick recalculation.
     */
    @GameTest
    public void testConnectionCacheOptimization(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 1, 1);

        // Place a pipe
        context.setBlock(pipePos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        PipeBlockEntity pipeEntity = (PipeBlockEntity) context.getBlockEntity(pipePos);
        if (pipeEntity == null) {
            context.fail("Pipe block entity should exist");
            return;
        }

        // Cache should be dirty initially
        context.assertTrue(
                pipeEntity.isConnectionCacheDirty(),
                "Connection cache should be dirty on first tick"
        );

        // Manually tick the pipe - should recalculate connections
        PipeBlockEntity.tick(
                context.getLevel(),
                pipePos,
                context.getBlockState(pipePos),
                pipeEntity
        );

        // After first tick, cache should be clean
        context.assertFalse(
                pipeEntity.isConnectionCacheDirty(),
                "Connection cache should be clean after first tick"
        );

        // Tick again - cache should remain clean (no topology change)
        PipeBlockEntity.tick(
                context.getLevel(),
                pipePos,
                context.getBlockState(pipePos),
                pipeEntity
        );

        context.assertFalse(
                pipeEntity.isConnectionCacheDirty(),
                "Connection cache should remain clean when no neighbors change"
        );

        // Place a neighbor block
        context.setBlock(pipePos.north(), Blocks.CHEST);

        // Cache should be dirty after neighbor change
        // (neighborChanged should have invalidated it automatically)
        context.assertTrue(
                pipeEntity.isConnectionCacheDirty(),
                "Connection cache should be dirty after neighbor change"
        );

        // Tick - should recalculate and clean cache
        PipeBlockEntity.tick(
                context.getLevel(),
                pipePos,
                context.getBlockState(pipePos),
                pipeEntity
        );

        context.assertFalse(
                pipeEntity.isConnectionCacheDirty(),
                "Connection cache should be clean after recalculation"
        );

        context.succeed();
    }
}
