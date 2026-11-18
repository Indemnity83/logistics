package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.compat.PlantCompat;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Orchard GameTest bodies, compiled into both loaders' {@code gametest} source sets. Fabric's
 * {@code OrchardGameTest} delegates to these methods and NeoForge's {@code OrchardGameTestRegistration}
 * references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every rig is the real thing: an oak sapling bone-mealed into a tree on grass three blocks south
 * of the farm, its trunk level with the farm and so inside its Reach, then the farm placed beside a
 * Creative Engine held on by a redstone block. A hopper under the farm pulls its apples into a chest
 * beside it. The farm's pick override stands in for random ticks, so a pick comes in ticks, not hours.
 */
public class OrchardGameTestBody {

    private static final BlockPos FARM = new BlockPos(3, 1, 3);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SWITCH = ENGINE.north();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.north();

    /** Where the tree's trunk stands: three blocks out, level with the farm. */
    private static final BlockPos TRUNK = FARM.south(3);

    /** How far around and above its trunk the tree's blocks are watched. */
    private static final int TREE_RADIUS = 3;
    private static final int TREE_HEIGHT = 9;

    /** Bone meal applications that grow a sapling into a tree many times over. */
    private static final int MAX_BONE_MEAL = 64;

    /** A tenth of the ticks pick an apple: about 10 RF/t of picking, well inside the farm's input. */
    private static final double PICK_CHANCE = 0.1;

    /**
     * An oak on grass in the Reach is picked: apples reach the chest through the hopper, the farm
     * tracks the tree, and not one log or leaf of it is broken.
     */
    public static void testPicksApplesWithoutBreakingTheTree(GameTestHelper context) {
        Map<BlockPos, Block> tree = new HashMap<>();
        FarmBlockEntity farm = rig(context, true, tree);
        if (farm == null) {
            return;
        }

        context.startSequence()
                .thenWaitUntil(() -> {
                    if (!chestHolds(context, Items.APPLE)) {
                        throw new GameTestAssertPosException("The hopper should pull picked apples into the chest; "
                                + "farm reports " + farm.status() + " tracking " + farm.pickPlots() + " trees", context.absolutePos(CHEST), CHEST, context.getTick());
                    }
                })
                .thenExecute(() -> {
                    if (farm.pickPlots() != 1) {
                        throw new GameTestAssertPosException("The farm should track one tree, tracks " + farm.pickPlots(), context.absolutePos(FARM), FARM, context.getTick());
                    }
                    assertTreeUnbroken(context, tree);
                })
                .thenSucceed();
    }

    /** Without power the Orchard picks nothing, and reports No power. */
    public static void testPicksNothingWithoutPower(GameTestHelper context) {
        Map<BlockPos, Block> tree = new HashMap<>();
        FarmBlockEntity farm = rig(context, false, tree);
        if (farm == null) {
            return;
        }

        context.runAfterDelay(300, () -> {
            if (chestHolds(context, Items.APPLE) || farmHolds(farm, Items.APPLE)) {
                throw new GameTestAssertPosException("An unpowered Orchard should pick no apples", context.absolutePos(FARM), FARM, context.getTick());
            }
            if (farm.status() != FarmStatus.NO_POWER) {
                throw new GameTestAssertPosException("Farm should report No power, reports " + farm.status(), context.absolutePos(FARM), FARM, context.getTick());
            }
            assertTreeUnbroken(context, tree);
            context.succeed();
        });
    }

    // ==================== Rig ====================

    /**
     * Grows an oak on grass at {@link #TRUNK} and records its logs and leaves in {@code tree}, then
     * places the farm, its output hopper and chest, and with {@code powered} a running Creative Engine
     * beside it. Returns {@code null}, with the test failed, when the tree did not grow or a block
     * entity is missing.
     */
    @Nullable
    private static FarmBlockEntity rig(GameTestHelper context, boolean powered, Map<BlockPos, Block> tree) {
        context.setBlock(TRUNK.below(), Blocks.GRASS_BLOCK);
        if (!growTree(context, TRUNK, Blocks.OAK_SAPLING.defaultBlockState())) {
            context.fail("The oak sapling on grass should grow into a tree");
            return null;
        }
        forEachTreeBlock(context, (pos, state) -> tree.put(pos, state.getBlock()));
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        context.setBlock(FARM, LogisticsForestry.BLOCK.ORCHARD.defaultBlockState());
        FarmBlockEntity farm = (FarmBlockEntity) context.getBlockEntity(FARM);
        if (farm == null) {
            context.fail("Expected the farm block entity");
            return null;
        }
        // Scan the Reach in a few ticks, and pick without waiting on random ticks.
        farm.setRateOverride(2_000, 20);
        farm.setPickOverride(PICK_CHANCE);
        if (powered) {
            context.setBlock(ENGINE_SWITCH, Blocks.REDSTONE_BLOCK);
            context.setBlock(ENGINE, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                    .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                    .setValue(AbstractEngineBlock.POWERED, true));
            CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(ENGINE);
            if (engine == null) {
                context.fail("Expected a creative engine at " + ENGINE);
                return null;
            }
            // 20 -> 40 RF/t: more than the Basic farm's 32 RF/t input.
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

    private interface TreeBlockVisitor {
        void visit(BlockPos pos, BlockState state);
    }

    /** Every log and leaves block around and above the trunk. */
    private static void forEachTreeBlock(GameTestHelper context, TreeBlockVisitor visitor) {
        for (int dy = 0; dy <= TREE_HEIGHT; dy++) {
            for (int dx = -TREE_RADIUS; dx <= TREE_RADIUS; dx++) {
                for (int dz = -TREE_RADIUS; dz <= TREE_RADIUS; dz++) {
                    BlockPos pos = TRUNK.offset(dx, dy, dz);
                    BlockState state = context.getBlockState(pos);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                        visitor.visit(pos, state);
                    }
                }
            }
        }
    }

    /** Throws when any log or leaves block of the tree as grown is gone or changed. */
    private static void assertTreeUnbroken(GameTestHelper context, Map<BlockPos, Block> tree) {
        if (tree.isEmpty()) {
            throw new GameTestAssertPosException("Expected a grown tree to watch", context.absolutePos(TRUNK), TRUNK, context.getTick());
        }
        tree.forEach((pos, block) -> {
            BlockState now = context.getBlockState(pos);
            if (!now.is(block)) {
                throw new GameTestAssertPosException("The Orchard should break nothing, " + block + " is now " + now, context.absolutePos(pos), pos, context.getTick());
            }
        });
    }

    private static boolean chestHolds(GameTestHelper context, Item item) {
        ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(CHEST);
        if (chest == null) {
            return false;
        }
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (chest.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean farmHolds(FarmBlockEntity farm, Item item) {
        for (int slot = 0; slot < farm.getContainerSize(); slot++) {
            if (farm.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }
}
