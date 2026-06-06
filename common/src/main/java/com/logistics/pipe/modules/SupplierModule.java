package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.pipe.network.NetDbg;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.pipe.network.SupplierModeConfig;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.SupplierScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Supplier module - maintains inventory stock levels by requesting items from the network.
 * Periodically checks connected inventory and requests items to maintain configured stock levels.
 * Items delivered to this pipe are routed to the connected inventory.
 *
 * <p>Configuration: Up to 9 supply slots, each with an item and target amount.
 * <p>GUI: Accessible with wrench, similar to RequesterModule.
 *
 * <p>Energy: charges {@value #RF_PER_DISPATCH_CYCLE} RF per active supply cycle, drawn from the network battery.
 */
public class SupplierModule implements Module, TickingModule, RoutingModule {
    /**
     * Supply modes for the Supplier Pipe.
     * Based on LogisticsPipes supply modes.
     */
    public enum SupplyMode {
        BULK50,     // ordinal 0 - request in bulk when inventory <= 50% of target
        INFINITE,   // ordinal 1 - continuously fill up to one stack of available space
        PARTIAL,    // ordinal 2 - request whatever is available (default)
        FULL,       // ordinal 3 - only request if full amount is available (all-or-nothing)
        BULK100     // ordinal 4 - request whenever any amount is missing (appended to preserve existing ordinals)
    }

    public static final String SUPPLIES = "supplies"; // NBT key for supply configurations
    private static final String SUPPLIER_DIRECTION = "supplier_direction";
    private static final String TICKS_SINCE_CHECK = "ticks_since_check";
    public static final String MODE = "mode"; // Supply mode
    private static final int CHECK_INTERVAL = 20;
    public static final int MAX_SUPPLY_SLOTS = 9;

    // Energy cost per active supply cycle (drawn from the network battery).
    private static final long RF_PER_DISPATCH_CYCLE = 20;

    @Override
    public void onTick(PipeContext ctx) {
        // Increment tick counter
        int ticks = ctx.getInt(this, TICKS_SINCE_CHECK, 0);
        ticks++;
        ctx.saveInt(this, TICKS_SINCE_CHECK, ticks);

        // Check inventory and request items periodically
        if (ticks >= CHECK_INTERVAL) {
            checkAndSupply(ctx);
            ctx.saveInt(this, TICKS_SINCE_CHECK, 0);
        }
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            setSupplierDirection(ctx, null);
            return;
        }

        Direction current = getSupplierDirection(ctx);
        if (current == null || !inventoryFaces.contains(current)) {
            setSupplierDirection(ctx, inventoryFaces.getFirst());
        }
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        net.minecraft.world.level.Level world = ctx.world();
        net.minecraft.core.BlockPos pos = ctx.pos();
        String moduleStateKey = ctx.moduleStateKey(this);
        serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (syncId, inventory, playerEntity) -> {
                    PipeBlockEntity pipeEntity =
                            world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null;
                    return new SupplierScreenHandler(syncId, inventory, pipeEntity, moduleStateKey);
                },
                net.minecraft.network.chat.Component.translatable("screen.logistics.supplier.items_to_keep_stocked")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult openItemConfig(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (syncId, inv, p) -> new SupplierScreenHandler(syncId, inv, player, hand),
                net.minecraft.network.chat.Component.translatable("screen.logistics.supplier.items_to_keep_stocked")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (!isSupplierFace(ctx, direction)) {
            return null;
        }
        String suffix = ctx.isInventoryConnection(direction) ? "_feature_extended" : "_feature";
        return LogisticsPipe.model("supplier_logistics_pipe" + suffix);
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        // Check if this item has reached its destination (this supplier pipe)
        BlockPos destination = item.getDestination();
        if (destination != null && destination.equals(ctx.pos())) {
            // Item has arrived at this supplier - route it to the connected inventory
            Direction supplierDir = getSupplierDirection(ctx);
            if (supplierDir != null && options.contains(supplierDir)) {
                if (!ctx.world().isClientSide()) {
                    NetDbg.out("[Supplier @ {}] Routing {} x{} to inventory ({})",
                            ctx.pos(), item.getStack().getItem(), item.getStack().getCount(), supplierDir);
                }
                return RoutePlan.reroute(supplierDir);
            }
        }

        // No specific routing for items just passing through
        return RoutePlan.pass();
    }

    /**
     * Check inventory levels and request items to maintain stock levels.
     * Uses network order tracking for pending amounts — accurate even when items
     * are consumed immediately (e.g. furnace input), avoiding NBT drift.
     */
    private void checkAndSupply(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        Direction supplierDir = getSupplierDirection(ctx);
        if (supplierDir == null) return;

        List<SupplyConfig> configs = getSupplyConfigs(ctx);
        if (configs.isEmpty()) return;

        if (!network.consumeEnergy(RF_PER_DISPATCH_CYCLE)) return;

        Map<ItemStack, Long> currentStock = scanInventory(ctx, supplierDir);
        SupplyMode mode = getMode(ctx);

        for (SupplyConfig config : configs) {
            if (config.itemId().isEmpty() || config.amount() <= 0) continue;

            ResourceId itemId = ResourceId.tryParse(config.itemId());
            if (itemId == null) continue;
            var itemHolder = BuiltInRegistries.ITEM.get(itemId.toIdentifier());
            if (itemHolder.isEmpty()) continue;

            ItemStack stack = new ItemStack(itemHolder.get().value());
            long currentAmount = getCurrentAmount(currentStock, stack);

            // Ask the network how many items are ordered but not yet physically delivered.
            // orderedForRequester is incremented on placeOrder() and decremented only when a
            // TravelingItem actually arrives (notifyDelivery). With the provider's async
            // dispatch queue there is an additional delay between the network accepting the
            // order and the items entering the pipe, so pendingAmount may remain > 0 for
            // several ticks after a provider starts working the order.
            long pendingAmount = network.getOrderedAmountFor(ctx.pos(), stack);

            long needed = config.amount() - currentAmount - pendingAmount;

            NetDbg.out("[Supplier @ {}] Checking {} (mode {}): target={}, current={}, pending={}, needed={}",
                    ctx.pos(), config.itemId(), mode, config.amount(), currentAmount, pendingAmount, needed);

            SupplierModeConfig modeConfig = SupplierModeConfig.forMode(mode, stack.getMaxStackSize());
            boolean shouldRequest = false;
            long toRequest = 0;

            if (modeConfig.isWindowBounded()) {
                // INFINITE mode: continuously top-up; ignore target amount.
                // Request = min(maxStack, roomForItem - inTransit), so we keep filling
                // available space even while items are already in transit.
                long availableSpace = getAvailableSpace(ctx, supplierDir, stack);
                long toFill = Math.min(modeConfig.maxOpenRequestWindow(),
                        Math.max(0, availableSpace - pendingAmount));
                if (toFill > 0) {
                    toRequest = toFill;
                    shouldRequest = true;
                }
            } else if (needed > 0 && modeConfig.isTriggerMet(currentAmount, config.amount())) {
                if (modeConfig.fulfillmentMode() == FulfillmentMode.FULL) {
                    // All-or-nothing: only request when full amount is available (chest)
                    // or on-demand craftable (Long.MAX_VALUE). Crafter-busy (0) waits
                    // for next check interval when the crafter re-advertises supply.
                    long available = network.getAvailableAmount(stack);
                    if (available >= needed) {
                        toRequest = needed;
                        shouldRequest = true;
                    }
                } else {
                    // PARTIAL: request needed regardless of availability;
                    // dispatch validation handles fulfillability (same as RequesterModule).
                    toRequest = needed;
                    shouldRequest = true;
                }
            }

            if (shouldRequest) {
                network.placeOrder(ItemStorageLookup.of(stack), toRequest, ctx.pos(), modeConfig.fulfillmentMode());
            }
        }
    }

    /**
     * Scan connected inventory to get current stock levels.
     */
    private Map<ItemStack, Long> scanInventory(PipeContext ctx, Direction direction) {
        Map<ItemStack, Long> stock = new HashMap<>();
        BlockPos targetPos = ctx.pos().relative(direction);
        IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, direction.getOpposite());

        if (storage == null) {
            return stock;
        }

        for (IItemView view : storage.contents()) {
            IItemKey key = view.resource();
            long amount = view.amount();
            if (amount <= 0) {
                continue;
            }

            ItemStack stack = key.toStack(1);

            // Find existing matching stack in map to properly aggregate
            ItemStack existingKey = null;
            for (ItemStack k : stock.keySet()) {
                if (ItemStack.isSameItemSameComponents(k, stack)) {
                    existingKey = k;
                    break;
                }
            }

            if (existingKey != null) {
                stock.put(existingKey, stock.get(existingKey) + amount);
            } else {
                stock.put(stack, amount);
            }
        }

        return stock;
    }

    /**
     * Calculate available space in inventory for a specific item.
     */
    private long getAvailableSpace(PipeContext ctx, Direction direction, ItemStack targetStack) {
        BlockPos targetPos = ctx.pos().relative(direction);
        IItemStorage storage = ItemStorageLookup.find(ctx.world(), targetPos, direction.getOpposite());

        if (storage == null) {
            return 0;
        }

        IItemKey targetKey = ItemStorageLookup.of(targetStack);
        return storage.insert(targetKey, Long.MAX_VALUE, true);
    }

    /**
     * Get current amount of an item in the inventory.
     */
    private long getCurrentAmount(Map<ItemStack, Long> stock, ItemStack target) {
        for (Map.Entry<ItemStack, Long> entry : stock.entrySet()) {
            if (ItemStack.isSameItemSameComponents(entry.getKey(), target)) {
                return entry.getValue();
            }
        }
        return 0;
    }

    /**
     * Get all configured supplies from NBT.
     */
    public List<SupplyConfig> getSupplyConfigs(PipeContext ctx) {
        CompoundTag supplies = ctx.getCompoundTag(this, SUPPLIES);
        List<SupplyConfig> configs = new ArrayList<>();

        for (int i = 0; i < MAX_SUPPLY_SLOTS; i++) {
            String key = String.valueOf(i);
            if (!supplies.contains(key)) {
                continue;
            }

            CompoundTag slotTag = NbtCompat.getCompoundOrEmpty(supplies, key);
            String itemId = NbtCompat.getString(slotTag, "item", "");
            int amount = NbtCompat.getInt(slotTag, "amount", 0);

            if (!itemId.isEmpty() && amount > 0) {
                configs.add(new SupplyConfig(itemId, amount));
            }
        }

        return configs;
    }

    /**
     * Set a supply configuration for a specific slot.
     */
    public void setSupplyConfig(PipeContext ctx, int slotIndex, String itemId, int amount) {
        if (slotIndex < 0 || slotIndex >= MAX_SUPPLY_SLOTS) {
            throw new IllegalArgumentException("Slot must be 0-" + (MAX_SUPPLY_SLOTS - 1));
        }

        CompoundTag supplies = ctx.getCompoundTag(this, SUPPLIES);
        String key = String.valueOf(slotIndex);

        if (itemId.isEmpty() || amount <= 0) {
            // Clear slot
            supplies.remove(key);
        } else {
            // Set slot
            CompoundTag slotTag = new CompoundTag();
            slotTag.putString("item", itemId);
            slotTag.putInt("amount", amount);
            supplies.put(key, slotTag);
        }

        if (!supplies.isEmpty()) {
            ctx.putCompoundTag(this, SUPPLIES, supplies);
        } else {
            ctx.remove(this, SUPPLIES);
        }

        ctx.markDirtyAndSync();
    }

    @Nullable
    protected Direction getSupplierDirection(PipeContext ctx) {
        return com.logistics.core.lib.serialization.DirectionSerializer.load(ctx, this, SUPPLIER_DIRECTION);
    }

    private void setSupplierDirection(PipeContext ctx, @Nullable Direction direction) {
        com.logistics.core.lib.serialization.DirectionSerializer.save(ctx, this, SUPPLIER_DIRECTION, direction);
    }

    private boolean isSupplierFace(PipeContext ctx, Direction direction) {
        return getSupplierDirection(ctx) == direction;
    }

    /**
     * Get the current supply mode.
     */
    public SupplyMode getMode(PipeContext ctx) {
        int ordinal = ctx.getInt(this, MODE, SupplyMode.PARTIAL.ordinal());
        return SupplyMode.values()[ordinal];
    }

    /**
     * Get the current supply mode as an integer (for GUI sync).
     */
    public int getModeOrdinal(PipeContext ctx) {
        return ctx.getInt(this, MODE, SupplyMode.PARTIAL.ordinal());
    }

    /**
     * Set the supply mode.
     */
    public void setMode(PipeContext ctx, SupplyMode mode) {
        ctx.saveInt(this, MODE, mode.ordinal());
        ctx.markDirtyAndSync();
    }

    /**
     * Set the supply mode from ordinal (for GUI).
     */
    public void setModeFromOrdinal(PipeContext ctx, int ordinal) {
        if (ordinal < 0 || ordinal >= SupplyMode.values().length) {
            throw new IllegalArgumentException("Invalid mode ordinal: " + ordinal);
        }
        setMode(ctx, SupplyMode.values()[ordinal]);
    }

    /**
     * Configuration for a single supply slot.
     */
    public record SupplyConfig(String itemId, int amount) {}
}
