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
    private static final int SCAN_INTERVAL = 20;      // Update crafter cache every 20 ticks
    private static final int ORDER_CHECK_INTERVAL = 20; // Check for orders every 20 ticks
    private static final int PULSE_DURATION = 4;      // Redstone pulse length (ticks)
    private static final int SOURCING_TIMEOUT = 600;  // 30 seconds
    private static final int COLLECTING_TIMEOUT = 200; // 10 seconds

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

        // Request each ingredient from the network (requester = this pipe's pos)
        for (int slot = 0; slot < 9; slot++) {
            String ingredientId = getIngredientItem(ctx, slot);
            if (ingredientId.isEmpty()) continue;
            int count = getIngredientCount(ctx, slot);

            ItemStack ingredientStack = resolveItem(ingredientId);
            if (ingredientStack.isEmpty()) continue;

            ItemRequest request = new ItemRequest(ctx.pos(), ingredientStack, count, ctx.world().getGameTime());
            network.addRequest(request);
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
        String resultId = getResultItem(ctx);

        // Result routing: item arrived during COLLECTING — route it to the requester
        if (state == CraftState.COLLECTING && !resultId.isEmpty()) {
            ItemStack resultStack = resolveItem(resultId);
            if (!resultStack.isEmpty() && ItemStack.isSameItemSameComponents(item.getStack(), resultStack)) {
                BlockPos requester = getPendingRequester(ctx);
                if (requester != null) {
                    // Find and mark the in-transit order
                    ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
                    if (network != null) {
                        long orderTime = getPendingOrderTime(ctx);
                        List<LogisticsOrder> orders = network.getOrdersFor(ctx.pos());
                        // The order may already have been removed by markShipped from requester - check inTransit
                        // Just ship directly: find any order for this requester
                        LogisticsOrder matchingOrder = null;
                        for (LogisticsOrder o : orders) {
                            if (o.requester().equals(requester) && o.createdAt() == orderTime) {
                                matchingOrder = o;
                                break;
                            }
                        }
                        if (matchingOrder != null) {
                            long extracted = Math.min(item.getStack().getCount(), matchingOrder.amount());
                            LogisticsOrder inTransit = network.markShipped(matchingOrder, extracted, ctx.world().getGameTime());
                            item.setInTransitOrder(inTransit);
                        }
                    }

                    item.setDestination(requester);
                    resetToIdle(ctx);
                    return RoutePlan.pass(); // Let NetworkRouterModule handle actual routing
                }
            }
        }

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
    private boolean insertIngredientIntoSlot(PipeContext ctx, Direction autocrafterDir, TravelingItem item) {
        BlockPos autocrafterPos = ctx.pos().relative(autocrafterDir);
        if (!(ctx.world().getBlockEntity(autocrafterPos) instanceof CrafterBlockEntity crafter)) return false;

        String itemId = BuiltInRegistries.ITEM.getKey(item.getStack().getItem()).toString();

        for (int slot = 0; slot < 9; slot++) {
            if (!itemId.equals(getIngredientItem(ctx, slot))) continue;
            if (!crafter.getItem(slot).isEmpty()) continue; // slot already occupied

            crafter.setItem(slot, item.getStack().copy());
            if (item.getInTransitOrder() != null) {
                ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
                if (network != null) network.confirmDelivery(item.getInTransitOrder());
            }
            return true;
        }
        return false;
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
