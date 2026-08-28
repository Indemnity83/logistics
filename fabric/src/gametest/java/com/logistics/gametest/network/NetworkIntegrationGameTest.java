package com.logistics.gametest.network;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Tick-based game tests for multi-block logistics network behaviours.
 *
 * <p>These tests verify that the pipe network layer (NetworkRegistry, SinkModule, routing)
 * functions correctly in a live Minecraft environment where block entities tick normally.
 * Test logic lives in {@link NetworkIntegrationGameTestBody} (shared with NeoForge — see
 * {@code common/src/gametest}).
 *
 * <p>Run all in-game: /test runall
 * Run one test:       /test run logistics-gametest.networkintegrationgametest.&lt;methodname&gt;
 */
public class NetworkIntegrationGameTest {

    /**
     * Verifies that placing connected logistics pipes causes a PipeNetwork to form.
     *
     * <p>Layout (y=1): [basic_logistics_pipe] [basic_logistics_pipe] [basic_logistics_pipe]
     * After a few ticks (pipes tick and register with NetworkRegistry), all three positions
     * should belong to the same network.
     *
     * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testlogisticsnetworkforms
     */
    @GameTest(maxTicks = 40)
    public void testLogisticsNetworkForms(GameTestHelper context) {
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
    @GameTest(maxTicks = 40)
    public void testInsertionPipeDeliversToChest(GameTestHelper context) {
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
    @GameTest(maxTicks = 40)
    public void testBasicSinkDeliveresToAdjacentChest(GameTestHelper context) {
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
    @GameTest(maxTicks = 100)
    public void testNetworkSplitsAndRejoins(GameTestHelper context) {
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
    @GameTest(maxTicks = 100)
    public void testProviderDeliversItemToRequester(GameTestHelper context) {
        NetworkIntegrationGameTestBody.testProviderDeliversItemToRequester(context);
    }

    // testSinkPriorityRoutesItemToHigherPrioritySink:
    //   Two basic logistics pipes connected to separate chests, one with higher priority.
    //   Items injected into the network should be routed to the higher-priority sink first.
}
