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
 * Wires {@link NetherWartFarmGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class NetherWartFarmGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "forestry/nether_wart_farm_harvests_and_replants_and_hopper_pulls_harvest",
            1200,
            NetherWartFarmGameTestBody::testHarvestsAndReplantsAndHopperPullsHarvest),
        new GameTestCase(
            "forestry/nether_wart_farm_refills_empty_plot_from_hopper_fed_wart",
            1200,
            NetherWartFarmGameTestBody::testRefillsEmptyPlotFromHopperFedWart),
        new GameTestCase(
            "forestry/nether_wart_farm_full_farm_spills_at_the_plot",
            1200,
            NetherWartFarmGameTestBody::testFullFarmSpillsAtThePlot),
        new GameTestCase(
            "forestry/nether_wart_farm_does_nothing_without_power",
            400,
            NetherWartFarmGameTestBody::testDoesNothingWithoutPower));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private NetherWartFarmGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "forestry/nether_wart_farm", TESTS, FUNCTIONS);
    }
}
