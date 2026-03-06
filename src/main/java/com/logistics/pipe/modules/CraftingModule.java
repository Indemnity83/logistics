package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.ui.CraftingScreenHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class CraftingModule implements Module {
    // NBT keys — recipe config
    private static final String RECIPE_ITEMS = "recipe_items";
    private static final String RECIPE_COUNTS = "recipe_counts";
    private static final String RESULT_ITEM = "result_item";
    private static final String RESULT_COUNT = "result_count";
    // NBT keys — behavior
    private static final String BLOCKING = "blocking";
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
    private static final int PULSE_DURATION = 4;    // Redstone pulse length (ticks)
    private static final int PULSE_COOLDOWN = 20;   // Ticks after pulse ends before next check

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
            ctx.moduleState(getStateKey()).remove(RESULT_ITEM);
            ctx.moduleState(getStateKey()).remove(RESULT_COUNT);
        } else {
            ctx.saveString(this, RESULT_ITEM, itemId);
            ctx.saveInt(this, RESULT_COUNT, Math.max(1, count));
        }
        ctx.markDirtyAndSync();
    }

    // ==================== Queue Accessors ====================

    private ListTag getQueue(PipeContext ctx) {
        return NbtCompat.getListOrEmpty(ctx.moduleState(getStateKey()), QUEUE);
    }

    private void saveQueue(PipeContext ctx, ListTag queue) {
        ctx.moduleState(getStateKey()).put(QUEUE, queue);
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

    // ==================== Module Interface ====================

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;

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

        // Pulse cycle: at tick 1 check ingredients and fire; at PULSE_DURATION+1 drive low;
        // reset the counter after PULSE_DURATION + PULSE_COOLDOWN ticks for the next check.
        int ticks = ctx.getInt(this, TICKS_PULSE, 0) + 1;
        ctx.saveInt(this, TICKS_PULSE, ticks);

        if (ticks == 1) {
            if (autocrafterHasIngredients(ctx, autocrafterDir)) {
                BlockState newState =
                        ctx.world().getBlockState(ctx.pos()).setValue(PipeBlock.CRAFTING, true);
                ctx.world().setBlock(ctx.pos(), newState, 3);
                ctx.world().updateNeighborsAt(ctx.pos(), newState.getBlock());
            }
        } else if (ticks == PULSE_DURATION + 1) {
            BlockState newState =
                    ctx.world().getBlockState(ctx.pos()).setValue(PipeBlock.CRAFTING, false);
            ctx.world().setBlock(ctx.pos(), newState, 3);
            ctx.world().updateNeighborsAt(ctx.pos(), newState.getBlock());
        } else if (ticks >= PULSE_DURATION + PULSE_COOLDOWN) {
            ctx.saveInt(this, TICKS_PULSE, 0);
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
        if (ctx.world().isClientSide()) return 0;

        String resultId = getResultItem(ctx);
        if (resultId.isEmpty()) return 0;
        if (findAutocrafterDirection(ctx) == null) return 0;

        ItemStack resultStack = resolveItem(resultId);
        if (resultStack.isEmpty() || !item.matches(resultStack)) return 0;

        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return 0;

        int resultCount = getResultCount(ctx);
        long batchCount = (amount + resultCount - 1L) / resultCount;

        // Aggregate ingredient amounts for all batches
        Map<String, Long> neededByItem = new LinkedHashMap<>();
        for (int slot = 0; slot < 9; slot++) {
            String ingredientId = getIngredientItem(ctx, slot);
            if (ingredientId.isEmpty()) continue;
            neededByItem.merge(ingredientId, (long) getIngredientCount(ctx, slot) * batchCount, Long::sum);
        }

        // No ingredients configured — reject the dispatch
        if (neededByItem.isEmpty()) return 0;

        // Pre-validate all ingredients before placing any orders — fail fast so we never
        // queue a craft with a missing ingredient order that will never arrive.
        Map<String, ItemStack> resolvedIngredients = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : neededByItem.entrySet()) {
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

        // Place ingredient orders immediately — each queued order orders its own ingredients
        // independently so materials arrive in parallel rather than waiting for prior orders.
        CompoundTag ingredientOrderIds = new CompoundTag();
        for (Map.Entry<String, Long> entry : neededByItem.entrySet()) {
            UUID ingredientOrderId = network.placeOrder(
                    ItemVariant.of(resolvedIngredients.get(entry.getKey())), entry.getValue(), ctx.pos());
            ingredientOrderIds.putString(entry.getKey(), ingredientOrderId.toString());
        }

        // Append entry to queue
        CompoundTag queueEntry = new CompoundTag();
        queueEntry.putString(ENTRY_REQ, requester.getX() + "," + requester.getY() + "," + requester.getZ());
        queueEntry.putString(ENTRY_DLV, deliveryId.toString());
        queueEntry.putLong(ENTRY_AMT, amount);
        queueEntry.put(ENTRY_IDS, ingredientOrderIds);

        ListTag queue = getQueue(ctx);
        queue.add(queueEntry);
        saveQueue(ctx, queue);
        ctx.saveInt(this, TICKS_PULSE, 0);

        // Blocking mode: immediately suppress supply so no further orders arrive
        if (isBlocking(ctx)) {
            network.registerSupply(ctx.pos(), new HashMap<>(), CRAFTER_PRIORITY);
        }

        return amount; // Promise: delivery happens after crafting completes
    }

    private void updateCrafterSupply(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        String resultId = getResultItem(ctx);
        if (resultId.isEmpty() || findAutocrafterDirection(ctx) == null) {
            network.registerSupply(ctx.pos(), new HashMap<>(), CRAFTER_PRIORITY);
            return;
        }

        // In blocking mode, suppress supply while the queue is non-empty so no further
        // orders are accepted. In non-blocking mode, continue advertising so additional
        // orders can be queued; the queue prevents dispatch loops since dispatches succeed.
        if (isBlocking(ctx) && isActive(ctx)) {
            network.registerSupply(ctx.pos(), new HashMap<>(), CRAFTER_PRIORITY);
            return;
        }

        ItemStack resultStack = resolveItem(resultId);
        if (resultStack.isEmpty()) {
            network.registerSupply(ctx.pos(), new HashMap<>(), CRAFTER_PRIORITY);
            return;
        }

        // Advertise Long.MAX_VALUE — on-demand crafting supply
        Map<ItemVariant, Long> craftable = new HashMap<>();
        craftable.put(ItemVariant.of(resultStack), Long.MAX_VALUE);
        network.registerSupply(ctx.pos(), craftable, CRAFTER_PRIORITY);
    }

    /**
     * Error reset: cancel all outstanding ingredient orders for every queued entry, clear
     * the queue, and turn off the redstone signal. Called when the autocrafter goes missing.
     */
    private void resetToIdle(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
        if (network != null) {
            ListTag queue = getQueue(ctx);
            for (int i = 0; i < queue.size(); i++) {
                CompoundTag entry = queue.getCompound(i).orElse(null);
                if (entry == null) continue;
                CompoundTag ingredientIds = NbtCompat.getCompoundOrEmpty(entry, ENTRY_IDS);
                for (String key : ingredientIds.keySet()) {
                    UUID id = parseUUID(NbtCompat.getString(ingredientIds, key, ""));
                    if (id != null) {
                        try {
                            network.cancelOrder(id);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        ctx.moduleState(getStateKey()).remove(QUEUE);
        ctx.saveInt(this, TICKS_PULSE, 0);
        ctx.markDirtyAndSync();

        // Clear CRAFTING block state if stuck on
        BlockState currentState = ctx.world().getBlockState(ctx.pos());
        if (currentState.hasProperty(PipeBlock.CRAFTING)
                && currentState.getValue(PipeBlock.CRAFTING)) {
            BlockState newState = currentState.setValue(PipeBlock.CRAFTING, false);
            ctx.world().setBlock(ctx.pos(), newState, 3);
            ctx.world().updateNeighborsAt(ctx.pos(), newState.getBlock());
        }
    }

    /**
     * Check if the autocrafter holds at least one complete set of recipe ingredients.
     */
    private boolean autocrafterHasIngredients(PipeContext ctx, Direction autocrafterDir) {
        BlockPos autocrafterPos = ctx.pos().relative(autocrafterDir);
        if (!(ctx.world().getBlockEntity(autocrafterPos) instanceof CrafterBlockEntity crafter))
            return false;

        for (int slot = 0; slot < 9; slot++) {
            String ingredientId = getIngredientItem(ctx, slot);
            if (ingredientId.isEmpty()) continue;
            int needed = getIngredientCount(ctx, slot);
            ItemStack inSlot = crafter.getItem(slot);
            if (inSlot.isEmpty()) return false;
            if (!ingredientId.equals(
                    BuiltInRegistries.ITEM.getKey(inSlot.getItem()).toString())) return false;
            if (inSlot.getCount() < needed) return false;
        }
        return true;
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
            ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
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
            if (entryRequester == null) { queue.remove(0); headChanged = true; continue; }

            long entryOrdered = NbtCompat.getLong(entry, ENTRY_AMT, 0L);
            UUID entryDeliveryId = parseUUID(NbtCompat.getString(entry, ENTRY_DLV, ""));

            long toSend = Math.min(available, entryOrdered);
            TravelingItem routed = new TravelingItem(
                    stack.copyWithCount((int) toSend),
                    fromDirection.getOpposite(), LogisticsPipe.CONFIG.ITEM_MIN_SPEED, entryRequester);
            if (entryDeliveryId != null) routed.setDeliveryId(entryDeliveryId);
            ctx.blockEntity().forceAddItem(routed, fromDirection);

            available -= (int) toSend;
            if (toSend >= entryOrdered) {
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
                BlockState newState = currentState.setValue(PipeBlock.CRAFTING, false);
                ctx.world().setBlock(ctx.pos(), newState, 3);
                ctx.world().updateNeighborsAt(ctx.pos(), newState.getBlock());
            }
        } else if (headChanged) {
            // Head was dequeued; reset pulse so the new head starts its craft cycle
            ctx.saveInt(this, TICKS_PULSE, 0);
        }

        // Surplus: items not consumed by any queued order flow freely into the network
        if (available > 0) {
            TravelingItem surplusItem = new TravelingItem(
                    stack.copyWithCount(available),
                    fromDirection.getOpposite(),
                    LogisticsPipe.CONFIG.ITEM_MIN_SPEED);
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

    // ==================== GUI ====================

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, p) ->
                        new CraftingScreenHandler(syncId, playerInventory, ctx.blockEntity()),
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
