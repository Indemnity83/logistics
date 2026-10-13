package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.forestry.humus.HumusGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared Humus GameTest bodies, compiled into both loaders' {@code gametest} source sets. Fabric's
 * {@code HumusGameTest} delegates to these methods and NeoForge's {@code HumusGameTestRegistration}
 * references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Light comes from a glowstone block set into the ground beside the humus rather than the sky,
 * so the result does not depend on the time of day the test happens to run at. Light propagates
 * asynchronously after a block change, so each test waits until the level it needs is actually
 * readable before random-ticking.
 */
public class HumusGameTestBody {

    /** Ticks for a neighbour update to reach the sapling. */
    private static final int SETTLE_TICKS = 5;

    /** Enough Humus random ticks that a 25% boost reaches the two steps a sapling needs. */
    private static final int MAX_RANDOM_TICKS = 1000;

    /** Random ticks given to a Humus that should not change. */
    private static final int IDLE_RANDOM_TICKS = 500;

    /**
     * Humus is in {@code #minecraft:dirt}, which saplings accept as soil, so an oak sapling can
     * stand on it — where it cannot on stone.
     */
    public static void testSaplingSurvivesOnHumus(GameTestHelper context) {
        BlockPos humus = new BlockPos(2, 1, 2);
        BlockPos sapling = humus.above();
        context.setBlock(humus, LogisticsForestry.BLOCK.HUMUS);

        if (!context.getBlockState(humus).is(BlockTags.DIRT)) {
            context.fail("Humus should be in #minecraft:dirt");
            return;
        }

        BlockPos stone = new BlockPos(5, 1, 5);
        context.setBlock(stone, Blocks.STONE);
        if (Blocks.OAK_SAPLING.defaultBlockState().canSurvive(context.getLevel(), context.absolutePos(stone.above()))) {
            context.fail("An oak sapling should not survive on stone");
            return;
        }
        if (!Blocks.OAK_SAPLING.defaultBlockState().canSurvive(context.getLevel(), context.absolutePos(sapling))) {
            context.fail("An oak sapling should survive on humus");
            return;
        }

        context.setBlock(sapling, Blocks.OAK_SAPLING);
        // A neighbour update makes the sapling re-check its support.
        context.setBlock(humus, LogisticsForestry.BLOCK.HUMUS);
        context.runAfterDelay(SETTLE_TICKS, () -> {
            context.assertBlockPresent(Blocks.OAK_SAPLING, sapling);
            context.succeed();
        });
    }

    /**
     * Random-ticking Humus in the light grows the oak sapling on it into a tree, and the tree its
     * boost grew turns the Humus under it to sand.
     */
    public static void testHumusBoostGrowsTreeAndTurnsToSand(GameTestHelper context) {
        BlockPos humus = new BlockPos(4, 1, 4);
        BlockPos sapling = humus.above();
        context.setBlock(humus, LogisticsForestry.BLOCK.HUMUS);
        context.setBlock(humus.east(), Blocks.GLOWSTONE);
        context.setBlock(sapling, Blocks.OAK_SAPLING);

        whenLight(context, sapling.above(), true, () -> {
            for (int i = 0; i < MAX_RANDOM_TICKS && isSapling(context, sapling); i++) {
                context.randomTick(humus);
            }

            context.assertBlockPresent(Blocks.OAK_LOG, sapling);
            context.assertBlockPresent(Blocks.SAND, humus);
            context.succeed();
        });
    }

    /**
     * Humus under a log, or under mangrove roots, turns to sand on its next random tick, however the
     * wood got there: a tree grown by vanilla ticks or bone meal is caught this way.
     */
    public static void testHumusUnderLogTurnsToSand(GameTestHelper context) {
        BlockPos underLog = new BlockPos(1, 1, 1);
        BlockPos underRoots = new BlockPos(5, 1, 5);
        context.setBlock(underLog, LogisticsForestry.BLOCK.HUMUS);
        context.setBlock(underLog.above(), Blocks.OAK_LOG);
        context.setBlock(underRoots, LogisticsForestry.BLOCK.HUMUS);
        context.setBlock(underRoots.above(), Blocks.MANGROVE_ROOTS);

        context.randomTick(underLog);
        context.randomTick(underRoots);

        context.assertBlockPresent(Blocks.SAND, underLog);
        context.assertBlockPresent(Blocks.OAK_LOG, underLog.above());
        context.assertBlockPresent(Blocks.SAND, underRoots);
        context.succeed();
    }

