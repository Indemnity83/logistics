package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
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

/**
 * Shared Arboretum GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code ArboretumGameTest} delegates to these methods and NeoForge's
 * {@code ArboretumGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every rig is the real thing: a Creative Engine kept running by a redstone block powers the
 * farm, a hopper behind it feeds it, and a hopper underneath pulls its produce into a chest. The
 * farm faces south over a 3×3 plot ({@link FarmBlockEntity#setAreaSizeOverride}) at y=1, soil at
 * y=0. Its planting lattice is the plot's four corners; the anchor of its one 2×2 group is the
 * near-left corner, looking south. Nothing stands above y=1 next to the plot, where it would stop
 * a near-row tree from growing.
 */
public class ArboretumGameTestBody {

    private static final BlockPos FARM = new BlockPos(3, 1, 1);
    private static final BlockPos ENGINE = new BlockPos(2, 1, 1);
    private static final BlockPos ENGINE_SWITCH = new BlockPos(1, 1, 1);
    private static final BlockPos INPUT_HOPPER = FARM.north();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = new BlockPos(3, 0, 0);

    /** Plot cells, rows from the farm outward, columns left to right looking south. */
    private static final BlockPos[][] PLOT = {
        {new BlockPos(4, 1, 2), new BlockPos(3, 1, 2), new BlockPos(2, 1, 2)},
        {new BlockPos(4, 1, 3), new BlockPos(3, 1, 3), new BlockPos(2, 1, 3)},
        {new BlockPos(4, 1, 4), new BlockPos(3, 1, 4), new BlockPos(2, 1, 4)},
    };

    private static final List<BlockPos> LATTICE = List.of(PLOT[0][0], PLOT[0][2], PLOT[2][0], PLOT[2][2]);

    /** Hoppers move one item per 8 ticks; a 3×3 pass at the default interval takes 90. */
    private static final int SETTLE_TICKS = 250;

    // ==================== Planting ====================

    /** Hopper-fed oak saplings go on the four lattice corners of the plot and nowhere else. */
    public static void testPlantsSaplingsOnLattice(GameTestHelper context) {
        rig(context, true);
        feed(context, new ItemStack(Items.OAK_SAPLING, 4));

        context.onEachTick(() -> assertOffLatticeUntouched(context));
        context.succeedWhen(() -> {
            for (BlockPos cell : LATTICE) {
                context.assertBlockPresent(Blocks.OAK_SAPLING, cell);
            }
        });
    }

    /**
     * A fertilized sapling grows into a tree; the farm fells it whole, its logs reach the chest
     * through the output hopper, and the cleared corner is planted again.
     */
    public static void testFellsGrownTreeAndReplants(GameTestHelper context) {
        rig(context, true);
        feed(
                context,
                new ItemStack(Items.BIRCH_SAPLING, 8),
                new ItemStack(LogisticsForestry.ITEM.FERTILIZER, 8));

        Set<BlockPos> grown = new HashSet<>();
        context.onEachTick(() -> {
            for (BlockPos cell : LATTICE) {
                if (context.getBlockState(cell).is(Blocks.BIRCH_LOG)) {
                    grown.add(cell);
                }
            }
        });
        context.succeedWhen(() -> {
            if (!chestHolds(context, Items.BIRCH_LOG)) {
                throw context.assertionException("Felled birch logs should reach the chest; farm is "
                        + context.getBlockEntity(FARM, FarmBlockEntity.class).status() + ", corners hold "
                        + LATTICE.stream().map(cell -> context.getBlockState(cell).toString()).toList());
            }
            for (BlockPos cell : grown) {
                if (context.getBlockState(cell).is(Blocks.BIRCH_SAPLING)) {
                    return;
                }
            }
            throw context.assertionException("A felled tree's corner should be replanted, grown so far: " + grown);
        });
    }

    // ==================== Gates ====================

