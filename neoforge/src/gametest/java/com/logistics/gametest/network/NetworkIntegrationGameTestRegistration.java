package com.logistics.gametest.network;

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
 * Wires {@link NetworkIntegrationGameTestBody}'s methods into MC's data-driven GameTest
 * registries — see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class NetworkIntegrationGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("network/logistics_network_forms", 60, NetworkIntegrationGameTestBody::testLogisticsNetworkForms),
        new GameTestCase(
            "network/insertion_pipe_delivers_to_chest", 60, NetworkIntegrationGameTestBody::testInsertionPipeDeliversToChest),
        new GameTestCase(
            "network/basic_sink_delivers_to_adjacent_chest",
            60,
            NetworkIntegrationGameTestBody::testBasicSinkDeliveresToAdjacentChest),
        new GameTestCase(
            "network/network_splits_and_rejoins", 120, NetworkIntegrationGameTestBody::testNetworkSplitsAndRejoins),
        new GameTestCase(
            "network/provider_delivers_item_to_requester",
            120,
            NetworkIntegrationGameTestBody::testProviderDeliversItemToRequester));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private NetworkIntegrationGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "network/network_integration", TESTS, FUNCTIONS);
    }
}
