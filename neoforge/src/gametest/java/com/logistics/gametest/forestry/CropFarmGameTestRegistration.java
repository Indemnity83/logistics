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
        new GameTestCase("forestry/crop_farm_tills_and_plants_wheat", 800, CropFarmGameTestBody::testTillsAndPlantsWheat),
        new GameTestCase(
            "forestry/crop_farm_irrigates_farmland_from_its_tank", 800, CropFarmGameTestBody::testIrrigatesFarmlandFromItsTank),
        new GameTestCase(
            "forestry/crop_farm_harvests_and_replants_from_returned_seeds",
            2400,
            CropFarmGameTestBody::testHarvestsAndReplantsFromReturnedSeeds),
        new GameTestCase(
            "forestry/crop_farm_plants_but_reports_no_water", 620, CropFarmGameTestBody::testPlantsButReportsNoWater),
        new GameTestCase("forestry/crop_farm_does_nothing_without_power", 300, CropFarmGameTestBody::testDoesNothingWithoutPower),
        new GameTestCase("forestry/crop_farm_leaves_torch_on_plot", 800, CropFarmGameTestBody::testLeavesTorchOnPlot),
        new GameTestCase(
            "forestry/crop_farm_lever_on_engine_does_not_pause_farm",
            800,
            CropFarmGameTestBody::testLeverOnEngineDoesNotPauseFarm));

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