    /** Saplings reach the farm, but without power nothing is planted. */
    public static void testNoPowerPlantsNothing(GameTestHelper context) {
        FarmBlockEntity farm = rig(context, false);
        feed(context, new ItemStack(Items.OAK_SAPLING, 4));

        context.runAfterDelay(SETTLE_TICKS, () -> {
            if (!hopper(context, INPUT_HOPPER).isEmpty()) {
                context.fail("The hopper should have fed its saplings into the farm");
                return;
            }
            if (!plotIsEmpty(context)) {
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
        FarmBlockEntity farm = rig(context, true);
        context.setBlock(FARM.east(), Blocks.REDSTONE_BLOCK);
        feed(context, new ItemStack(Items.OAK_SAPLING, 4));

        context.runAfterDelay(SETTLE_TICKS, () -> {
            if (!hopper(context, INPUT_HOPPER).isEmpty()) {
                context.fail("The hopper should have fed its saplings into the farm");
                return;
            }
            if (!plotIsEmpty(context)) {
                return;
            }
            if (farm.status() != FarmStatus.PAUSED) {
                context.fail("A farm with a redstone signal should report Paused, was " + farm.status());
                return;
            }
            context.succeed();
        });
    }

    // ==================== Safety ====================

    /**
     * Stone under a planting corner is never dug up for soil, and logs dressed in player-placed
     * leaves standing in the plot are a build, not a tree: neither is touched while the farm
     * plants the other corners.
     */
    public static void testLeavesStoneAndPlayerBuildsAlone(GameTestHelper context) {
        rig(context, true);
        BlockPos stoneCorner = PLOT[0][2];
        context.setBlock(stoneCorner.below(), Blocks.STONE);

        BlockPos pillar = PLOT[1][1];
        BlockState persistentLeaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        List<BlockPos> build = new ArrayList<>();
        for (int dy = 0; dy < 4; dy++) {
            build.add(pillar.above(dy));
            context.setBlock(pillar.above(dy), Blocks.OAK_LOG);
        }
        List<BlockPos> leaves = new ArrayList<>();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            leaves.add(pillar.above(3).relative(side));
        }
        leaves.add(pillar.above(4));
        leaves.forEach(pos -> context.setBlock(pos, persistentLeaves));

        feed(context, new ItemStack(Items.DIRT, 4), new ItemStack(Items.OAK_SAPLING, 4));

        context.onEachTick(() -> {
            context.assertBlockPresent(Blocks.STONE, stoneCorner.below());
            build.forEach(pos -> context.assertBlockPresent(Blocks.OAK_LOG, pos));
            leaves.forEach(pos -> context.assertBlockPresent(Blocks.OAK_LEAVES, pos));
        });
        context.runAfterDelay(SETTLE_TICKS, () -> {
            for (BlockPos cell : LATTICE) {
                if (cell.equals(stoneCorner)) {
                    context.assertBlockPresent(Blocks.AIR, cell);
                } else {
                    context.assertBlockPresent(Blocks.OAK_SAPLING, cell);
                }
            }
            context.succeed();
        });
    }

    // ==================== Soil ====================

    /**
     * Humus in the soil slots replaces the dirt under each planting corner before it is planted,
     * and the dirt dug out arrives in the chest.
     */
    public static void testHumusReplacesDirtUnderPlantingCorners(GameTestHelper context) {
        rig(context, true);
        // Humus first, so it is all in the farm before the saplings that let it plant.
        feed(
                context,
                new ItemStack(LogisticsForestry.BLOCK.HUMUS, 4),
                new ItemStack(Items.OAK_SAPLING, 4));

        context.onEachTick(() -> assertOffLatticeUntouched(context));
        context.succeedWhen(() -> {
            for (BlockPos cell : LATTICE) {
                context.assertBlockPresent(LogisticsForestry.BLOCK.HUMUS, cell.below());
                context.assertBlockPresent(Blocks.OAK_SAPLING, cell);
            }
            for (BlockPos[] row : PLOT) {
                context.assertBlockPresent(Blocks.DIRT, row[1].below());
            }
            context.assertContainerContains(CHEST, Items.DIRT);
        });
    }

    /**
     * Every germling the Arboretum accepts can be planted on both of its soils, including the
     * mangrove propagule, azalea and cherry; dark oak needs a 2×2 group.
     */
    public static void testEveryGermlingGrowsOnArboretumSoils(GameTestHelper context) {
        FarmLogic logic = ((FarmMachineBlock) LogisticsForestry.BLOCK.ARBORETUM).logic();
        BlockPos dirtCell = new BlockPos(2, 1, 2);
        BlockPos humusCell = new BlockPos(5, 1, 5);
        context.setBlock(dirtCell.below(), Blocks.DIRT);
        context.setBlock(humusCell.below(), LogisticsForestry.BLOCK.HUMUS);

        List<Item> germlings = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(LogisticsForestry.TAG.ARBORETUM_GERMLINGS)) {
            germlings.add(holder.value());
        }
        for (Item required : List.of(
                Items.OAK_SAPLING, Items.CHERRY_SAPLING, Items.AZALEA, Items.FLOWERING_AZALEA,
                Items.MANGROVE_PROPAGULE, Items.DARK_OAK_SAPLING)) {
            if (!germlings.contains(required)) {
                context.fail("The Arboretum should accept " + required + " as a germling");
                return;
            }
        }
        for (Item germling : germlings) {
            ItemStack stack = new ItemStack(germling);
            if (!logic.isGermling(stack)) {
                context.fail("The Arboretum's germling check should accept " + germling);
                return;
            }
            for (BlockPos cell : List.of(dirtCell, humusCell)) {
                BlockState plant = logic.plantState(stack, context.absolutePos(cell), null);
                if (plant == null || !plant.canSurvive(context.getLevel(), context.absolutePos(cell))) {
                    context.fail(germling + " should be plantable on " + context.getBlockState(cell.below()));
                    return;
                }
            }
        }
        if (!new ItemStack(Items.DARK_OAK_SAPLING).is(LogisticsForestry.TAG.REQUIRES_2X2)
                || new ItemStack(Items.OAK_SAPLING).is(LogisticsForestry.TAG.REQUIRES_2X2)) {
            context.fail("Dark oak, and not oak, should need a 2×2 group");
            return;
        }
        if (!logic.isSoil(new ItemStack(LogisticsForestry.BLOCK.HUMUS)) || !logic.isSoil(new ItemStack(Items.DIRT))) {
            context.fail("Humus and dirt should be Arboretum soils");
            return;
        }
        context.succeed();
    }

    // ==================== Rig ====================

    /**
     * Dirt under the plot, the farm facing south over it, a hopper behind and one below the farm,
     * and a chest after the output hopper; with {@code powered}, a running Creative Engine beside it.
     */
    private static FarmBlockEntity rig(GameTestHelper context, boolean powered) {
        for (BlockPos[] row : PLOT) {
            for (BlockPos cell : row) {
                context.setBlock(cell.below(), Blocks.DIRT);
            }
        }
        context.setBlock(
                FARM,
                LogisticsForestry.BLOCK.ARBORETUM.defaultBlockState().setValue(FarmMachineBlock.FACING, Direction.SOUTH));
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        context.setBlock(INPUT_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.SOUTH));

        FarmBlockEntity farm = context.getBlockEntity(FARM, FarmBlockEntity.class);
        farm.setAreaSizeOverride(3);

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

    private static void assertOffLatticeUntouched(GameTestHelper context) {
        for (BlockPos[] row : PLOT) {
            for (BlockPos cell : row) {
                if (!LATTICE.contains(cell)) {
                    context.assertBlockPresent(Blocks.AIR, cell);
                }
            }
        }
    }

    /** Fails the test and returns false when anything stands in the plot. */
    private static boolean plotIsEmpty(GameTestHelper context) {
        for (BlockPos[] row : PLOT) {
            for (BlockPos cell : row) {
                Block block = context.getBlockState(cell).getBlock();
                if (block != Blocks.AIR) {
                    context.fail("Nothing should have been planted, found " + block + " at " + cell);
                    return false;
                }
            }
        }
        return true;
    }
}
