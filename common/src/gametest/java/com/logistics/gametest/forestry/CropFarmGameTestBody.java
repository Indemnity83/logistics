package com.logistics.gametest.forestry;

import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.FarmSettings;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Crop Farm GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code CropFarmGameTest} delegates to these methods and NeoForge's
 * {@code CropFarmGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every test builds the same rig with real blocks: the farm faces east over a 3×3 plot
 * ({@link FarmBlockEntity#setAreaSizeOverride}), a Creative Engine held on by a redstone block
 * powers it from behind, a hopper on top feeds it, and a hopper underneath pulls its produce into a
 * chest. Water comes from a Glass Tank through a Fluid Extractor Pipe driven by a second engine.
 * Glowstone beside the plot keeps it bright whatever the sky does.
 */
public class CropFarmGameTestBody {

    private static final BlockPos FARM = new BlockPos(2, 2, 3);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SIGNAL = ENGINE.west();
    private static final BlockPos INPUT_HOPPER = FARM.above();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.below();
    private static final BlockPos WATER_PIPE = FARM.north();
    private static final BlockPos WATER_TANK = WATER_PIPE.north();
    private static final BlockPos PIPE_ENGINE = WATER_PIPE.west();
    private static final BlockPos PIPE_ENGINE_SIGNAL = PIPE_ENGINE.west();
    private static final BlockPos LAMP = new BlockPos(6, 1, 3);

    /** The 3×3 plot in front of the farm, at its Y; soil is one block below each. */
    private static final List<BlockPos> PLOTS = plots();

    private static final int AREA_SIZE = 3;
    private static final int MAX_MOISTURE = 7;
    private static final long TANK_MB = 4_000;

    private static List<BlockPos> plots() {
        List<BlockPos> plots = new ArrayList<>();
        for (int x = 3; x <= 5; x++) {
            for (int z = 2; z <= 4; z++) {
                plots.add(new BlockPos(x, 2, z));
            }
        }
        return plots;
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
        context.setBlock(LAMP, Blocks.GLOWSTONE);

        context.setBlock(FARM, LogisticsForestry.BLOCK.CROP_FARM.defaultBlockState()
                .setValue(FarmMachineBlock.FACING, Direction.EAST));
        context.setBlock(INPUT_HOPPER, Blocks.HOPPER);
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER);

        FarmBlockEntity farm = context.getBlockEntity(FARM, FarmBlockEntity.class);
        HopperBlockEntity input = context.getBlockEntity(INPUT_HOPPER, HopperBlockEntity.class);
        if (farm == null || input == null) {
            context.fail("Expected the farm and input hopper block entities");
            return null;
        }
        farm.setAreaSizeOverride(AREA_SIZE);

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
            tank = context.getBlockEntity(WATER_TANK, GlassTankBlockEntity.class);
            FluidPipeBlockEntity pipe = context.getBlockEntity(WATER_PIPE, FluidPipeBlockEntity.class);
            if (tank == null || pipe == null
                    || placeEngine(context, PIPE_ENGINE, PIPE_ENGINE_SIGNAL, Direction.EAST) == null) {
                context.fail("Expected the glass tank, extractor pipe and its engine");
                return null;
            }
            // Filling the source tank is a precondition; the pipe carries it into the farm.
            tank.tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(TANK_MB));
            pipe.setFeatureDirection(Direction.NORTH);
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
        CreativeEngineBlockEntity engine = context.getBlockEntity(pos, CreativeEngineBlockEntity.class);
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
                throw context.assertionException(plot, "Plot should hold wheat, has " + context.getBlockState(plot));
            }
            if (!context.getBlockState(plot.below()).is(Blocks.FARMLAND)) {
                throw context.assertionException(plot.below(), "Wheat should stand on farmland");
            }
        }
    }

    /** Dirt and grass under the plot are tilled into farmland, watered, and sown with hopper-fed seeds. */
    public static void testTillsAndPlantsWheat(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        for (int i = 0; i < PLOTS.size(); i += 2) {
            context.setBlock(PLOTS.get(i).below(), Blocks.GRASS_BLOCK);
        }
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.succeedWhen(() -> assertWheatEverywhere(context, PLOTS));
    }

    /** Dry farmland is wetted to full moisture with water the pipe delivered into the farm's tank. */
    public static void testIrrigatesFarmlandFromItsTank(GameTestHelper context) {
        Rig rig = build(context, farmland(0), true, true);
        if (rig == null) {
            return;
        }
        long irrigatedAtLeast = FluidUnits.mb((long) PLOTS.size() * FarmSettings.fromConfig().waterPerIrrigateMb());

        context.succeedWhen(() -> {
            for (BlockPos plot : PLOTS) {
                BlockState soil = context.getBlockState(plot.below());
                if (!soil.is(Blocks.FARMLAND) || soil.getValue(BlockStateProperties.MOISTURE) != MAX_MOISTURE) {
                    throw context.assertionException(plot.below(), "Farmland should be fully wet, is " + soil);
                }
            }
            if (rig.tank().tank().getAmount() > 0) {
                throw context.assertionException(WATER_TANK, "All the tank's water should have reached the farm");
            }
            long left = farmWater(rig.farm());
            if (left <= 0 || left > FluidUnits.mb(TANK_MB) - irrigatedAtLeast) {
                throw context.assertionException(FARM, "Irrigating 9 plots should have drawn on the farm's water, has "
                        + FluidUnits.toMillibuckets(left) + " mB of " + TANK_MB);
            }
        });
    }

    /**
     * Fertilized wheat ripens, is harvested into the chest through the output hopper, and plots are
     * sown again from the seeds the harvest returned.
     *
     * <p>The hopper holds exactly one seed per plot, so every sowing beyond those nine used a
     * returned seed: replants = (wheat standing + harvests) - 9. A harvest drops exactly one wheat,
     * so harvests are counted as the wheat in the chest, the output hopper and the farm. Not every
     * plot need be replanted: the output hopper is free to carry returned seeds off to the chest.
     */
    public static void testHarvestsAndReplantsFromReturnedSeeds(GameTestHelper context) {
        Rig rig = build(context, farmland(MAX_MOISTURE), true, true);
        if (rig == null) {
            return;
        }
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));
        rig.input().setItem(1, new ItemStack(LogisticsForestry.ITEM.FERTILIZER, 8));

        context.succeedWhen(() -> {
            context.assertContainerContains(CHEST, Items.WHEAT);
            int harvests = count(context.getBlockEntity(CHEST, ChestBlockEntity.class), Items.WHEAT)
                    + count(context.getBlockEntity(OUTPUT_HOPPER, HopperBlockEntity.class), Items.WHEAT)
                    + count(rig.farm(), Items.WHEAT);
            int standing = 0;
            for (BlockPos plot : PLOTS) {
                if (context.getBlockState(plot).is(Blocks.WHEAT)) {
                    standing++;
                }
            }
            if (standing + harvests <= PLOTS.size()) {
                throw context.assertionException(FARM, "A harvested plot should have been replanted from returned"
                        + " seeds: " + harvests + " harvested, " + standing + " standing");
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

    /** Without water the farm cannot irrigate: the farmland stays dry and the farm reports No water. */
    public static void testReportsNoWater(GameTestHelper context) {
        Rig rig = build(context, farmland(0), true, false);
        if (rig == null) {
            return;
        }
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        // Two passes over the nine plots at the default 10-tick work interval.
        context.runAfterDelay(200, () -> {
            for (BlockPos plot : PLOTS) {
                BlockState soil = context.getBlockState(plot.below());
                // Dry farmland with nothing on it may revert to dirt on a random tick; it never gets wet.
                if (soil.is(Blocks.FARMLAND) && soil.getValue(BlockStateProperties.MOISTURE) > 0) {
                    throw context.assertionException(plot.below(), "Farmland should stay dry without water");
                }
                if (!context.getBlockState(plot).isAir()) {
                    throw context.assertionException(plot, "Nothing should be planted on dry farmland");
                }
            }
            if (rig.farm().status() != FarmStatus.NO_WATER) {
                throw context.assertionException(FARM, "Farm should report No water, reports " + rig.farm().status());
            }
            context.succeed();
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
                    throw context.assertionException(plot.below(), "Nothing should be tilled without power");
                }
                if (!context.getBlockState(plot).isAir()) {
                    throw context.assertionException(plot, "Nothing should be planted without power");
                }
            }
            if (rig.farm().status() != FarmStatus.NO_POWER) {
                throw context.assertionException(FARM, "Farm should report No power, reports " + rig.farm().status());
            }
            context.succeed();
        });
    }

    /** A torch standing on a plot is left alone, as is the dirt under it; the rest is farmed. */
    public static void testLeavesTorchOnPlot(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        BlockPos torch = PLOTS.get(PLOTS.size() / 2);
        context.setBlock(torch, Blocks.TORCH);
        List<BlockPos> others = new ArrayList<>(PLOTS);
        others.remove(torch);
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, others.size()));

        context.succeedWhen(() -> {
            context.assertBlockPresent(Blocks.TORCH, torch);
            context.assertBlockPresent(Blocks.DIRT, torch.below());
            assertWheatEverywhere(context, others);
        });
    }
}
