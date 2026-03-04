package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.network.ItemRequest;
import com.logistics.core.lib.network.LogisticsOrder;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.ui.CraftingScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Crafting pipe module - presents craftable items as available to the network,
 * sources ingredients from the network, routes them into an adjacent Minecraft Autocrafter,
 * triggers crafting via a redstone pulse, then collects and routes the result to the requester.
 *
 * <p>State machine: IDLE → SOURCING → TRIGGERING → COLLECTING → IDLE
 *
 * <p>Placement rule: The crafting pipe must be placed adjacent to an autocrafter.
 * The pipe automatically detects the autocrafter in any adjacent direction.
 */
public class CraftingModule implements Module {
    // NBT keys
    private static final String RECIPE_ITEMS = "recipe_items";
    private static final String RECIPE_COUNTS = "recipe_counts";
    private static final String RESULT_ITEM = "result_item";
    private static final String RESULT_COUNT = "result_count";
    private static final String CRAFT_STATE = "craft_state";
    private static final String BLOCKING = "blocking";
    private static final String TICKS_SCAN = "ticks_scan";
    private static final String TICKS_ORDER = "ticks_order";
    private static final String TICKS_TRIGGER = "ticks_trigger";
    private static final String TICKS_TIMEOUT = "ticks_timeout";
    private static final String PENDING_REQUESTER = "pending_requester";
    private static final String PENDING_ORDER_TIME = "pending_order_time";

    // Timing constants
    private static final int SCAN_INTERVAL = 20;        // Update crafter cache every 20 ticks
    private static final int ORDER_CHECK_INTERVAL = 20;  // Check for orders every 20 ticks
    private static final int PULSE_DURATION = 4;        // Redstone pulse length (ticks)
    // SOURCING_TIMEOUT resets each time an ingredient is placed — this is the "no progress" limit.
    // Must be >= IN_TRANSIT_CLEANUP_TTL in RequestMatcher (1200 ticks / 60s) so we never give up
    // and re-request while the original ingredient items are still in-transit.
    private static final int SOURCING_TIMEOUT = 1200;   // 60 seconds with no progress
    // COLLECTING_TIMEOUT starts after the redstone pulse is sent (end of TRIGGERING).
    private static final int COLLECTING_TIMEOUT = 200;  // 10 seconds after crafting signal

    public enum CraftState {
        IDLE,
        SOURCING,
        TRIGGERING,
        COLLECTING
    }

    // ==================== NBT State Accessors ====================

    public CraftState getCraftState(PipeContext ctx) {
        int ordinal = ctx.getInt(this, CRAFT_STATE, 0);
        CraftState[] values = CraftState.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CraftState.IDLE;
    }

    private void setCraftState(PipeContext ctx, CraftState state) {
        ctx.saveInt(this, CRAFT_STATE, state.ordinal());
        ctx.markDirtyAndSync();
    }

    public boolean isBlocking(PipeContext ctx) {
        return ctx.getInt(this, BLOCKING, 0) == 1;
    }

    public void setBlocking(PipeContext ctx, boolean blocking) {
        ctx.saveInt(this, BLOCKING, blocking ? 1 : 0);
        ctx.markDirtyAndSync();
    }

    /** Get recipe ingredient item ID for slot 0-8. Returns "" if empty. */
    public String getIngredientItem(PipeContext ctx, int slot) {
        CompoundTag items = ctx.getCompoundTag(this, RECIPE_ITEMS);
        return NbtCompat.getString(items, String.valueOf(slot), "");
    }

    /** Get recipe ingredient count for slot 0-8. */
    public int getIngredientCount(PipeContext ctx, int slot) {
        CompoundTag counts = ctx.getCompoundTag(this, RECIPE_COUNTS);
        return NbtCompat.getInt(counts, String.valueOf(slot), 1);
    }

    /** Set recipe ingredient for slot 0-8. Empty itemId clears slot. */
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

