package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.network.ItemRequest;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.ui.SupplierScreenHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.logistics.LogisticsMod.LOGGER;

/**
 * Supplier module - maintains inventory stock levels by requesting items from the network.
 * Periodically checks connected inventory and requests items to maintain configured stock levels.
 * Items delivered to this pipe are routed to the connected inventory.
 *
 * <p>Configuration: Up to 9 supply slots, each with an item and target amount.
 * <p>GUI: Accessible with wrench, similar to RequesterModule.
 *
 * <p>Energy: TODO (Phase 11): 1 RF per item requested
 */
public class SupplierModule implements Module {
    /**
     * Supply modes for the Supplier Pipe.
     * Based on LogisticsPipes supply modes.
     */
    public enum SupplyMode {
        STOCKED,    // Bulk50 - only request when inventory <= 50% of target
        INFINITE,   // Request 1 stack at a time (gradual filling)
        PARTIAL,    // Request whatever is available (default)
        FULL        // Only request if full amount is available (all-or-nothing)
    }

    private static final String SUPPLIES = "supplies"; // NBT key for supply configurations
    private static final String SUPPLIER_DIRECTION = "supplier_direction";
    private static final String TICKS_SINCE_CHECK = "ticks_since_check";
    private static final String PENDING_REQUESTS = "pending_requests"; // Track in-transit items per item type
    private static final String PREVIOUS_AMOUNTS = "previous_amounts"; // Track previous inventory amounts to detect deliveries
    private static final String MODE = "mode"; // Supply mode
    private static final int CHECK_INTERVAL = 20; // Check inventory every 20 ticks (1 second)
    public static final int MAX_SUPPLY_SLOTS = 9;

