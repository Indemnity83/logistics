package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.ItemRequest;
import com.logistics.core.lib.network.NetworkRegistry;
import com.logistics.core.lib.network.PipeNetwork;
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
    private static final String SUPPLIES = "supplies"; // NBT key for supply configurations
    private static final String SUPPLIER_DIRECTION = "supplier_direction";
    private static final String TICKS_SINCE_CHECK = "ticks_since_check";
    private static final String PENDING_REQUESTS = "pending_requests"; // Track in-transit items per item type
    private static final String DELIVERY_COOLDOWN_REMAINING = "delivery_cooldown_remaining"; // Ticks remaining before allowing new requests
    private static final int CHECK_INTERVAL = 20; // Check inventory every 20 ticks (1 second)
    private static final int PENDING_TIMEOUT = 1200; // Clear pending after 1200 ticks (60 seconds) if not delivered
    private static final int DELIVERY_COOLDOWN = 100; // Wait 100 ticks (5 seconds) after routing items before making new requests
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
                net.minecraft.network.chat.Component.translatable("screen.logistics.supplier")));
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
                    // Set cooldown to prevent immediate re-requesting during insertion delay
                    ctx.saveInt(this, DELIVERY_COOLDOWN_REMAINING, DELIVERY_COOLDOWN);
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
        PipeNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
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

        // Decrement and check delivery cooldown
        int cooldownRemaining = ctx.getInt(this, DELIVERY_COOLDOWN_REMAINING, 0);
        if (cooldownRemaining > 0) {
            cooldownRemaining -= CHECK_INTERVAL;
            if (cooldownRemaining > 0) {
                ctx.saveInt(this, DELIVERY_COOLDOWN_REMAINING, cooldownRemaining);
                LOGGER.debug("[Supplier @ {}] In delivery cooldown ({} ticks remaining), skipping requests",
                        ctx.pos(), cooldownRemaining);
                return;
            } else {
                // Cooldown expired
                LOGGER.debug("[Supplier @ {}] Delivery cooldown expired, resuming requests", ctx.pos());
                ctx.remove(this, DELIVERY_COOLDOWN_REMAINING);
            }
        }

        // Load pending requests and decrement timeouts
        CompoundTag pending = ctx.getCompoundTag(this, PENDING_REQUESTS);
        decrementAndCleanupPending(pending);

        // Get current inventory contents
        Map<ItemStack, Long> currentStock = scanInventory(ctx, supplierDir);

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

            // If inventory has reached or exceeded target, clear pending (items were delivered successfully)
            if (currentAmount >= config.amount() && pendingAmount > 0) {
                LOGGER.info("[Supplier @ {}] Delivery confirmed for {}: inventory={}, target={}, clearing pending={}",
                        ctx.pos(), config.itemId(), currentAmount, config.amount(), pendingAmount);
                pending.remove(config.itemId());
                pendingAmount = 0;
            }

            // Calculate actual need: target - current - pending
            long needed = config.amount() - currentAmount - pendingAmount;

            LOGGER.debug("[Supplier @ {}] Checking {}: target={}, current={}, pending={}, needed={}",
                    ctx.pos(), config.itemId(), config.amount(), currentAmount, pendingAmount, needed);

            // If inventory needs more items, create request
            if (needed > 0) {
                long available = network.getAvailableAmount(stack);

                if (available > 0) {
                    long toRequest = Math.min(needed, available);
                    LOGGER.info("[Supplier @ {}] Requesting {} x{} from network (available: {})",
                            ctx.pos(), config.itemId(), toRequest, available);

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
                } else {
                    LOGGER.warn("[Supplier @ {}] Need {} x{} but NONE available in network!",
                            ctx.pos(), config.itemId(), needed);
                }
            }
        }

        // Save updated pending requests
        if (!pending.isEmpty()) {
            ctx.putCompoundTag(this, PENDING_REQUESTS, pending);
        } else {
            ctx.remove(this, PENDING_REQUESTS);
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
            stock.merge(stack, amount, Long::sum);
        }

        return stock;
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
        entry.putInt("ticksRemaining", PENDING_TIMEOUT);

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
        CompoundTag state = ctx.moduleState(getStateKey());
        String directionStr = NbtCompat.getString(state, SUPPLIER_DIRECTION, "");
        if (directionStr.isEmpty()) {
            return null;
        }
        try {
            return Direction.from3DDataValue(Integer.parseInt(directionStr));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setSupplierDirection(PipeContext ctx, @Nullable Direction direction) {
        Direction current = getSupplierDirection(ctx);
        if (current == direction) {
            return;
        }

        if (direction == null) {
            ctx.remove(this, SUPPLIER_DIRECTION);
        } else {
            ctx.saveString(this, SUPPLIER_DIRECTION, String.valueOf(direction.get3DDataValue()));
        }

        ctx.markDirtyAndSync();
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
     * Configuration for a single supply slot.
     */
    public record SupplyConfig(String itemId, int amount) {}
}
