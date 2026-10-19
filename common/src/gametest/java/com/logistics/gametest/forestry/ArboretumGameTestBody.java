package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.compat.PlantCompat;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.machine.area.RingArea;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Arboretum GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code ArboretumGameTest} delegates to these methods and NeoForge's
 * {@code ArboretumGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every rig is the real thing: a Creative Engine kept running by a redstone block powers the
 * farm, a hopper on top feeds it, and a hopper underneath pulls its produce into a chest beside it.
 * The farm at the centre works the 40-plot ring between a 3×3 and a 7×7 square around it
 * ({@link FarmBlockEntity#setAreaOverride}) at y=2, one block above it, with the soil at y=1 level
 * with the farm on a floor at y=0, and keeps six blocks clear above each plot
 * ({@link FarmBlockEntity#setClearanceHeightOverride}) so the clearing stays inside the structure.
 * The structure's ceiling, six blocks above the plots, leaves a birch too little headroom, so trees
 * grown from saplings are oaks, whose shortest form fits. The engine, hoppers and chest stand in the 3×3
 * inside the ring, which the farm never touches. Saplings go on every even step of the outer lane;
 * the inner lane, beside the 3×3, stays bare. The real Basic ring is covered by
 * {@link ArboretumFieldGameTestBody}.
 */
public class ArboretumGameTestBody {

    private static final BlockPos FARM = new BlockPos(3, 1, 3);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SWITCH = ENGINE.south();
    private static final BlockPos INPUT_HOPPER = FARM.above();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.north();

    private static final RingArea RING = new RingArea(FARM.above(), 3, 7);
    private static final int CLEARANCE = 6;

    /** Every plot of the ring, in scan order. */
    private static final List<BlockPos> PLOTS = plots(false);

    /** The plots that take a sapling: every even step of the outer lane. */
    private static final List<BlockPos> SPOTS = plots(true);

    /** Hoppers move one item per 8 ticks: long after a few saplings are all in the farm. */
    private static final int SETTLE_TICKS = 250;

    /** Logs in each column of a hand-set 2×2 trunk. */
    private static final int TRUNK_HEIGHT = 4;

    /** Bone meal applications that grow a sapling into a tree many times over. */
    private static final int MAX_BONE_MEAL = 64;

    private static List<BlockPos> plots(boolean spotsOnly) {
        List<BlockPos> plots = new ArrayList<>();
        for (int i = 0; i < RING.plotCount(); i++) {
            if (!spotsOnly || (RING.lane(i) == 1 && RING.step(i) % 2 == 0)) {
                plots.add(RING.plot(i));
            }
        }
        return List.copyOf(plots);
    }

    // ==================== Planting ====================

    /** Hopper-fed oak saplings go on the ring's spots and nowhere else; the inner lane stays bare. */
    public static void testPlantsSaplingsOnLattice(GameTestHelper context) {
        rig(context, true, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        feed(context, new ItemStack(Items.OAK_SAPLING, SPOTS.size()));

        context.onEachTick(() -> assertOffLatticeUntouched(context));
        context.succeedWhen(() -> {
            for (BlockPos cell : SPOTS) {
                assertOakPlanted(context, cell);
            }
        });
    }

    /**
     * A sapling grows into a tree; the farm fells it whole, its logs reach the chest through the
     * output hopper, and the cleared spot is planted again. The saplings fed fill every spot with
     * some to spare for replanting; the test bone-meals each spot, but never one that has already
     * grown a tree, so a replanted sapling stays a sapling. Humus rides along to relay the sand each
     * felled tree leaves.
     */
    public static void testFellsGrownTreeAndReplants(GameTestHelper context) {
        rig(context, true, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        feed(
                context,
                new ItemStack(Items.OAK_SAPLING, SPOTS.size() + 8),
                new ItemStack(LogisticsForestry.BLOCK.HUMUS, SPOTS.size()));

        Set<BlockPos> grown = new HashSet<>();
        context.onEachTick(() -> {
            for (BlockPos cell : SPOTS) {
                BlockState state = context.getBlockState(cell);
                if (state.is(Blocks.OAK_LOG)) {
                    grown.add(cell);
                } else if (state.is(Blocks.OAK_SAPLING) && !grown.contains(cell)) {
                    BlockPos pos = context.absolutePos(cell);
                    PlantCompat.fertilize(context.getLevel(), context.getLevel().getRandom(), pos, state);
                }
            }
        });
        context.succeedWhen(() -> {
            if (!chestHolds(context, Items.OAK_LOG)) {
                throw context.assertionException("Felled oak logs should reach the chest; farm is "
                        + context.getBlockEntity(FARM, FarmBlockEntity.class).status() + ", spots hold "
                        + SPOTS.stream().map(cell -> context.getBlockState(cell).toString()).toList());
            }
            for (BlockPos cell : grown) {
                if (context.getBlockState(cell).is(Blocks.OAK_SAPLING)) {
                    return;
                }
            }
            throw context.assertionException("A felled tree's spot should be replanted, grown so far: " + grown);
        });
    }

    // ==================== Gates ====================

    /** Saplings reach the farm, but without power nothing is planted. */
    public static void testNoPowerPlantsNothing(GameTestHelper context) {
        FarmBlockEntity farm = rig(context, false, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        feed(context, new ItemStack(Items.OAK_SAPLING, 4));

        context.runAfterDelay(SETTLE_TICKS, () -> {
            if (!hopper(context, INPUT_HOPPER).isEmpty()) {
                context.fail("The hopper should have fed its saplings into the farm");
                return;
            }
            if (!ringIsEmpty(context)) {
                return;
            }
            if (farm.status() != FarmStatus.NO_POWER) {
                context.fail("An unpowered farm should report No power, was " + farm.status());
                return;
            }
            context.succeed();
        });
    }

    /** A redstone signal on the farm pauses it: powered and stocked, it still plants nothing. */
    public static void testRedstonePausesFarm(GameTestHelper context) {
        FarmBlockEntity farm = rig(context, true, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        context.setBlock(FARM.east(), Blocks.REDSTONE_BLOCK);
        feed(context, new ItemStack(Items.OAK_SAPLING, 4));

        context.runAfterDelay(SETTLE_TICKS, () -> {
            if (!hopper(context, INPUT_HOPPER).isEmpty()) {
                context.fail("The hopper should have fed its saplings into the farm");
                return;
            }
            if (!ringIsEmpty(context)) {
                return;
            }
            if (farm.status() != FarmStatus.PAUSED) {
                context.fail("A farm with a redstone signal should report Paused, was " + farm.status());
                return;
            }
            context.succeed();
        });
    }

    // ==================== Clearing ====================

    /**
     * Stone standing on a spot and planks three blocks above another plot are cleared
     * into the chest, and the spot is planted. Logs dressed in player-placed leaves are a build, not a
     * tree, and the farm's own kind of block: they are neither felled nor cleared.
     */
    public static void testClearsBlocksFromRing(GameTestHelper context) {
        rig(context, true, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        BlockPos stone = SPOTS.get(1);
        BlockPos planks = PLOTS.get(1).above(3);
        context.setBlock(stone, Blocks.STONE);
        context.setBlock(planks, Blocks.OAK_PLANKS);

        BlockPos pillar = SPOTS.get(SPOTS.size() - 1);
        BlockState persistentLeaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        List<BlockPos> build = new ArrayList<>();
        for (int dy = 0; dy < 4; dy++) {
            build.add(pillar.above(dy));
            context.setBlock(pillar.above(dy), Blocks.OAK_LOG);
        }
        List<BlockPos> leaves = new ArrayList<>();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos leaf = pillar.above(3).relative(side);
            if (leaf.getX() >= 0 && leaf.getZ() >= 0) {
                leaves.add(leaf); // the pillar stands on the ring's west edge
            }
        }
        leaves.add(pillar.above(4));
        leaves.forEach(pos -> context.setBlock(pos, persistentLeaves));

        feed(context, new ItemStack(Items.OAK_SAPLING, SPOTS.size()));

        context.onEachTick(() -> {
            build.forEach(pos -> context.assertBlockPresent(Blocks.OAK_LOG, pos));
            leaves.forEach(pos -> context.assertBlockPresent(Blocks.OAK_LEAVES, pos));
        });
        context.succeedWhen(() -> {
            context.assertBlockPresent(Blocks.OAK_SAPLING, stone);
            context.assertBlockPresent(Blocks.AIR, planks);
            if (!chestHolds(context, Items.STONE) && !chestHolds(context, Items.COBBLESTONE)) {
                throw context.assertionException(stone, "The cleared stone should have reached the chest");
            }
            if (!chestHolds(context, Items.OAK_PLANKS)) {
                throw context.assertionException(planks, "The cleared planks should have reached the chest");
            }
        });
    }

    // ==================== Soil ====================

    /**
     * Humus in the soil slots replaces the dirt under every plot of the ring, and the dirt dug out
     * arrives in the chest; the spots are planted on the Humus.
     */
    public static void testHumusReplacesDirtAcrossTheRing(GameTestHelper context) {
        rig(context, true, Blocks.DIRT.defaultBlockState());
        // Humus first, so it is all in the farm before the saplings that let it plant.
        feed(
                context,
                new ItemStack(LogisticsForestry.BLOCK.HUMUS, PLOTS.size()),
                new ItemStack(Items.OAK_SAPLING, SPOTS.size()));

        context.onEachTick(() -> assertOffLatticeUntouched(context));
        context.succeedWhen(() -> {
            for (BlockPos cell : PLOTS) {
                if (!spentByTree(context, cell)) {
                    context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, cell.below());
                }
            }
            for (BlockPos cell : SPOTS) {
                assertOakPlanted(context, cell);
            }
            context.assertContainerContains(CHEST, Items.DIRT);
        });
    }

    /**
     * Placed on a grass floor, the farm lays Humus level with itself and plants saplings on top,
     * leaving the floor below alone: grass, or dirt where vanilla kills grass under the Humus, never
     * dug out. The soil layer was air, so no dirt reaches the chest.
     */
    public static void testLaysHumusLevelWithFarmOnGrass(GameTestHelper context) {
        int floorY = FARM.getY() - 1;
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                context.setBlock(new BlockPos(x, floorY, z), Blocks.GRASS_BLOCK);
            }
        }
        rig(context, true, null, null);
        feed(
                context,
                new ItemStack(LogisticsForestry.BLOCK.HUMUS, PLOTS.size()),
                new ItemStack(Items.OAK_SAPLING, SPOTS.size()));

        context.onEachTick(() -> {
            assertOffLatticeUntouched(context);
            for (BlockPos cell : PLOTS) {
                BlockPos floor = cell.below(2);
                BlockState below = context.getBlockState(floor);
                if (!below.is(Blocks.GRASS_BLOCK) && !below.is(Blocks.DIRT)) {
                    throw context.assertionException(floor, "The floor under the soil should be left alone, is " + below);
                }
            }
        });
        context.succeedWhen(() -> {
            for (BlockPos cell : PLOTS) {
                if (!spentByTree(context, cell)) {
                    context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, cell.below());
                }
            }
            for (BlockPos cell : SPOTS) {
                assertOakPlanted(context, cell);
            }
            if (chestHolds(context, Items.DIRT)) {
                throw context.assertionException(CHEST, "Laying Humus on air should dig no dirt out");
            }
        });
    }

    /**
     * A tree grown on Humus is felled into the chest; the Humus under it is spent and turns to sand,
     * which the farm digs out into the chest before laying fresh Humus from its soil slots.
     */
    public static void testFelledTreeLeavesSandThatIsRelaidWithHumus(GameTestHelper context) {
        rig(context, true, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        // On the east edge: every column a growing oak needs clear lies inside the structure.
        BlockPos spot = SPOTS.get(4);
        if (!growTree(context, spot, Blocks.OAK_SAPLING.defaultBlockState())) {
            context.fail("Bone meal should have grown the oak sapling into a tree");
            return;
        }
        feed(context, new ItemStack(LogisticsForestry.BLOCK.HUMUS, 4));

        context.succeedWhen(() -> {
            if (!chestHolds(context, Items.OAK_LOG)) {
                throw context.assertionException(spot, "The felled oak logs should have reached the chest");
            }
            if (!chestHolds(context, Items.SAND)) {
                throw context.assertionException(
                        spot.below(), "The sand left under the felled tree should have reached the chest");
            }
            context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, spot.below());
        });
    }

    /**
     * A dark oak's 2×2 trunk standing on Humus in the ring's north-west corner, which the farm never
     * plants but a player or nature may grow, is felled into the chest, and every one of the four
     * Humus blocks under it is left as sand. No Humus is fed, so the sand stays. Bone meal does not
     * grow a dark oak in this corner of the test structure, so the trunk and a natural canopy over it
     * are set by hand.
     */
    public static void testFelled2x2TreeLeavesSandUnderAllFourCells(GameTestHelper context) {
        rig(context, true, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        int y = RING.origin().getY();
        List<BlockPos> trunk =
                List.of(new BlockPos(0, y, 0), new BlockPos(1, y, 0), new BlockPos(0, y, 1), new BlockPos(1, y, 1));
        BlockState canopy = Blocks.DARK_OAK_LEAVES
                .defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 1);
        for (BlockPos cell : trunk) {
            if (!PLOTS.contains(cell)) {
                context.fail(cell + " should be a plot of the ring");
                return;
            }
            for (int dy = 0; dy < TRUNK_HEIGHT; dy++) {
                context.setBlock(cell.above(dy), Blocks.DARK_OAK_LOG);
            }
            context.setBlock(cell.above(TRUNK_HEIGHT), canopy);
        }

        context.succeedWhen(() -> {
            if (!chestHolds(context, Items.DARK_OAK_LOG)) {
                throw context.assertionException(trunk.get(0), "The felled dark oak logs should have reached the chest");
            }
            for (BlockPos cell : trunk) {
                context.assertBlockPresent(Blocks.AIR, cell);
                context.assertBlockPresent(Blocks.SAND, cell.below());
            }
        });
    }

    /**
     * With no Humus in its soil slots the farm leaves sand under a plot where it is and plants nothing
     * on it, reporting No soil, while the Humus plots around it are planted.
     */
    public static void testWithoutHumusSandIsLeftUnplanted(GameTestHelper context) {
        FarmBlockEntity farm = rig(context, true, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        BlockPos spot = SPOTS.get(1);
        context.setBlock(spot.below(), Blocks.SAND);
        feed(context, new ItemStack(Items.OAK_SAPLING, SPOTS.size()));

        context.onEachTick(() -> {
            context.assertBlockPresent(Blocks.SAND, spot.below());
            context.assertBlockPresent(Blocks.AIR, spot);
        });
        context.succeedWhen(() -> {
            for (BlockPos cell : SPOTS) {
                if (!cell.equals(spot)) {
                    assertOakPlanted(context, cell);
                }
            }
            context.assertTrue(
                    farm.status() == FarmStatus.NO_SOIL, "The farm should report No soil, was " + farm.status());
        });
    }

    /**
     * Every germling the Arboretum accepts can be planted on Humus, its only soil, including the
     * mangrove propagule, azalea and cherry. Saplings that need a 2×2 group, such as dark oak, are
     * refused.
     */
    public static void testEveryGermlingGrowsOnArboretumSoils(GameTestHelper context) {
        FarmLogic logic = ((FarmMachineBlock) LogisticsForestry.BLOCK.ARBORETUM).logic();
        BlockPos humusCell = new BlockPos(5, 1, 5);
        context.setBlock(humusCell.below(), LogisticsForestry.BLOCK.HUMUS);

        List<Item> germlings = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(LogisticsForestry.TAG.ARBORETUM_GERMLINGS)) {
            germlings.add(holder.value());
        }
        for (Item required : List.of(
                Items.OAK_SAPLING, Items.CHERRY_SAPLING, Items.AZALEA, Items.FLOWERING_AZALEA,
                Items.MANGROVE_PROPAGULE)) {
            if (!germlings.contains(required)) {
                context.fail("The Arboretum should accept " + required + " as a germling");
                return;
            }
        }
        for (Item germling : germlings) {
            ItemStack stack = new ItemStack(germling);
            if (stack.is(LogisticsForestry.TAG.REQUIRES_2X2)) {
                if (logic.isGermling(stack)) {
                    context.fail("The Arboretum should refuse " + germling + ", which needs a 2×2 group");
                    return;
                }
                continue;
            }
            if (!logic.isGermling(stack)) {
                context.fail("The Arboretum's germling check should accept " + germling);
                return;
            }
            BlockState plant = logic.plantState(stack, context.absolutePos(humusCell), null);
            if (plant == null || !plant.canSurvive(context.getLevel(), context.absolutePos(humusCell))) {
                context.fail(germling + " should be plantable on Humus");
                return;
            }
            if (!logic.isOwnGrowth(plant)) {
                context.fail("A planted " + germling + " should be the Arboretum's own growth");
                return;
            }
        }
        if (!new ItemStack(Items.DARK_OAK_SAPLING).is(LogisticsForestry.TAG.REQUIRES_2X2)
                || new ItemStack(Items.OAK_SAPLING).is(LogisticsForestry.TAG.REQUIRES_2X2)) {
            context.fail("Dark oak, and not oak, should need a 2×2 group");
            return;
        }
        if (logic.isGermling(new ItemStack(Items.DARK_OAK_SAPLING))) {
            context.fail("The Arboretum should refuse dark oak saplings");
            return;
        }
        if (!logic.isSoil(new ItemStack(LogisticsForestry.BLOCK.HUMUS)) || logic.isSoil(new ItemStack(Items.DIRT))) {
            context.fail("Humus, and not dirt, should be the Arboretum's soil");
            return;
        }
        context.succeed();
    }

    // ==================== Rig ====================

    /** {@link #rig(GameTestHelper, boolean, BlockState, BlockState)} on a stone floor, which keeps sand from falling. */
    private static FarmBlockEntity rig(GameTestHelper context, boolean powered, BlockState ground) {
        return rig(context, powered, ground, Blocks.STONE.defaultBlockState());
    }

    /**
     * {@code ground} under the ring on {@code floor} ({@code null} lays none), the farm at its centre,
     * a hopper on top of and one below the farm, and a chest after the output hopper; with
     * {@code powered}, a running Creative Engine beside it. The farm is started.
     */
    private static FarmBlockEntity rig(
            GameTestHelper context, boolean powered, @Nullable BlockState ground, @Nullable BlockState floor) {
        for (BlockPos cell : PLOTS) {
            if (floor != null) {
                context.setBlock(cell.below(2), floor);
            }
            if (ground != null) {
                context.setBlock(cell.below(), ground);
            }
        }
        context.setBlock(FARM, LogisticsForestry.BLOCK.ARBORETUM.defaultBlockState());
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        context.setBlock(INPUT_HOPPER, Blocks.HOPPER);

        FarmBlockEntity farm = context.getBlockEntity(FARM, FarmBlockEntity.class);
        farm.setAreaOverride(RING.inner(), RING.outer());
        farm.setClearanceHeightOverride(CLEARANCE);
        farm.start();

        if (powered) {
            context.setBlock(ENGINE_SWITCH, Blocks.REDSTONE_BLOCK);
            context.setBlock(ENGINE, LogisticsPower.BLOCK.CREATIVE_ENGINE
                    .defaultBlockState()
                    .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                    .setValue(AbstractEngineBlock.POWERED, true));
            CreativeEngineBlockEntity engine = context.getBlockEntity(ENGINE, CreativeEngineBlockEntity.class);
            // 20 -> 40 -> 80 -> 160 RF/t: enough to fell a tree at full speed.
            engine.cycleOutputLevel();
            engine.cycleOutputLevel();
            engine.cycleOutputLevel();
        }
        return farm;
    }

    /** Plants {@code sapling} at {@code pos} and bone-meals it into a tree; false if it never grew. */
    private static boolean growTree(GameTestHelper context, BlockPos pos, BlockState sapling) {
        context.setBlock(pos, sapling);
        BlockPos absolute = context.absolutePos(pos);
        for (int i = 0; i < MAX_BONE_MEAL; i++) {
            BlockState state = context.getBlockState(pos);
            if (!state.is(sapling.getBlock())) {
                return state.is(BlockTags.LOGS);
            }
            PlantCompat.fertilize(context.getLevel(), context.getLevel().getRandom(), absolute, state);
        }
        return false;
    }

    /** Loads the input hopper, one stack per slot in order; it feeds the slots front to back. */
    private static void feed(GameTestHelper context, ItemStack... stacks) {
        HopperBlockEntity hopper = context.getBlockEntity(INPUT_HOPPER, HopperBlockEntity.class);
        for (int slot = 0; slot < stacks.length; slot++) {
            hopper.setItem(slot, stacks[slot]);
        }
    }

    private static boolean chestHolds(GameTestHelper context, Item item) {
        ChestBlockEntity chest = context.getBlockEntity(CHEST, ChestBlockEntity.class);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (chest.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static HopperBlockEntity hopper(GameTestHelper context, BlockPos pos) {
        return context.getBlockEntity(pos, HopperBlockEntity.class);
    }

    /**
     * Throws unless {@code cell} holds an oak sapling, or that sapling has already grown into a tree.
     * Humus random ticks can grow a planted sapling mid-test; the farm then fells it, leaving sand.
     */
    private static void assertOakPlanted(GameTestHelper context, BlockPos cell) {
        BlockState state = context.getBlockState(cell);
        if (state.is(Blocks.OAK_SAPLING) || state.is(Blocks.OAK_LOG) || spentByTree(context, cell)) {
            return;
        }
        throw context.assertionException(
                cell, "Expected an oak sapling, found " + state + " on " + context.getBlockState(cell.below()));
    }

    /** Whether {@code cell} grew an oak that the farm felled into the chest, spending its Humus to sand. */
    private static boolean spentByTree(GameTestHelper context, BlockPos cell) {
        return context.getBlockState(cell.below()).is(Blocks.SAND) && chestHolds(context, Items.OAK_LOG);
    }

    private static void assertOffLatticeUntouched(GameTestHelper context) {
        for (BlockPos cell : PLOTS) {
            if (!SPOTS.contains(cell)) {
                context.assertBlockPresent(Blocks.AIR, cell);
            }
        }
    }

    /** Fails the test and returns false when anything stands in the ring. */
    private static boolean ringIsEmpty(GameTestHelper context) {
        for (BlockPos cell : PLOTS) {
            Block block = context.getBlockState(cell).getBlock();
            if (block != Blocks.AIR) {
                context.fail("Nothing should have been planted, found " + block + " at " + cell);
                return false;
            }
        }
        return true;
    }
}
