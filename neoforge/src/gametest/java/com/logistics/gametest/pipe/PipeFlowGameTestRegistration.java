package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link PipeFlowGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class PipeFlowGameTestRegistration {

    private PipeFlowGameTestRegistration() {}

    /**
    * Verifies that an item injected into a pipe travels to an adjacent chest.
    *
    * <p>Layout (y=1): [copper_transport_pipe] → [chest]
    * Item is injected at speed 0.05f (~20 ticks/segment).
    *
    * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testitemsflowintoadjacentchest
    */
    @GameTest(template = "empty", batch = "pipeflow", timeoutTicks = 40)
    public static void testItemsFlowIntoAdjacentChest(GameTestHelper context) {
        PipeFlowGameTestBody.testItemsFlowIntoAdjacentChest(context);
    }

    /**
    * Verifies that an item traverses three connected pipe segments and arrives at a chest.
    *
    * <p>Layout (y=1): [pipe1] → [pipe2] → [pipe3] → [chest]
    * Item is injected at speed 0.5f (~2 ticks/segment × 3 segments ≈ 6 ticks total),
    * well within the default 20-tick limit.
    *
    * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testitemtraversesmultiplesegments
    */
    @GameTest(template = "empty", batch = "pipeflow")
    public static void testItemTraversesMultipleSegments(GameTestHelper context) {
        PipeFlowGameTestBody.testItemTraversesMultipleSegments(context);
    }

    /**
    * Verifies that a void pipe destroys items instead of letting them reach a downstream chest.
    *
    * <p>Layout (y=1): [copper_transport_pipe] → [void_pipe] → [chest]
    * An item injected at speed 0.5f should reach (and be voided by) the void pipe within a few
    * ticks. After 15 ticks the chest should still be empty and no items should remain in transit.
    *
    * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testvoidpipedeletesincomingitems
    */
    @GameTest(template = "empty", batch = "pipeflow", timeoutTicks = 30)
    public static void testVoidPipeDeletesIncomingItems(GameTestHelper context) {
        PipeFlowGameTestBody.testVoidPipeDeletesIncomingItems(context);
    }

    /**
    * Verifies that an extractor pipe autonomously pulls items from an adjacent chest
    * and routes them to a destination chest.
    *
    * <p>Layout (y=1): [source_chest] ← [item_extractor_pipe] → [dest_chest]
    * The extractor auto-selects WEST (toward source_chest) on placement; energy is injected
    * directly so no engine is required. The item travels at ITEM_MIN_SPEED through the pipe
    * and is deposited in the destination chest via BlockConnectionModule.
    *
    * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testextractorpullsitemfromchest
    */
    @GameTest(template = "empty", batch = "pipeflow", timeoutTicks = 100)
    public static void testExtractorPullsItemFromChest(GameTestHelper context) {
        PipeFlowGameTestBody.testExtractorPullsItemFromChest(context);
    }

    /**
    * Verifies that {@link TravelingItem#CODEC} can serialize and deserialize an enchanted
    * {@link ItemStack} without throwing {@code IllegalStateException: Can't access registry}.
    *
    * <p>Regression test for: the crash described above, caused by using {@code NbtOps.INSTANCE}
    * instead of {@code RegistryOps} when encoding registry-backed components (e.g. enchantments).
    * The fix ensures every {@code ItemStack.CODEC} call site in the pipe layer uses a
    * registry-aware {@code RegistryOps} context.
    *
    * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testenchantedtravelingitemserialization
    */
    @GameTest(template = "empty", batch = "pipeflow", timeoutTicks = 1)
    public static void testEnchantedTravelingItemSerialization(GameTestHelper context) {
        PipeFlowGameTestBody.testEnchantedTravelingItemSerialization(context);
    }
}