    @Nullable
    private BlockPos getPendingRequester(PipeContext ctx) {
        String s = ctx.getString(this, PENDING_REQUESTER, "");
        if (s.isEmpty()) return null;
        try {
            String[] parts = s.split(",");
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }

    private void setPendingRequester(PipeContext ctx, @Nullable BlockPos pos) {
        if (pos == null) {
            ctx.moduleState(getStateKey()).remove(PENDING_REQUESTER);
        } else {
            ctx.saveString(this, PENDING_REQUESTER, pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }
    }

    private long getPendingOrderTime(PipeContext ctx) {
        return ctx.getLong(this, PENDING_ORDER_TIME, -1L);
    }

    private void setPendingOrderTime(PipeContext ctx, long time) {
        ctx.saveLong(this, PENDING_ORDER_TIME, time);
    }

    // ==================== Autocrafter Detection ====================

    /**
     * Find an adjacent autocrafter in any direction.
     * @return Direction toward the autocrafter, or null if none found
     */
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

        // Periodic: update crafter cache
        int scanTicks = ctx.getInt(this, TICKS_SCAN, 0) + 1;
        ctx.saveInt(this, TICKS_SCAN, scanTicks);
        if (scanTicks >= SCAN_INTERVAL) {
            ctx.saveInt(this, TICKS_SCAN, 0);
            updateCrafterCache(ctx);
        }

        // Dispatch to state machine
        CraftState state = getCraftState(ctx);
        switch (state) {
            case IDLE -> {
                // Periodically check for orders
                int orderTicks = ctx.getInt(this, TICKS_ORDER, 0) + 1;
                ctx.saveInt(this, TICKS_ORDER, orderTicks);
                if (orderTicks >= ORDER_CHECK_INTERVAL) {
                    ctx.saveInt(this, TICKS_ORDER, 0);
                    processOrders(ctx);
                }
            }
            case SOURCING -> tickSourcing(ctx);
            case TRIGGERING -> tickTriggering(ctx);
            case COLLECTING -> tickCollecting(ctx);
        }
    }

    /** Update the crafter cache so the network knows we can craft the result item. */
    private void updateCrafterCache(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        String resultId = getResultItem(ctx);
        if (resultId.isEmpty()) {
            network.updateCrafterCache(ctx.pos(), new HashMap<>(), ctx.world().getGameTime());
            return;
        }

        // Only advertise if there's an adjacent autocrafter
        if (findAutocrafterDirection(ctx) == null) {
            network.updateCrafterCache(ctx.pos(), new HashMap<>(), ctx.world().getGameTime());
            return;
        }

        // Advertise Long.MAX_VALUE - on-demand crafting supply
        ItemStack resultStack = resolveItem(resultId);
        if (resultStack.isEmpty()) {
            network.updateCrafterCache(ctx.pos(), new HashMap<>(), ctx.world().getGameTime());
            return;
        }

        Map<ItemStack, Long> craftable = new HashMap<>();
        craftable.put(resultStack, Long.MAX_VALUE);
        network.updateCrafterCache(ctx.pos(), craftable, ctx.world().getGameTime());
    }

    /** Check network for pending orders and start sourcing ingredients. */
    private void processOrders(PipeContext ctx) {
        // Only start a new craft in IDLE (or if non-blocking allows it)
        if (getCraftState(ctx) != CraftState.IDLE) return;

        String resultId = getResultItem(ctx);
        if (resultId.isEmpty()) return;

        if (findAutocrafterDirection(ctx) == null) return;

        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        List<LogisticsOrder> orders = network.getOrdersFor(ctx.pos());
        if (orders.isEmpty()) return;

        LogisticsOrder order = orders.get(0);
        // Save requester info
        setPendingRequester(ctx, order.requester());
        setPendingOrderTime(ctx, order.createdAt());
        ctx.markDirty();

        // Aggregate ingredient amounts by item type across all slots before requesting.
        // Two plank slots (slot 0 + slot 4) must become ONE request for 2 planks total,
        // not two separate requests — otherwise each triggers its own independent craft
        // cycle and we produce far more items than needed.
        Map<String, Integer> neededByItem = new LinkedHashMap<>();
        for (int slot = 0; slot < 9; slot++) {
            String ingredientId = getIngredientItem(ctx, slot);
            if (ingredientId.isEmpty()) continue;
            neededByItem.merge(ingredientId, getIngredientCount(ctx, slot), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : neededByItem.entrySet()) {
            ItemStack ingredientStack = resolveItem(entry.getKey());
            if (ingredientStack.isEmpty()) continue;

            long alreadyOrdered = network.getOrderedAmountFor(ctx.pos(), ingredientStack);
            long needed = entry.getValue() - alreadyOrdered;
            if (needed <= 0) continue;

            network.addRequest(new ItemRequest(ctx.pos(), ingredientStack, needed, ctx.world().getGameTime()));
        }

        setCraftState(ctx, CraftState.SOURCING);
        ctx.saveInt(this, TICKS_TIMEOUT, 0);
    }

    /** SOURCING: check if all ingredients have arrived in the autocrafter. */
    private void tickSourcing(PipeContext ctx) {
        int timeout = ctx.getInt(this, TICKS_TIMEOUT, 0) + 1;
        ctx.saveInt(this, TICKS_TIMEOUT, timeout);

        if (timeout >= SOURCING_TIMEOUT) {
            // Give up and return to IDLE
            resetToIdle(ctx);
            return;
        }

        Direction autocrafterDir = findAutocrafterDirection(ctx);
        if (autocrafterDir == null) {
            resetToIdle(ctx);
            return;
        }

        // Check if autocrafter has all required ingredients
        if (autocrafterHasIngredients(ctx, autocrafterDir)) {
            setCraftState(ctx, CraftState.TRIGGERING);
            ctx.saveInt(this, TICKS_TRIGGER, 0);
            ctx.saveInt(this, TICKS_TIMEOUT, 0);
        }
    }

    /** TRIGGERING: emit redstone pulse to activate autocrafter. */
    private void tickTriggering(PipeContext ctx) {
        int triggerTick = ctx.getInt(this, TICKS_TRIGGER, 0) + 1;
        ctx.saveInt(this, TICKS_TRIGGER, triggerTick);

        if (triggerTick == 1) {
            // Rising edge: set CRAFTING=true, emit redstone
            BlockState newState = ctx.state().setValue(PipeBlock.CRAFTING, true);
            ctx.world().setBlock(ctx.pos(), newState, 3);
            ctx.world().updateNeighborsAt(ctx.pos(), newState.getBlock());
        } else if (triggerTick > PULSE_DURATION) {
            // Falling edge: clear CRAFTING=false
            BlockState newState = ctx.world().getBlockState(ctx.pos()).setValue(PipeBlock.CRAFTING, false);
            ctx.world().setBlock(ctx.pos(), newState, 3);
            ctx.world().updateNeighborsAt(ctx.pos(), newState.getBlock());
            setCraftState(ctx, CraftState.COLLECTING);
            ctx.saveInt(this, TICKS_TIMEOUT, 0);
        }
    }

    /** COLLECTING: wait for the result to arrive via route(). Timeout safety. */
    private void tickCollecting(PipeContext ctx) {
        int timeout = ctx.getInt(this, TICKS_TIMEOUT, 0) + 1;
        ctx.saveInt(this, TICKS_TIMEOUT, timeout);

        if (timeout >= COLLECTING_TIMEOUT) {
            resetToIdle(ctx);
        }
    }

    private void resetToIdle(PipeContext ctx) {
        setCraftState(ctx, CraftState.IDLE);
        setPendingRequester(ctx, null);
        setPendingOrderTime(ctx, -1L);
        ctx.saveInt(this, TICKS_TIMEOUT, 0);

        // Ensure CRAFTING block state is cleared
        BlockState currentState = ctx.world().getBlockState(ctx.pos());
        if (currentState.hasProperty(PipeBlock.CRAFTING) && currentState.getValue(PipeBlock.CRAFTING)) {
            BlockState newState = currentState.setValue(PipeBlock.CRAFTING, false);
            ctx.world().setBlock(ctx.pos(), newState, 3);
            ctx.world().updateNeighborsAt(ctx.pos(), newState.getBlock());
        }
    }

    /** Check if the autocrafter already holds all required ingredients, per slot. */
    private boolean autocrafterHasIngredients(PipeContext ctx, Direction autocrafterDir) {
        BlockPos autocrafterPos = ctx.pos().relative(autocrafterDir);
        if (!(ctx.world().getBlockEntity(autocrafterPos) instanceof CrafterBlockEntity crafter)) return false;

        for (int slot = 0; slot < 9; slot++) {
            String ingredientId = getIngredientItem(ctx, slot);
            if (ingredientId.isEmpty()) continue;
            int needed = getIngredientCount(ctx, slot);

            ItemStack inSlot = crafter.getItem(slot);
            if (inSlot.isEmpty()) return false;
            if (!ingredientId.equals(BuiltInRegistries.ITEM.getKey(inSlot.getItem()).toString())) return false;
            if (inSlot.getCount() < needed) return false;
        }
        return true;
    }

    // ==================== Routing ====================

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (ctx.world().isClientSide()) return RoutePlan.pass();

        CraftState state = getCraftState(ctx);

        // Result items arrive with their destination already set by onExternalInsert.
        // Nothing to do here — NetworkRouterModule routes them to the requester.

        // Ingredient routing: item is an ingredient destined for this pipe — route it toward the autocrafter
        if (state == CraftState.SOURCING && item.getDestination() != null && item.getDestination().equals(ctx.pos())) {
            Direction autocrafterDir = findAutocrafterDirection(ctx);
            if (autocrafterDir != null && options.contains(autocrafterDir)) {
                if (isIngredient(ctx, item.getStack())) {
                    item.setDestination(null); // Delivered
                    return RoutePlan.reroute(autocrafterDir);
                }
            }
        }

        return RoutePlan.pass();
    }

    /** Check if this item matches any recipe ingredient. */
    private boolean isIngredient(PipeContext ctx, ItemStack stack) {
        for (int slot = 0; slot < 9; slot++) {
            String id = getIngredientItem(ctx, slot);
            if (id.isEmpty()) continue;
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // ==================== Capability hooks ====================

    @Override
    public boolean canAcceptFromNonPipe(PipeContext ctx, Direction from) {
        // Allow the autocrafter to push its output into this pipe
        Direction autocrafterDir = findAutocrafterDirection(ctx);
        return autocrafterDir != null && autocrafterDir == from;
    }

    /**
     * Intercept ingredient delivery into the adjacent autocrafter.
     * Called by PipeRuntime at SERVER_EXIT_THRESHOLD instead of the default generic
     * storage.insert(), so we can place each ingredient into its exact recipe slot.
     * Returns null if handled (item consumed), or item if not handled (fall through to default).
     */
    @Override
    @Nullable
    public TravelingItem onTransferToStorage(PipeContext ctx, TravelingItem item, Direction direction) {
        if (ctx.world().isClientSide()) return item;
        if (getCraftState(ctx) != CraftState.SOURCING) return item;

        Direction autocrafterDir = findAutocrafterDirection(ctx);
        if (autocrafterDir == null || autocrafterDir != direction) return item;
        if (!isIngredient(ctx, item.getStack())) return item;

        return insertIngredientIntoSlot(ctx, direction, item) ? null : item;
    }

    /**
     * Insert an ingredient directly into the matching recipe slot in the adjacent autocrafter.
     * Uses Container.setItem() directly to bypass the autocrafter's canPlaceItem() equalization
     * logic, which rejects insertion into a slot when a later slot with the same ingredient
     * is still empty. We own the recipe layout, so we place items exactly where we want them.
     */
    /**
     * Distribute an ingredient stack across all matching empty recipe slots in the autocrafter.
     * A consolidated request (e.g., 2 planks for slot 0 + slot 4) arrives as a single
     * TravelingItem with count=2 and must be split across both slots.
     */
    private boolean insertIngredientIntoSlot(PipeContext ctx, Direction autocrafterDir, TravelingItem item) {
        BlockPos autocrafterPos = ctx.pos().relative(autocrafterDir);
        if (!(ctx.world().getBlockEntity(autocrafterPos) instanceof CrafterBlockEntity crafter)) return false;

        String itemId = BuiltInRegistries.ITEM.getKey(item.getStack().getItem()).toString();
        int remaining = item.getStack().getCount();

        for (int slot = 0; slot < 9 && remaining > 0; slot++) {
            if (!itemId.equals(getIngredientItem(ctx, slot))) continue;
            if (!crafter.getItem(slot).isEmpty()) continue;

            int toPlace = Math.min(remaining, getIngredientCount(ctx, slot));
            crafter.setItem(slot, item.getStack().copyWithCount(toPlace));
            remaining -= toPlace;
        }

        if (remaining < item.getStack().getCount()) {
            // At least some was placed — consider the item consumed
            if (item.getInTransitOrder() != null) {
                ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
                if (network != null) network.confirmDelivery(item.getInTransitOrder());
            }
            // Reset the sourcing timeout — the timeout is "time with no progress"
            ctx.saveInt(this, TICKS_TIMEOUT, 0);
            return true;
        }
        return false;
    }

    /**
     * Intercept the autocrafter's result being pushed into this pipe.
     * Splits the stack at insertion time: the ordered amount gets a destination=requester
     * and an in-transit order, while any surplus enters as a fresh unrouted item.
     * Both TravelingItems receive proper routing decisions when they cross ROUTE_POINT.
     */
    @Override
    public boolean onExternalInsert(PipeContext ctx, ItemStack stack, Direction fromDirection) {
        // Guard by pending requester rather than exact state: the autocrafter can eject while
        // we are still in TRIGGERING (before onTick has advanced us to COLLECTING).
        BlockPos requester = getPendingRequester(ctx);
        if (requester == null) return false;

        Direction autocrafterDir = findAutocrafterDirection(ctx);
        if (autocrafterDir == null || autocrafterDir != fromDirection) return false;

        String resultId = getResultItem(ctx);
        if (resultId.isEmpty()) return false;
        ItemStack resultStack = resolveItem(resultId);
        if (resultStack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, resultStack)) return false;

        // Determine how many to send to the requester vs. return to the network
        long ordered = stack.getCount();
        LogisticsOrder inTransit = null;
        ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
        if (network != null) {
            long orderTime = getPendingOrderTime(ctx);
            for (LogisticsOrder o : network.getOrdersFor(ctx.pos())) {
                if (o.requester().equals(requester) && o.createdAt() == orderTime) {
                    ordered = Math.min(stack.getCount(), o.amount());
                    inTransit = network.markShipped(o, ordered, ctx.world().getGameTime());
                    break;
                }
            }
        }

        // Ordered portion: tagged with destination and in-transit order
        ItemStack orderedStack = stack.copy();
        orderedStack.setCount((int) ordered);
        TravelingItem orderedItem = new TravelingItem(
                orderedStack, fromDirection.getOpposite(), LogisticsPipe.CONFIG.ITEM_MIN_SPEED, requester);
        if (inTransit != null) orderedItem.setInTransitOrder(inTransit);
        ctx.blockEntity().forceAddItem(orderedItem, fromDirection);

        // Surplus (if recipe yields more than was ordered): unrouted, flows freely into the network
        int surplus = stack.getCount() - (int) ordered;
        if (surplus > 0) {
            ItemStack surplusStack = stack.copy();
            surplusStack.setCount(surplus);
            TravelingItem surplusItem = new TravelingItem(
                    surplusStack, fromDirection.getOpposite(), LogisticsPipe.CONFIG.ITEM_MIN_SPEED);
            ctx.blockEntity().forceAddItem(surplusItem, fromDirection);
        }

        resetToIdle(ctx);
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
                (syncId, playerInventory, p) -> new CraftingScreenHandler(
                        syncId,
                        playerInventory,
                        ctx.blockEntity()
                ),
                Component.translatable("screen.logistics.crafting")
        ));

        return InteractionResult.SUCCESS;
    }

    // ==================== Helpers ====================

    /** Resolve an item ID to an ItemStack. Returns EMPTY if not found. */
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
