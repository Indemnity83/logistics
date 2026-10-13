package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.compat.PlantCompat;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.machine.area.RingArea;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.FarmSettings;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.forestry.farm.crop.CropFarmLogic;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Crop Farm GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code CropFarmGameTest} delegates to these methods and NeoForge's
 * {@code CropFarmGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every test builds the same rig with real blocks: the farm at the centre works the 16-plot ring
 * between a 3×3 and a 5×5 square around it ({@link FarmBlockEntity#setAreaOverride}), at its Y,
 * with soil one block below. Everything else stands in the 3×3 inside the ring, which the farm never
 * touches: a Creative Engine held on by a redstone block powers the farm from the west, a hopper on
 * top feeds it, and a hopper underneath pulls its produce into a chest. Water comes from a Glass Tank
 * through a Fluid Extractor Pipe on the north side, driven by a second engine. Two glowstone lamps
 * above the inner square keep the ring bright whatever the sky does.
 */
public class CropFarmGameTestBody {

    private static final BlockPos FARM = new BlockPos(3, 2, 3);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SIGNAL = ENGINE.south();
    private static final BlockPos INPUT_HOPPER = FARM.above();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.below();
    private static final BlockPos WATER_PIPE = FARM.north();
    private static final BlockPos WATER_TANK = WATER_PIPE.above();
    private static final BlockPos PIPE_ENGINE = WATER_PIPE.east();
    private static final BlockPos PIPE_ENGINE_SIGNAL = PIPE_ENGINE.above();
    private static final List<BlockPos> LAMPS = List.of(new BlockPos(2, 4, 2), new BlockPos(4, 4, 4));

    private static final int RING_INNER = 3;
    private static final int RING_OUTER = 5;

    /** The ring's plots, at the farm's Y, in scan order; soil is one block below each. */
    private static final List<BlockPos> PLOTS = plots();

    private static final int MAX_MOISTURE = 7;
    private static final long TANK_MB = 4_000;

    private static List<BlockPos> plots() {
        RingArea ring = new RingArea(FARM, RING_INNER, RING_OUTER);
        List<BlockPos> plots = new ArrayList<>();
        for (int i = 0; i < ring.plotCount(); i++) {
            plots.add(ring.plot(i));
        }
        return List.copyOf(plots);
    }

    private record Rig(FarmBlockEntity farm, HopperBlockEntity input, @Nullable GlassTankBlockEntity tank) {}

    /**
     * Builds the rig over {@code soil} (one block per plot, under it). Returns {@code null}, with
     * the test failed, if a block entity is missing.
     */
    @Nullable
    private static Rig build(GameTestHelper context, BlockState soil, boolean power, boolean water) {
        for (BlockPos plot : PLOTS) {
            context.setBlock(plot.below(), soil);
        }
        LAMPS.forEach(lamp -> context.setBlock(lamp, Blocks.GLOWSTONE));

        context.setBlock(FARM, LogisticsForestry.BLOCK.CROP_FARM.defaultBlockState()
                .setValue(FarmMachineBlock.FACING, Direction.EAST));
        context.setBlock(INPUT_HOPPER, Blocks.HOPPER);
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER);

        FarmBlockEntity farm = (FarmBlockEntity) context.getBlockEntity(FARM);
        HopperBlockEntity input = (HopperBlockEntity) context.getBlockEntity(INPUT_HOPPER);
        if (farm == null || input == null) {
            context.fail("Expected the farm and input hopper block entities");
            return null;
        }
        farm.setAreaOverride(RING_INNER, RING_OUTER);

        if (power) {
            CreativeEngineBlockEntity engine = placeEngine(context, ENGINE, ENGINE_SIGNAL, Direction.EAST);
            if (engine == null) {
                return null;
            }
            // 20 -> 40 -> 80 -> 160 RF/t, above the farm's 128 RF/t input cap.
            engine.cycleOutputLevel();
            engine.cycleOutputLevel();
            engine.cycleOutputLevel();
        }

        GlassTankBlockEntity tank = null;
        if (water) {
            context.setBlock(WATER_TANK, LogisticsPipe.BLOCK.GLASS_TANK);
            context.setBlock(WATER_PIPE, LogisticsPipe.BLOCK.FLUID_EXTRACTOR_PIPE);
            tank = (GlassTankBlockEntity) context.getBlockEntity(WATER_TANK);
            FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) context.getBlockEntity(WATER_PIPE);
            if (tank == null || pipe == null
                    || placeEngine(context, PIPE_ENGINE, PIPE_ENGINE_SIGNAL, Direction.WEST) == null) {
                context.fail("Expected the glass tank, extractor pipe and its engine");
                return null;
            }
            // Filling the source tank is a precondition; the pipe carries it into the farm.
            tank.tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(TANK_MB));
            pipe.setFeatureDirection(Direction.UP);
        }
        return new Rig(farm, input, tank);
    }

    @Nullable
    private static CreativeEngineBlockEntity placeEngine(
            GameTestHelper context, BlockPos pos, BlockPos signal, Direction facing) {
        context.setBlock(signal, Blocks.REDSTONE_BLOCK);
        context.setBlock(pos, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, facing)
                .setValue(AbstractEngineBlock.POWERED, true));
        CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(pos);
        if (engine == null) {
            context.fail("Expected a creative engine at " + pos);
        }
        return engine;
    }

    private static BlockState farmland(int moisture) {
        return Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, moisture);
    }

    private static long farmWater(FarmBlockEntity farm) {
        long total = 0;
        for (IFluidView view : farm.fluidStorage(null).contents()) {
            total += view.amount();
        }
        return total;
    }

    private static void assertWheatEverywhere(GameTestHelper context, List<BlockPos> plots) {
        for (BlockPos plot : plots) {
            if (!context.getBlockState(plot).is(Blocks.WHEAT)) {
                throw new GameTestAssertPosException("Plot should hold wheat, has " + context.getBlockState(plot), context.absolutePos(plot), plot, context.getTick());
            }
            if (!context.getBlockState(plot.below()).is(Blocks.FARMLAND)) {
                throw new GameTestAssertPosException("Wheat should stand on farmland", context.absolutePos(plot.below()), plot.below(), context.getTick());
            }
        }
    }

    private static void assertChestHolds(GameTestHelper context, Item item, String what) {
        if (count((ChestBlockEntity) context.getBlockEntity(CHEST), item) == 0) {
            throw new GameTestAssertPosException(what + " should have reached the chest", context.absolutePos(CHEST), CHEST, context.getTick());
        }
    }

    /**
     * Dirt under the ring is tilled into farmland and sown with hopper-fed seeds; short grass growing
     * on one plot is cleared first.
     */
    public static void testTillsAndPlantsWheat(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        context.setBlock(PLOTS.get(1), Blocks.SHORT_GRASS);
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.succeedWhen(() -> assertWheatEverywhere(context, PLOTS));
    }

    /**
     * Dry farmland under growing wheat is wetted with water the pipe delivered into the farm's tank.
     * Wetted farmland is checked to be above the irrigation threshold rather than at full moisture,
     * since vanilla dries it a step per random tick.
     */
    public static void testIrrigatesFarmlandFromItsTank(GameTestHelper context) {
        Rig rig = build(context, farmland(0), true, true);
        if (rig == null) {
            return;
        }
        for (BlockPos plot : PLOTS) {
            context.setBlock(plot, Blocks.WHEAT);
        }
        long irrigatedAtLeast = FluidUnits.mb((long) PLOTS.size() * FarmSettings.fromConfig().waterPerIrrigateMb());

        context.succeedWhen(() -> {
            for (BlockPos plot : PLOTS) {
                BlockState soil = context.getBlockState(plot.below());
                if (!soil.is(Blocks.FARMLAND)
                        || soil.getValue(BlockStateProperties.MOISTURE) <= CropFarmLogic.IRRIGATE_AT_MOISTURE) {
                    throw new GameTestAssertPosException("Farmland should have been wetted, is " + soil, context.absolutePos(plot.below()), plot.below(), context.getTick());
                }
            }
            if (rig.tank().tank().getAmount() > 0) {
                throw new GameTestAssertPosException("All the tank's water should have reached the farm", context.absolutePos(WATER_TANK), WATER_TANK, context.getTick());
            }
            long left = farmWater(rig.farm());
            if (left <= 0 || left > FluidUnits.mb(TANK_MB) - irrigatedAtLeast) {
                throw new GameTestAssertPosException("Irrigating " + PLOTS.size() + " plots should have drawn on"
                        + " the farm's water, has " + FluidUnits.toMillibuckets(left) + " mB of " + TANK_MB, context.absolutePos(FARM), FARM, context.getTick());
            }
        });
    }

    /**
     * Wheat ripens, is harvested into the chest through the output hopper, and plots are sown again
     * from the seeds the harvest returned. The test bone-meals growing wheat so it ripens quickly.
     *
     * <p>The hopper holds exactly one seed per plot, so every sowing beyond those used a returned
     * seed: replants = (wheat standing + harvests) - plots. A harvest drops exactly one wheat, so
     * harvests are counted as the wheat in the chest, the output hopper and the farm. Not every plot
     * need be replanted: the output hopper is free to carry returned seeds off to the chest.
     */
    public static void testHarvestsAndReplantsFromReturnedSeeds(GameTestHelper context) {
        Rig rig = build(context, farmland(MAX_MOISTURE), true, true);
        if (rig == null) {
            return;
        }
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.onEachTick(() -> {
            for (BlockPos plot : PLOTS) {
                BlockState state = context.getBlockState(plot);
                if (state.is(Blocks.WHEAT)) {
                    BlockPos pos = context.absolutePos(plot);
                    PlantCompat.fertilize(context.getLevel(), context.getLevel().getRandom(), pos, state);
                }
            }
        });
        context.succeedWhen(() -> {
            // Not assertContainerContains: on this version it demands exactly one.
            int inChest = count((ChestBlockEntity) context.getBlockEntity(CHEST), Items.WHEAT);
            if (inChest == 0) {
                throw new GameTestAssertPosException("Wheat should reach the chest", context.absolutePos(CHEST), CHEST, context.getTick());
            }
            int harvests = inChest
                    + count((HopperBlockEntity) context.getBlockEntity(OUTPUT_HOPPER), Items.WHEAT)
                    + count(rig.farm(), Items.WHEAT);
            int standing = 0;
            for (BlockPos plot : PLOTS) {
                if (context.getBlockState(plot).is(Blocks.WHEAT)) {
                    standing++;
                }
            }
            if (standing + harvests <= PLOTS.size()) {
                throw new GameTestAssertPosException("A harvested plot should have been replanted from returned"
                        + " seeds: " + harvests + " harvested, " + standing + " standing", context.absolutePos(FARM), FARM, context.getTick());
            }
        });
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
     * Without water the farm still sows, but cannot irrigate: the farmland stays dry under the wheat
     * and the farm reports No water.
     */
    public static void testPlantsButReportsNoWater(GameTestHelper context) {
        Rig rig = build(context, farmland(0), true, false);
        if (rig == null) {
            return;
        }
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.succeedWhen(() -> {
            assertWheatEverywhere(context, PLOTS);
            for (BlockPos plot : PLOTS) {
                BlockState soil = context.getBlockState(plot.below());
                if (soil.getValue(BlockStateProperties.MOISTURE) > 0) {
                    throw new GameTestAssertPosException("Farmland should stay dry without water", context.absolutePos(plot.below()), plot.below(), context.getTick());
                }
            }
            if (rig.farm().status() != FarmStatus.NO_WATER) {
                throw new GameTestAssertPosException("Farm should report No water, reports " + rig.farm().status(), context.absolutePos(FARM), FARM, context.getTick());
            }
        });
    }

    /** Without power the farm tills, waters and plants nothing, and reports No power. */
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), false, true);
        if (rig == null) {
            return;
        }
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.runAfterDelay(200, () -> {
            for (BlockPos plot : PLOTS) {
                if (context.getBlockState(plot.below()).is(Blocks.FARMLAND)) {
                    throw new GameTestAssertPosException("Nothing should be tilled without power", context.absolutePos(plot.below()), plot.below(), context.getTick());
                }
                if (!context.getBlockState(plot).isAir()) {
                    throw new GameTestAssertPosException("Nothing should be planted without power", context.absolutePos(plot), plot, context.getTick());
                }
            }
            if (rig.farm().status() != FarmStatus.NO_POWER) {
                throw new GameTestAssertPosException("Farm should report No power, reports " + rig.farm().status(), context.absolutePos(FARM), FARM, context.getTick());
            }
            context.succeed();
        });
    }

    /**
     * An engine on top of the farm, switched on by a lever on the engine, powers the farm without
     * relaying the lever's signal into it: the farm is not paused, and sows seeds fed in from a
     * hopper beside it.
     */
    public static void testLeverOnEngineDoesNotPauseFarm(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), false, true);
        if (rig == null) {
            return;
        }
        BlockPos engineOnTop = INPUT_HOPPER;
        BlockPos lever = engineOnTop.above();
        context.setBlock(engineOnTop, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.DOWN));
        CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(engineOnTop);
        if (engine == null) {
            context.fail("Expected a creative engine on top of the farm");
            return;
        }
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        context.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
        context.pullLever(lever);

        context.setBlock(ENGINE, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        HopperBlockEntity feeder = (HopperBlockEntity) context.getBlockEntity(ENGINE);
        if (feeder == null) {
            context.fail("Expected the feeding hopper beside the farm");
            return;
        }
        feeder.setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.succeedWhen(() -> {
            if (!context.getBlockState(engineOnTop).getValue(AbstractEngineBlock.POWERED)) {
                throw new GameTestAssertPosException("The lever should switch the engine on", context.absolutePos(engineOnTop), engineOnTop, context.getTick());
            }
            if (rig.farm().status() == FarmStatus.PAUSED) {
                throw new GameTestAssertPosException("A lever on the engine should not pause the farm", context.absolutePos(FARM), FARM, context.getTick());
            }
            assertWheatEverywhere(context, PLOTS);
        });
    }

    // ==================== Clearing ====================

    /** A torch standing on a plot is cleared into the chest, and the plot is farmed like the rest. */
    public static void testClearsTorchFromPlot(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        context.setBlock(PLOTS.get(PLOTS.size() / 2), Blocks.TORCH);
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.succeedWhen(() -> {
            assertChestHolds(context, Items.TORCH, "The cleared torch");
            assertWheatEverywhere(context, PLOTS);
        });
    }

    /** Cobblestone two blocks above a plot is cleared into the chest, and the ring is farmed. */
    public static void testClearsBlockAbovePlot(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        BlockPos cobble = PLOTS.get(3).above(2);
        context.setBlock(cobble, Blocks.COBBLESTONE);
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.succeedWhen(() -> {
            context.assertBlockPresent(Blocks.AIR, cobble);
            assertChestHolds(context, Items.COBBLESTONE, "The cleared cobblestone");
            assertWheatEverywhere(context, PLOTS);
        });
    }

    /**
     * A chest standing on a plot is a block entity, so it is never broken and never emptied: the
     * chest and its contents are still there once the farm has sown the rest of the ring.
     */
    public static void testNeverBreaksChestInRing(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        BlockPos stash = PLOTS.get(PLOTS.size() / 2);
        context.setBlock(stash, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(stash);
        if (chest == null) {
            context.fail("Expected the chest in the ring");
            return;
        }
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        List<BlockPos> others = new ArrayList<>(PLOTS);
        others.remove(stash);
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, others.size()));

        context.onEachTick(() -> context.assertBlockPresent(Blocks.CHEST, stash));
        context.succeedWhen(() -> {
            assertWheatEverywhere(context, others);
            ChestBlockEntity standing = (ChestBlockEntity) context.getBlockEntity(stash);
            if (count(standing, Items.DIAMOND) != 5) {
                throw new GameTestAssertPosException("The chest in the ring should still hold its 5 diamonds", context.absolutePos(stash), stash, context.getTick());
            }
            if (count((ChestBlockEntity) context.getBlockEntity(CHEST), Items.DIAMOND) != 0) {
                throw new GameTestAssertPosException("No diamond should have left the chest in the ring", context.absolutePos(CHEST), CHEST, context.getTick());
            }
        });
    }

    /** A block in the square inside the ring, right beside the farm, is never touched. */
    public static void testLeavesInnerSquareAlone(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        BlockPos inner = FARM.east();
        context.setBlock(inner, Blocks.COBBLESTONE);
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.onEachTick(() -> context.assertBlockPresent(Blocks.COBBLESTONE, inner));
        context.succeedWhen(() -> {
            assertWheatEverywhere(context, PLOTS);
            if (count((ChestBlockEntity) context.getBlockEntity(CHEST), Items.COBBLESTONE) != 0) {
                throw new GameTestAssertPosException("Nothing should have been cleared from inside the ring", context.absolutePos(CHEST), CHEST, context.getTick());
            }
        });
    }

    /**
     * A grass block under a plot is not prepared soil: it is broken, its drop reaches the chest, and
     * dirt from the soil slots takes its place before it is tilled and sown.
     */
    public static void testReplacesGrassWithDirt(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        BlockPos grass = PLOTS.get(5).below();
        context.setBlock(grass, Blocks.GRASS_BLOCK);
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));
        rig.input().setItem(1, new ItemStack(Items.DIRT, PLOTS.size()));

        context.succeedWhen(() -> {
            assertChestHolds(context, Items.DIRT, "The grass block's drop");
            assertWheatEverywhere(context, PLOTS);
        });
    }
}
