package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * Shared pipe-infrastructure GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each loader's
 * own registration mechanism: Fabric's {@code @GameTest}-annotated {@code PipeInfrastructureGameTest}
 * delegates to these methods, and NeoForge's {@code PipeInfrastructureGameTestRegistration} references
 * them directly as {@code Consumer<GameTestHelper>} method references.
 */
public class PipeInfrastructureGameTestBody {

    /**
     * Simple test to verify game test infrastructure works.
     */
    public static void verifyGameTestWorks(GameTestHelper context) {
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
    public static void testPipePlacement(GameTestHelper context) {
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
    public static void testMultiplePipeTypes(GameTestHelper context) {
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
     * A pipe surrounded by four pipes must record those four as PIPE connections, and record
     * nothing above or below it.
     *
     * <p>Reads the cached connection types the renderer and the router both consume, after letting
     * the server tick the pipe — placing the neighbours is not enough on its own, the recalculation
     * has to run. Asserting only that the centre has a block entity (all this test used to do)
     * passes even when connection tracking is completely broken.
     */
    public static void testPipeConnections(GameTestHelper context) {
        BlockPos center = new BlockPos(1, 1, 1);

        // Place a central pipe with pipes on all four horizontal sides
        context.setBlock(center, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.north(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.south(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.east(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(center.west(), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);

        // The connection cache is recalculated on tick, so give the server a few before reading it.
        context.runAfterDelay(3, () -> {
            PipeBlockEntity centerEntity = context.getBlockEntity(center, PipeBlockEntity.class);
            if (centerEntity == null) {
                context.fail("Center pipe should have block entity");
                return;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                PipeConnection.Type type = centerEntity.getCachedConnectionType(direction);
                if (type != PipeConnection.Type.PIPE) {
                    context.fail("Center pipe should report a PIPE connection to its " + direction
                            + " neighbour, got: " + type);
                    return;
                }
            }

            for (Direction direction : Direction.Plane.VERTICAL) {
                PipeConnection.Type type = centerEntity.getCachedConnectionType(direction);
                if (type != PipeConnection.Type.NONE) {
                    context.fail("Center pipe should report no connection " + direction
                            + " (nothing is there), got: " + type);
                    return;
                }
            }

            context.succeed();
        });
    }

    /**
     * The connection cache is recalculated once after a topology change and then left alone.
     *
     * <p>Driven by the server's own tick loop rather than by calling {@code PipeBlockEntity.tick}
     * directly: the flag going clean is then evidence that the pipe is actually registered as a
     * ticking block entity in the world, which a direct static call cannot show.
     */
    public static void testConnectionCacheOptimization(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 1, 1);

        // Place a pipe
        context.setBlock(pipePos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        PipeBlockEntity pipeEntity = context.getBlockEntity(pipePos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Pipe block entity should exist");
            return;
        }

        // Cache is dirty the moment the pipe is placed, before the world has ticked it.
        context.assertTrue(
                pipeEntity.isConnectionCacheDirty(),
                "Connection cache should be dirty on first tick"
        );

        // Let the world tick the pipe — that, not a direct tick() call, is what must clean it.
        context.runAfterDelay(2, () -> {
            context.assertFalse(
                    pipeEntity.isConnectionCacheDirty(),
                    "Connection cache should be clean once the server has ticked the pipe"
            );

            // More ticks with no topology change must not dirty it again.
            context.runAfterDelay(3, () -> {
                context.assertFalse(
                        pipeEntity.isConnectionCacheDirty(),
                        "Connection cache should remain clean when no neighbors change"
                );

                // A neighbor change invalidates it through neighborChanged.
                context.setBlock(pipePos.north(), Blocks.CHEST);
                context.assertTrue(
                        pipeEntity.isConnectionCacheDirty(),
                        "Connection cache should be dirty after neighbor change"
                );

                context.runAfterDelay(2, () -> {
                    context.assertFalse(
                            pipeEntity.isConnectionCacheDirty(),
                            "Connection cache should be clean after the server recalculates it"
                    );
                    context.succeed();
                });
            });
        });
    }
}
