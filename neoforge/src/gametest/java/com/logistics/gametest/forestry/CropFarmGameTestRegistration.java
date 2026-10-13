package com.logistics.gametest.forestry;

import com.logistics.gametest.GameTestCase;
import com.logistics.gametest.GameTestRegistrationSupport;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wires {@link CropFarmGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class CropFarmGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("forestry/crop_farm_tills_and_plants_wheat", 1200, CropFarmGameTestBody::testTillsAndPlantsWheat),
        new GameTestCase("forestry/crop_farm_hoeing_spends_water", 1200, CropFarmGameTestBody::testHoeingSpendsWater),
        new GameTestCase(
            "forestry/crop_farm_harvest_returns_plot_to_dirt_and_retill_pays_water",
            2400,
            CropFarmGameTestBody::testHarvestReturnsPlotToDirtAndRetillPaysWater),
        new GameTestCase(
            "forestry/crop_farm_moistens_planted_farmland_without_water",
            1200,
            CropFarmGameTestBody::testMoistensPlantedFarmlandWithoutWater),
        new GameTestCase(
            "forestry/crop_farm_harvests_and_replants_from_returned_seeds",
            3000,
            CropFarmGameTestBody::testHarvestsAndReplantsFromReturnedSeeds),
        new GameTestCase(
            "forestry/crop_farm_harvests_but_stops_tilling_without_water",
            1200,
            CropFarmGameTestBody::testHarvestsButStopsTillingWithoutWater),
        new GameTestCase("forestry/crop_farm_does_nothing_without_power", 300, CropFarmGameTestBody::testDoesNothingWithoutPower),
        new GameTestCase(
            "forestry/crop_farm_lever_on_engine_does_not_pause_farm",
            1200,
            CropFarmGameTestBody::testLeverOnEngineDoesNotPauseFarm),
        new GameTestCase("forestry/crop_farm_clears_torch_from_plot", 1200, CropFarmGameTestBody::testClearsTorchFromPlot),
        new GameTestCase("forestry/crop_farm_clears_block_above_plot", 1200, CropFarmGameTestBody::testClearsBlockAbovePlot),
        new GameTestCase("forestry/crop_farm_never_breaks_chest_in_ring", 1200, CropFarmGameTestBody::testNeverBreaksChestInRing),
        new GameTestCase(
            "forestry/crop_farm_leaves_inner_square_alone", 1200, CropFarmGameTestBody::testLeavesInnerSquareAlone),
        new GameTestCase(
            "forestry/crop_farm_replaces_grass_with_dirt", 1200, CropFarmGameTestBody::testReplacesGrassWithDirt),
        new GameTestCase(
            "forestry/crop_farm_lays_soil_level_with_farm_on_grass",
            2400,
            CropFarmGameTestBody::testLaysSoilLevelWithFarmOnGrass));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private CropFarmGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "forestry/crop_farm", TESTS, FUNCTIONS);
    }
}
