package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.pipe.TransferHandlerModule;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.*;
import com.logistics.core.lib.pipe.Module;
import com.logistics.pipe.network.CraftBatchingService;
import com.logistics.core.lib.network.CrafterBufferState;
import com.logistics.core.lib.network.CrafterSnapshot;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.network.RecipeIngredient;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.CraftingScreenHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Crafting pipe module - presents craftable items as available to the network,
 * sources ingredients from the network, routes them into an adjacent Minecraft Autocrafter,
 * and triggers crafting via a redstone pulse whenever a complete recipe set is loaded.
 *
 * <p>Triggering is poll-based: every {@code PULSE_COOLDOWN} ticks, if the autocrafter holds a
 * complete recipe set the pipe pulses its redstone output. Multiple batches are handled
 * naturally — the crafter fires each time a full set of ingredients is present.
 *
 * <p>Ingredients are routed to the autocrafter slot that currently holds the fewest of that
 * item type relative to the recipe requirement (fewest-complete-batches-first distribution).
 *
 * <p>Orders are queued: a second dispatch is accepted while the first is still crafting, and
 * ingredient orders for all queued entries are placed immediately so materials arrive in
 * parallel rather than serially.
 */
public class CraftingModule implements Module, TickingModule, RoutingModule, DispatchableModule, TransferHandlerModule {
    /** Max items to craft per 6-tick cycle (one tier step above = larger batches). */
    private final int itemLimit;
    /** Max output stacks to dispatch per cycle (future use; stored for tier identity). */
    private final int stackLimit;

    public CraftingModule(int itemLimit, int stackLimit) {
        this.itemLimit = itemLimit;
        this.stackLimit = stackLimit;
    }

    // NBT keys — recipe config (public for item-mode access)
    public static final String RECIPE_ITEMS = "recipe_items";
    public static final String RECIPE_COUNTS = "recipe_counts";
    public static final String RESULT_ITEM = "result_item";
    public static final String RESULT_COUNT = "result_count";
    // NBT keys — behavior (public for item-mode access)
    public static final String BLOCKING = "blocking";
    // NBT keys — timing
    private static final String TICKS_SCAN = "ticks_scan";
    private static final String TICKS_PULSE = "ticks_pulse";
    // NBT keys — order queue (ListTag of CompoundTag entries)
    private static final String QUEUE = "queue";
    private static final String ENTRY_REQ = "req";   // BlockPos as "x,y,z"
    private static final String ENTRY_DLV = "dlv";   // UUID as string
    private static final String ENTRY_AMT = "amt";   // remaining amount (long)
    private static final String ENTRY_IDS = "ids";   // ingredient order IDs (CompoundTag)
    // Timing constants
    private static final int SCAN_INTERVAL = 20;    // Update crafter supply every 20 ticks
    private static final int CRAFT_INTERVAL = 6;    // Execute direct crafts every 6 ticks

    // Dispatch priority: crafters are fallback (prefer real stock in providers)
    private static final int CRAFTER_PRIORITY = 5;

    // ==================== NBT State Accessors ====================

    /** True when there is at least one order in the queue. */
    public boolean isActive(PipeContext ctx) {
        return !getQueue(ctx).isEmpty();
    }

    public boolean isBlocking(PipeContext ctx) {
        return ctx.getInt(this, BLOCKING, 0) == 1;
    }

    public void setBlocking(PipeContext ctx, boolean blocking) {
        ctx.saveInt(this, BLOCKING, blocking ? 1 : 0);
        ctx.markDirtyAndSync();
    }

    public String getIngredientItem(PipeContext ctx, int slot) {
        CompoundTag items = ctx.getCompoundTag(this, RECIPE_ITEMS);
        return NbtCompat.getString(items, String.valueOf(slot), "");
    }

    public int getIngredientCount(PipeContext ctx, int slot) {
        CompoundTag counts = ctx.getCompoundTag(this, RECIPE_COUNTS);
        return NbtCompat.getInt(counts, String.valueOf(slot), 1);
    }

    public void setIngredient(PipeContext ctx, int slot, String itemId, int count) {
        if (slot < 0 || slot > 8) return;
        CompoundTag items = ctx.getCompoundTag(this, RECIPE_ITEMS);
        CompoundTag counts = ctx.getCompoundTag(this, RECIPE_COUNTS);
        if (itemId == null || itemId.isEmpty()) {
            items.remove(String.valueOf(slot));
            counts.remove(String.valueOf(slot));
        } else {
            items.putString(String.valueOf(slot), itemId);
            counts.putInt(String.valueOf(slot), Math.max(1, count));
        }
        ctx.putCompoundTag(this, RECIPE_ITEMS, items);
        ctx.putCompoundTag(this, RECIPE_COUNTS, counts);
        ctx.markDirtyAndSync();
    }

    public String getResultItem(PipeContext ctx) {
        return ctx.getString(this, RESULT_ITEM, "");
    }

    public int getResultCount(PipeContext ctx) {
        return ctx.getInt(this, RESULT_COUNT, 1);
    }

