package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Tick-based game tests verifying that items physically move through pipe networks.
 *
 * <p>Test logic lives in {@link PipeFlowGameTestBody} (shared with NeoForge — see
 * {@code common/src/gametest}), except {@link #testChestItemStorageReachable}, which stays here:
 * it specifically verifies Fabric API's own vanilla-chest-to-ItemStorage adapter, which has no
 * NeoForge equivalent.
 *
 * <p>Default tick limit (Fabric @GameTest maxTicks) is 20. Tests that need more time set
 * {@code maxTicks} explicitly.
 *
 * <p>Run all in-game: /test runall
 * Run one test:       /test run logistics-gametest.pipeflowgametest.&lt;methodname&gt;
 */
public class PipeFlowGameTest {

    /**
     * Verifies that an item injected into a pipe travels to an adjacent chest.
     *
     * <p>Layout (y=1): [copper_transport_pipe] → [chest]
     * Item is injected at speed 0.05f (~20 ticks/segment).
     *
     * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testitemsflowintoadjacentchest
     */
    @GameTest(maxTicks = 40)
    public void testItemsFlowIntoAdjacentChest(GameTestHelper context) {
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
    @GameTest
    public void testItemTraversesMultipleSegments(GameTestHelper context) {
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
    @GameTest(maxTicks = 30)
    public void testVoidPipeDeletesIncomingItems(GameTestHelper context) {
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
    @GameTest(maxTicks = 100)
    public void testExtractorPullsItemFromChest(GameTestHelper context) {
        PipeFlowGameTestBody.testExtractorPullsItemFromChest(context);
    }

    /**
     * Verifies that {@link com.logistics.core.lib.pipe.TravelingItem#CODEC} can serialize and
     * deserialize an enchanted item stack without throwing
     * {@code IllegalStateException: Can't access registry}.
     *
     * <p>Regression test for: the crash described above, caused by using {@code NbtOps.INSTANCE}
     * instead of {@code RegistryOps} when encoding registry-backed components (e.g. enchantments).
     * The fix ensures every {@code ItemStack.CODEC} call site in the pipe layer uses a
     * registry-aware {@code RegistryOps} context.
     *
     * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testenchantedtravelingitemserialization
     */
    @GameTest(maxTicks = 1)
    public void testEnchantedTravelingItemSerialization(GameTestHelper context) {
        PipeFlowGameTestBody.testEnchantedTravelingItemSerialization(context);
    }

    /**
     * Verifies that the Fabric ItemStorage API is accessible on a chest adjacent to a pipe.
     * This is a prerequisite sanity check — other tests rely on this working correctly.
     *
     * <p>Fabric-only: this tests Fabric API's own vanilla-chest adapter, not anything this mod
     * implements, so there is no NeoForge counterpart to port it to.
     *
     * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testchestitemstoragereachable
     */
    // loader-only: tests Fabric API's own vanilla-chest ItemStorage adapter, not this mod's code
    @GameTest
    public void testChestItemStorageReachable(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos chestPos = new BlockPos(1, 1, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);

        Storage<ItemVariant> chestStorage = ItemStorage.SIDED.find(
                context.getLevel(),
                context.absolutePos(chestPos),
                Direction.WEST);

        if (chestStorage == null) {
            context.fail("Chest should expose ItemStorage on its west face (toward the pipe)");
            return;
        }

        // Insert a diamond into the chest through the ItemStorage API
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = chestStorage.insert(ItemVariant.of(Items.DIAMOND), 1, transaction);
            if (inserted != 1) {
                context.fail("Expected to insert 1 diamond into chest via ItemStorage, got: " + inserted);
            }
            transaction.commit();
        }

        context.assertContainerContains(chestPos, Items.DIAMOND);
        context.succeed();
    }
}