    /** Humus with nothing growing on it, or a plant that is not a sapling, never changes. */
    public static void testHumusWithoutSaplingNeverChanges(GameTestHelper context) {
        BlockPos bare = new BlockPos(1, 1, 1);
        BlockPos flowered = new BlockPos(5, 1, 1);
        context.setBlock(bare, LogisticsForestry.BLOCK.HUMUS);
        context.setBlock(bare.east(), Blocks.GLOWSTONE);
        context.setBlock(flowered, LogisticsForestry.BLOCK.HUMUS);
        context.setBlock(flowered.east(), Blocks.GLOWSTONE);
        context.setBlock(flowered.above(), Blocks.DANDELION);

        // Lit, so neither staying unchanged is down to a lack of light.
        context.startSequence()
                .thenWaitUntil(() -> {
                    assertBrightness(context, bare.above(), true);
                    assertBrightness(context, flowered.above(), true);
                })
                .thenExecute(() -> {
                    for (int i = 0; i < IDLE_RANDOM_TICKS; i++) {
                        context.randomTick(bare);
                        context.randomTick(flowered);
                    }

                    context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, bare);
                    context.assertBlockPresent(Blocks.AIR, bare.above());
                    context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, flowered);
                    context.assertBlockPresent(Blocks.DANDELION, flowered.above());
                    context.succeed();
                });
    }

    /**
     * A sapling sealed in the dark gets no boost: it is still at its first growth stage after
     * Humus random ticks that would have advanced it in the light.
     */
    public static void testNoGrowthInDarkness(GameTestHelper context) {
        BlockPos humus = new BlockPos(3, 1, 3);
        BlockPos sapling = humus.above();
        context.setBlock(humus, LogisticsForestry.BLOCK.HUMUS);
        context.setBlock(sapling, Blocks.OAK_SAPLING);
        // Seal the sapling and the space above it on every face.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            context.setBlock(sapling.relative(side), Blocks.STONE);
            context.setBlock(sapling.above().relative(side), Blocks.STONE);
        }
        context.setBlock(sapling.above(2), Blocks.STONE);

        whenLight(context, sapling.above(), false, () -> {
            for (int i = 0; i < IDLE_RANDOM_TICKS; i++) {
                context.randomTick(humus);
            }

            context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, humus);
            context.assertBlockProperty(sapling, SaplingBlock.STAGE, 0);
            context.succeed();
        });
    }

    private static boolean isSapling(GameTestHelper context, BlockPos pos) {
        BlockState state = context.getBlockState(pos);
        return state.getBlock() instanceof SaplingBlock;
    }

    /**
     * Runs {@code action} once the light at {@code pos} is (or is not) enough for a sapling to grow,
     * retrying each tick until the test times out.
     */
    private static void whenLight(GameTestHelper context, BlockPos pos, boolean bright, Runnable action) {
        context.startSequence()
                .thenWaitUntil(() -> assertBrightness(context, pos, bright))
                .thenExecute(action);
    }

    /** Throws unless the light at {@code pos} is (or is not) enough for a sapling to grow. */
    private static void assertBrightness(GameTestHelper context, BlockPos pos, boolean bright) {
        int brightness = context.getLevel().getMaxLocalRawBrightness(context.absolutePos(pos));
        context.assertTrue(
                HumusGrowth.hasLightToGrow(brightness) == bright,
                "Expected the light at " + pos + " to be " + (bright ? "at least " : "below ")
                        + HumusGrowth.MIN_BRIGHTNESS + ", was " + brightness);
    }
}
