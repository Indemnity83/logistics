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
        new GameTestCase(
            "forestry/crop_farm_harvests_and_replants_and_hopper_pulls_harvest",
            1200,
            CropFarmGameTestBody::testHarvestsAndReplantsAndHopperPullsHarvest),
        new GameTestCase(
            "forestry/crop_farm_full_farm_spills_at_the_plot",
            1200,
            CropFarmGameTestBody::testFullFarmSpillsAtThePlot),
        new GameTestCase(
            "forestry/crop_farm_does_nothing_without_power",
            400,
            CropFarmGameTestBody::testDoesNothingWithoutPower),
        new GameTestCase(
            "forestry/crop_farm_redstone_signal_pauses_farm",
            400,
            CropFarmGameTestBody::testRedstoneSignalPausesFarm),
        new GameTestCase(
            "forestry/crop_farm_lever_on_engine_does_not_pause_farm",
            1200,
            CropFarmGameTestBody::testLeverOnEngineDoesNotPauseFarm),
        new GameTestCase(
            "forestry/crop_farm_tier_survives_break_and_replace",
            100,
            CropFarmGameTestBody::testTierSurvivesBreakAndReplace),
        new GameTestCase(
            "forestry/crop_farm_raising_tier_grows_energy_buffer",
            600,
            CropFarmGameTestBody::testRaisingTierGrowsEnergyBuffer));

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
