package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachine;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Nether Wart Farm GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code NetherWartFarmGameTest} delegates to these methods and NeoForge's
 * {@code NetherWartFarmGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every test builds the same rig with real blocks: the farm, a Creative Engine held on by a
 * redstone block powering it from the west, and nether wart on soul sand two blocks out from the
 * farm, one level above it, inside its Reach. A hopper under the farm pulls its produce into a chest.
 */
public class NetherWartFarmGameTestBody {

    private static final BlockPos FARM = new BlockPos(3, 2, 3);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SIGNAL = ENGINE.south();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.below();
    /** A redstone block here locks the output hopper; it touches neither the farm nor the chest. */
    private static final BlockPos HOPPER_LOCK = OUTPUT_HOPPER.east();
    /** A hopper on top of the farm feeds it nether wart. */
    private static final BlockPos INPUT_HOPPER = FARM.above();
    /** Checks per second, so a pass over the Reach takes well under a second. */
    private static final int PLOT_CHECKS_PER_SECOND = 400;

    /** The field: every position two blocks out from the farm, one above it, so its soul sand is level with the farm. */
    private static final List<BlockPos> PLOTS = plots();

    private static final BlockState YOUNG = Blocks.NETHER_WART.defaultBlockState();
    private static final BlockState RIPE = YOUNG.setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE);

    private static List<BlockPos> plots() {
        List<BlockPos> plots = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                    plots.add(FARM.offset(dx, 1, dz));
                }
            }
        }
        return List.copyOf(plots);
    }

    /**
     * Builds the rig, {@code wart} on soul sand at {@code plots}, the output hopper and its chest;
     * returns {@code null}, with the test failed, if a block entity is missing.
     */
    @Nullable
    private static FarmBlockEntity build(GameTestHelper context, List<BlockPos> plots, BlockState wart, boolean power) {
        for (BlockPos plot : plots) {
            context.setBlock(plot.below(), Blocks.SOUL_SAND);
            context.setBlock(plot, wart);
        }
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER);
        context.setBlock(FARM, LogisticsForestry.BLOCK.NETHER_WART_FARM.defaultBlockState());
        FarmBlockEntity farm = (FarmBlockEntity) context.getBlockEntity(FARM);
        if (farm == null) {
            context.fail("Expected the farm block entity");
            return null;
        }
        if (power) {
            context.setBlock(ENGINE_SIGNAL, Blocks.REDSTONE_BLOCK);
            context.setBlock(ENGINE, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                    .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                    .setValue(AbstractEngineBlock.POWERED, true));
            CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(ENGINE);
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

    private static boolean isRipe(BlockState state) {
        return state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
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

    /**
     * A ripe wart field is harvested with vanilla's loot and every plot replanted from its own drops,
     * which always hold a wart; the hopper under the farm pulls the rest out into the chest.
     */
    public static void testHarvestsAndReplantsAndHopperPullsHarvest(GameTestHelper context) {
        FarmBlockEntity farm = build(context, PLOTS, RIPE, true);
        if (farm == null) {
            return;
        }

        context.succeedWhen(() -> {
            for (BlockPos plot : PLOTS) {
                BlockState state = context.getBlockState(plot);
                if (!state.is(Blocks.NETHER_WART) || isRipe(state)) {
                    throw new GameTestAssertPosException("The plot should be harvested and replanted, holds " + state, context.absolutePos(plot), plot, context.getTick());
                }
                if (!context.getBlockState(plot.below()).is(Blocks.SOUL_SAND)) {
                    throw new GameTestAssertPosException("The soul sand should stay soul sand", context.absolutePos(plot.below()), plot.below(), context.getTick());
                }
            }
            int pulled = count((ChestBlockEntity) context.getBlockEntity(CHEST), Items.NETHER_WART);
            if (pulled < PLOTS.size()) {
                throw new GameTestAssertPosException("The hopper should pull at least " + PLOTS.size()
                        + " nether wart into the chest, " + pulled + " so far", context.absolutePos(CHEST), CHEST, context.getTick());
            }
        });
    }

    /**
     * Growing wart is taken on as a Plot. Picked by hand, it leaves an Empty Plot that the farm
     * refills with the wart a hopper on top fed it; once that wart is ripe the farm harvests and
     * replants it, and the hopper under it pulls the harvest into the chest.
     *
     * <p>The output hopper stays locked until the replant: it would otherwise pull the fed wart out
     * of the farm within ticks, leaving the Empty Plot nothing to refill it with.
     */
    public static void testRefillsEmptyPlotFromHopperFedWart(GameTestHelper context) {
        BlockPos plot = PLOTS.get(0);
        context.setBlock(HOPPER_LOCK, Blocks.REDSTONE_BLOCK);
        FarmBlockEntity farm = build(context, List.of(plot), YOUNG, true);
        if (farm == null) {
            return;
        }
        farm.setRateOverride(PLOT_CHECKS_PER_SECOND, 20);
        context.setBlock(INPUT_HOPPER, Blocks.HOPPER);
        HopperBlockEntity feed = (HopperBlockEntity) context.getBlockEntity(INPUT_HOPPER);
        if (feed == null) {
            context.fail("Expected the input hopper");
            return;
        }
        feed.setItem(0, new ItemStack(Items.NETHER_WART));

        context.startSequence()
                .thenWaitUntil(() -> {
                    if (count(farm, Items.NETHER_WART) != 1 || count(feed, Items.NETHER_WART) != 0) {
                        throw new GameTestAssertPosException("The hopper should feed its wart into the farm", context.absolutePos(INPUT_HOPPER), INPUT_HOPPER, context.getTick());
                    }
                })
                // Long enough for passes over the Reach to take the growing wart on as a Plot.
                .thenIdle(40)
                .thenExecute(() -> context.setBlock(plot, Blocks.AIR))
                .thenWaitUntil(() -> {
                    BlockState state = context.getBlockState(plot);
                    if (!state.is(Blocks.NETHER_WART) || count(farm, Items.NETHER_WART) != 0) {
                        throw new GameTestAssertPosException("The Empty Plot should be refilled with the fed wart, holds "
                                + state + "; farm reports " + farm.status(), context.absolutePos(plot), plot, context.getTick());
                    }
                })
                .thenExecute(() -> {
                    context.setBlock(plot, RIPE);
                    context.setBlock(HOPPER_LOCK, Blocks.AIR);
                })
                .thenWaitUntil(() -> {
                    BlockState state = context.getBlockState(plot);
                    if (!state.is(Blocks.NETHER_WART) || isRipe(state)) {
                        throw new GameTestAssertPosException("The ripe wart should be harvested and replanted, holds "
                                + state, context.absolutePos(plot), plot, context.getTick());
                    }
                    if (count((ChestBlockEntity) context.getBlockEntity(CHEST), Items.NETHER_WART) == 0) {
                        throw new GameTestAssertPosException("The hopper should pull the harvest into the chest", context.absolutePos(CHEST), CHEST, context.getTick());
                    }
                })
                .thenSucceed();
    }

    /**
     * A farm whose inventory is full keeps harvesting: the ripe wart's drops land at its plot, and the
     * plot is replanted from them all the same.
     */
    public static void testFullFarmSpillsAtThePlot(GameTestHelper context) {
        BlockPos plot = PLOTS.get(0);
        context.setBlock(HOPPER_LOCK, Blocks.REDSTONE_BLOCK);
        FarmBlockEntity farm = build(context, List.of(plot), RIPE, true);
        if (farm == null) {
            return;
        }
        for (int slot = 0; slot < FarmMachine.SLOT_COUNT; slot++) {
            farm.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        Vec3 drop = Vec3.atCenterOf(context.absolutePos(plot));
        boolean[] spilled = {false};

        context.onEachTick(() -> {
            if (spilled[0]) {
                return;
            }
            AABB everywhere = new AABB(context.absolutePos(BlockPos.ZERO)).inflate(16.0);
            for (ItemEntity item : context.getLevel().getEntitiesOfClass(ItemEntity.class, everywhere)) {
                if (!item.getItem().is(Items.NETHER_WART)) {
                    continue;
                }
                // Loose items drift, so they are checked the tick they first appear.
                if (item.position().distanceTo(drop) > 1.0) {
                    throw new GameTestAssertPosException("The wart should drop at its plot, landed at "
                            + item.position() + ", " + item.position().distanceTo(drop) + " away", context.absolutePos(plot), plot, context.getTick());
                }
                spilled[0] = true;
            }
        });
        context.succeedWhen(() -> {
            if (!spilled[0]) {
                throw new GameTestAssertPosException("The full farm should drop the wart at the plot", context.absolutePos(plot), plot, context.getTick());
            }
            BlockState state = context.getBlockState(plot);
            if (!state.is(Blocks.NETHER_WART) || isRipe(state)) {
                throw new GameTestAssertPosException("The plot should be replanted with nether wart, holds " + state, context.absolutePos(plot), plot, context.getTick());
            }
            if (farm.status() != FarmStatus.INVENTORY_FULL) {
                throw new GameTestAssertPosException("The farm should report Inventory full, reports " + farm.status(), context.absolutePos(FARM), FARM, context.getTick());
            }
        });
    }

    /** Without power the farm harvests nothing, and reports No power. */
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        FarmBlockEntity farm = build(context, PLOTS, RIPE, false);
        if (farm == null) {
            return;
        }

        context.runAfterDelay(300, () -> {
            for (BlockPos plot : PLOTS) {
                if (!isRipe(context.getBlockState(plot))) {
                    throw new GameTestAssertPosException("Nothing should be harvested without power", context.absolutePos(plot), plot, context.getTick());
                }
            }
            if (farm.status() != FarmStatus.NO_POWER) {
                throw new GameTestAssertPosException("Farm should report No power, reports " + farm.status(), context.absolutePos(FARM), FARM, context.getTick());
            }
            context.succeed();
        });
    }
}
