package com.logistics.gametest.pipe;

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
 * Wires {@link PipeFlowGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 *
 * <p>Fabric's {@code testChestItemStorageReachable} has no entry here: it verifies Fabric API's
 * own vanilla-chest-to-ItemStorage adapter, which has no NeoForge equivalent to test.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class PipeFlowGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("pipe/items_flow_into_adjacent_chest", 60, PipeFlowGameTestBody::testItemsFlowIntoAdjacentChest),
        new GameTestCase("pipe/item_traverses_multiple_segments", 40, PipeFlowGameTestBody::testItemTraversesMultipleSegments),
        new GameTestCase("pipe/void_pipe_deletes_incoming_items", 50, PipeFlowGameTestBody::testVoidPipeDeletesIncomingItems),
        new GameTestCase("pipe/extractor_pulls_item_from_chest", 120, PipeFlowGameTestBody::testExtractorPullsItemFromChest),
        new GameTestCase(
            "pipe/enchanted_traveling_item_serialization", 20, PipeFlowGameTestBody::testEnchantedTravelingItemSerialization));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private PipeFlowGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/pipe_flow", TESTS, FUNCTIONS);
    }
}
