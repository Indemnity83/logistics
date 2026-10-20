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
        new GameTestCase(
            "forestry/crop_farm_tills_and_plants_without_water",
            1200,
            CropFarmGameTestBody::testTillsAndPlantsWithoutWater),
        new GameTestCase("forestry/crop_farm_harvest_keeps_farmland", 2400, CropFarmGameTestBody::testHarvestKeepsFarmland),
        new GameTestCase(
            "forestry/crop_farm_irrigates_ring_farmland_from_tank",
            1200,
            CropFarmGameTestBody::testIrrigatesRingFarmlandFromTank),
        new GameTestCase(
            "forestry/crop_farm_farmland_dries_without_water", 220, CropFarmGameTestBody::testFarmlandDriesWithoutWater),
        new GameTestCase(
            "forestry/crop_farm_surveying_farm_does_not_irrigate",
            1200,
            CropFarmGameTestBody::testSurveyingFarmDoesNotIrrigate),
        new GameTestCase(
            "forestry/crop_farm_never_irrigates_outside_ring",
            1200,
            CropFarmGameTestBody::testNeverIrrigatesOutsideRing),
        new GameTestCase(
            "forestry/crop_farm_harvests_and_replants_from_returned_seeds",
            3000,
            CropFarmGameTestBody::testHarvestsAndReplantsFromReturnedSeeds),
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
            "forestry/crop_farm_tills_grass_in_place", 1200, CropFarmGameTestBody::testTillsGrassInPlace),
        new GameTestCase(
            "forestry/crop_farm_lays_soil_level_with_farm_on_grass",
            2400,
            CropFarmGameTestBody::testLaysSoilLevelWithFarmOnGrass),
        new GameTestCase(
            "forestry/crop_farm_tier_survives_break_and_replace",
            100,
            CropFarmGameTestBody::testTierSurvivesBreakAndReplace),
        new GameTestCase(
            "forestry/crop_farm_raising_tier_grows_energy_buffer",
            600,
            CropFarmGameTestBody::testRaisingTierGrowsEnergyBuffer),
        new GameTestCase(
            "forestry/crop_farm_survey_changes_nothing_until_start",
            1200,
            CropFarmGameTestBody::testSurveyChangesNothingUntilStart),
        new GameTestCase(
            "forestry/crop_farm_upgrade_works_new_lanes_immediately",
            600,
            CropFarmGameTestBody::testUpgradeWorksNewLanesImmediately));

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
