package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.DirectionSerializer;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.network.NetworkRegistry;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import com.logistics.pipe.ui.RequesterScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Requester module - creates requests for items from the network.
 * Periodically checks configured requests and creates ItemRequest objects if items are available.
 * Items delivered to this pipe are routed to the connected inventory.
 *
 * <p>Configuration: Up to 9 request slots, each with an item and amount.
 * <p>GUI: TODO (Phase 9) - For now, use NBT commands or placeholder interaction.
 *
 * <p>Energy: TODO (Phase 11): 5 RF per request cycle
 */
public class RequesterModule implements Module, TickingModule {
    private static final String REQUESTS = "requests";
    private static final String REQUESTER_DIRECTION = "requester_direction";
    private static final String TICKS_SINCE_REQUEST = "ticks_since_request";
    private static final int REQUEST_INTERVAL = 20;
    public static final int MAX_REQUEST_SLOTS = 9;
    public static final int MAX_REQUEST_AMOUNT = 576;
    // TODO(Phase 11): Energy costs
    // private static final int RF_PER_REQUEST_CYCLE = 5;

    @Override
    public void onTick(PipeContext ctx) {
        int ticks = ctx.getInt(this, TICKS_SINCE_REQUEST, 0);
        ticks++;
        ctx.saveInt(this, TICKS_SINCE_REQUEST, ticks);

        if (ticks >= REQUEST_INTERVAL) {
            processRequests(ctx);
            ctx.saveInt(this, TICKS_SINCE_REQUEST, 0);
        }
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            setRequesterDirection(ctx, null);
            return;
        }

