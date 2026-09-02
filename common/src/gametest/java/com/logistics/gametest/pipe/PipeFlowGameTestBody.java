package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Shared pipe-flow GameTest bodies (see {@code common/build.gradle}). One Fabric test in the
 * sibling {@code PipeFlowGameTest} — {@code testChestItemStorageReachable} — stays Fabric-only: it
 * specifically verifies Fabric API's own vanilla-chest-to-ItemStorage adapter, which has no NeoForge
 * equivalent to test here. Every other test originally used that same Fabric API purely as
 * test-setup plumbing (filling/reading a chest) and has been rewritten below to use vanilla
 * {@link ChestBlockEntity} access instead, which behaves identically on both loaders.
 */
public class PipeFlowGameTestBody {

    /**
     * Verifies that an item injected into a pipe travels to an adjacent chest.
     *
     * <p>Layout (y=1): [copper_transport_pipe] → [chest]
     * Item is injected at speed 0.05f (~20 ticks/segment).
     *
     * <p>Run in-game: /test run logistics-gametest.pipeflowgametest.testitemsflowintoadjacentchest
     */
    public static void testItemsFlowIntoAdjacentChest(GameTestHelper context) {
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
    public static void testItemTraversesMultipleSegments(GameTestHelper context) {
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
    public static void testVoidPipeDeletesIncomingItems(GameTestHelper context) {
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
    public static void testExtractorPullsItemFromChest(GameTestHelper context) {
        BlockPos sourceChestPos = new BlockPos(0, 1, 0);
        BlockPos extractorPos = new BlockPos(1, 1, 0);
        BlockPos destChestPos = new BlockPos(2, 1, 0);

        // Place source chest first so the extractor can detect it on placement
        context.setBlock(sourceChestPos, Blocks.CHEST);
        context.setBlock(extractorPos, LogisticsPipe.BLOCK.ITEM_EXTRACTOR_PIPE);
        context.setBlock(destChestPos, Blocks.CHEST);

        // Pre-fill source chest with a diamond
        ChestBlockEntity sourceChest = (ChestBlockEntity) context.getBlockEntity(sourceChestPos);
        if (sourceChest == null) {
            context.fail("Expected source chest block entity");
            return;
        }
        sourceChest.setItem(0, new ItemStack(Items.DIAMOND));

        // Inject energy directly so extraction fires at tick 8 (640 RF >= 1 × 10 RF cost)
        PipeBlockEntity extractor = (PipeBlockEntity) context.getBlockEntity(extractorPos);
        if (extractor == null) {
            context.fail("Extractor pipe should have a block entity");
            return;
        }
        EnergyComponent energy = extractor.getEnergy();
        energy.setAmount(640L);

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
    public static void testEnchantedTravelingItemSerialization(GameTestHelper context) {
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
     * Verifies an item in transit survives its pipe being torn down and rebuilt from saved NBT,
     * and still reaches the chest afterwards.
     *
     * <p>Layout (y=1): [pipe1] → [pipe2] → [pipe3] → [chest]. A slow item is injected into pipe1,
     * then pipe2 — carrying it mid-flight — is saved, replaced with a fresh block entity, and
     * reloaded from that NBT.
     *
     * <p>Reconstruction, not a chunk unload: {@code setBlock(AIR)} fires
     * {@code PipeBlockEntity#preRemoveSideEffects}, which drops the in-transit items as entities and
     * is never called on a real unload. A stray diamond is therefore left on the floor — assert on
     * the chest, never on a world-wide item count. See TESTING.md.
     */
    public static void testTravelingItemSurvivesPipeReconstruction(GameTestHelper context) {
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

        // Slow enough (~20 ticks per segment) that the item is still inside pipe2 when it is rebuilt.
        TravelingItem diamond = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.WEST, 0.05f);
        if (!entry.forceAddItem(diamond, Direction.WEST)) {
            context.fail("Pipe should accept the force-injected diamond");
            return;
        }

        HolderLookup.Provider registries = context.getLevel().registryAccess();
        boolean[] rebuilt = {false};

        context.succeedWhen(() -> {
            if (!rebuilt[0]) {
                PipeBlockEntity middle = (PipeBlockEntity) context.getBlockEntity(pipe2);
                if (middle == null) {
                    throw new GameTestAssertException("Middle pipe should have a block entity");
                }
                if (middle.getTravelingItems().isEmpty()) {
                    throw new GameTestAssertException("Waiting for the diamond to enter the middle pipe");
                }

                CompoundTag saved = middle.saveCustomOnly(registries);
                context.setBlock(pipe2, Blocks.AIR);
                context.setBlock(pipe2, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
                // Set before the assertions below: they run inside succeedWhen's retry loop, which
                // swallows the exception and re-runs each tick. Rebuilding again would re-drop items
                // every tick and bury the real failure behind a misleading "waiting" message.
                rebuilt[0] = true;

                PipeBlockEntity rebuiltPipe = (PipeBlockEntity) context.getBlockEntity(pipe2);
                if (rebuiltPipe == null) {
                    throw new GameTestAssertException("Expected a fresh PipeBlockEntity at " + pipe2);
                }
                // A fresh pipe starts empty, so a restored item can only have come from the NBT.
                if (!rebuiltPipe.getTravelingItems().isEmpty()) {
                    throw new GameTestAssertException(
                            "Replaced pipe should start empty before its saved data is loaded");
                }
                rebuiltPipe.loadCustomOnly(saved, registries);
                // If this ever trips, succeedWhen swallows it and the run reports the chest
                // assertion timing out instead; check this message in the log for the real cause.
                if (rebuiltPipe.getTravelingItems().isEmpty()) {
                    throw new GameTestAssertException(
                            "Reconstructed pipe lost the in-transit diamond when loading its saved data");
                }
            }

            context.assertContainerContains(chestPos, Items.DIAMOND);
        });
    }
}
