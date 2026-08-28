package com.logistics.gametest.network;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link NetworkIntegrationGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class NetworkIntegrationGameTestRegistration {

    private NetworkIntegrationGameTestRegistration() {}

    /**
    * Verifies that placing connected logistics pipes causes a PipeNetwork to form.
    *
    * <p>Layout (y=1): [basic_logistics_pipe] [basic_logistics_pipe] [basic_logistics_pipe]
    * After a few ticks (pipes tick and register with NetworkRegistry), all three positions
    * should belong to the same network.
    *
    * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testlogisticsnetworkforms
    */
    @GameTest(template = "empty", batch = "networkintegration", timeoutTicks = 40)
    public static void testLogisticsNetworkForms(GameTestHelper context) {
        NetworkIntegrationGameTestBody.testLogisticsNetworkForms(context);
    }

    /**
    * Verifies that an insertion pipe delivers an injected item to an adjacent chest.
    *
    * <p>Layout (y=1): [item_insertion_pipe] → [chest]
    * The InsertionModule prefers to route items toward adjacent inventories, so a diamond
    * injected from the west should arrive in the chest without any network or energy setup.
    *
    * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testinsertionpipedeliverstochest
    */
    @GameTest(template = "empty", batch = "networkintegration", timeoutTicks = 40)
    public static void testInsertionPipeDeliversToChest(GameTestHelper context) {
        NetworkIntegrationGameTestBody.testInsertionPipeDeliversToChest(context);
    }

    /**
    * Verifies that a basic logistics pipe (default-route sink) routes an incoming item to an
    * adjacent inventory.
    *
    * <p>Layout (y=1): [basic_logistics_pipe] → [chest]
    * The default-route flag must be enabled explicitly (it is off by default, matching the
    * in-game behaviour where a player must toggle it via the wrench GUI). Once enabled,
    * any item arriving at the pipe will be deposited into the adjacent chest.
    *
    * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testbasicsinkdeliverstoadjacentchest
    */
    @GameTest(template = "empty", batch = "networkintegration", timeoutTicks = 40)
    public static void testBasicSinkDeliveresToAdjacentChest(GameTestHelper context) {
        NetworkIntegrationGameTestBody.testBasicSinkDeliveresToAdjacentChest(context);
    }

    /**
    * Verifies that the logistics network can be split and re-formed.
    *
    * <p>Layout (y=1): [basic_logistics_pipe] [basic_logistics_pipe] [basic_logistics_pipe]
    * After the initial network forms, the middle pipe is removed (network splits into two),
    * then replaced (network merges back into one). The final state should be a single network.
    *
    * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testnetworksplitsandrejoins
    */
    @GameTest(template = "empty", batch = "networkintegration", timeoutTicks = 100)
    public static void testNetworkSplitsAndRejoins(GameTestHelper context) {
        NetworkIntegrationGameTestBody.testNetworkSplitsAndRejoins(context);
    }

    /**
    * Verifies the end-to-end provider→requester delivery flow.
    *
    * <p>A provider pipe scans a source chest and registers supply; a requester pipe is
    * configured to request 4 diamonds; the network dispatches the order; the provider
    * extracts the items and injects them into the pipe; they travel through the transport
    * pipe and arrive in the destination chest.
    *
    * <p>Timing: provider scans at tick 6, requester fires at tick 20, provider drains
    * dispatch queue at tick 24, items arrive in dest chest around tick 40.
    *
    * <p>Layout (y=1):
    * [source_chest] ← [provider_pipe] → [transport_pipe] → [requester_pipe] → [dest_chest]
    *   (0,1,0)            (1,1,0)           (2,1,0)             (3,1,0)          (4,1,0)
    *
    * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testproviderdeliversitemtorequester
    */
    @GameTest(template = "empty", batch = "networkintegration", timeoutTicks = 100)
    public static void testProviderDeliversItemToRequester(GameTestHelper context) {
        NetworkIntegrationGameTestBody.testProviderDeliversItemToRequester(context);
    }
}
