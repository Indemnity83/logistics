package com.logistics.gametest.pipe;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.pipe.block.FluidConnection;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared GameTest bodies for the four fluid-routing pipes — Insertion, Bypass, Merger and Void — compiled into
 * both loaders' {@code gametest} source sets (see {@code common/build.gradle}).
 *
 * <p>Every test feeds its pipe the way a player would: a stocked Glass Tank drained by a real, engine-powered
 * Fluid Extractor Pipe. Stocking that source tank (and pre-filling a tank under test, to keep the run short) is
 * the only hand-placed fluid; every assertion reads what a real tank or pipe further along received.
 */
public class FluidRoutingGameTestBody {

    private static final BlockPos SOURCE_TANK = new BlockPos(0, 2, 2);
    private static final BlockPos EXTRACTOR = SOURCE_TANK.east();
    /** First position downstream of the extractor, where each test starts its line. */
    private static final BlockPos LINE_START = EXTRACTOR.east();

    private static final long SOURCE_MB = 8_000;
    /** How far below capacity the insertion test's tanks start, so topping them off fits the tick budget. */
    private static final long TOP_UP_MB = 200;
    /** What the bypass test's side tank holds; any change means the pipe filled or drained it. */
    private static final long SIDE_TANK_MB = 1_000;
    /** Ticks to let the pipes compute their connections before the merger's output face is wrenched. */
    private static final int SETTLE_TICKS = 5;

    /**
     * Wiki claim (Usage): "The Insertion Fluid Pipe fills adjacent tanks and machines before passing fluid
     * further down the line. Chain several to top off a row of tanks in sequence."
     *
     * <p>Two Insertion Fluid Pipes in a row, each beside a nearly-full tank: the first tank must be topped off
     * before the second receives anything, and the second is topped off from the first pipe's overflow.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Insertion_Fluid_Pipe#Usage">wiki/Insertion Fluid Pipe.txt § Usage</a>
     */
    public static void insertionPipesTopOffTanksInSequence(GameTestHelper context) {
        BlockPos firstPipePos = LINE_START;
        BlockPos secondPipePos = firstPipePos.east();
        BlockPos firstTankPos = firstPipePos.south();
        BlockPos secondTankPos = secondPipePos.south();

        GlassTankBlockEntity source = placeSource(context);
        context.setBlock(firstPipePos, LogisticsPipe.BLOCK.INSERTION_FLUID_PIPE);
        context.setBlock(secondPipePos, LogisticsPipe.BLOCK.INSERTION_FLUID_PIPE);
        GlassTankBlockEntity firstTank = placeTank(context, firstTankPos);
        GlassTankBlockEntity secondTank = placeTank(context, secondTankPos);
        firstTank.setContents(SimpleFluidKey.of(Fluids.WATER), firstTank.capacity() - FluidUnits.mb(TOP_UP_MB));
        secondTank.setContents(SimpleFluidKey.of(Fluids.WATER), secondTank.capacity() - FluidUnits.mb(TOP_UP_MB));
        long secondTankStart = secondTank.amount();
        stock(source);

        context.onEachTick(() -> {
            if (secondTank.amount() != secondTankStart && firstTank.amount() < firstTank.capacity()) {
                throw context.assertionException(
                        "Insertion pipe passed fluid down the line while its adjacent tank still had room");
            }
        });
        context.succeedWhen(() -> {
            if (firstTank.amount() < firstTank.capacity()) {
                throw context.assertionException("First insertion pipe should top off its adjacent tank");
            }
            if (secondTank.amount() < secondTank.capacity()) {
                throw context.assertionException("Overflow should top off the next tank down the line");
            }
        });
    }

    /**
     * Wiki claim (Usage): "The Bypass Fluid Pipe connects only to other fluid pipes, never to tanks or
     * machines. Use it to run a fluid line straight past a handler without filling or draining it."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Bypass_Fluid_Pipe#Usage">wiki/Bypass Fluid Pipe.txt § Usage</a>
     */
    public static void bypassPipeRunsPastAdjacentTank(GameTestHelper context) {
        BlockPos bypassPos = LINE_START;
        BlockPos sideTankPos = bypassPos.south();
        BlockPos nextPipePos = bypassPos.east();
        BlockPos endTankPos = nextPipePos.east();

        GlassTankBlockEntity source = placeSource(context);
        context.setBlock(bypassPos, LogisticsPipe.BLOCK.BYPASS_FLUID_PIPE);
        context.setBlock(nextPipePos, LogisticsPipe.BLOCK.COPPER_FLUID_PIPE);
        GlassTankBlockEntity sideTank = placeTank(context, sideTankPos);
        GlassTankBlockEntity endTank = placeTank(context, endTankPos);
        sideTank.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(SIDE_TANK_MB));
        long sideTankStart = sideTank.amount();
        FluidPipeBlockEntity bypass = pipe(context, bypassPos);
        stock(source);