    public void setResult(PipeContext ctx, String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) {
            ctx.moduleState(this).remove(RESULT_ITEM);
            ctx.moduleState(this).remove(RESULT_COUNT);
        } else {
            ctx.saveString(this, RESULT_ITEM, itemId);
            ctx.saveInt(this, RESULT_COUNT, Math.max(1, count));
        }
        ctx.markDirtyAndSync();
    }

    // ==================== Static Tag Helpers (for item-mode) ====================

    public static String getIngredientItemFromTag(CompoundTag tag, int slot) {
        CompoundTag items = NbtCompat.getCompoundOrEmpty(tag, RECIPE_ITEMS);
        return NbtCompat.getString(items, String.valueOf(slot), "");
    }

    public static int getIngredientCountFromTag(CompoundTag tag, int slot) {
        CompoundTag counts = NbtCompat.getCompoundOrEmpty(tag, RECIPE_COUNTS);
        return NbtCompat.getInt(counts, String.valueOf(slot), 1);
    }

    public static String getResultItemFromTag(CompoundTag tag) {
        return NbtCompat.getString(tag, RESULT_ITEM, "");
    }

    public static int getResultCountFromTag(CompoundTag tag) {
        return NbtCompat.getInt(tag, RESULT_COUNT, 1);
    }

    // ==================== Item Config ====================

    @Override
    public InteractionResult openItemConfig(
            ServerPlayer player, net.minecraft.world.InteractionHand hand, ItemStack stack) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new CraftingScreenHandler(syncId, inv, player, hand),
                stack.getHoverName()));
        return InteractionResult.SUCCESS;
    }

    // ==================== Queue Accessors ====================

    private ListTag getQueue(PipeContext ctx) {
        return NbtCompat.getListOrEmpty(ctx.moduleState(this), QUEUE);
    }

    private void saveQueue(PipeContext ctx, ListTag queue) {
        ctx.moduleState(this).put(QUEUE, queue);
        ctx.markDirtyAndSync();
    }

    @Nullable
    private static BlockPos parseBlockPos(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            String[] parts = s.split(",");
            return new BlockPos(
                    Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static UUID parseUUID(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Autocrafter Detection ====================

    @Nullable
    public Direction findAutocrafterDirection(PipeContext ctx) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = ctx.pos().relative(dir);
            if (ctx.world().getBlockEntity(neighborPos) instanceof CrafterBlockEntity) {
                return dir;
            }
        }
        return null;
    }

    /**
     * Reads the adjacent autocrafter's current slot contents and recipe result, and writes them
     * into this module's ghost recipe config. Clears any slots in the crafter that are empty.
     */
    public void importFromAutocrafter(PipeContext ctx) {
        if (!(ctx.world() instanceof ServerLevel serverLevel)) return;
        Direction dir = findAutocrafterDirection(ctx);
        if (dir == null) return;
        BlockPos autocrafterPos = ctx.pos().relative(dir);
        if (!(serverLevel.getBlockEntity(autocrafterPos) instanceof CrafterBlockEntity crafter)) return;

        // Collect all ingredient data locally before touching NBT
        CompoundTag items = new CompoundTag();
        CompoundTag counts = new CompoundTag();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = crafter.getItem(slot);
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                items.putString(String.valueOf(slot), itemId);
                counts.putInt(String.valueOf(slot), Math.max(1, stack.getCount()));
            }
        }
        ctx.putCompoundTag(this, RECIPE_ITEMS, items);
        ctx.putCompoundTag(this, RECIPE_COUNTS, counts);

        // Compute and store recipe result
        CraftingInput craftInput = crafter.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> recipeOpt = CrafterBlock.getPotentialResults(serverLevel, craftInput);
        CompoundTag state = ctx.moduleState(this);
        if (recipeOpt.isPresent()) {
            CraftingRecipe recipe = recipeOpt.get().value();
            ItemStack result = recipe.assemble(craftInput, serverLevel.registryAccess());
            if (!result.isEmpty()) {
                state.putString(RESULT_ITEM, BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
                state.putInt(RESULT_COUNT, Math.max(1, result.getCount()));
            } else {
                state.remove(RESULT_ITEM);
                state.remove(RESULT_COUNT);
            }
        } else {
            state.remove(RESULT_ITEM);
            state.remove(RESULT_COUNT);
        }

        ctx.markDirtyAndSync();
    }

    // ==================== Module Interface ====================

    @Override
    public void onTick(PipeContext ctx) {
        // Periodic: update crafter supply table
        int scanTicks = ctx.getInt(this, TICKS_SCAN, 0) + 1;
        ctx.saveInt(this, TICKS_SCAN, scanTicks);
        if (scanTicks >= SCAN_INTERVAL) {
            ctx.saveInt(this, TICKS_SCAN, 0);
            updateCrafterSupply(ctx);
        }

        if (!isActive(ctx)) return;

        Direction autocrafterDir = findAutocrafterDirection(ctx);
        if (autocrafterDir == null) {
            resetToIdle(ctx);
            return;
        }

        // Every CRAFT_INTERVAL ticks: execute craft batches directly via the recipe API.
        int ticks = ctx.getInt(this, TICKS_PULSE, 0) + 1;
        ctx.saveInt(this, TICKS_PULSE, ticks);
        if (ticks >= CRAFT_INTERVAL) {
            ctx.saveInt(this, TICKS_PULSE, 0);
            performDirectCrafts(ctx, autocrafterDir);
        }
    }

    /**
     * Execute up to {@code itemLimit / resultCount} craft cycles directly against the adjacent
     * autocrafter, bypassing its redstone trigger. Each cycle: resolves the recipe from the
     * autocrafter's current slot contents, assembles the result, and consumes one ingredient
     * set (shrinks each occupied slot by 1 — identical to vanilla {@code dispenseFrom} logic).
     * All output is accumulated into a single stack and dispatched through the normal queue
     * via {@link #onExternalInsert}. The vanilla autocrafter animation is triggered once per
     * call (regardless of batch count) so the player gets visual feedback.
     */
    private void performDirectCrafts(PipeContext ctx, Direction autocrafterDir) {
        if (!(ctx.world() instanceof ServerLevel serverLevel)) return;

        BlockPos autocrafterPos = ctx.pos().relative(autocrafterDir);
        if (!(ctx.world().getBlockEntity(autocrafterPos) instanceof CrafterBlockEntity crafter)) return;

        int resultCount = getResultCount(ctx);
        if (resultCount <= 0) return;

        // Resolve once — used every iteration to guard against partial ingredient fills
        // accidentally matching a different recipe (e.g. cobble+redstone → dropper before
        // the bow needed for a dispenser arrives).
        ItemStack configuredResult = resolveItem(getResultItem(ctx));
        if (configuredResult.isEmpty()) return;

        // itemLimit caps by raw item count; stackLimit caps by output stacks (stackLimit × maxStackSize).
        // Use whichever is more restrictive, but always allow at least 1 batch.
        int stackItemCap = stackLimit * configuredResult.getMaxStackSize();
        int maxBatches = Math.min(
                Math.max(1, itemLimit / resultCount),
                Math.max(1, stackItemCap / resultCount));

        ItemStack collectedResult = ItemStack.EMPTY;
        int batchesDone = 0;

        for (int i = 0; i < maxBatches; i++) {
            // Rebuild CraftingInput each iteration — slot contents change as we consume ingredients.
            CraftingInput craftInput = crafter.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> recipeOpt =
                    CrafterBlock.getPotentialResults(serverLevel, craftInput);
            if (recipeOpt.isEmpty()) break; // Incomplete recipe set — stop

            CraftingRecipe recipe = recipeOpt.get().value();
            ItemStack result = recipe.assemble(craftInput, serverLevel.registryAccess());
            if (result.isEmpty()) break;

            if (!ItemStack.isSameItemSameComponents(result, configuredResult)) {
                break;
            }

            // Get remaining items (empty containers etc.) BEFORE consuming ingredients.
            // CraftingInput holds live ItemStack references from the autocrafter — after
            // shrink(1) those stacks have count=0, so isEmpty()=true and
            // hasCraftingRemainingItem() returns false. Vanilla dispenseFrom() also
            // calls getRemainingItems before shrinking for the same reason.
            List<ItemStack> remainingItems = recipe.getRemainingItems(craftInput);

            // Consume one set of ingredients (shrink each occupied slot by 1).
            // Identical to vanilla CrafterBlock.dispenseFrom() ingredient consumption logic.
            crafter.getItems().forEach(stack -> {
                if (!stack.isEmpty()) stack.shrink(1);
            });

            // Accumulate output across batches
            if (collectedResult.isEmpty()) {
                collectedResult = result.copy();
            } else if (ItemStack.isSameItemSameComponents(collectedResult, result)) {
                collectedResult.grow(result.getCount());
            } else {
                // Different output type mid-run (shouldn't happen; flush and reset)
                onExternalInsert(ctx, collectedResult, autocrafterDir);
                collectedResult = result.copy();
            }

            // Dispatch remaining items (e.g. empty buckets returned by milk-bucket recipes)
            for (ItemStack remaining : remainingItems) {
                if (!remaining.isEmpty()) {
                    TravelingItem surplus = new TravelingItem(
                            remaining.copy(),
                            autocrafterDir.getOpposite(),
                            LogisticsConfig.get().pipe.minSpeed);
                    ctx.blockEntity().forceAddItem(surplus, autocrafterDir);
                }
            }

            batchesDone++;
        }

        if (!collectedResult.isEmpty()) {
            NetDbg.out("[Crafter @ {}] Performed {} craft batch(es) → {}x{}", ctx.pos(), batchesDone, collectedResult.getCount(), collectedResult.getItem());
            onExternalInsert(ctx, collectedResult, autocrafterDir);
        } else if (batchesDone == 0) {
            NetDbg.out("[Crafter @ {}] Recipe incomplete after {} batches", ctx.pos(), batchesDone);
        }

        if (batchesDone > 0) {
            crafter.setChanged();

            // Trigger the vanilla autocrafter's crafting animation once per cycle.
            // CrafterBlockEntity.serverTick() counts down and resets CRAFTING=false automatically.
            crafter.setCraftingTicksRemaining(CRAFT_INTERVAL);
            BlockState autocrafterState = serverLevel.getBlockState(autocrafterPos);
            if (autocrafterState.hasProperty(CrafterBlock.CRAFTING)) {
                serverLevel.setBlock(
                        autocrafterPos,
                        autocrafterState.setValue(CrafterBlock.CRAFTING, true),
                        2); // flag 2 = notify clients only, no neighbor block update
            }

            // Update the pipe's own visual CRAFTING indicator (no redstone — visual only).
            // Only (re-)enable when there is still work remaining; onExternalInsert may have
            // already cleared CRAFTING=false when the last queued entry was just fulfilled.
            BlockState pipeState = ctx.world().getBlockState(ctx.pos());
            if (pipeState.hasProperty(PipeBlock.CRAFTING)
                    && !pipeState.getValue(PipeBlock.CRAFTING)
                    && isActive(ctx)) {
                ctx.world().setBlock(ctx.pos(), pipeState.setValue(PipeBlock.CRAFTING, true), 2);
            }
        }
    }

    /**
     * Called by the network to start a crafting run.
     * Adds the order to the queue and immediately places ingredient orders so materials
     * arrive in parallel with any currently-active crafting. Returns {@code amount} as a
     * promise; delivery happens after crafting completes.
     */
    @Override
    public long onDispatch(
            PipeContext ctx, BlockPos requester, ItemVariant item, long amount, UUID deliveryId) {
        String resultId = getResultItem(ctx);
        if (resultId.isEmpty()) {
            NetDbg.out("[Crafter @ {}] Dispatch rejected: no result configured", ctx.pos());
            return 0;
        }
        Direction autocrafterDir = findAutocrafterDirection(ctx);
        if (autocrafterDir == null) {
            NetDbg.out("[Crafter @ {}] Dispatch rejected: no autocrafter found", ctx.pos());
            return 0;
        }

        ItemStack resultStack = resolveItem(resultId);
        if (resultStack.isEmpty() || !item.matches(resultStack)) {
            NetDbg.out("[Crafter @ {}] Dispatch rejected: item mismatch (requested={}, result={})", ctx.pos(), item, resultId);
            return 0;
        }

        if (amount <= 0) return 0;

        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return 0;

        int resultCount = getResultCount(ctx);
        if (resultCount <= 0) {
            NetDbg.out("[Crafter @ {}] Dispatch rejected: resultCount={}", ctx.pos(), resultCount);
            return 0;
        }

        // Cap batches to what the autocrafter's input buffer can currently absorb
        CrafterBufferState bufferState = computeBufferState(ctx, autocrafterDir);
        long batchCount = new CraftBatchingService().safeBatchCount(amount, resultCount, bufferState);
        if (batchCount <= 0) {
            NetDbg.out("[Crafter @ {}] Dispatch rejected: buffer full", ctx.pos());
            return -1; // buffer full: defer, don't remove from supply table
        }

        // Cap to the originally-requested amount: the recipe may produce more per batch than was
        // ordered (e.g. 4 planks/craft for a 2-plank order). Committing only what was requested
        // means onExternalInsert routes the excess as un-destined surplus to a sink rather than
        // force-routing all crafted output to the requester regardless of how much they wanted.
        long actualAmount = Math.min(amount, batchCount * resultCount);

        // Aggregate total ingredient amounts needed for the capped batch count
        Map<String, Long> totalNeededByItem = new LinkedHashMap<>();
        for (int slot = 0; slot < 9; slot++) {
            String ingredientId = getIngredientItem(ctx, slot);
            if (ingredientId.isEmpty()) continue;
            totalNeededByItem.merge(ingredientId, (long) getIngredientCount(ctx, slot) * batchCount, Long::sum);
        }

        // No ingredients configured — reject the dispatch
        if (totalNeededByItem.isEmpty()) {
            NetDbg.out("[Crafter @ {}] Dispatch rejected: no ingredients configured", ctx.pos());
            return 0;
        }

        // Pre-validate all ingredients before placing any orders — fail fast so we never
        // queue a craft with a missing ingredient order that will never arrive.
        Map<String, ItemStack> resolvedIngredients = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : totalNeededByItem.entrySet()) {
            ItemStack ingredientStack = resolveItem(entry.getKey());
            if (ingredientStack.isEmpty()) {
                LogisticsPipe.LOGGER.warn(
                        "[Crafter @ {}] Cannot fulfil craft of '{}': ingredient '{}' is not a known item."
                                + " Check the recipe configured in the crafting pipe.",
                        ctx.pos(), getResultItem(ctx), entry.getKey());
                return 0;
            }
            resolvedIngredients.put(entry.getKey(), ingredientStack);
        }

        // Compute how many of each ingredient the new queue entry must order from the network.
        //
        // The crafter's "usable pool" for a new entry is:
        //   physicalStock + pending - totalCommittedByExistingEntries
        //
        // where pending = orders still in transit (getOrderedAmountFor), and
        // totalCommitted = what all existing queue entries have already claimed (regardless of
        // whether those ingredients arrived physically or are still en route).
        // The new entry only needs to order the shortfall from that free pool.
        //
        // This correctly handles concurrent requests: ingredients ordered by Entry A that have
        // already physically arrived in the crafter are "committed" to A's quota, so Entry B
        // cannot mistakenly treat them as free stock.
        BlockPos autocrafterPos = ctx.pos().relative(autocrafterDir);
        CrafterBlockEntity crafterEntity = null;
        if (ctx.world().getBlockEntity(autocrafterPos) instanceof CrafterBlockEntity ce) {
            crafterEntity = ce;
        }

        ListTag existingQueue = getQueue(ctx);

        Map<String, Long> neededByItem = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : totalNeededByItem.entrySet()) {
            String ingredientId = entry.getKey();
            long needed = entry.getValue();

            // Physical stock = items currently in the autocrafter slots.
            long physicalStock = 0;
            if (crafterEntity != null) {
                for (int slot = 0; slot < 9; slot++) {
                    if (!ingredientId.equals(getIngredientItem(ctx, slot))) continue;
                    ItemStack inSlot = crafterEntity.getItem(slot);
                    if (inSlot.isEmpty()) continue;
                    // Only count if the slot actually holds the expected ingredient.
                    // A stale or wrong item should not inflate the available stock estimate.
                    if (!ingredientId.equals(BuiltInRegistries.ITEM.getKey(inSlot.getItem()).toString())) continue;
                    physicalStock += inSlot.getCount();
                }
            }

            // Orders placed by this crafter pipe that haven't physically arrived yet
            long pending = network.getOrderedAmountFor(ctx.pos(), resolvedIngredients.get(ingredientId));

            // Ingredients committed to all existing queue entries (arrived + still in transit)
            long totalCommitted = 0;
            for (int qi = 0; qi < existingQueue.size(); qi++) {
                CompoundTag qe = existingQueue.getCompound(qi).orElse(null);
                if (qe == null) continue;
                long qeAmt = NbtCompat.getLong(qe, ENTRY_AMT, 0L);
                long qeBatches = resultCount > 0 ? (qeAmt + resultCount - 1) / resultCount : 0;
                for (int slot = 0; slot < 9; slot++) {
                    if (!ingredientId.equals(getIngredientItem(ctx, slot))) continue;
                    totalCommitted += (long) getIngredientCount(ctx, slot) * qeBatches;
                }
            }

            // Free pool = total that will eventually be in the crafter minus what's already spoken for
            long freePool = Math.max(0, physicalStock + pending - totalCommitted);
            long toOrder = Math.max(0, needed - freePool);

            if (toOrder > 0) {
                neededByItem.put(ingredientId, toOrder);
            }
        }

        // Place ingredient orders immediately — each queued order orders its own ingredients
        // independently so materials arrive in parallel rather than waiting for prior orders.
        CompoundTag ingredientOrderIds = new CompoundTag();
        for (Map.Entry<String, Long> entry : neededByItem.entrySet()) {
            UUID ingredientOrderId = network.placeOrder(
                    ItemVariant.of(resolvedIngredients.get(entry.getKey())), entry.getValue(), ctx.pos());
            ingredientOrderIds.putString(entry.getKey(), ingredientOrderId.toString());
        }

        // Append entry to queue (for the capped amount we actually accepted)
        CompoundTag queueEntry = new CompoundTag();
        queueEntry.putString(ENTRY_REQ, requester.getX() + "," + requester.getY() + "," + requester.getZ());
        queueEntry.putString(ENTRY_DLV, deliveryId.toString());
        queueEntry.putLong(ENTRY_AMT, actualAmount);
        queueEntry.put(ENTRY_IDS, ingredientOrderIds);

        ListTag queue = getQueue(ctx);
        queue.add(queueEntry);
        saveQueue(ctx, queue);
        ctx.saveInt(this, TICKS_PULSE, 0);
        NetDbg.out("[Crafter @ {}] Queue entry created: {}x{} for requester {} ({} batches)", ctx.pos(), actualAmount, resultId, requester, batchCount);

        // Blocking mode: immediately suppress supply so no further orders arrive
        if (isBlocking(ctx)) {
            network.registerSupply(ctx.pos(), new HashMap<>(), CRAFTER_PRIORITY);
            network.unregisterProviderCheck(ctx.pos());
        }

        return actualAmount; // Promise: delivery happens after crafting completes (capped to buffer capacity)
    }

    /**
     * Compute the autocrafter's current input buffer capacity: how many batches worth of
     * ingredients can still fit in the crafter's input slots. Returns {@link CrafterBufferState#FULL}
     * when the crafter cannot be found.
     */
    private CrafterBufferState computeBufferState(PipeContext ctx, Direction autocrafterDir) {
        BlockPos autocrafterPos = ctx.pos().relative(autocrafterDir);
        if (!(ctx.world().getBlockEntity(autocrafterPos) instanceof CrafterBlockEntity crafter)) {
            return CrafterBufferState.FULL;
        }
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());

        // Aggregate per unique ingredient: total slot space and total recipe count per batch.
        // Tracking by ingredient ID handles recipes where the same item appears in multiple slots.
        Map<String, long[]> perIngredient = new LinkedHashMap<>(); // ingredientId → [totalSpace, recipeCountPerBatch]
        boolean hasRecipeSlots = false;
        for (int slot = 0; slot < 9; slot++) {
            String ingredientId = getIngredientItem(ctx, slot);
            if (ingredientId.isEmpty()) continue;
            hasRecipeSlots = true;
            int recipeCount = Math.max(1, getIngredientCount(ctx, slot));
            ItemStack inSlot = crafter.getItem(slot);
            int current = inSlot.isEmpty() ? 0 : inSlot.getCount();
            ItemStack ingredient = resolveItem(ingredientId);
            int maxStack = ingredient.isEmpty() ? 64 : ingredient.getMaxStackSize();
            int space = Math.max(0, maxStack - current);
            long[] agg = perIngredient.computeIfAbsent(ingredientId, k -> new long[2]);
            agg[0] += space;
            agg[1] += recipeCount;
        }
        if (!hasRecipeSlots) return CrafterBufferState.FULL;

        int minBatchCapacity = Integer.MAX_VALUE;
        for (Map.Entry<String, long[]> entry : perIngredient.entrySet()) {
            long totalSpace = entry.getValue()[0];
            long recipeCountPerBatch = entry.getValue()[1];

            // Deduct ingredient orders already placed but not yet physically arrived.
            // This prevents double-dispatching when the controller re-dispatches a partial
            // crafter order before the first batch's ingredients have been inserted.
            if (network != null) {
                ItemStack ingredient = resolveItem(entry.getKey());
                if (!ingredient.isEmpty()) {
                    long pending = network.getOrderedAmountFor(ctx.pos(), ingredient);
                    totalSpace = Math.max(0, totalSpace - pending);
                }
            }

            long batches = totalSpace / recipeCountPerBatch;
            minBatchCapacity = (int) Math.min(minBatchCapacity, batches);
        }

        if (minBatchCapacity == Integer.MAX_VALUE) return CrafterBufferState.FULL;
        // maxCraftsFromOutputSpace = unlimited: the pipe accepts output directly via onExternalInsert
        return new CrafterBufferState(minBatchCapacity, Integer.MAX_VALUE);
    }

    /**
     * Build a {@link CrafterSnapshot} from the current recipe config and autocrafter state.
     * Returns {@code null} when the recipe or autocrafter is not fully configured.
     */
    @Nullable
    private CrafterSnapshot buildCrafterSnapshot(PipeContext ctx) {
        String resultId = getResultItem(ctx);
        if (resultId.isEmpty()) return null;
        ItemStack resultStack = resolveItem(resultId);
        if (resultStack.isEmpty()) return null;
        int resultCount = getResultCount(ctx);
        if (resultCount <= 0) return null;
        Direction autocrafterDir = findAutocrafterDirection(ctx);
        if (autocrafterDir == null) return null;

        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            String ingredientId = getIngredientItem(ctx, slot);
            if (ingredientId.isEmpty()) continue;
            ItemStack ingredientStack = resolveItem(ingredientId);
            if (ingredientStack.isEmpty()) continue;
            ingredients.add(new RecipeIngredient(ItemVariant.of(ingredientStack), getIngredientCount(ctx, slot)));
        }
        if (ingredients.isEmpty()) return null;

        CrafterBufferState buffer = computeBufferState(ctx, autocrafterDir);
        return new CrafterSnapshot(ctx.pos(), ItemVariant.of(resultStack), resultCount, ingredients, buffer);
    }

    private void updateCrafterSupply(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        String resultId = getResultItem(ctx);
        if (resultId.isEmpty() || findAutocrafterDirection(ctx) == null) {
            network.registerSupply(ctx.pos(), new HashMap<>(), CRAFTER_PRIORITY);
            network.unregisterProviderCheck(ctx.pos());
            network.unregisterCrafterSnapshot(ctx.pos());
            return;
        }

        // In blocking mode, suppress supply while the queue is non-empty so no further
        // orders are accepted. In non-blocking mode, continue advertising so additional
        // orders can be queued; the queue prevents dispatch loops since dispatches succeed.
        if (isBlocking(ctx) && isActive(ctx)) {
            network.registerSupply(ctx.pos(), new HashMap<>(), CRAFTER_PRIORITY);
            network.unregisterProviderCheck(ctx.pos());
            network.unregisterCrafterSnapshot(ctx.pos());
            return;
        }

        ItemStack resultStack = resolveItem(resultId);
        if (resultStack.isEmpty()) {
            network.registerSupply(ctx.pos(), new HashMap<>(), CRAFTER_PRIORITY);
            network.unregisterProviderCheck(ctx.pos());
            network.unregisterCrafterSnapshot(ctx.pos());
            return;
        }

        // Advertise 0 — signals "craftable on demand" (no stock, but can fulfill any order)
        Map<ItemVariant, Long> craftable = new HashMap<>();
        craftable.put(ItemVariant.of(resultStack), 0L);
        network.registerSupply(ctx.pos(), craftable, CRAFTER_PRIORITY);

        // Register buffer snapshot so RequestPlanner can cap batch sizes
        CrafterSnapshot snapshot = buildCrafterSnapshot(ctx);
        if (snapshot != null) {
            network.registerCrafterSnapshot(ctx.pos(), snapshot);
        } else {
            network.unregisterCrafterSnapshot(ctx.pos());
        }

        network.registerProviderCheck(ctx.pos(), (amount, checker) -> {
            int resultCount = getResultCount(ctx);
            if (resultCount <= 0) return List.of();
            long batchCount = (amount + resultCount - 1L) / resultCount;
            List<ItemVariant> missing = new ArrayList<>();
            for (int slot = 0; slot < 9; slot++) {
                String id = getIngredientItem(ctx, slot);
                if (id.isEmpty()) continue;
                long needed = (long) getIngredientCount(ctx, slot) * batchCount;
                ItemStack stack = resolveItem(id);
                if (stack.isEmpty()) {
                    // Ingredient ID is not in the registry — recipe is misconfigured.
                    // Use a placeholder so the missing list records a failure rather than
                    // silently skipping the slot and letting a doomed order proceed.
                    missing.addAll(checker.check(new ItemStack(net.minecraft.world.item.Items.AIR), needed));
                    continue;
                }
                missing.addAll(checker.check(stack, needed));
            }
            return missing;
        });
    }

    /** Cancel all ingredient orders recorded in a single queue {@code entry}. */
    private void cancelEntryOrders(PipeContext ctx, CompoundTag entry) {
        ILogisticsNetwork network = ctx.network();
        if (network == null) return;
        CompoundTag ingredientIds = NbtCompat.getCompoundOrEmpty(entry, ENTRY_IDS);
        for (String key : ingredientIds.keySet()) {
            UUID id = parseUUID(NbtCompat.getString(ingredientIds, key, ""));
            if (id != null) {
                try {
                    network.cancelOrder(id);
                } catch (Exception e) {
                    LogisticsPipe.LOGGER.warn(
                            "[Crafter @ {}] Failed to cancel ingredient order {}", ctx.pos(), id, e);
                }
            }
        }
    }

    /**
     * Error reset: cancel all outstanding ingredient orders for every queued entry, clear
     * the queue, and turn off the redstone signal. Called when the autocrafter goes missing.
     */
    private void resetToIdle(PipeContext ctx) {
        ListTag queue = getQueue(ctx);
        for (int i = 0; i < queue.size(); i++) {
            CompoundTag entry = queue.getCompound(i).orElse(null);
            if (entry != null) cancelEntryOrders(ctx, entry);
        }

        ctx.moduleState(this).remove(QUEUE);
        ctx.saveInt(this, TICKS_PULSE, 0);
        ctx.markDirtyAndSync();

        // Clear CRAFTING visual state if stuck on
        BlockState currentState = ctx.world().getBlockState(ctx.pos());
        if (currentState.hasProperty(PipeBlock.CRAFTING)
                && currentState.getValue(PipeBlock.CRAFTING)) {
            ctx.world().setBlock(ctx.pos(), currentState.setValue(PipeBlock.CRAFTING, false), 2);
        }
    }

    // ==================== Routing ====================

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (ctx.world().isClientSide()) return RoutePlan.pass();

        // Ingredient routing: item destined for this crafting pipe → redirect to autocrafter
        if (isActive(ctx)
                && item.getDestination() != null
                && item.getDestination().equals(ctx.pos())) {
            Direction autocrafterDir = findAutocrafterDirection(ctx);
            if (autocrafterDir != null && options.contains(autocrafterDir)) {
                if (isIngredient(ctx, item.getStack())) {
                    item.setDestination(null);
                    return RoutePlan.reroute(autocrafterDir);
                }
            }
        }

        return RoutePlan.pass();
    }

    private boolean isIngredient(PipeContext ctx, ItemStack stack) {
        String stackId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (int slot = 0; slot < 9; slot++) {
            String id = getIngredientItem(ctx, slot);
            if (id.isEmpty()) continue;
            if (stackId.equals(id)) return true;
        }
        return false;
    }

    // ==================== Capability hooks ====================

    @Override
    public boolean canAcceptFromNonPipe(PipeContext ctx, Direction from) {
        Direction autocrafterDir = findAutocrafterDirection(ctx);
        return autocrafterDir != null && autocrafterDir == from;
    }

    /**
     * Intercept ingredient delivery into the adjacent autocrafter.
     * Distributes the ingredient across matching recipe slots using a fewest-complete-batches-first
     * strategy: slots with the least progress relative to their recipe count receive items first.
     */
    @Override
    @Nullable
    public TravelingItem onTransferToStorage(
            PipeContext ctx, TravelingItem item, Direction direction) {
        if (ctx.world().isClientSide()) return item;
        if (!isActive(ctx)) return item;

        Direction autocrafterDir = findAutocrafterDirection(ctx);
        if (autocrafterDir == null || autocrafterDir != direction) return item;
        if (!isIngredient(ctx, item.getStack())) return item;

        int placed = distributeIngredient(ctx, autocrafterDir, item);

        if (placed <= 0) return item;

        // Accounting
        if (item.getDeliveryId() != null) {
            ILogisticsNetwork network = ctx.network();
            if (network != null) {
                network.notifyDelivery(ctx.pos(), ItemVariant.of(item.getStack()), placed);
            }
        }

        item.getStack().shrink(placed);
        return item.getStack().isEmpty() ? null : item;
    }

    /**
     * Distribute an ingredient stack across matching recipe slots.
     * Uses fewest-complete-batches-first ordering: the slot that is furthest behind relative to
     * its recipe count receives items first, ensuring all slots fill at the same batch rate.
     *
     * @return number of items actually placed
     */
    private int distributeIngredient(
            PipeContext ctx, Direction autocrafterDir, TravelingItem item) {
        BlockPos autocrafterPos = ctx.pos().relative(autocrafterDir);
        if (!(ctx.world().getBlockEntity(autocrafterPos) instanceof CrafterBlockEntity crafter))
            return 0;

        String itemId = BuiltInRegistries.ITEM.getKey(item.getStack().getItem()).toString();

        // Collect matching recipe slots that are empty or already hold the same item type
        List<Integer> matchingSlots = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            if (!itemId.equals(getIngredientItem(ctx, slot))) continue;
            ItemStack inSlot = crafter.getItem(slot);
            if (!inSlot.isEmpty() && !itemId.equals(BuiltInRegistries.ITEM.getKey(inSlot.getItem()).toString())) continue;
            matchingSlots.add(slot);
        }
        if (matchingSlots.isEmpty()) return 0;

        int n = matchingSlots.size();
        int[] current = new int[n];
        int[] recipeCounts = new int[n];
        for (int i = 0; i < n; i++) {
            int slot = matchingSlots.get(i);
            ItemStack inSlot = crafter.getItem(slot);
            current[i] = inSlot.isEmpty() ? 0 : inSlot.getCount();
            recipeCounts[i] = getIngredientCount(ctx, slot);
        }

        int remaining = item.getStack().getCount();
        int maxStackSize = item.getStack().getMaxStackSize();

        // Fill round by round: each round brings all slots with the minimum batch count up to the
        // next batch level. This ensures all slots fill at the same rate regardless of recipe counts.
        while (remaining > 0) {
            int minBatches = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                minBatches = Math.min(minBatches, current[i] / recipeCounts[i]);
            }

            boolean placedAny = false;
            for (int i = 0; i < n && remaining > 0; i++) {
                if (current[i] / recipeCounts[i] != minBatches) continue;
                int target = Math.min((minBatches + 1) * recipeCounts[i], maxStackSize);
                int canAdd = target - current[i];
                if (canAdd <= 0) continue;
                int toPlace = Math.min(remaining, canAdd);
                current[i] += toPlace;
                remaining -= toPlace;
                placedAny = true;
            }
            if (!placedAny) break; // All slots maxed out
        }

        // Apply changes to crafter
        int totalPlaced = item.getStack().getCount() - remaining;
        if (totalPlaced > 0) {
            for (int i = 0; i < n; i++) {
                applySlotCount(crafter, matchingSlots.get(i), item.getStack(), current[i]);
            }
        }

        return totalPlaced;
    }

    /**
     * Apply a new count to a crafter slot. If the slot is empty, creates a new stack from
     * {@code ingredient}; otherwise copies the existing stack with the new count.
     * No-ops when the count hasn't changed.
     */
    private static void applySlotCount(CrafterBlockEntity crafter, int slot, ItemStack ingredient, int newCount) {
        ItemStack inSlot = crafter.getItem(slot);
        int originalCount = inSlot.isEmpty() ? 0 : inSlot.getCount();
        if (newCount == originalCount) return;
        if (inSlot.isEmpty()) {
            crafter.setItem(slot, ingredient.copyWithCount(newCount));
        } else {
            crafter.setItem(slot, inSlot.copyWithCount(newCount));
        }
    }

    /**
     * Intercept the autocrafter's result being pushed into this pipe.
     * Distributes items FIFO across queued entries: satisfies the head entry first,
     * then continues into subsequent entries if the batch covers more than one order.
     * Only items remaining after the queue is exhausted flow freely as surplus.
     */
    @Override
    public boolean onExternalInsert(PipeContext ctx, ItemStack stack, Direction fromDirection) {
        ListTag queue = getQueue(ctx);
        if (queue.isEmpty()) return false;

        Direction autocrafterDir = findAutocrafterDirection(ctx);
        if (autocrafterDir == null || autocrafterDir != fromDirection) return false;

        String resultId = getResultItem(ctx);
        if (resultId.isEmpty()) return false;
        ItemStack resultStack = resolveItem(resultId);
        if (resultStack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, resultStack))
            return false;

        // Consume items FIFO across the queue: satisfy as many queued entries as possible
        // before treating any remainder as surplus. This prevents leaking items when the
        // autocrafter outputs a batch that covers more than one queued order.
        int available = stack.getCount();
        boolean headChanged = false;

        while (available > 0 && !queue.isEmpty()) {
            CompoundTag entry = queue.getCompound(0).orElse(null);
            if (entry == null) { queue.remove(0); headChanged = true; continue; }

            BlockPos entryRequester = parseBlockPos(NbtCompat.getString(entry, ENTRY_REQ, ""));
            if (entryRequester == null) {
                cancelEntryOrders(ctx, entry);
                queue.remove(0);
                headChanged = true;
                continue;
            }

            long entryOrdered = NbtCompat.getLong(entry, ENTRY_AMT, 0L);
            UUID entryDeliveryId = parseUUID(NbtCompat.getString(entry, ENTRY_DLV, ""));

            long toSend = Math.min(available, entryOrdered);
            TravelingItem routed = new TravelingItem(
                    stack.copyWithCount((int) toSend),
                    fromDirection.getOpposite(), LogisticsConfig.get().pipe.minSpeed, entryRequester);
            if (entryDeliveryId != null) routed.setDeliveryId(entryDeliveryId);
            ctx.blockEntity().forceAddItem(routed, fromDirection);

            available -= (int) toSend;
            if (toSend >= entryOrdered) {
                cancelEntryOrders(ctx, entry);
                queue.remove(0);
                headChanged = true;
            } else {
                entry.putLong(ENTRY_AMT, entryOrdered - toSend);
                break;
            }
        }

        saveQueue(ctx, queue);

        if (queue.isEmpty()) {
            ctx.saveInt(this, TICKS_PULSE, 0);
            BlockState currentState = ctx.world().getBlockState(ctx.pos());
            if (currentState.hasProperty(PipeBlock.CRAFTING)
                    && currentState.getValue(PipeBlock.CRAFTING)) {
                ctx.world().setBlock(ctx.pos(), currentState.setValue(PipeBlock.CRAFTING, false), 2);
            }
            updateCrafterSupply(ctx);
        } else if (headChanged) {
            // Head was dequeued; reset pulse so the new head starts its craft cycle
            ctx.saveInt(this, TICKS_PULSE, 0);
        }

        // Surplus: items not consumed by any queued order flow freely into the network
        if (available > 0) {
            TravelingItem surplusItem = new TravelingItem(
                    stack.copyWithCount(available),
                    fromDirection.getOpposite(),
                    LogisticsConfig.get().pipe.minSpeed);
            ctx.blockEntity().forceAddItem(surplusItem, fromDirection);
        }

        return true;
    }

    // ==================== Visual ====================

    @Override
    @Nullable
    public ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        Direction autocrafterDir = findAutocrafterDirection(ctx);
        if (autocrafterDir != null && autocrafterDir == direction) {
            return LogisticsPipe.model("crafting_logistics_pipe_feature_extended");
        }
        return null;
    }

    // ==================== Lifecycle ====================

    @Override
    public void onDetach(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;

        // Cancel all outstanding ingredient orders
        ListTag queue = getQueue(ctx);
        for (int i = 0; i < queue.size(); i++) {
            CompoundTag entry = queue.getCompound(i).orElse(null);
            if (entry != null) cancelEntryOrders(ctx, entry);
        }

        // Clear queue and tick counters so stale state doesn't survive a re-attach
        ctx.moduleState(this).remove(QUEUE);
        ctx.saveInt(this, TICKS_PULSE, 0);
        ctx.saveInt(this, TICKS_SCAN, 0);

        // Unconditional network teardown — do NOT delegate to updateCrafterSupply because
        // that method may re-register supply when a recipe and autocrafter are still present
        // (e.g. chassis slot removal while the autocrafter is still adjacent).
        ILogisticsNetwork network = ctx.network();
        if (network != null) {
            network.registerSupply(ctx.pos(), new HashMap<>(), CRAFTER_PRIORITY);
            network.unregisterProviderCheck(ctx.pos());
            network.unregisterCrafterSnapshot(ctx.pos());
        }

        // Clear visual CRAFTING indicator
        BlockState currentState = ctx.world().getBlockState(ctx.pos());
        if (currentState.hasProperty(PipeBlock.CRAFTING) && currentState.getValue(PipeBlock.CRAFTING)) {
            ctx.world().setBlock(ctx.pos(), currentState.setValue(PipeBlock.CRAFTING, false), 2);
        }

        ctx.markDirtyAndSync();
    }

    // ==================== GUI ====================

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, p) ->
                        new CraftingScreenHandler(syncId, playerInventory, ((PipeBlockEntity) ctx.blockEntity())),
                Component.translatable("screen.logistics.crafting")));

        return InteractionResult.SUCCESS;
    }

    // ==================== Helpers ====================

    private static ItemStack resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        try {
            ResourceId rid = ResourceId.tryParse(itemId);
            if (rid == null) return ItemStack.EMPTY;
            var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder.isEmpty()) return ItemStack.EMPTY;
            var item = holder.get().value();
            if (item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
