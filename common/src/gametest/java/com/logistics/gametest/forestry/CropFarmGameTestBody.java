package com.logistics.gametest.forestry;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.compat.PlantCompat;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.machine.area.RingArea;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachine;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.FarmMenu;
import com.logistics.forestry.farm.FarmSettings;
import com.logistics.forestry.farm.FarmState;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.forestry.farm.FarmSurvey;
import com.logistics.forestry.farm.FarmTierProfile;
import com.logistics.forestry.farm.Hydration;
import com.logistics.forestry.farm.LevelFarmWorld;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Crop Farm GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code CropFarmGameTest} delegates to these methods and NeoForge's
 * {@code CropFarmGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every test builds the same rig with real blocks: the farm at the centre works the 16-plot ring
 * between a 3×3 and a 5×5 square around it ({@link FarmBlockEntity#setAreaOverride}), one block
 * above it, with the soil level with the farm. Everything else stands in the 3×3 inside the ring,
 * which the farm never touches: a Creative Engine held on by a redstone block powers the farm from
 * the west, a hopper on top feeds it, and a hopper underneath pulls its produce into a chest. Water
 * comes from a Glass Tank through a Fluid Extractor Pipe on the north side, driven by a second
 * engine. Two glowstone lamps above the inner square keep the ring bright whatever the sky does.
 * The farm is started, except in the survey tests, which press Start themselves.
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

    /** The ring's plots, one block above the farm, in scan order; each one's soil is level with the farm. */
    private static final List<BlockPos> PLOTS = plots();

    private static final int MAX_MOISTURE = 7;
    private static final long TANK_MB = 4_000;

    private static List<BlockPos> plots() {
        RingArea ring = new RingArea(FARM.above(), RING_INNER, RING_OUTER);
        List<BlockPos> plots = new ArrayList<>();
        for (int i = 0; i < ring.plotCount(); i++) {
            plots.add(ring.plot(i));
        }
        return List.copyOf(plots);
    }

    private record Rig(FarmBlockEntity farm, HopperBlockEntity input, @Nullable GlassTankBlockEntity tank) {}

    /**
     * Builds the rig over {@code soil} (one block per plot, under it; {@code null} lays none). Returns
     * {@code null}, with the test failed, if a block entity is missing.
     */
    @Nullable
    private static Rig build(GameTestHelper context, @Nullable BlockState soil, boolean power, boolean water) {
        return build(context, soil, power, water ? TANK_MB : 0);
    }

    /** {@link #build(GameTestHelper, BlockState, boolean, boolean)} with {@code waterMb} in the source tank; 0 for no water rig. */
    @Nullable
    private static Rig build(GameTestHelper context, @Nullable BlockState soil, boolean power, long waterMb) {
        Rig rig = placeRig(context, soil, power, waterMb);
        if (rig != null) {
            rig.farm().start();
        }
        return rig;
    }

    /** The rig as {@link #build(GameTestHelper, BlockState, boolean, long)} builds it, with the farm left surveying. */
    @Nullable
    private static Rig placeRig(GameTestHelper context, @Nullable BlockState soil, boolean power, long waterMb) {
        boolean water = waterMb > 0;
        if (soil != null) {
            for (BlockPos plot : PLOTS) {
                context.setBlock(plot.below(), soil);
            }
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
            tank.tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(waterMb));
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

    /** The water one irrigation of {@code soil} costs where the test runs, in mB. */
    private static long irrigationMb(GameTestHelper context, BlockPos soil) {
        return Hydration.waterMb(
                FarmSettings.fromConfig().waterPerIrrigationMb(),
                LevelFarmWorld.climate(context.getLevel(), context.absolutePos(soil)));
    }

    /**
     * Glass over {@code soil}'s column, just above what the farm keeps clear, so rain never reaches
     * the plot and only irrigation can wet it.
     */
    private static BlockPos roof(GameTestHelper context, Rig rig, BlockPos soil) {
        BlockPos roof = soil.above(1 + rig.farm().logic().clearanceHeight());
        context.setBlock(roof, Blocks.GLASS);
        return roof;
    }

    /** {@code soil} as farmland of {@code moisture}, given one vanilla random tick; returns the moisture after it. */
    private static int randomTick(GameTestHelper context, BlockPos soil, int moisture) {
        context.setBlock(soil, farmland(moisture));
        ServerLevel level = context.getLevel();
        BlockPos pos = context.absolutePos(soil);
        level.getBlockState(pos).randomTick(level, pos, level.getRandom());
        BlockState after = context.getBlockState(soil);
        if (!after.is(Blocks.FARMLAND)) {
            throw new GameTestAssertPosException("The farmland should still be farmland, is " + after, context.absolutePos(soil), soil, context.getTick());
        }
        return after.getValue(BlockStateProperties.MOISTURE);
    }

    /** Fails until every drop of the source tank has reached the farm. */
    private static void assertTankDrained(GameTestHelper context, Rig rig) {
        if (rig.tank() != null && rig.tank().tank().getAmount() > 0) {
            throw new GameTestAssertPosException("All the tank's water should have reached the farm", context.absolutePos(WATER_TANK), WATER_TANK, context.getTick());
        }
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

    /** With no water at all, the farm still tills the whole ring and sows it, never reporting No water. */
    public static void testTillsAndPlantsWithoutWater(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, false);
        if (rig == null) {
            return;
        }
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));

        context.succeedWhen(() -> {
            assertWheatEverywhere(context, PLOTS);
            if (farmWater(rig.farm()) != 0) {
                throw new GameTestAssertPosException("The farm should have no water", context.absolutePos(FARM), FARM, context.getTick());
            }
            if (rig.farm().status() == FarmStatus.NO_WATER) {
                throw new GameTestAssertPosException("Farm should not report No water", context.absolutePos(FARM), FARM, context.getTick());
            }
        });
    }

    /**
     * Mature wheat is harvested and replanted on the same farmland: the harvest leaves the plot's
     * farmland, at the moisture it had, and the plot is never dirt. The test bone-meals that one
     * plot's wheat until it is harvested; young wheat on every other plot keeps the rest of the ring
     * idle.
     */
    public static void testHarvestKeepsFarmland(GameTestHelper context) {
        Rig rig = build(context, farmland(MAX_MOISTURE), true, true);
        if (rig == null) {
            return;
        }
        BlockPos target = PLOTS.get(PLOTS.size() / 2);
        BlockPos soil = target.below();
        for (BlockPos plot : PLOTS) {
            context.setBlock(plot, Blocks.WHEAT);
        }
        // The output hopper may carry the harvest's seeds off before they are replanted.
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 4));
        boolean[] harvested = {false};
        BlockState[] soilBefore = {context.getBlockState(soil)};
        String[] problem = {null};

        context.onEachTick(() -> {
            BlockState ground = context.getBlockState(soil);
            if (!ground.is(Blocks.FARMLAND) && problem[0] == null) {
                problem[0] = "The plot's soil should stay farmland, became " + ground;
            }
            if (harvested[0]) {
                return;
            }
            BlockState state = context.getBlockState(target);
            if (!state.is(Blocks.WHEAT) || state.getValue(BlockStateProperties.AGE_7) < 7 && hasHarvest(context, rig)) {
                harvested[0] = true;
                if (ground != soilBefore[0] && problem[0] == null) {
                    problem[0] = "The harvest should leave the farmland as it was, " + soilBefore[0] + ", is " + ground;
                }
                return;
            }
            soilBefore[0] = ground;
            PlantCompat.fertilize(context.getLevel(), context.getLevel().getRandom(), context.absolutePos(target), state);
        });
        context.succeedWhen(() -> {
            if (problem[0] != null) {
                throw new GameTestAssertPosException(problem[0], context.absolutePos(soil), soil, context.getTick());
            }
            if (!harvested[0]) {
                throw new GameTestAssertPosException("The ripe wheat should have been harvested", context.absolutePos(target), target, context.getTick());
            }
            if (!context.getBlockState(target).is(Blocks.WHEAT)) {
                throw new GameTestAssertPosException("The plot should have been replanted", context.absolutePos(target), target, context.getTick());
            }
        });
    }

    /** Whether any wheat has been harvested: in the chest, the output hopper or the farm. */
    private static boolean hasHarvest(GameTestHelper context, Rig rig) {
        return count((ChestBlockEntity) context.getBlockEntity(CHEST), Items.WHEAT)
                        + count((HopperBlockEntity) context.getBlockEntity(OUTPUT_HOPPER), Items.WHEAT)
                        + count(rig.farm(), Items.WHEAT)
                > 0;
    }

    /**
     * A working farm irrigates dry farmland of its ring through vanilla's own random tick: the
     * farmland goes to full moisture and the farm's tank drops by exactly one irrigation's water.
     */
    public static void testIrrigatesRingFarmlandFromTank(GameTestHelper context) {
        Rig rig = build(context, farmland(MAX_MOISTURE), true, true);
        if (rig == null) {
            return;
        }
        PLOTS.forEach(plot -> context.setBlock(plot, Blocks.WHEAT));
        BlockPos soil = PLOTS.get(PLOTS.size() / 2).below();
        BlockPos roof = roof(context, rig, soil);

        context.succeedWhen(() -> {
            assertTankDrained(context, rig);
            long before = farmWater(rig.farm());
            int moisture = randomTick(context, soil, 0);
            long spent = before - farmWater(rig.farm());
            if (moisture != MAX_MOISTURE) {
                throw new GameTestAssertPosException("Irrigated farmland should be fully wet, is " + moisture, context.absolutePos(soil), soil, context.getTick());
            }
            long charge = FluidUnits.mb(irrigationMb(context, soil));
            if (spent != charge) {
                throw new GameTestAssertPosException("The irrigation should cost " + FluidUnits.toMillibuckets(charge)
                        + " mB, cost " + FluidUnits.toMillibuckets(spent) + " mB", context.absolutePos(FARM), FARM, context.getTick());
            }
            context.setBlock(roof, Blocks.AIR);
        });
    }

    /** With an empty tank the farm cannot irrigate: a random tick dries its farmland one level and nothing is spent. */
    public static void testFarmlandDriesWithoutWater(GameTestHelper context) {
        Rig rig = build(context, farmland(MAX_MOISTURE), true, false);
        if (rig == null) {
            return;
        }
        PLOTS.forEach(plot -> context.setBlock(plot, Blocks.WHEAT));
        BlockPos soil = PLOTS.get(PLOTS.size() / 2).below();
        BlockPos roof = roof(context, rig, soil);

        context.runAfterDelay(40, () -> {
            int moisture = randomTick(context, soil, 5);
            if (moisture != 4) {
                throw new GameTestAssertPosException("Unirrigated farmland should dry from 5 to 4, is " + moisture, context.absolutePos(soil), soil, context.getTick());
            }
            if (farmWater(rig.farm()) != 0) {
                throw new GameTestAssertPosException("The farm should have spent no water", context.absolutePos(FARM), FARM, context.getTick());
            }
            context.setBlock(roof, Blocks.AIR);
            context.succeed();
        });
    }

    /** A farm that is still surveying never irrigates: its farmland dries and its water stays in the tank. */
    public static void testSurveyingFarmDoesNotIrrigate(GameTestHelper context) {
        Rig rig = placeRig(context, farmland(MAX_MOISTURE), true, TANK_MB);
        if (rig == null) {
            return;
        }
        PLOTS.forEach(plot -> context.setBlock(plot, Blocks.WHEAT));
        BlockPos soil = PLOTS.get(PLOTS.size() / 2).below();
        BlockPos roof = roof(context, rig, soil);

        context.succeedWhen(() -> {
            assertTankDrained(context, rig);
            long before = farmWater(rig.farm());
            int moisture = randomTick(context, soil, 5);
            if (moisture != 4) {
                throw new GameTestAssertPosException("A surveying farm's farmland should dry from 5 to 4, is " + moisture, context.absolutePos(soil), soil, context.getTick());
            }
            if (farmWater(rig.farm()) != before) {
                throw new GameTestAssertPosException("A surveying farm should spend no water", context.absolutePos(FARM), FARM, context.getTick());
            }
            if (rig.farm().state() != FarmState.SURVEY) {
                throw new GameTestAssertPosException("The farm should still be surveying", context.absolutePos(FARM), FARM, context.getTick());
            }
            context.setBlock(roof, Blocks.AIR);
        });
    }

    /**
     * Farmland in the courtyard or outside the ring is never irrigated by a working farm with water:
     * a random tick dries both and the farm spends nothing.
     */
    public static void testNeverIrrigatesOutsideRing(GameTestHelper context) {
        Rig rig = build(context, farmland(MAX_MOISTURE), true, true);
        if (rig == null) {
            return;
        }
        PLOTS.forEach(plot -> context.setBlock(plot, Blocks.WHEAT));
        // Under a lamp, which keeps the rain off.
        BlockPos courtyard = new BlockPos(4, FARM.getY(), 4);
        BlockPos outside = new BlockPos(0, FARM.getY(), FARM.getZ());
        BlockPos outsideRoof = outside.above(2);
        context.setBlock(outsideRoof, Blocks.GLASS);

        context.succeedWhen(() -> {
            assertTankDrained(context, rig);
            long before = farmWater(rig.farm());
            for (BlockPos soil : List.of(courtyard, outside)) {
                int moisture = randomTick(context, soil, 5);
                if (moisture != 4) {
                    throw new GameTestAssertPosException("Farmland off the ring should dry from 5 to 4, is " + moisture, context.absolutePos(soil), soil, context.getTick());
                }
            }
            if (farmWater(rig.farm()) != before) {
                throw new GameTestAssertPosException("Farmland off the ring should cost the farm nothing", context.absolutePos(FARM), FARM, context.getTick());
            }
            context.setBlock(courtyard, Blocks.AIR);
            context.setBlock(outside, Blocks.AIR);
            context.setBlock(outsideRoof, Blocks.AIR);
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

    /** Without power the farm tills and plants nothing, and reports No power. */
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
     * relaying the lever's signal into it: a farm set to run only without a signal is not paused,
     * and sows seeds fed in from a hopper beside it.
     */
    public static void testLeverOnEngineDoesNotPauseFarm(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), false, true);
        if (rig == null) {
            return;
        }
        rig.farm().setRedstoneMode(RedstoneMode.LOW);
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
     * A grass block under a plot is tillable: it is hoed into farmland where it stands and sown, never
     * broken, so no dirt reaches the output and none of the dirt in the soil slots is laid.
     */
    public static void testTillsGrassInPlace(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        BlockPos grass = PLOTS.get(5).below();
        context.setBlock(grass, Blocks.GRASS_BLOCK);
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));
        rig.input().setItem(1, new ItemStack(Items.DIRT, PLOTS.size()));

        context.succeedWhen(() -> {
            assertWheatEverywhere(context, PLOTS);
            assertNoDirtInOutput(context, rig);
            int unlaid = count(rig.input(), Items.DIRT) + count(rig.farm(), Items.DIRT);
            if (unlaid != PLOTS.size()) {
                throw new GameTestAssertPosException("No dirt should have been laid, " + (PLOTS.size() - unlaid)
                        + " was", context.absolutePos(grass), grass, context.getTick());
            }
        });
    }

    // ==================== Survey and Start ====================

    private static final int SURVEY_TICKS = 120;

    /**
     * On grass, a placed farm only surveys: for {@value #SURVEY_TICKS} ticks the torch, the planks,
     * the short grass, the chest and the ground in its ring stay as they are (grass may die back to
     * dirt under the planks by itself) and its energy only ever rises,
     * while the survey flags the torch and planks and protects the chest. Start, pressed through
     * the farm's menu, then clears the torch, planks and grass, and never the chest or its diamonds.
     */
    public static void testSurveyChangesNothingUntilStart(GameTestHelper context) {
        Rig rig = placeRig(context, Blocks.GRASS_BLOCK.defaultBlockState(), true, TANK_MB);
        if (rig == null) {
            return;
        }
        FarmBlockEntity farm = rig.farm();
        BlockPos torch = PLOTS.get(2);
        BlockPos planks = PLOTS.get(6);
        BlockPos stash = PLOTS.get(10);
        BlockPos grass = PLOTS.get(13);
        context.setBlock(torch, Blocks.TORCH);
        context.setBlock(planks, Blocks.OAK_PLANKS);
        context.setBlock(grass, Blocks.SHORT_GRASS);
        context.setBlock(stash, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(stash);
        if (chest == null) {
            context.fail("Expected the chest in the ring");
            return;
        }
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));

        // Every column the farm would clear; the grass under it may die back to dirt by itself.
        Map<BlockPos, BlockState> ring = new HashMap<>();
        for (BlockPos plot : PLOTS) {
            for (int dy = 0; dy < 5; dy++) {
                ring.put(plot.above(dy), context.getBlockState(plot.above(dy)));
            }
        }
        long[] lastEnergy = {farm.energyStorage(null).getAmount()};

        context.onEachTick(() -> {
            if (farm.state() != FarmState.SURVEY) {
                return;
            }
            ring.forEach((pos, state) -> {
                if (context.getBlockState(pos) != state) {
                    throw new GameTestAssertPosException("Surveying should change nothing, but " + state
                            + " became " + context.getBlockState(pos), context.absolutePos(pos), pos, context.getTick());
                }
            });
            long energy = farm.energyStorage(null).getAmount();
            if (energy < lastEnergy[0]) {
                throw new GameTestAssertPosException("Surveying should draw no RF, the farm went from "
                        + lastEnergy[0] + " to " + energy, context.absolutePos(FARM), FARM, context.getTick());
            }
            lastEnergy[0] = energy;
            for (BlockPos plot : PLOTS) {
                BlockState soil = context.getBlockState(plot.below());
                if (!soil.is(Blocks.GRASS_BLOCK) && !soil.is(Blocks.DIRT)) {
                    throw new GameTestAssertPosException("Surveying should leave the ground, it is " + soil, context.absolutePos(plot.below()), plot.below(), context.getTick());
                }
            }
            if (context.getTick() > 2 && farm.status() != FarmStatus.SURVEYING) {
                throw new GameTestAssertPosException("The farm should report Surveying, reports " + farm.status(), context.absolutePos(FARM), FARM, context.getTick());
            }
        });
        context.runAtTickTime(SURVEY_TICKS, () -> {
            FarmSurvey survey = farm.survey();
            if (!survey.flaggedAt().contains(context.absolutePos(torch))
                    || !survey.flaggedAt().contains(context.absolutePos(planks))) {
                throw new GameTestAssertPosException("The survey should flag the torch and planks, found " + survey, context.absolutePos(torch), torch, context.getTick());
            }
            if (!survey.protectedAt().contains(context.absolutePos(stash))) {
                throw new GameTestAssertPosException("The survey should protect the chest, found " + survey, context.absolutePos(stash), stash, context.getTick());
            }
            if (survey.toClear() < 3) {
                throw new GameTestAssertPosException("The survey should count torch, planks and grass, found "
                        + survey, context.absolutePos(grass), grass, context.getTick());
            }
            Player player = context.makeMockPlayer(GameType.SURVIVAL);
            FarmMenu menu = (FarmMenu) farm.createMenuProvider().createMenu(0, player.getInventory(), player);
            if (!menu.clickMenuButton(player, FarmMenu.BUTTON_START) || farm.state() != FarmState.ACTIVE) {
                throw new GameTestAssertPosException("Start should set the farm working", context.absolutePos(FARM), FARM, context.getTick());
            }
        });
        context.onEachTick(() -> context.assertBlockPresent(Blocks.CHEST, stash));
        context.succeedWhen(() -> {
            if (farm.state() != FarmState.ACTIVE) {
                throw new GameTestAssertPosException("Waiting for Start", context.absolutePos(FARM), FARM, context.getTick());
            }
            context.assertBlockPresent(Blocks.AIR, torch);
            context.assertBlockPresent(Blocks.AIR, planks);
            context.assertBlockPresent(Blocks.AIR, grass);
            if (count((ChestBlockEntity) context.getBlockEntity(stash), Items.DIAMOND) != 5) {
                throw new GameTestAssertPosException("The chest in the ring should still hold its 5 diamonds", context.absolutePos(stash), stash, context.getTick());
            }
        });
    }

    /**
     * A working farm raised a tier, its ring widened from 5×5 to 7×7 to fit the test structure, at once
     * shows the new lanes' torch as flagged and reports it, then clears it without any confirmation.
     */
    public static void testUpgradeWorksNewLanesImmediately(GameTestHelper context) {
        Rig rig = build(context, Blocks.GRASS_BLOCK.defaultBlockState(), true, true);
        if (rig == null) {
            return;
        }
        FarmBlockEntity farm = rig.farm();
        BlockPos torch = new BlockPos(0, FARM.getY() + 1, FARM.getZ());
        context.setBlock(torch.below(), Blocks.GRASS_BLOCK);
        context.setBlock(torch, Blocks.TORCH);
        long[] upgradedAt = {-1};

        context.runAtTickTime(20, () -> {
            context.assertBlockPresent(Blocks.TORCH, torch);
            // The tier's own ring reaches past the structure, so the wider 7×7 stands in for it.
            farm.setAreaOverride(RING_INNER, RING_OUTER + 2);
            if (!farm.tierComponent().setTier(MachineTier.INDUSTRIAL)) {
                throw new GameTestAssertPosException("The farm should take the Industrial tier", context.absolutePos(FARM), FARM, context.getTick());
            }
            Optional<Component> feedback = farm.upgradeFeedback(MachineTier.INDUSTRIAL);
            FarmSurvey lanes = farm.survey();
            if (feedback.isEmpty() || lanes.flagged() != 1
                    || !lanes.flaggedAt().contains(context.absolutePos(torch))) {
                throw new GameTestAssertPosException("The upgrade should report the new lanes' torch, found "
                        + lanes + " and " + feedback, context.absolutePos(torch), torch, context.getTick());
            }
            if (!farm.previewShown()) {
                throw new GameTestAssertPosException("The upgrade should show the preview", context.absolutePos(FARM), FARM, context.getTick());
            }
            upgradedAt[0] = context.getTick();
        });
        context.succeedWhen(() -> {
            if (upgradedAt[0] < 0) {
                throw new GameTestAssertPosException("Waiting for the upgrade", context.absolutePos(FARM), FARM, context.getTick());
            }
            context.assertBlockPresent(Blocks.AIR, torch);
            if (context.getTick() - upgradedAt[0] > 100) {
                context.fail("The new lane should be worked straight away, took " + (context.getTick() - upgradedAt[0])
                        + " ticks");
            }
        });
    }

    // ==================== Levels ====================

    /**
     * Placed on a grass floor, the farm lays its dirt level with itself, tills it and sows wheat on
     * top, leaving the floor below alone: grass, or dirt where vanilla kills grass under the laid soil,
     * never tilled. The soil layer was air, so laying soil digs nothing out, and grass that spreads
     * onto laid dirt before it is tilled is hoed where it stands: no dirt ever reaches the output.
     */
    public static void testLaysSoilLevelWithFarmOnGrass(GameTestHelper context) {
        int floorY = FARM.getY() - 1;
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                context.setBlock(new BlockPos(x, floorY, z), Blocks.GRASS_BLOCK);
            }
        }
        Rig rig = build(context, null, true, true);
        if (rig == null) {
            return;
        }
        rig.input().setItem(0, new ItemStack(Items.WHEAT_SEEDS, PLOTS.size()));
        rig.input().setItem(1, new ItemStack(Items.DIRT, PLOTS.size()));

        context.onEachTick(() -> {
            for (BlockPos plot : PLOTS) {
                BlockPos floor = plot.below(2);
                BlockState below = context.getBlockState(floor);
                if (!below.is(Blocks.GRASS_BLOCK) && !below.is(Blocks.DIRT)) {
                    throw new GameTestAssertPosException("The floor under the soil should be left alone, is " + below, context.absolutePos(floor), floor, context.getTick());
                }
            }
            assertNoDirtInOutput(context, rig);
        });
        context.succeedWhen(() -> assertWheatEverywhere(context, PLOTS));
    }

    // ==================== Tiers ====================

    /**
     * A farm raised to Industrial and mined drops an item carrying its tier; placing that item again
     * gives an Industrial farm, with the Industrial ring and buffers.
     */
    public static void testTierSurvivesBreakAndReplace(GameTestHelper context) {
        context.setBlock(FARM.below(), Blocks.STONE);
        context.setBlock(FARM, LogisticsForestry.BLOCK.CROP_FARM.defaultBlockState()
                .setValue(FarmMachineBlock.FACING, Direction.EAST));
        FarmBlockEntity placed = (FarmBlockEntity) context.getBlockEntity(FARM);
        if (placed == null) {
            context.fail("Expected the farm block entity");
            return;
        }
        placed.tierComponent().setTier(MachineTier.INDUSTRIAL);

        context.getLevel().destroyBlock(context.absolutePos(FARM), true);
        AABB around = new AABB(context.absolutePos(FARM)).inflate(2.0);
        List<ItemEntity> dropped = context.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(item -> item.getItem().is(LogisticsForestry.BLOCK.CROP_FARM.asItem()))
                .toList();
        if (dropped.size() != 1) {
            context.fail("Breaking the farm should drop exactly one farm item, dropped " + dropped.size());
            return;
        }
        ItemStack item = dropped.get(0).getItem().copy();
        dropped.get(0).discard();
        if (item.get(LogisticsCore.DATA.MACHINE_TIER) != MachineTier.INDUSTRIAL) {
            context.fail("The dropped farm should carry its tier, carries " + item.get(LogisticsCore.DATA.MACHINE_TIER));
            return;
        }

        // Clicking the top of the stone below, item in hand, places the farm back where it was.
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        context.placeAt(player, item, FARM.below(2), Direction.UP);
        FarmBlockEntity replaced = (FarmBlockEntity) context.getBlockEntity(FARM);
        if (replaced == null) {
            context.fail("Placing the dropped item should put a farm back");
            return;
        }
        FarmTierProfile industrial = FarmSettings.fromConfig().profile(MachineTier.INDUSTRIAL);
        if (replaced.tier() != MachineTier.INDUSTRIAL) {
            context.fail("The replaced farm should be Industrial, is " + replaced.tier());
            return;
        }
        if (replaced.areaSize() != industrial.outerSize()
                || replaced.energyStorage(null).getCapacity() != industrial.energyCapacity()) {
            context.fail("The replaced farm should work the Industrial ring with the Industrial buffer, works "
                    + replaced.areaSize() + " with " + replaced.energyStorage(null).getCapacity() + " RF");
            return;
        }
        context.succeed();
    }

    /**
     * Raising a powered farm's tier grows its ring and its energy buffer: the engine keeps filling it
     * past the Basic capacity, at the Industrial input rate.
     */
    public static void testRaisingTierGrowsEnergyBuffer(GameTestHelper context) {
        Rig rig = build(context, Blocks.DIRT.defaultBlockState(), true, false);
        if (rig == null) {
            return;
        }
        FarmSettings settings = FarmSettings.fromConfig();
        FarmTierProfile basic = settings.profile(MachineTier.BASIC);
        FarmTierProfile industrial = settings.profile(MachineTier.INDUSTRIAL);
        FarmBlockEntity farm = rig.farm();
        // The tier's own ring reaches past the test structure, so it is only measured, never worked.
        farm.setAreaOverride(0, 0);
        int basicRing = farm.areaSize();
        long basicCapacity = farm.energyStorage(null).getCapacity();
        farm.tierComponent().setTier(MachineTier.INDUSTRIAL);
        int industrialRing = farm.areaSize();
        farm.setAreaOverride(RING_INNER, RING_OUTER);
        if (basicRing != basic.outerSize() || basicCapacity != basic.energyCapacity()) {
            context.fail("A new farm should be Basic, works " + basicRing + " with " + basicCapacity + " RF");
            return;
        }
        if (industrialRing != industrial.outerSize()) {
            context.fail("The ring should reach " + industrial.outerSize() + ", reaches " + industrialRing);
            return;
        }
        context.succeedWhen(() -> {
            long capacity = farm.energyStorage(null).getCapacity();
            if (capacity != industrial.energyCapacity()) {
                throw new GameTestAssertPosException("The buffer should hold " + industrial.energyCapacity()
                        + " RF, holds " + capacity, context.absolutePos(FARM), FARM, context.getTick());
            }
            long stored = farm.energyStorage(null).getAmount();
            if (stored <= basic.energyCapacity()) {
                throw new GameTestAssertPosException("The engine should fill the buffer past the Basic "
                        + basic.energyCapacity() + " RF, has " + stored, context.absolutePos(FARM), FARM, context.getTick());
            }
        });
    }

    /** Throws if any dirt is in the farm's output, the output hopper or the chest. */
    private static void assertNoDirtInOutput(GameTestHelper context, Rig rig) {
        int dug = count((ChestBlockEntity) context.getBlockEntity(CHEST), Items.DIRT)
                + count((HopperBlockEntity) context.getBlockEntity(OUTPUT_HOPPER), Items.DIRT)
                + outputCount(rig.farm(), Items.DIRT);
        if (dug > 0) {
            throw new GameTestAssertPosException("No dirt should be dug out of the soil layer, " + dug + " was", context.absolutePos(CHEST), CHEST, context.getTick());
        }
    }

    private static int outputCount(FarmBlockEntity farm, Item item) {
        int total = 0;
        for (int slot = FarmMachine.OUTPUT_START; slot < FarmMachine.OUTPUT_END; slot++) {
            ItemStack stack = farm.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
