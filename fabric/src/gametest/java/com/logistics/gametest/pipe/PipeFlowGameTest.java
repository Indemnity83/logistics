package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;

/**
 * Tick-based game tests verifying that items physically move through pipe networks.
 *
 * <p>Tests inject items via {@link PipeBlockEntity#forceAddItem} (bypasses the ingress check that
 * prevents non-pipe insertion) and verify delivery after the server has ticked normally.
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
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testItemsFlowIntoAdjacentChest(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos chestPos = new BlockPos(1, 1, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);

        PipeBlockEntity pipe = (PipeBlockEntity) context.getBlockEntity(pipePos);
        if (pipe == null) {
            context.fail("Copper transport pipe should have a block entity");
            return;
        }

        // Use forceAddItem to bypass the ingress check (pipes only accept from other pipes by default).
        // Speed 0.05f = ~20 ticks per segment; the item should arrive well within 40 ticks.
        TravelingItem diamond = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.WEST, 0.05f);
        boolean accepted = pipe.forceAddItem(diamond, Direction.WEST);
        if (!accepted) {
            context.fail("Pipe should accept the force-injected diamond");
            return;
        }

        // Succeed as soon as the diamond appears in the chest (polls each tick).
        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIAMOND));
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
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty")
    public void testItemTraversesMultipleSegments(GameTestHelper context) {
        BlockPos pipe1 = new BlockPos(0, 1, 0);
        BlockPos pipe2 = new BlockPos(1, 1, 0);
        BlockPos pipe3 = new BlockPos(2, 1, 0);
        BlockPos chestPos = new BlockPos(3, 1, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(pipe3, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(pipe2, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(pipe1, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);

        PipeBlockEntity entry = (PipeBlockEntity) context.getBlockEntity(pipe1);
        if (entry == null) {
            context.fail("Entry pipe should have a block entity");
            return;
        }

        // Speed 0.5f: ~2 ticks per segment × 3 segments ≈ 6 ticks — fits the 20-tick default.
        TravelingItem fastEmerald = new TravelingItem(new ItemStack(Items.EMERALD), Direction.WEST, 0.5f);
        boolean accepted = entry.forceAddItem(fastEmerald, Direction.WEST);
        if (!accepted) {
            context.fail("Entry pipe should accept the force-injected emerald");
            return;
        }

        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.EMERALD));
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
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 30)
    public void testVoidPipeDeletesIncomingItems(GameTestHelper context) {
        BlockPos transportPos = new BlockPos(0, 1, 0);
        BlockPos voidPos = new BlockPos(1, 1, 0);
        BlockPos chestPos = new BlockPos(2, 1, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(voidPos, LogisticsPipe.BLOCK.ITEM_VOID_PIPE);
        context.setBlock(transportPos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);

        PipeBlockEntity pipe = (PipeBlockEntity) context.getBlockEntity(transportPos);
        if (pipe == null) {
            context.fail("Transport pipe should have a block entity");
            return;
        }

        // Inject a diamond at speed 0.5f; it should reach the void pipe in ~2 ticks and be deleted.
        TravelingItem diamond = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.WEST, 0.5f);
        boolean accepted = pipe.forceAddItem(diamond, Direction.WEST);
        if (!accepted) {
            context.fail("Transport pipe should accept the force-injected diamond");
            return;
        }

        // After 15 ticks the void pipe should have consumed the item; the chest must be empty
        // and no items should remain in transit in either the transport or void pipe.
        context.runAfterDelay(15, () -> {
            // Chest must remain empty
            context.assertContainerEmpty(chestPos);

            PipeBlockEntity transportPipe = (PipeBlockEntity) context.getBlockEntity(transportPos);
            if (transportPipe != null && !transportPipe.getTravelingItems().isEmpty()) {
                context.fail("Transport pipe should have no items in transit after void consumed them, found: "
                        + transportPipe.getTravelingItems().size());
                return;
            }

            PipeBlockEntity voidPipe = (PipeBlockEntity) context.getBlockEntity(voidPos);
            if (voidPipe != null && !voidPipe.getTravelingItems().isEmpty()) {
                context.fail("Void pipe should have no items in transit after voiding, found: "
                        + voidPipe.getTravelingItems().size());
                return;
            }

            context.succeed();
        });
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
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testExtractorPullsItemFromChest(GameTestHelper context) {
        BlockPos sourceChestPos = new BlockPos(0, 1, 0);
        BlockPos extractorPos = new BlockPos(1, 1, 0);
        BlockPos destChestPos = new BlockPos(2, 1, 0);

        // Place source chest first so the extractor can detect it on placement
        context.setBlock(sourceChestPos, Blocks.CHEST);
        context.setBlock(extractorPos, LogisticsPipe.BLOCK.ITEM_EXTRACTOR_PIPE);
        context.setBlock(destChestPos, Blocks.CHEST);

        // Pre-fill source chest with a diamond
        Storage<ItemVariant> sourceStorage = ItemStorage.SIDED.find(
                context.getLevel(),
                context.absolutePos(sourceChestPos),
                Direction.EAST);
        if (sourceStorage == null) {
            context.fail("Source chest should expose ItemStorage on its east face");
            return;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = sourceStorage.insert(ItemVariant.of(Items.DIAMOND), 1, transaction);
            if (inserted != 1) {
                context.fail("Expected to insert 1 diamond into source chest, got: " + inserted);
            }
            transaction.commit();
        }

        // Inject energy directly so extraction fires at tick 8 (640 RF >= 1 × 10 RF cost)
        PipeBlockEntity extractor = (PipeBlockEntity) context.getBlockEntity(extractorPos);
        if (extractor == null) {
            context.fail("Extractor pipe should have a block entity");
            return;
        }
        EnergyComponent energy = extractor.getEnergy();
        energy.amount = 640L;

        context.succeedWhen(() -> context.assertContainerContains(destChestPos, Items.DIAMOND));
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
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1)
    public void testEnchantedTravelingItemSerialization(GameTestHelper context) {
        ServerLevel level = context.getLevel();

        // Enchantments are data-driven and require a live registry — not available in unit tests.
        HolderLookup.RegistryLookup<Enchantment> enchLookup =
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchLookup.getOrThrow(Enchantments.SHARPNESS);
        ItemStack enchantedSword = new ItemStack(Items.DIAMOND_SWORD);
        enchantedSword.enchant(sharpness, 5);

        TravelingItem original = new TravelingItem(enchantedSword, Direction.EAST, 0.05f);

        // Regression test: before the RegistryOps fix, this threw
        // IllegalStateException: Can't access registry (enchantments require registry-aware ops).
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag encoded;
        try {
            encoded = TravelingItem.CODEC.encodeStart(ops, original).getOrThrow();
        } catch (Exception e) {
            context.fail("TravelingItem serialization threw with registry-backed enchantment: " + e.getMessage());
            return;
        }

        TravelingItem decoded;
        try {
            decoded = TravelingItem.CODEC.parse(ops, encoded).getOrThrow();
        } catch (Exception e) {
            context.fail("TravelingItem deserialization threw: " + e.getMessage());
            return;
        }

        ItemEnchantments enchants = decoded.getStack().get(DataComponents.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            context.fail("Enchantments lost after TravelingItem serialize+deserialize");
            return;
        }
        for (Holder<Enchantment> h : enchants.keySet()) {
            if (h.is(Enchantments.SHARPNESS) && enchants.getLevel(h) == 5) {
                context.succeed();
                return;
            }
        }
        context.fail("Sharpness V not intact after TravelingItem serialize+deserialize");
    }

    /**
     * Verifies that the Fabric ItemStorage API is accessible on a chest adjacent to a pipe.
     * This is a prerequisite sanity check — other tests rely on this working correctly.
     *
     * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testchestitemstoragereachable
     */
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty")
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