    // TODO(Phase 11): Energy costs
    // private static final int RF_PER_ITEM = 1;

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) {
            return;
        }

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
        serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (syncId, inventory, playerEntity) -> {
                    PipeBlockEntity pipeEntity =
                            world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null;
                    return new SupplierScreenHandler(syncId, inventory, pipeEntity);
                },
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
                    LOGGER.info("[Supplier @ {}] Routing {} x{} to inventory ({})",
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
     * Tracks pending requests per item type to avoid duplicates while allowing
     * multiple different items to be requested simultaneously.
     */
    private void checkAndSupply(PipeContext ctx) {
        // Ensure this pipe is part of a network (creates/joins on first tick after load)
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) {
            return;
        }

        Direction supplierDir = getSupplierDirection(ctx);
        if (supplierDir == null) {
            return;
        }

        // TODO(Phase 11): Check energy availability
        // if (ctx.getEnergy() < RF_PER_ITEM) return;

        List<SupplyConfig> configs = getSupplyConfigs(ctx);
        if (configs.isEmpty()) {
            return;
        }

        LOGGER.debug("[Supplier @ {}] Scanning inventory (direction: {})", ctx.pos(), supplierDir);

        // Load pending requests and decrement timeouts
        CompoundTag pending = ctx.getCompoundTag(this, PENDING_REQUESTS);
        decrementAndCleanupPending(pending);

        // Get current inventory contents
        Map<ItemStack, Long> currentStock = scanInventory(ctx, supplierDir);

        // Load previous inventory amounts to detect deliveries
        CompoundTag previousAmounts = ctx.getCompoundTag(this, PREVIOUS_AMOUNTS);

        // Get current mode
        SupplyMode mode = getMode(ctx);

        // Check each configured supply and create requests for missing items
        for (SupplyConfig config : configs) {
            if (config.itemId().isEmpty() || config.amount() <= 0) {
                continue;
            }

            // Parse item from ID
            ResourceId itemId = ResourceId.tryParse(config.itemId());
            if (itemId == null) {
                continue;
            }
            var itemHolder = BuiltInRegistries.ITEM.get(itemId.toIdentifier());
            if (itemHolder.isEmpty()) {
                continue;
            }
            Item item = itemHolder.get().value();

            ItemStack stack = new ItemStack(item);
            long currentAmount = getCurrentAmount(currentStock, stack);

            // Get pending amount for this item (items currently in transit)
            long pendingAmount = getPendingAmount(pending, config.itemId());

            // Detect deliveries by comparing to previous amount.
            // Limitation: if items are manually removed from the inventory in the same tick that
            // a delivery arrives, the net change may be zero or negative and the delivery goes
            // undetected. This leaves pendingAmount inflated until ORDER_TTL expires and clears
            // it. A proper fix would require delivery receipts (a callback when the item reaches
            // its destination), which is tracked for a future phase.
            long previousAmount = NbtCompat.getLong(previousAmounts, config.itemId(), 0);

            if (currentAmount > previousAmount && pendingAmount > 0) {
                // Items were delivered - decrement pending by the amount delivered
                long delivered = currentAmount - previousAmount;
                long newPending = Math.max(0, pendingAmount - delivered);

                if (newPending == 0) {
                    pending.remove(config.itemId());
                    LOGGER.info("[Supplier @ {}] Delivery confirmed for {}: received {} (pending cleared)",
                            ctx.pos(), config.itemId(), delivered);
                } else {
                    CompoundTag itemPending = NbtCompat.getCompoundOrEmpty(pending, config.itemId());
                    itemPending.putLong("amount", newPending);
                    pending.put(config.itemId(), itemPending);
                    LOGGER.info("[Supplier @ {}] Delivery confirmed for {}: received {} (pending: {} -> {})",
                            ctx.pos(), config.itemId(), delivered, pendingAmount, newPending);
                }

                pendingAmount = newPending;
            }

            // Update previous amount for next tick
            previousAmounts.putLong(config.itemId(), currentAmount);

            // Calculate actual need: target - current - pending
            long needed = config.amount() - currentAmount - pendingAmount;

            LOGGER.debug("[Supplier @ {}] Checking {} (mode {}): target={}, current={}, pending={}, needed={}",
                    ctx.pos(), config.itemId(), mode, config.amount(), currentAmount, pendingAmount, needed);

            // Mode-specific request logic
            boolean shouldRequest = false;
            long toRequest = 0;
            long available = network.getAvailableAmount(stack);

            // Infinite mode ignores target and just fills available space
            if (mode == SupplyMode.INFINITE) {
                long availableSpace = getAvailableSpace(ctx, supplierDir, stack);
                long spaceToFill = availableSpace - pendingAmount;
                if (spaceToFill > 0) {
                    int maxStackSize = stack.getMaxStackSize();
                    toRequest = Math.min(Math.min(maxStackSize, spaceToFill), available);
                    shouldRequest = toRequest > 0;
                    if (shouldRequest) {
                        LOGGER.info("[Supplier @ {}] Infinite mode: requesting {} x{} (space available: {}, 1 stack at a time)",
                                ctx.pos(), config.itemId(), toRequest, availableSpace);
                    }
                }
            } else if (needed > 0) {
                switch (mode) {
                    case STOCKED:
                        // Bulk50 - only request when inventory drops to 50% or less of target
                        if (currentAmount <= config.amount() / 2) {
                            toRequest = Math.min(needed, available);
                            shouldRequest = toRequest > 0;
                            long percentFull = config.amount() > 0 ? (currentAmount * 100) / config.amount() : 0;
                            if (shouldRequest) {
                                LOGGER.info("[Supplier @ {}] Stocked mode: requesting {} x{} (at {}%)",
                                        ctx.pos(), config.itemId(), toRequest, percentFull);
                            }
                        }
                        break;

                    case FULL:
                        // Only request if the full needed amount is available
                        if (available >= needed) {
                            toRequest = needed;
                            shouldRequest = true;
                            LOGGER.info("[Supplier @ {}] Full mode: requesting {} x{} (full amount available)",
                                    ctx.pos(), config.itemId(), toRequest);
                        } else {
                            LOGGER.debug("[Supplier @ {}] Full mode: need {} but only {} available, waiting",
                                    ctx.pos(), config.itemId(), needed, available);
                        }
                        break;

                    case PARTIAL:
                    default:
                        // Request whatever is available (original behavior)
                        toRequest = Math.min(needed, available);
                        shouldRequest = toRequest > 0;
                        if (shouldRequest) {
                            LOGGER.info("[Supplier @ {}] Partial mode: requesting {} x{} from network (available: {})",
                                    ctx.pos(), config.itemId(), toRequest, available);
                        }
                        break;
                }
            }

            // Create request if mode logic approved it
            if (shouldRequest) {
                ItemRequest request = new ItemRequest(
                    ctx.pos(),
                    stack,
                    toRequest,
                    ctx.world().getGameTime()
                );
                network.addRequest(request);

                // Track this request as pending with timeout
                addPendingRequest(pending, config.itemId(), toRequest);

                // TODO(Phase 11): Consume energy
                // ctx.setEnergy(ctx.getEnergy() - (toRequest * RF_PER_ITEM));
            } else if (needed > 0 && available == 0) {
                LOGGER.warn("[Supplier @ {}] Need {} x{} but NONE available in network!",
                        ctx.pos(), config.itemId(), needed);
            }
        }

        // Save updated pending requests
        if (!pending.isEmpty()) {
            ctx.putCompoundTag(this, PENDING_REQUESTS, pending);
        } else {
            ctx.remove(this, PENDING_REQUESTS);
        }

        // Save previous inventory amounts for next tick's delivery detection
        if (!previousAmounts.isEmpty()) {
            ctx.putCompoundTag(this, PREVIOUS_AMOUNTS, previousAmounts);
        } else {
            ctx.remove(this, PREVIOUS_AMOUNTS);
        }
    }

    /**
     * Scan connected inventory to get current stock levels.
     */
    private Map<ItemStack, Long> scanInventory(PipeContext ctx, Direction direction) {
        Map<ItemStack, Long> stock = new HashMap<>();
        BlockPos targetPos = ctx.pos().relative(direction);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());

        if (storage == null) {
            return stock;
        }

        for (StorageView<ItemVariant> view : storage) {
            ItemVariant variant = view.getResource();
            if (variant.isBlank()) {
                continue;
            }

            long amount = view.getAmount();
            if (amount <= 0) {
                continue;
            }

            ItemStack stack = variant.toStack();

            // Find existing matching stack in map to properly aggregate
            ItemStack existingKey = null;
            for (ItemStack key : stock.keySet()) {
                if (ItemStack.isSameItemSameComponents(key, stack)) {
                    existingKey = key;
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
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(ctx.world(), targetPos, direction.getOpposite());

        if (storage == null) {
            return 0;
        }

        ItemVariant targetVariant = ItemVariant.of(targetStack);
        long availableSpace = 0;

        for (StorageView<ItemVariant> view : storage) {
            long capacity = view.getCapacity();
            ItemVariant variant = view.getResource();
            long currentAmount = view.getAmount();

            if (variant.isBlank()) {
                // Empty slot - can hold full stack
                availableSpace += targetStack.getMaxStackSize();
            } else if (variant.equals(targetVariant)) {
                // Slot has same item - can hold more up to capacity
                long spaceInSlot = capacity - currentAmount;
                availableSpace += spaceInSlot;
            }
            // Different item - can't use this slot
        }

        return availableSpace;
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
     * Get pending amount for a specific item (items in transit).
     */
    private long getPendingAmount(CompoundTag pending, String itemId) {
        if (!pending.contains(itemId)) {
            return 0;
        }
        CompoundTag entry = NbtCompat.getCompoundOrEmpty(pending, itemId);
        return NbtCompat.getLong(entry, "amount", 0);
    }

    /**
     * Add a pending request for tracking with a fresh timeout.
     */
    private void addPendingRequest(CompoundTag pending, String itemId, long amount) {
        CompoundTag entry = new CompoundTag();

        // Add to existing pending amount if there is one
        long existing = getPendingAmount(pending, itemId);
        entry.putLong("amount", existing + amount);

        // Reset timeout to full duration when adding new request
        entry.putInt("ticksRemaining", LogisticsPipe.CONFIG.ORDER_TTL);

        pending.put(itemId, entry);
    }

    /**
     * Decrement timeouts and clean up expired pending requests.
     * Called every CHECK_INTERVAL ticks (1 second).
     */
    private void decrementAndCleanupPending(CompoundTag pending) {
        List<String> toRemove = new ArrayList<>();

        for (String key : pending.keySet()) {
            CompoundTag entry = NbtCompat.getCompoundOrEmpty(pending, key);
            int ticksRemaining = NbtCompat.getInt(entry, "ticksRemaining", 0);
            long pendingAmount = NbtCompat.getLong(entry, "amount", 0);

            // Decrement by CHECK_INTERVAL (amount of time since last check)
            ticksRemaining -= CHECK_INTERVAL;

            if (ticksRemaining <= 0) {
                // Timeout expired - item assumed lost in transit
                LOGGER.warn("[Supplier] Pending timeout expired for {}: {} items lost in transit",
                        key, pendingAmount);
                toRemove.add(key);
            } else {
                // Update remaining ticks
                entry.putInt("ticksRemaining", ticksRemaining);
            }
        }

        for (String key : toRemove) {
            pending.remove(key);
        }
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
    private Direction getSupplierDirection(PipeContext ctx) {
        return com.logistics.core.lib.storage.DirectionSerializer.load(ctx, this, SUPPLIER_DIRECTION);
    }

    private void setSupplierDirection(PipeContext ctx, @Nullable Direction direction) {
        com.logistics.core.lib.storage.DirectionSerializer.save(ctx, this, SUPPLIER_DIRECTION, direction);
    }

    private boolean isSupplierFace(PipeContext ctx, Direction direction) {
        return getSupplierDirection(ctx) == direction;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        // TODO(Phase 11): Accept energy for supply costs
        return false; // For now, no energy required
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
