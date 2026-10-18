package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.compat.PlantCompat;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.machine.area.RingArea;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmGeometry;
import com.logistics.forestry.farm.FarmMachine;
import com.logistics.forestry.farm.FarmTierProfile;
import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.LevelTreeView;
import com.logistics.forestry.logic.TreeFeller;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arboretum GameTests at the real Basic tier, in the 15×18×15 {@code logistics_gametest:arboretum_field}
 * structure: the 11×11 ring, a block of canopy overhang on each side and room for full-grown oaks.
 * Fabric's {@code ArboretumGameTest} and NeoForge's {@code ArboretumGameTestRegistration} run them
 * in that structure.
 *
 * <p>The farm stands at the centre, one block above a floor at y=0, and works its own ring: plots at
 * y=2, their Humus at y=1. A Creative Engine powers it, a hopper on top feeds it and a hopper under
 * it pulls its produce into a chest. The farm works faster than its tier
 * ({@link FarmBlockEntity#setRateOverride}) so that whole fell-and-replant cycles fit a test; its
 * energy input is still the tier's.
 *
 * <p>Glass fills every courtyard column above the rig: markers where a canopy could reach, which the
 * farm must never break. Saplings are grown with bone meal on a seeded random, so each run grows the
 * same trees.
 */
public class ArboretumFieldGameTestBody {

    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/gametest/arboretum");

    private static final BlockPos FARM = new BlockPos(7, 1, 7);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SWITCH = ENGINE.south();
    private static final BlockPos INPUT_HOPPER = FARM.above();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.north();

    /** The Basic ring, in structure coordinates. */
    private static final RingArea RING =
            FarmGeometry.ring(FARM.above(), FarmTierProfile.defaults(MachineTier.BASIC).outerSize());

    /** Every plot of the ring. */
    private static final List<BlockPos> PLOTS = plots();

    /** What Basic plants: lane 1, every other step. */
    private static final List<BlockPos> SPOTS = spots();

    private static final int HEIGHT = 18;

    /** Fast enough that energy, not the work budget, paces the farm. */
    private static final int CHECKS_PER_SECOND = 400;

    private static final int ACTIONS_PER_SECOND = 400;

    private static final long SEED = 0x4152424F52L;

    /** Bone meal applications that grow a sapling into a tree many times over. */
    private static final int MAX_BONE_MEAL = 64;

    private static final int CYCLES = 3;

    /** How far from its sapling a grown oak's logs may stand, across and up. */
    private static final int TREE_REACH = 6;

    private static final int TREE_HEIGHT = 20;

    /** How long a cycle's felled trees may take to be replanted before the spots left count as unproductive. */
    private static final int CYCLE_DEADLINE_TICKS = 1500;

    private static List<BlockPos> plots() {
        List<BlockPos> plots = new ArrayList<>();
        for (int i = 0; i < RING.plotCount(); i++) {
            plots.add(RING.plot(i));
        }
        return List.copyOf(plots);
    }

    private static List<BlockPos> spots() {
        List<BlockPos> spots = new ArrayList<>();
        for (int step = 0; step < RING.laneLength(1); step += 2) {
            spots.add(RING.plot(1, step));
        }
        return List.copyOf(spots);
    }

    // ==================== Productivity ====================

    /**
     * Fed oak saplings and Humus through its hopper, the farm plants every spot; the saplings are
     * grown into trees, the farm fells them and replants, three times over. Every sapling grows, no
     * straight trunk touches another tree, at least 90% of the spot-cycles produce a tree, every
     * felled tree is replanted, logs, saplings and sticks come out, and nothing in the courtyard
     * changes. A fancy oak's branch may join two trees; the farm fells them together.
     */
    public static void testBasicRingStaysProductive(GameTestHelper context) {
        FarmBlockEntity farm = rig(context);
        // The output hopper also pulls the saplings the farm would replant from, so more follow the Humus.
        feed(
                context,
                new ItemStack(Items.OAK_SAPLING, SPOTS.size() + 4),
                new ItemStack(LogisticsForestry.BLOCK.HUMUS, 64),
                new ItemStack(Items.OAK_SAPLING, 64),
                new ItemStack(LogisticsForestry.BLOCK.HUMUS, 32));
        Map<BlockPos, Block> courtyard = snapshotCourtyard(context);

        int[] grown = new int[CYCLES];
        int[] branched = new int[CYCLES];
        int[] joined = new int[CYCLES];
        int[] replanted = new int[CYCLES];
        long[] cycleTicks = new long[CYCLES];
        int[] cycle = {0};
        long[] phaseStart = {context.getTick()};
        boolean[] done = {false};

        context.onEachTick(() -> {
            assertCourtyardUnchanged(context, courtyard);
            if (done[0]) {
                return;
            }
            List<BlockPos> planted = SPOTS.stream()
                    .filter(spot -> context.getBlockState(spot).is(Blocks.OAK_SAPLING))
                    .toList();
            long elapsed = context.getTick() - phaseStart[0];
            boolean allPlanted = planted.size() == SPOTS.size();
            if (!allPlanted && (cycle[0] == 0 || elapsed < CYCLE_DEADLINE_TICKS)) {
                return;
            }
            if (cycle[0] > 0) {
                replanted[cycle[0] - 1] = planted.size();
                cycleTicks[cycle[0] - 1] = elapsed;
            }
            if (cycle[0] == CYCLES) {
                done[0] = true;
                return;
            }
            Map<BlockPos, Set<BlockPos>> trees = new HashMap<>();
            for (int i = 0; i < SPOTS.size(); i++) {
                if (planted.contains(SPOTS.get(i))) {
                    trees.put(SPOTS.get(i), grow(context, SPOTS.get(i), cycle[0] * 100L + i));
                }
            }
            joined[cycle[0]] = assertTrunksApart(context, trees);
            branched[cycle[0]] = (int) trees.entrySet().stream()
                    .filter(tree -> !isStraight(tree.getKey(), tree.getValue()))
                    .count();
            grown[cycle[0]] = planted.size();
            cycle[0]++;
            phaseStart[0] = context.getTick();
        });

        context.succeedWhen(() -> {
            if (!done[0]) {
                throw context.assertionException(FARM, "Still on cycle " + cycle[0] + " of " + CYCLES + "; farm is "
                        + farm.status() + ", spots hold " + spotStates(context));
            }
            for (BlockPos spot : SPOTS) {
                if (!context.getBlockState(spot).is(Blocks.OAK_SAPLING)) {
                    throw context.assertionException(spot, "Every felled tree should be replanted, found "
                            + context.getBlockState(spot) + " on " + context.getBlockState(spot.below()) + "; farm is " + farm.status() + ", produce " + produce(context, farm));
                }
            }
            int productive = 0;
            for (int c = 0; c < CYCLES; c++) {
                productive += Math.min(grown[c], replanted[c]);
            }
            int possible = CYCLES * SPOTS.size();
            Map<Item, Integer> produce = produce(context, farm);
            LOGGER.info(
                    "Basic arboretum: {} spots; per cycle grown {} (branched {}, joined by a branch {}), "
                            + "replanted {} in {} ticks; {}/{} spot-cycles productive; produce {}",
                    SPOTS.size(), List.of(grown[0], grown[1], grown[2]), List.of(branched[0], branched[1], branched[2]),
                    List.of(joined[0], joined[1], joined[2]), List.of(replanted[0], replanted[1], replanted[2]),
                    List.of(cycleTicks[0], cycleTicks[1], cycleTicks[2]), productive, possible, produce);
            if (productive * 10 < possible * 9) {
                throw context.assertionException(FARM, "Only " + productive + " of " + possible
                        + " spot-cycles grew a tree that was felled and replanted");
            }
            for (Item item : List.of(Items.OAK_LOG, Items.OAK_SAPLING, Items.STICK)) {
                if (produce.getOrDefault(item, 0) == 0) {
                    throw context.assertionException(CHEST, "The farm's output should hold " + item + ", holds " + produce);
                }
            }
        });
    }

    // ==================== Felling bounds ====================

    /**
     * Two neighbouring farm trees whose canopies overlap are both felled whole. A log just outside the
     * ring, touching both trunks diagonally, is outside the wood bounds and is never taken; nothing in
     * the courtyard changes.
     */
    public static void testFellsNeighbouringCanopies(GameTestHelper context) {
        FarmBlockEntity farm = rig(context);
        Map<BlockPos, Block> courtyard = snapshotCourtyard(context);
        BlockPos west = RING.plot(1, 4);
        BlockPos east = RING.plot(1, 6);
        context.setBlock(west, Blocks.OAK_SAPLING);
        context.setBlock(east, Blocks.OAK_SAPLING);
        Map<BlockPos, Set<BlockPos>> trees = new HashMap<>();
        trees.put(west, grow(context, west, 1));
        trees.put(east, grow(context, east, 2));
        if (!isStraight(west, trees.get(west)) || !isStraight(east, trees.get(east))) {
            context.fail("The seeded saplings should grow straight oaks");
            return;
        }
        assertTrunksApart(context, trees);

        FellPlan westTree = plan(context, west);
        FellPlan eastTree = plan(context, east);
        if (westTree.blocks().stream().noneMatch(eastTree.blocks()::contains)) {
            context.fail("The two trees' canopies should overlap");
            return;
        }
        int logs = countLogs(context, westTree) + countLogs(context, eastTree);
        BlockPos outside = west.east().north().above(2);
        context.setBlock(outside, Blocks.OAK_LOG);

        context.onEachTick(() -> {
            assertCourtyardUnchanged(context, courtyard);
            context.assertBlockPresent(Blocks.OAK_LOG, outside);
        });
        context.succeedWhen(() -> {
            for (BlockPos plot : PLOTS) {
                for (int dy = 0; dy < HEIGHT - plot.getY(); dy++) {
                    BlockState state = context.getBlockState(plot.above(dy));
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                        throw context.assertionException(plot.above(dy), "Both trees should be felled whole, found "
                                + state + "; farm is " + farm.status());
                    }
                }
            }
            int felled = produce(context, farm).getOrDefault(Items.OAK_LOG, 0);
            if (felled < logs) {
                throw context.assertionException(CHEST, "All " + logs + " logs should reach the output, found " + felled);
            }
        });
    }

    /**
     * A natural oak standing just outside the ring, its canopy over the ring and its trunk touching
     * two farm trunks diagonally. The farm plants under it and fells its own trees beside it, but
     * never plans or breaks its wood. Its leaves all lie within the felling bounds, which reach past
     * the ring; how many the farm takes is logged.
     */
    public static void testForeignTreeWoodIsNeverFelled(GameTestHelper context) {
        BlockPos foreign = new BlockPos(RING.min().getX() - 1, RING.origin().getY(), FARM.getZ());
        context.setBlock(foreign.below(2), Blocks.STONE);
        context.setBlock(foreign.below(), Blocks.DIRT);
        context.setBlock(foreign, Blocks.OAK_SAPLING);
        grow(context, foreign, 3);
        FellPlan foreignTree = TreeFeller.plan(
                new LevelTreeView(context.getLevel()),
                context.absolutePos(foreign),
                TreeFeller.Limits.around(context.absolutePos(foreign), 8));
        Set<BlockPos> foreignWood = new HashSet<>();
        Set<BlockPos> foreignLeaves = new HashSet<>();
        for (BlockPos absolute : foreignTree.blocks()) {
            BlockPos pos = relative(context, absolute);
            if (context.getBlockState(pos).is(BlockTags.LOGS)) {
                if (RING.contains(pos)) {
                    context.fail("The seeded oak reaches into the ring at " + pos + "; it should stand outside it");
                    return;
                }
                foreignWood.add(pos);
            } else {
                foreignLeaves.add(pos);
            }
        }
        if (foreignLeaves.stream().noneMatch(RING::contains)) {
            context.fail("The foreign oak's canopy should hang over the ring");
            return;
        }
        BlockPos south = RING.plot(1, 30 + 4);
        BlockPos north = RING.plot(1, 30 + 6);
        for (BlockPos spot : List.of(south, north)) {
            if (Math.max(Math.abs(spot.getX() - foreign.getX()), Math.abs(spot.getZ() - foreign.getZ())) != 1) {
                context.fail(spot + " should be a spot beside the foreign trunk");
                return;
            }
        }

        FarmBlockEntity farm = rig(context);
        Map<BlockPos, Block> courtyard = snapshotCourtyard(context);
        feed(context, new ItemStack(Items.OAK_SAPLING, SPOTS.size() + 8), new ItemStack(LogisticsForestry.BLOCK.HUMUS, 8));
        assertNeverPlanned(context, foreignWood);

        Set<BlockPos> grown = new HashSet<>();
        context.onEachTick(() -> {
            assertCourtyardUnchanged(context, courtyard);
            foreignWood.forEach(pos -> context.assertBlockPresent(Blocks.OAK_LOG, pos));
            if (grown.isEmpty() && SPOTS.stream().allMatch(spot -> context.getBlockState(spot).is(Blocks.OAK_SAPLING))) {
                grow(context, south, 4);
                grow(context, north, 5);
                grown.add(south);
                grown.add(north);
                assertNeverPlanned(context, foreignWood);
            }
        });
        context.succeedWhen(() -> {
            if (grown.isEmpty()) {
                throw context.assertionException(FARM, "Every spot should be planted, under the foreign canopy too; "
                        + "farm is " + farm.status() + ", spots hold " + spotStates(context));
            }
            for (BlockPos spot : grown) {
                context.assertBlockPresent(Blocks.OAK_SAPLING, spot);
            }
            if (produce(context, farm).getOrDefault(Items.OAK_LOG, 0) == 0) {
                throw context.assertionException(CHEST, "The farm trees beside the foreign oak should be felled");
            }
            long overRing = foreignLeaves.stream()
                    .filter(RING::contains)
                    .filter(pos -> context.getBlockState(pos).is(Blocks.OAK_LEAVES))
                    .count();
            long beyondRing = foreignLeaves.stream()
                    .filter(pos -> !RING.contains(pos))
                    .filter(pos -> !context.getBlockState(pos).is(Blocks.OAK_LEAVES))
                    .count();
            LOGGER.info(
                    "Foreign oak: {} logs untouched; {} of {} leaves over the ring remain; {} of {} leaves beyond it broken",
                    foreignWood.size(), overRing, foreignLeaves.stream().filter(RING::contains).count(), beyondRing,
                    foreignLeaves.stream().filter(pos -> !RING.contains(pos)).count());
        });
    }

    // ==================== Rig ====================

    /**
     * Humus under every plot of the Basic ring on a stone floor, the farm at its centre with its
     * hoppers, chest and a running Creative Engine, and glass markers above the courtyard. Started.
     */
    private static FarmBlockEntity rig(GameTestHelper context) {
        for (BlockPos plot : PLOTS) {
            context.setBlock(plot.below(2), Blocks.STONE);
            context.setBlock(plot.below(), LogisticsForestry.BLOCK.HUMUS);
        }
        for (BlockPos column : courtyardColumns()) {
            for (int y = INPUT_HOPPER.getY(); y < HEIGHT; y++) {
                BlockPos pos = column.atY(y);
                if (!pos.equals(INPUT_HOPPER)) {
                    context.setBlock(pos, Blocks.GLASS);
                }
            }
        }
        context.setBlock(FARM, LogisticsForestry.BLOCK.ARBORETUM.defaultBlockState());
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        context.setBlock(INPUT_HOPPER, Blocks.HOPPER);

        FarmBlockEntity farm = context.getBlockEntity(FARM, FarmBlockEntity.class);
        farm.setRateOverride(CHECKS_PER_SECOND, ACTIONS_PER_SECOND);
        farm.start();

        context.setBlock(ENGINE_SWITCH, Blocks.REDSTONE_BLOCK);
        context.setBlock(ENGINE, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        CreativeEngineBlockEntity engine = context.getBlockEntity(ENGINE, CreativeEngineBlockEntity.class);
        // 20 -> 40 -> 80 RF/t: more than a Basic farm takes.
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        return farm;
    }

    /** Loads the input hopper, one stack per slot in order; it feeds the slots front to back. */
    private static void feed(GameTestHelper context, ItemStack... stacks) {
        HopperBlockEntity hopper = context.getBlockEntity(INPUT_HOPPER, HopperBlockEntity.class);
        for (int slot = 0; slot < stacks.length; slot++) {
            hopper.setItem(slot, stacks[slot]);
        }
    }

    /**
     * Bone-meals the sapling at {@code pos} into a tree on a random seeded by {@code salt} and returns
     * the logs it grew; throws if it never grows.
     */
    private static Set<BlockPos> grow(GameTestHelper context, BlockPos pos, long salt) {
        Set<BlockPos> before = logsAround(context, pos);
        ServerLevel level = context.getLevel();
        RandomSource random = RandomSource.create(SEED ^ salt);
        BlockPos absolute = context.absolutePos(pos);
        for (int i = 0; i < MAX_BONE_MEAL; i++) {
            BlockState state = context.getBlockState(pos);
            if (!state.is(BlockTags.SAPLINGS)) {
                break;
            }
            PlantCompat.fertilize(level, random, absolute, state);
        }
        if (!context.getBlockState(pos).is(BlockTags.LOGS)) {
            throw context.assertionException(pos, "The sapling should grow into a tree within " + MAX_BONE_MEAL
                    + " bone meal, is " + context.getBlockState(pos));
        }
        Set<BlockPos> grown = logsAround(context, pos);
        grown.removeAll(before);
        return grown;
    }

    /** Every log within a tree's reach of {@code base}. */
    private static Set<BlockPos> logsAround(GameTestHelper context, BlockPos base) {
        Set<BlockPos> logs = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-TREE_REACH, 0, -TREE_REACH), base.offset(TREE_REACH, TREE_HEIGHT, TREE_REACH))) {
            if (context.getBlockState(pos).is(BlockTags.LOGS)) {
                logs.add(pos.immutable());
            }
        }
        return logs;
    }

    // ==================== Checks ====================

    /** The farm's own felling plan for the tree at {@code plot}. */
    private static FellPlan plan(GameTestHelper context, BlockPos plot) {
        RingArea ring = FarmGeometry.ring(context.absolutePos(RING.origin()), RING.outer());
        return TreeFeller.plan(
                new LevelTreeView(context.getLevel()),
                context.absolutePos(plot),
                TreeFeller.Limits.within(ring.bounds(), ring.keepOut()));
    }

    /**
     * Throws unless every tree in {@code trees}, each spot with the logs it grew, is fellable and no
     * straight trunk touches another tree's wood, even diagonally. Returns how many pairs of trees a
     * branch joins: a fancy oak's branches can reach across the gap between spots.
     */
    private static int assertTrunksApart(GameTestHelper context, Map<BlockPos, Set<BlockPos>> trees) {
        for (BlockPos spot : trees.keySet()) {
            FellPlan tree = plan(context, spot);
            if (!tree.isFound()) {
                throw context.assertionException(spot, "The tree grown here should be fellable: " + tree.reason());
            }
        }
        List<BlockPos> spots = new ArrayList<>(trees.keySet());
        int joined = 0;
        for (int a = 0; a < spots.size(); a++) {
            for (int b = a + 1; b < spots.size(); b++) {
                BlockPos first = spots.get(a);
                BlockPos second = spots.get(b);
                if (!touch(trees.get(first), trees.get(second))) {
                    continue;
                }
                if (isStraight(first, trees.get(first)) && isStraight(second, trees.get(second))) {
                    throw context.assertionException(first, "The straight trunks at " + first + " and " + second + " touch");
                }
                joined++;
            }
        }
        return joined;
    }

    /** Whether every log a tree grew stands in its spot's column. */
    private static boolean isStraight(BlockPos spot, Set<BlockPos> logs) {
        return logs.stream().allMatch(log -> log.getX() == spot.getX() && log.getZ() == spot.getZ());
    }

    private static boolean touch(Set<BlockPos> first, Set<BlockPos> second) {
        for (BlockPos a : first) {
            for (BlockPos b : second) {
                if (Math.abs(a.getX() - b.getX()) <= 1 && Math.abs(a.getY() - b.getY()) <= 1
                        && Math.abs(a.getZ() - b.getZ()) <= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Throws if the farm's plan from any plot of the ring would take a block of {@code wood}. */
    private static void assertNeverPlanned(GameTestHelper context, Set<BlockPos> wood) {
        for (BlockPos plot : PLOTS) {
            for (BlockPos absolute : plan(context, plot).blocks()) {
                if (wood.contains(relative(context, absolute))) {
                    throw context.assertionException(plot, "Felling from here would take the foreign tree's wood");
                }
            }
        }
    }

    private static int countLogs(GameTestHelper context, FellPlan tree) {
        int logs = 0;
        for (BlockPos absolute : tree.blocks()) {
            if (context.getLevel().getBlockState(absolute).is(Blocks.OAK_LOG)) {
                logs++;
            }
        }
        return logs;
    }

    private static List<BlockPos> courtyardColumns() {
        List<BlockPos> columns = new ArrayList<>();
        int half = FarmGeometry.COURTYARD / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                columns.add(new BlockPos(FARM.getX() + dx, 0, FARM.getZ() + dz));
            }
        }
        return columns;
    }

    /** Every block of the courtyard's columns, floor to ceiling. */
    private static Map<BlockPos, Block> snapshotCourtyard(GameTestHelper context) {
        Map<BlockPos, Block> blocks = new HashMap<>();
        for (BlockPos column : courtyardColumns()) {
            for (int y = 0; y < HEIGHT; y++) {
                BlockPos pos = column.atY(y);
                blocks.put(pos, context.getBlockState(pos).getBlock());
            }
        }
        return blocks;
    }

    private static void assertCourtyardUnchanged(GameTestHelper context, Map<BlockPos, Block> courtyard) {
        courtyard.forEach((pos, block) -> {
            Block now = context.getBlockState(pos).getBlock();
            if (now != block) {
                throw context.assertionException(pos, "The courtyard should never change: " + block + " became " + now);
            }
        });
    }

    /** Everything the farm has put out, in its output slots and the chest after them. */
    private static Map<Item, Integer> produce(GameTestHelper context, FarmBlockEntity farm) {
        Map<Item, Integer> items = new HashMap<>();
        for (int slot = FarmMachine.OUTPUT_START; slot < FarmMachine.OUTPUT_END; slot++) {
            ItemStack stack = farm.getItem(slot);
            items.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        ChestBlockEntity chest = context.getBlockEntity(CHEST, ChestBlockEntity.class);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            items.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        items.remove(Items.AIR);
        return items;
    }

    /**
     * {@code absolute} in structure coordinates. {@link GameTestHelper#relativePos} turns an
     * unrotated test's positions half way round, so it cannot undo {@link GameTestHelper#absolutePos}.
     */
    private static BlockPos relative(GameTestHelper context, BlockPos absolute) {
        BlockPos relative = absolute.subtract(context.absolutePos(BlockPos.ZERO));
        if (!context.absolutePos(relative).equals(absolute)) {
            throw context.assertionException("The test structure should not be rotated");
        }
        return relative;
    }

    private static List<String> spotStates(GameTestHelper context) {
        return SPOTS.stream()
                .map(spot -> spot.toShortString() + "=" + context.getBlockState(spot).getBlock())
                .toList();
    }
}
