package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.compat.PlantCompat;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Arboretum GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code ArboretumGameTest} delegates to these methods and NeoForge's
 * {@code ArboretumGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every rig is the real thing: an oak sapling bone-mealed into a tree three blocks south of the
 * farm, level with it and so inside its Reach, then the farm placed beside a Creative Engine held on
 * by a redstone block. A hopper under the farm pulls its produce into a chest beside it.
 */
public class ArboretumGameTestBody {

    private static final BlockPos FARM = new BlockPos(3, 1, 3);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SWITCH = ENGINE.north();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.north();

    /** Where the tree's trunk stands: three blocks out, on the ground level with the farm's base. */
    private static final BlockPos TRUNK = FARM.south(3);

    /** How high above its base a felled tree's blocks are looked for. */
    private static final int TREE_HEIGHT = 9;

    /** Bone meal applications that grow a sapling into a tree many times over. */
    private static final int MAX_BONE_MEAL = 64;

    /**
     * An oak bone-mealed on Humus in the Reach grows the small Humus tree; the farm fells it whole,
     * logs above its Reach included, replants an oak sapling on the Humus, and the hopper pulls the
     * logs out into the chest.
     */
    public static void testFellsHumusTreeWholeAndReplants(GameTestHelper context) {
        FarmBlockEntity farm = rig(context, LogisticsForestry.BLOCK.HUMUS.defaultBlockState(), true);
        if (farm == null) {
            return;
        }

        context.succeedWhen(() -> {
            assertNoTreeLeft(context);
            BlockState replant = context.getBlockState(TRUNK);
            if (!replant.is(Blocks.OAK_SAPLING)) {
                throw context.assertionException(TRUNK, "The Humus should be replanted with an oak sapling, holds "
                        + replant + "; farm reports " + farm.status());
            }
            if (!context.getBlockState(TRUNK.below()).is(LogisticsForestry.BLOCK.HUMUS)) {
                throw context.assertionException(TRUNK.below(), "The Humus should stay Humus");
            }
            if (!chestHolds(context, Items.OAK_LOG)) {
                throw context.assertionException(CHEST, "The hopper should pull the felled logs into the chest");
            }
        });
    }

    /** A tree grown on grass inside the Reach is never felled. */
    public static void testLeavesTreeOnGrassStanding(GameTestHelper context) {
        FarmBlockEntity farm = rig(context, Blocks.GRASS_BLOCK.defaultBlockState(), true);
        if (farm == null) {
            return;
        }

        context.runAfterDelay(400, () -> {
            assertTreeStanding(context, "A tree on grass should be left standing");
            if (farm.status() != FarmStatus.IDLE) {
                throw context.assertionException(FARM, "The farm should be Idle, reports " + farm.status());
            }
            context.succeed();
        });
    }

    /** Without power the farm fells nothing, and reports No power. */
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        FarmBlockEntity farm = rig(context, LogisticsForestry.BLOCK.HUMUS.defaultBlockState(), false);
        if (farm == null) {
            return;
        }

        context.runAfterDelay(300, () -> {
            assertTreeStanding(context, "Nothing should be felled without power");
            if (farm.status() != FarmStatus.NO_POWER) {
                throw context.assertionException(FARM, "Farm should report No power, reports " + farm.status());
            }
            context.succeed();
        });
    }

    /** A farm set to run only without a redstone signal stops while one reaches it, and reports Paused. */
    public static void testRedstoneSignalPausesFarm(GameTestHelper context) {
        FarmBlockEntity farm = rig(context, LogisticsForestry.BLOCK.HUMUS.defaultBlockState(), true);
        if (farm == null) {
            return;
        }
        farm.setRedstoneMode(RedstoneMode.LOW);
        context.setBlock(FARM.east(), Blocks.REDSTONE_BLOCK);

        context.runAfterDelay(300, () -> {
            assertTreeStanding(context, "A paused farm should fell nothing");
            if (farm.status() != FarmStatus.PAUSED) {
                throw context.assertionException(FARM, "Farm should report Paused, reports " + farm.status());
            }
            context.succeed();
        });
    }

    // ==================== Rig ====================

    /**
     * Grows an oak on {@code ground} at {@link #TRUNK}, then places the farm, its output hopper and
     * chest, and with {@code powered} a running Creative Engine beside it. Returns {@code null}, with
     * the test failed, when the tree did not grow or a block entity is missing.
     */
    @Nullable
    private static FarmBlockEntity rig(GameTestHelper context, BlockState ground, boolean powered) {
        context.setBlock(TRUNK.below(), ground);
        if (!growTree(context, TRUNK, Blocks.OAK_SAPLING.defaultBlockState())) {
            context.fail("The oak sapling on " + ground + " should grow into a tree");
            return null;
        }
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        context.setBlock(FARM, LogisticsForestry.BLOCK.ARBORETUM.defaultBlockState()
                .setValue(FarmMachineBlock.FACING, Direction.SOUTH));
        FarmBlockEntity farm = context.getBlockEntity(FARM, FarmBlockEntity.class);
        if (farm == null) {
            context.fail("Expected the farm block entity");
            return null;
        }
        // A spare sapling, in case the tree's leaves roll none.
        farm.setItem(0, new ItemStack(Items.OAK_SAPLING));
        if (powered) {
            context.setBlock(ENGINE_SWITCH, Blocks.REDSTONE_BLOCK);
            context.setBlock(ENGINE, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                    .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                    .setValue(AbstractEngineBlock.POWERED, true));
            CreativeEngineBlockEntity engine = context.getBlockEntity(ENGINE, CreativeEngineBlockEntity.class);
            if (engine == null) {
                context.fail("Expected a creative engine at " + ENGINE);
                return null;
            }
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

    /** Throws while any log or leaves of the tree stand above its trunk's base, within two blocks of it. */
    private static void assertNoTreeLeft(GameTestHelper context) {
        for (int dy = 0; dy <= TREE_HEIGHT; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = TRUNK.offset(dx, dy, dz);
                    BlockState state = context.getBlockState(pos);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                        throw context.assertionException(pos, "The tree should be felled whole, " + state + " is left");
                    }
                }
            }
        }
    }

    private static void assertTreeStanding(GameTestHelper context, String message) {
        if (!context.getBlockState(TRUNK).is(BlockTags.LOGS)) {
            throw context.assertionException(TRUNK, message + ", its trunk is " + context.getBlockState(TRUNK));
        }
    }

    private static boolean chestHolds(GameTestHelper context, Item item) {
        ChestBlockEntity chest = context.getBlockEntity(CHEST, ChestBlockEntity.class);
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
}
