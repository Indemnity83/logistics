package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Cocoa Plantation GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code CocoaPlantationGameTest} delegates to these methods and NeoForge's
 * {@code CocoaPlantationGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every rig is the real thing: a two-log jungle column the player would place, two blocks east of
 * the farm and inside its Reach, with a cocoa pod on its north and south faces at both heights, and
 * a Creative Engine held on by a redstone block powering the farm from the west.
 */
public class CocoaPlantationGameTestBody {

    private static final BlockPos FARM = new BlockPos(3, 2, 3);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SIGNAL = ENGINE.south();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.below();
    private static final BlockPos INPUT_HOPPER = FARM.above();

    /** Checks per second, so a pass over the Reach takes well under a second. */
    private static final int CHECKS_PER_SECOND = 400;

    /** The player's jungle logs: a column two blocks east of the farm, level with it and one above. */
    private static final List<BlockPos> LOGS = List.of(FARM.east(2), FARM.east(2).above());

    /** A pod on the north and south face of each log. */
    private static final List<Pod> PODS = List.of(
            new Pod(LOGS.get(0).north(), Direction.SOUTH),
            new Pod(LOGS.get(0).south(), Direction.NORTH),
            new Pod(LOGS.get(1).north(), Direction.SOUTH),
            new Pod(LOGS.get(1).south(), Direction.NORTH));

    /** A pod at {@code pos}, facing the log it hangs on. */
    private record Pod(BlockPos pos, Direction facing) {
        BlockState state(int age) {
            return Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, facing).setValue(CocoaBlock.AGE, age);
        }
    }

    /**
     * Ripe pods are picked with vanilla's loot and each replanted from its own beans, facing the same
     * log; the logs are left standing, and the hopper under the farm pulls the spare beans into the chest.
     */
    public static void testPicksAndReplantsAndHopperPullsHarvest(GameTestHelper context) {
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER);
        FarmBlockEntity farm = build(context, CocoaBlock.MAX_AGE, true);
        if (farm == null) {
            return;
        }

        context.succeedWhen(() -> {
            assertLogsStanding(context);
            for (Pod pod : PODS) {
                assertGrowingPod(context, pod, "The ripe pod should be picked and replanted facing its log");
            }
            int pulled = count(context.getBlockEntity(CHEST, ChestBlockEntity.class), Items.COCOA_BEANS);
            if (pulled < PODS.size()) {
                throw context.assertionException(CHEST, "The hopper should pull the spare beans into the chest, "
                        + pulled + " so far");
            }
        });
    }

    /**
     * Pods the farm took on as Plots and the player then broke leave Empty Plots, which wait for beans;
     * beans a hopper feeds in from above replant every one, facing its log.
     */
    public static void testHopperFedBeansReplantEmptyPlots(GameTestHelper context) {
        FarmBlockEntity farm = build(context, 0, true);
        if (farm == null) {
            return;
        }
        farm.setRateOverride(CHECKS_PER_SECOND, 20);

        context.startSequence()
                .thenExecuteAfter(40, () -> PODS.forEach(pod -> context.setBlock(pod.pos(), Blocks.AIR)))
                .thenWaitUntil(() -> {
                    if (farm.status() != FarmStatus.WAITING_FOR_SEEDS) {
                        throw context.assertionException(FARM, "The farm should wait for beans, reports " + farm.status());
                    }
                })
                .thenExecute(() -> {
                    context.setBlock(
                            INPUT_HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
                    HopperBlockEntity hopper = context.getBlockEntity(INPUT_HOPPER, HopperBlockEntity.class);
                    if (hopper == null) {
                        throw context.assertionException(INPUT_HOPPER, "Expected the input hopper");
                    }
                    hopper.setItem(0, new ItemStack(Items.COCOA_BEANS, PODS.size()));
                })
                .thenWaitUntil(() -> {
                    assertLogsStanding(context);
                    for (Pod pod : PODS) {
                        assertGrowingPod(context, pod, "The Empty Plot should be replanted from the hopper's beans");
                    }
                })
                .thenSucceed();
    }

    /**
     * A pod on the outer face of a log on the Reach's edge, one block outside the Reach, is picked and
     * replanted; once the player breaks the replant, the farm refills it from the beans it kept.
     */
    public static void testPicksAndRefillsPodOutsideReachOnLogInside(GameTestHelper context) {
        BlockPos log = FARM.east(3);
        Pod outer = new Pod(log.east(), Direction.WEST);
        context.setBlock(log, Blocks.JUNGLE_LOG);
        context.setBlock(outer.pos(), outer.state(CocoaBlock.MAX_AGE));
        FarmBlockEntity farm = build(context, List.of(), List.of(), 0, true);
        if (farm == null) {
            return;
        }
        farm.setRateOverride(CHECKS_PER_SECOND, 20);

        context.startSequence()
                .thenWaitUntil(() -> {
                    assertGrowingPod(context, outer, "The ripe pod outside the Reach should be picked and replanted");
                    if (!context.getBlockState(log).is(Blocks.JUNGLE_LOG)) {
                        throw context.assertionException(log, "The farm should never touch the log");
                    }
                })
                .thenExecute(() -> context.setBlock(outer.pos(), Blocks.AIR))
                .thenWaitUntil(() -> assertGrowingPod(context, outer, "The Empty Plot outside the Reach should be refilled"))
                .thenSucceed();
    }

    /** Without power the farm picks nothing, and reports No power. */
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        FarmBlockEntity farm = build(context, CocoaBlock.MAX_AGE, false);
        if (farm == null) {
            return;
        }

        context.runAfterDelay(300, () -> {
            for (Pod pod : PODS) {
                BlockState state = context.getBlockState(pod.pos());
                if (!state.equals(pod.state(CocoaBlock.MAX_AGE))) {
                    throw context.assertionException(pod.pos(), "Nothing should be picked without power, holds " + state);
                }
            }
            if (farm.status() != FarmStatus.NO_POWER) {
                throw context.assertionException(FARM, "Farm should report No power, reports " + farm.status());
            }
            context.succeed();
        });
    }

    // ==================== Rig ====================

    /**
     * Places the logs, pods of {@code age} on them, the farm and, with {@code powered}, a running
     * Creative Engine beside it. Returns {@code null}, with the test failed, if a block entity is missing.
     */
    @Nullable
    private static FarmBlockEntity build(GameTestHelper context, int age, boolean powered) {
        return build(context, LOGS, PODS, age, powered);
    }

    @Nullable
    private static FarmBlockEntity build(
            GameTestHelper context, List<BlockPos> logs, List<Pod> pods, int age, boolean powered) {
        logs.forEach(log -> context.setBlock(log, Blocks.JUNGLE_LOG));
        pods.forEach(pod -> context.setBlock(pod.pos(), pod.state(age)));
        context.setBlock(FARM, LogisticsForestry.BLOCK.COCOA_PLANTATION.defaultBlockState());
        FarmBlockEntity farm = context.getBlockEntity(FARM, FarmBlockEntity.class);
        if (farm == null) {
            context.fail("Expected the farm block entity");
            return null;
        }
        if (powered) {
            context.setBlock(ENGINE_SIGNAL, Blocks.REDSTONE_BLOCK);
            context.setBlock(ENGINE, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                    .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                    .setValue(AbstractEngineBlock.POWERED, true));
            CreativeEngineBlockEntity engine = context.getBlockEntity(ENGINE, CreativeEngineBlockEntity.class);
            if (engine == null) {
                context.fail("Expected a creative engine at " + ENGINE);
                return null;
            }
            // 20 -> 40 -> 80 -> 160 RF/t, above the farm's input cap.
            engine.cycleOutputLevel();
            engine.cycleOutputLevel();
            engine.cycleOutputLevel();
        }
        return farm;
    }

    private static void assertGrowingPod(GameTestHelper context, Pod pod, String message) {
        BlockState state = context.getBlockState(pod.pos());
        if (!state.is(Blocks.COCOA)
                || state.getValue(CocoaBlock.FACING) != pod.facing()
                || state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE) {
            throw context.assertionException(pod.pos(), message + ", holds " + state);
        }
    }

    private static void assertLogsStanding(GameTestHelper context) {
        for (BlockPos log : LOGS) {
            if (!context.getBlockState(log).is(Blocks.JUNGLE_LOG)) {
                throw context.assertionException(log, "The farm should never touch the logs, holds "
                        + context.getBlockState(log));
            }
        }
    }

    private static int count(@Nullable Container container, Item item) {
        if (container == null) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