        Direction current = getRequesterDirection(ctx);
        if (current == null || !inventoryFaces.contains(current)) {
            setRequesterDirection(ctx, inventoryFaces.getFirst());
        }
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        Level world = ctx.world();
        BlockPos pos = ctx.pos();
        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, inventory, p) -> new RequesterScreenHandler(
                        syncId,
                        inventory,
                        world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null
                ),
                Component.translatable("screen.logistics.requester")
        ));
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (!isRequesterFace(ctx, direction)) {
            return null;
        }
        String suffix = ctx.isInventoryConnection(direction) ? "_feature_extended" : "_feature";
        return LogisticsPipe.model("requester_logistics_pipe" + suffix);
    }

    /**
     * Process configured requests - check network availability and create ItemRequest objects.
     * Only processes one request per cycle to avoid flooding the network.
     */
    private void processRequests(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) {
            return;
        }

        // TODO(Phase 11): Check energy availability
        // if (ctx.getEnergy() < RF_PER_REQUEST_CYCLE) return;

        List<RequestConfig> configs = getRequestConfigs(ctx);
        if (configs.isEmpty()) {
            return;
        }

        for (int i = 0; i < configs.size(); i++) {
            RequestConfig config = configs.get(i);
            if (config.itemId().isEmpty() || config.amount() <= 0) {
                continue;
            }

            ResourceId itemId = ResourceId.tryParse(config.itemId());
            if (itemId == null) {
                continue;
            }
            // MC 1.21.1: get() returns Item directly, not Optional<Holder<Item>>
            Item item = BuiltInRegistries.ITEM.get(itemId.toIdentifier());
            if (item == null) {
                continue;
            }

            ItemStack stack = new ItemStack(item);
            long alreadyOrdered = network.getOrderedAmountFor(ctx.pos(), stack);
            long needed = config.amount() - alreadyOrdered;

            long clamped = Math.min(needed, MAX_REQUEST_AMOUNT);
            if (clamped > 0) {
                NetDbg.out("[Requester @ {}] Placed order for {}x{}", ctx.pos(), clamped, item);
                network.placeOrder(ItemVariant.of(stack), clamped, ctx.pos());

                // TODO(Phase 11): Consume energy
                // ctx.setEnergy(ctx.getEnergy() - RF_PER_REQUEST_CYCLE);

                break; // Only process one request per cycle
            } else {
                NetDbg.out("[Requester @ {}] Slot {} skipped: need={}, pending={}", ctx.pos(), i, needed, alreadyOrdered);
            }
        }
    }

    /**
     * Get all configured requests from NBT.
     * @param ctx Pipe context
     * @return List of configured requests (empty if none)
     */
    public List<RequestConfig> getRequestConfigs(PipeContext ctx) {
        CompoundTag requests = ctx.getCompoundTag(this, REQUESTS);
        List<RequestConfig> configs = new ArrayList<>();

        for (int i = 0; i < MAX_REQUEST_SLOTS; i++) {
            String key = String.valueOf(i);
            if (!requests.contains(key)) {
                continue;
            }

            CompoundTag slotTag = NbtCompat.getCompoundOrEmpty(requests, key);
            String itemId = NbtCompat.getString(slotTag, "item", "");
            int amount = NbtCompat.getInt(slotTag, "amount", 0);

            if (!itemId.isEmpty() && amount > 0) {
                configs.add(new RequestConfig(itemId, amount));
            }
        }

        return configs;
    }

    /**
     * Set a request configuration for a specific slot.
     * @param ctx Pipe context
     * @param slotIndex Request slot index (0-8)
     * @param itemId Item resource ID (empty to clear slot)
     * @param amount Amount to request (0 or negative to clear slot)
     */
    public void setRequestConfig(PipeContext ctx, int slotIndex, String itemId, int amount) {
        if (slotIndex < 0 || slotIndex >= MAX_REQUEST_SLOTS) {
            throw new IllegalArgumentException("Slot must be 0-" + (MAX_REQUEST_SLOTS - 1));
        }

        CompoundTag requests = ctx.getCompoundTag(this, REQUESTS);
        String key = String.valueOf(slotIndex);

        if (itemId.isEmpty() || amount <= 0) {
            requests.remove(key);
        } else {
            int clamped = Math.min(amount, MAX_REQUEST_AMOUNT);
            CompoundTag slotTag = new CompoundTag();
            slotTag.putString("item", itemId);
            slotTag.putInt("amount", clamped);
            requests.put(key, slotTag);
        }

        if (!requests.isEmpty()) {
            ctx.putCompoundTag(this, REQUESTS, requests);
        } else {
            ctx.remove(this, REQUESTS);
        }

        ctx.markDirtyAndSync();
    }

    @Nullable
    private Direction getRequesterDirection(PipeContext ctx) {
        return DirectionSerializer.load(ctx, this, REQUESTER_DIRECTION);
    }

    private void setRequesterDirection(PipeContext ctx, @Nullable Direction direction) {
        DirectionSerializer.save(ctx, this, REQUESTER_DIRECTION, direction);
    }

    private boolean isRequesterFace(PipeContext ctx, Direction direction) {
        return getRequesterDirection(ctx) == direction;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        // TODO(Phase 11): Accept energy for request costs
        return false;
    }

    /**
     * Additive/manual order — places {@code amount} as a new discrete order regardless of
     * what is already in-flight. Each call creates an independent request, so two calls
     * will result in two deliveries. Intended for GUI-triggered one-shot requests.
     *
     * <p>This differs from {@link #processRequests}, which is top-up ordering: it checks
     * how much is already ordered and only requests the shortfall to reach the slot target.
     * Do not consolidate the two — the distinct semantics are intentional.
     */
    public void requestItem(PipeContext ctx, ItemStack stack, int amount) {
        if (ctx.world().isClientSide()) {
            return;
        }

        if (amount <= 0 || amount > MAX_REQUEST_AMOUNT) {
            return;
        }

        // TODO(Phase 11): Check and consume energy (5 RF per request)

        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) {
            return;
        }

        ItemVariant variant = ItemVariant.of(stack);

        // Pre-validate: fail immediately only if there is no supply path at all for this item
        // (no provider and no crafter). For craftable items we allow queuing even when
        // ingredients are temporarily committed to other in-progress orders — the order will
        // wait in the queue until the crafter has buffer space and ingredients are available.
        long available = network.getAvailableAmount(stack);
        String itemName = stack.getHoverName().getString();
        if (available == 0) {
            // Nothing can produce this item
            NetDbg.out("[Requester @ {}] No supply found for {} (need={})", ctx.pos(), stack.getItem(), amount);
            sendAlert(ctx, Component.literal(
                    "[Logistics] Cannot fill order for " + itemName + " \u2014 not available in network"));
            return;
        }
        if (available != Long.MAX_VALUE && available < amount) {
            // Directly stocked item with insufficient quantity (not craftable)
            sendAlert(ctx, Component.literal(
                    "[Logistics] Cannot fill order for " + itemName + " \u2014 only "
                            + available + " available, need " + amount));
            return;
        }

        // For craftable items, validate upfront that real stock + crafter output can cover
        // the full requested amount. getMissingIngredients accounts for existing stock and
        // asks the crafter only for the remainder, so this mirrors the dispatch logic exactly.
        if (available == Long.MAX_VALUE) {
            List<ItemVariant> missing = network.getMissingIngredients(variant, amount);
            if (!missing.isEmpty()) {
                String missingNames = missing.stream()
                        .map(v -> v.toStack().getHoverName().getString())
                        .collect(Collectors.joining(", "));
                sendAlert(ctx, Component.literal(
                        "[Logistics] Cannot fill order for " + itemName
                                + " \u2014 missing ingredients: " + missingNames));
                return;
            }
        }

        network.placeOrder(variant, amount, ctx.pos(), FulfillmentMode.FULL);
    }

    private void sendAlert(PipeContext ctx, Component msg) {
        if (!(ctx.world() instanceof ServerLevel serverLevel)) return;
        double x = ctx.pos().getX(), y = ctx.pos().getY(), z = ctx.pos().getZ();
        for (ServerPlayer player : serverLevel.getPlayers(p -> p.distanceToSqr(x, y, z) < 64 * 64)) {
            player.sendSystemMessage(msg);
        }
    }

    /**
     * Configuration for a single request slot.
     * @param itemId Item resource ID (e.g., "minecraft:diamond")
     * @param amount Amount to request when available
     */
    public record RequestConfig(String itemId, int amount) {}
}