        context.onEachTick(() -> {
            if (sideTank.amount() != sideTankStart) {
                throw context.assertionException("Bypass pipe must neither fill nor drain the tank beside it");
            }
        });
        context.succeedWhen(() -> {
            if (bypass.connection(Direction.SOUTH) != FluidConnection.NONE) {
                throw context.assertionException("Bypass pipe should not connect to the tank beside it");
            }
            if (endTank.amount() <= 0) {
                throw context.assertionException("Fluid should run past the side tank to the end of the line");
            }
        });
    }

    /**
     * Wiki claim (Usage): "The Merger Fluid Pipe lets fluid leave only through a wrench-set output face".
     *
     * <p>The merger sits between two tanks; the wrench points its output at one, and only that one fills.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Merger_Fluid_Pipe#Usage">wiki/Merger Fluid Pipe.txt § Usage</a>
     */
    public static void mergerPipeOutputsOnlyThroughWrenchSetFace(GameTestHelper context) {
        BlockPos mergerPos = LINE_START;
        BlockPos outputTankPos = mergerPos.east();
        BlockPos otherTankPos = mergerPos.south();

        GlassTankBlockEntity source = placeSource(context);
        context.setBlock(mergerPos, LogisticsPipe.BLOCK.MERGER_FLUID_PIPE);
        GlassTankBlockEntity outputTank = placeTank(context, outputTankPos);
        GlassTankBlockEntity otherTank = placeTank(context, otherTankPos);

        context.runAfterDelay(SETTLE_TICKS, () -> {
            wrenchOutputFace(context, mergerPos, Direction.EAST);
            stock(source);
        });
        context.onEachTick(() -> {
            if (otherTank.amount() != 0) {
                throw context.assertionException("Merger pipe let fluid out through a face other than its output");
            }
        });
        context.succeedWhen(() -> {
            if (FluidUnits.toMillibuckets(outputTank.amount()) < 100) {
                throw context.assertionException("Merger pipe should fill the tank on its output face");
            }
        });
    }

    /**
     * Wiki claim (Usage): "The Merger Fluid Pipe lets fluid leave only through a wrench-set output face, and
     * never flow back in."
     *
     * <p>A line runs into the merger's output face. The merger must refuse every offer, however long the line
     * presses against it.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Merger_Fluid_Pipe#Usage">wiki/Merger Fluid Pipe.txt § Usage</a>
     */
    public static void mergerPipeRefusesFluidThroughItsOutputFace(GameTestHelper context) {
        BlockPos feedPos = LINE_START;
        BlockPos mergerPos = feedPos.east();
        BlockPos beyondTankPos = mergerPos.east();

        GlassTankBlockEntity source = placeSource(context);
        context.setBlock(feedPos, LogisticsPipe.BLOCK.COPPER_FLUID_PIPE);
        context.setBlock(mergerPos, LogisticsPipe.BLOCK.MERGER_FLUID_PIPE);
        placeTank(context, beyondTankPos);
        FluidPipeBlockEntity feed = pipe(context, feedPos);
        FluidPipeBlockEntity merger = pipe(context, mergerPos);

        context.runAfterDelay(SETTLE_TICKS, () -> {
            wrenchOutputFace(context, mergerPos, Direction.WEST);
            stock(source);
        });
        int[] ticksPressed = {0};
        context.onEachTick(() -> {
            if (merger.totalMillibuckets() != 0) {
                throw context.assertionException("Fluid flowed back into the merger through its output face");
            }
            if (feed.totalMillibuckets() > 0) {
                ticksPressed[0]++;
            }
        });
        context.succeedWhen(() -> {
            if (ticksPressed[0] < 60) {
                throw context.assertionException("The feed line should press fluid against the merger for a while");
            }
        });
    }

    /**
     * Wiki claim (Usage): "The Void Fluid Pipe destroys any fluid that flows into it, acting as a bottomless
     * sink."
     *
     * <p>Fluid that reaches the void pipe is gone: the line as a whole holds less than the source gave up, and
     * nothing comes out the other side.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Void_Fluid_Pipe#Usage">wiki/Void Fluid Pipe.txt § Usage</a>
     */
    public static void voidPipeDestroysFluidFlowingIntoIt(GameTestHelper context) {
        BlockPos feedPos = LINE_START;
        BlockPos voidPos = feedPos.east();
        BlockPos beyondPipePos = voidPos.east();
        BlockPos beyondTankPos = beyondPipePos.south();

        GlassTankBlockEntity source = placeSource(context);
        context.setBlock(feedPos, LogisticsPipe.BLOCK.COPPER_FLUID_PIPE);
        context.setBlock(voidPos, LogisticsPipe.BLOCK.VOID_FLUID_PIPE);
        context.setBlock(beyondPipePos, LogisticsPipe.BLOCK.COPPER_FLUID_PIPE);
        GlassTankBlockEntity beyondTank = placeTank(context, beyondTankPos);
        FluidPipeBlockEntity extractor = pipe(context, EXTRACTOR);
        FluidPipeBlockEntity feed = pipe(context, feedPos);
        FluidPipeBlockEntity voidPipe = pipe(context, voidPos);
        FluidPipeBlockEntity beyondPipe = pipe(context, beyondPipePos);
        stock(source);

        context.onEachTick(() -> {
            if (beyondPipe.totalMillibuckets() != 0 || beyondTank.amount() != 0) {
                throw context.assertionException("Void pipe passed fluid on instead of destroying it");
            }
        });
        context.succeedWhen(() -> {
            long held = FluidUnits.toMillibuckets(source.amount())
                    + extractor.totalMillibuckets()
                    + feed.totalMillibuckets()
                    + voidPipe.totalMillibuckets();
            long destroyed = SOURCE_MB - held;
            if (destroyed < 200) {
                throw context.assertionException("Void pipe should have destroyed fluid, only " + destroyed + " mB gone");
            }
        });
    }

    // ==================== Rig ====================

    /**
     * Places the feed every test shares: an empty Glass Tank drained by a Fluid Extractor Pipe, powered by a
     * Creative Engine held on by a real redstone block, pushing east into {@link #LINE_START}. Returns the
     * source tank; {@link #stock} fills it.
     */
    private static GlassTankBlockEntity placeSource(GameTestHelper context) {
        BlockPos enginePos = EXTRACTOR.north();
        GlassTankBlockEntity source = placeTank(context, SOURCE_TANK);
        context.setBlock(EXTRACTOR, LogisticsPipe.BLOCK.FLUID_EXTRACTOR_PIPE);
        context.setBlock(enginePos.north(), Blocks.REDSTONE_BLOCK);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.SOUTH)
                .setValue(AbstractEngineBlock.POWERED, true));
        pipe(context, EXTRACTOR).setFeatureDirection(Direction.WEST);
        return source;
    }

    private static void stock(GlassTankBlockEntity source) {
        source.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(SOURCE_MB));
    }

    private static GlassTankBlockEntity placeTank(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.GLASS_TANK);
        GlassTankBlockEntity tank = context.getBlockEntity(pos, GlassTankBlockEntity.class);
        if (tank == null) {
            throw context.assertionException("Expected a glass tank block entity at " + pos);
        }
        return tank;
    }

    private static FluidPipeBlockEntity pipe(GameTestHelper context, BlockPos pos) {
        FluidPipeBlockEntity pipe = context.getBlockEntity(pos, FluidPipeBlockEntity.class);
        if (pipe == null) {
            throw context.assertionException("Expected a fluid pipe block entity at " + pos);
        }
        return pipe;
    }

    /** Uses a real Wrench on the merger until its output face points {@code face}, as a player would. */
    private static void wrenchOutputFace(GameTestHelper context, BlockPos pos, Direction face) {
        FluidPipeBlockEntity merger = pipe(context, pos);
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wrench = new ItemStack(LogisticsCore.ITEM.WRENCH);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        BlockPos absolute = context.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        for (int i = 0; i < Direction.values().length && merger.featureDirection() != face; i++) {
            wrench.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        }
        if (merger.featureDirection() != face) {
            throw context.assertionException("Wrench never set the merger's output face to " + face);
        }
    }
}
