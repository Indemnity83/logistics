package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared Humus GameTest bodies, compiled into both loaders' {@code gametest} source sets. Fabric's
 * {@code HumusGameTest} delegates to these methods and NeoForge's {@code HumusGameTestRegistration}
 * references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Saplings are grown with bone meal through {@link BoneMealItem#growCrop}, the path a player's
 * or dispenser's bone meal takes, so each loader's own growth hooks are exercised.
 */
public class HumusGameTestBody {

    /** Ticks for a neighbour update to reach the sapling. */
    private static final int SETTLE_TICKS = 5;

    /** Bone meal applications that grow a sapling many times over. */
    private static final int MAX_BONE_MEAL = 64;

    /** Random ticks given to a Humus that should not change. */
    private static final int IDLE_RANDOM_TICKS = 500;

    /** Humus trees grown in search of one carrying propagules; a 4-log mangrove has nowhere to hang one. */
    private static final int MANGROVE_ATTEMPTS = 10;

    /** How far around a trunk, and how high above it, a Humus tree's blocks are looked for. */
    private static final int TREE_RADIUS = 3;

    private static final int TREE_HEIGHT = 9;

    /** Where a single test tree's Humus goes: the middle of the 8x8 test area. */
    private static final BlockPos HUMUS_POS = new BlockPos(4, 1, 4);

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

    /** An oak sapling on Humus grows the small tree, and the Humus under it stays Humus. */
    public static void testOakGrowsSmallTree(GameTestHelper context) {
        growOnHumus(context, Blocks.OAK_SAPLING);
        assertSmallTree(context, HUMUS_POS.above(), Blocks.OAK_LOG, state -> state.is(Blocks.OAK_LEAVES));
        context.succeed();
    }

    /** A single dark oak sapling, which vanilla only grows as a 2x2 group, grows the small tree on Humus. */
    public static void testDarkOakGrowsFromOneSapling(GameTestHelper context) {
        growOnHumus(context, Blocks.DARK_OAK_SAPLING);
        assertSmallTree(context, HUMUS_POS.above(), Blocks.DARK_OAK_LOG, state -> state.is(Blocks.DARK_OAK_LEAVES));
        context.succeed();
    }

    /** An azalea on Humus grows the small tree, with no rooted dirt and no bending trunk. */
    public static void testAzaleaGrowsSmallTree(GameTestHelper context) {
        growOnHumus(context, Blocks.AZALEA);
        assertSmallTree(
                context,
                HUMUS_POS.above(),
                Blocks.OAK_LOG,
                state -> state.is(Blocks.AZALEA_LEAVES) || state.is(Blocks.FLOWERING_AZALEA_LEAVES));
        context.succeed();
    }

    /**
     * A mangrove propagule on Humus grows the small tree with no roots, and the propagules hanging
     * from its leaves are fully grown. Hanging is random and a 4-log tree has no leaf with room
     * below it, so trees are regrown until one carries propagules.
     */
    public static void testMangroveGrowsSmallTreeWithGrownPropagules(GameTestHelper context) {
        BlockPos sapling = HUMUS_POS.above();
        for (int attempt = 0; attempt < MANGROVE_ATTEMPTS; attempt++) {
            clearTreeSpace(context, sapling);
            growOnHumus(context, Blocks.MANGROVE_PROPAGULE);
            assertSmallTree(context, sapling, Blocks.MANGROVE_LOG, state -> state.is(Blocks.MANGROVE_LEAVES));

            List<BlockPos> propagules = treeSpace(sapling)
                    .filter(pos -> context.getBlockState(pos).is(Blocks.MANGROVE_PROPAGULE))
                    .map(BlockPos::immutable)
                    .toList();
            for (BlockPos propagule : propagules) {
                context.assertBlockProperty(propagule, MangrovePropaguleBlock.HANGING, true);
                context.assertBlockProperty(propagule, MangrovePropaguleBlock.AGE, 4);
            }
            if (!propagules.isEmpty()) {
                context.succeed();
                return;
            }
        }
        context.fail("No Humus mangrove carried a propagule in " + MANGROVE_ATTEMPTS + " trees");
    }

    /**
     * A 2x2 group of dark oak saplings on Humus grows four small trees, one per sapling: growing one
     * leaves the other three saplings, where vanilla would grow all four into one mega tree.
     */
    public static void testDarkOakGroupGrowsFourTrees(GameTestHelper context) {
        List<BlockPos> saplings = List.of(
                new BlockPos(3, 2, 3), new BlockPos(4, 2, 3), new BlockPos(3, 2, 4), new BlockPos(4, 2, 4));
        for (BlockPos sapling : saplings) {
            context.setBlock(sapling.below(), LogisticsForestry.BLOCK.HUMUS);
            context.setBlock(sapling, Blocks.DARK_OAK_SAPLING);
        }

        boneMeal(context, saplings.getFirst());
        context.assertBlockPresent(Blocks.DARK_OAK_LOG, saplings.getFirst());
        for (BlockPos other : saplings.subList(1, saplings.size())) {
            context.assertBlockPresent(Blocks.DARK_OAK_SAPLING, other);
        }

        for (BlockPos other : saplings.subList(1, saplings.size())) {
            boneMeal(context, other);
        }
        for (BlockPos sapling : saplings) {
            context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, sapling.below());
            for (int y = 0; y < 4; y++) {
                context.assertBlockPresent(Blocks.DARK_OAK_LOG, sapling.above(y));
            }
        }
        treeSpace(saplings.getFirst()).forEach(pos -> {
            boolean inGroup = saplings.stream().anyMatch(s -> s.getX() == pos.getX() && s.getZ() == pos.getZ());
            boolean tooTall = pos.getY() >= saplings.getFirst().getY() + 6;
            if (context.getBlockState(pos).is(BlockTags.LOGS) && (!inGroup || tooTall)) {
                throw context.assertionException(pos, "A log outside the four small trunks");
            }
        });
        context.succeed();
    }

    /**
     * Off Humus, saplings grow as in vanilla: an azalea on dirt grows its own tree over rooted
     * dirt, and a single dark oak sapling on dirt takes no bone meal.
     */
    public static void testSaplingsOffHumusGrowAsVanilla(GameTestHelper context) {
        BlockPos azalea = new BlockPos(2, 2, 2);
        context.setBlock(azalea.below(), Blocks.DIRT);
        context.setBlock(azalea, Blocks.AZALEA);
        BlockPos darkOak = new BlockPos(6, 2, 6);
        context.setBlock(darkOak.below(), Blocks.DIRT);
        context.setBlock(darkOak, Blocks.DARK_OAK_SAPLING);

        boneMeal(context, azalea);
        context.assertBlockPresent(Blocks.ROOTED_DIRT, azalea.below());
        context.assertBlockPresent(Blocks.OAK_LOG, azalea);

        if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), context.getLevel(), context.absolutePos(darkOak))) {
            throw context.assertionException(darkOak, "A lone dark oak sapling off Humus should take no bone meal");
        }
        context.assertBlockPresent(Blocks.DARK_OAK_SAPLING, darkOak);
        context.succeed();
    }

    /**
     * Humus is permanent and gives no growth boost: under a log it never turns to sand, and random
     * ticks never advance a lit sapling on it.
     */
    public static void testHumusIsPermanentAndGivesNoBoost(GameTestHelper context) {
        BlockPos underLog = new BlockPos(1, 1, 1);
        context.setBlock(underLog, LogisticsForestry.BLOCK.HUMUS);
        context.setBlock(underLog.above(), Blocks.OAK_LOG);
        BlockPos humus = new BlockPos(5, 1, 5);
        BlockPos sapling = humus.above();
        context.setBlock(humus, LogisticsForestry.BLOCK.HUMUS);
        context.setBlock(humus.east(), Blocks.GLOWSTONE);
        context.setBlock(sapling, Blocks.OAK_SAPLING);

        // Lit, so the sapling staying put is not down to a lack of light.
        context.startSequence()
                .thenWaitUntil(() -> {
                    int brightness = context.getLevel().getMaxLocalRawBrightness(context.absolutePos(sapling.above()));
                    context.assertTrue(
                            brightness >= SaplingBlock.BRIGHTNESS_FOR_SAPLING_GROWTH,
                            "Expected the sapling to be lit, light was " + brightness);
                })
                .thenExecute(() -> {
                    for (int i = 0; i < IDLE_RANDOM_TICKS; i++) {
                        context.randomTick(underLog);
                        context.randomTick(humus);
                    }

                    context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, underLog);
                    context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, humus);
                    context.assertBlockProperty(sapling, SaplingBlock.STAGE, 0);
                    context.succeed();
                });
    }

    /** Lays Humus at {@link #HUMUS_POS}, plants {@code germling} on it and bone-meals it until it grows. */
    private static void growOnHumus(GameTestHelper context, Block germling) {
        BlockPos sapling = HUMUS_POS.above();
        context.setBlock(HUMUS_POS, LogisticsForestry.BLOCK.HUMUS);
        context.setBlock(sapling, germling);
        boneMeal(context, sapling);
        if (context.getBlockState(sapling).is(germling)) {
            throw context.assertionException(sapling, "The germling should have grown on Humus");
        }
    }

    /** Bone-meals the germling at {@code pos} until it is no longer a germling, or gives up. */
    private static void boneMeal(GameTestHelper context, BlockPos pos) {
        BlockPos absolute = context.absolutePos(pos);
        for (int i = 0; i < MAX_BONE_MEAL && context.getBlockState(pos).is(BlockTags.SAPLINGS); i++) {
            BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), context.getLevel(), absolute);
        }
    }

    /**
     * Asserts the small Humus tree stands at {@code base}: a straight trunk of 4–6 {@code log}s with
     * no other logs around it, topped by {@code leaves}, over Humus that is still Humus.
     */
    private static void assertSmallTree(GameTestHelper context, BlockPos base, Block log, Predicate<BlockState> leaves) {
        context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, base.below());
        int height = 0;
        while (context.getBlockState(base.above(height)).is(log)) {
            height++;
        }
        if (height < 4 || height > 6) {
            throw context.assertionException(base, "Trunk should be 4-6 logs, was " + height);
        }
        if (!leaves.test(context.getBlockState(base.above(height)))) {
            throw context.assertionException(base.above(height), "The trunk should be topped by its leaves");
        }
        treeSpace(base).forEach(pos -> {
            boolean onTrunk = pos.getX() == base.getX() && pos.getZ() == base.getZ();
            BlockState state = context.getBlockState(pos);
            if (!onTrunk && state.is(BlockTags.LOGS)) {
                throw context.assertionException(pos, "A log outside the straight trunk");
            }
            if (state.is(Blocks.MANGROVE_ROOTS) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_CARPET)) {
                throw context.assertionException(pos, "A Humus tree should have no roots");
            }
        });
    }

    /** Every position a Humus tree on {@code base} can reach. */
    private static Stream<BlockPos> treeSpace(BlockPos base) {
        return BlockPos.betweenClosedStream(
                base.offset(-TREE_RADIUS, -1, -TREE_RADIUS), base.offset(TREE_RADIUS, TREE_HEIGHT, TREE_RADIUS));
    }

    private static void clearTreeSpace(GameTestHelper context, BlockPos base) {
        treeSpace(base).map(BlockPos::immutable).forEach(pos -> context.setBlock(pos, Blocks.AIR));
    }
}
