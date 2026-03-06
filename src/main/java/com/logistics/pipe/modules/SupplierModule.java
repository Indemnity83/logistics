package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
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
        BULK50,     // ordinal 0 - only request when inventory <= 50% of target
        INFINITE,   // ordinal 1 - request 1 stack at a time (gradual filling)
        PARTIAL,    // ordinal 2 - request whatever is available (default)
        FULL,       // ordinal 3 - only request if full amount is available (all-or-nothing)
        BULK100     // ordinal 4 - only request when inventory is completely empty (appended to preserve existing ordinals)
    }

    private static final String SUPPLIES = "supplies"; // NBT key for supply configurations
    private static final String SUPPLIER_DIRECTION = "supplier_direction";
    private static final String TICKS_SINCE_CHECK = "ticks_since_check";
    private static final String MODE = "mode"; // Supply mode
    private static final int CHECK_INTERVAL = 100;
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
                    LOGGER.debug("[Supplier @ {}] Routing {} x{} to inventory ({})",
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

        Map<ItemStack, Long> currentStock = scanInventory(ctx, supplierDir);
        SupplyMode mode = getMode(ctx);

        for (SupplyConfig config : configs) {
            if (config.itemId().isEmpty() || config.amount() <= 0) continue;

            ResourceId itemId = ResourceId.tryParse(config.itemId());
            if (itemId == null) {
                continue;
            }
            // MC 1.21.1: get() returns Item directly, not Optional<Holder<Item>>
            Item itemHolder = BuiltInRegistries.ITEM.get(itemId.toIdentifier());
            if (itemHolder == null) {
                continue;
            }

            ItemStack stack = new ItemStack(itemHolder);
            long currentAmount = getCurrentAmount(currentStock, stack);

            // Ask the network how many items are still in pending orders for us.
            // This drops to 0 as soon as the provider ships, so fast-consuming inventories
            // (furnaces) are handled correctly without any NBT drift.
            long pendingAmount = network.getOrderedAmountFor(ctx.pos(), stack);

            long needed = config.amount() - currentAmount - pendingAmount;

            LOGGER.debug("[Supplier @ {}] Checking {} (mode {}): target={}, current={}, pending={}, needed={}",
                    ctx.pos(), config.itemId(), mode, config.amount(), currentAmount, pendingAmount, needed);

            long available = network.getAvailableAmount(stack);
            boolean shouldRequest = false;
            long toRequest = 0;

            if (mode == SupplyMode.INFINITE) {
                long availableSpace = getAvailableSpace(ctx, supplierDir, stack);
                long spaceToFill = availableSpace - pendingAmount;
                if (spaceToFill > 0) {
                    toRequest = Math.min(Math.min(stack.getMaxStackSize(), spaceToFill), available);
                    shouldRequest = toRequest > 0;
                }
            } else if (needed > 0) {
                switch (mode) {
                    case BULK50:
                        if (currentAmount <= config.amount() / 2) {
                            toRequest = Math.min(needed, available);
                            shouldRequest = toRequest > 0;
                        }
                        break;
                    case BULK100:
                        if (currentAmount == 0) {
                            toRequest = Math.min(needed, available);
                            shouldRequest = toRequest > 0;
                        }
                        break;
                    case FULL:
                        if (available >= needed) {
                            toRequest = needed;
                            shouldRequest = true;
                        }
                        break;
                    case PARTIAL:
                    default:
                        toRequest = Math.min(needed, available);
                        shouldRequest = toRequest > 0;
                        break;
                }
            }

            if (shouldRequest) {
                network.placeOrder(ItemVariant.of(stack), toRequest, ctx.pos());
            }
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
