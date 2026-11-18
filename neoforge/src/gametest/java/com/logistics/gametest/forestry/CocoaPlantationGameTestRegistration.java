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
 * Wires {@link CocoaPlantationGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class CocoaPlantationGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "forestry/cocoa_plantation_picks_and_replants_and_hopper_pulls_harvest", 1200,
            CocoaPlantationGameTestBody::testPicksAndReplantsAndHopperPullsHarvest),
        new GameTestCase(
            "forestry/cocoa_plantation_hopper_fed_beans_replant_empty_plots", 1200,
            CocoaPlantationGameTestBody::testHopperFedBeansReplantEmptyPlots),
        new GameTestCase(
            "forestry/cocoa_plantation_picks_and_refills_pod_outside_reach_on_log_inside", 1200,
            CocoaPlantationGameTestBody::testPicksAndRefillsPodOutsideReachOnLogInside),
        new GameTestCase(
            "forestry/cocoa_plantation_does_nothing_without_power", 400,
            CocoaPlantationGameTestBody::testDoesNothingWithoutPower));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private CocoaPlantationGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "forestry/cocoa_plantation", TESTS, FUNCTIONS);
    }
}
