package com.logistics.pipe.modules;

import com.logistics.core.lib.network.ItemRequest;
import com.logistics.core.lib.network.NetworkRegistry;
import com.logistics.core.lib.network.PipeNetwork;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.PipeContext;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
public class RequesterModule implements Module {
    private static final String REQUESTS = "requests"; // NBT key for request configurations
    private static final String REQUESTER_DIRECTION = "requester_direction";
    private static final String TICKS_SINCE_REQUEST = "ticks_since_request";
    private static final int REQUEST_INTERVAL = 20; // Check requests every 20 ticks (1 second)
    public static final int MAX_REQUEST_SLOTS = 9;
    // TODO(Phase 11): Energy costs
    // private static final int RF_PER_REQUEST_CYCLE = 5;

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) {
            return;
        }

        // Increment tick counter
        int ticks = ctx.getInt(this, TICKS_SINCE_REQUEST, 0);
        ticks++;
        ctx.saveInt(this, TICKS_SINCE_REQUEST, ticks);

        // Process requests periodically
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

        // TODO(Phase 9): Open GUI for request configuration
        // For now, cycle through inventory connections
        List<Direction> connected = ctx.getInventoryConnections();

        if (connected.isEmpty()) {
            setRequesterDirection(ctx, null);
            return InteractionResult.SUCCESS;
        }

        Direction current = getRequesterDirection(ctx);
        Direction next = nextInCycle(connected, current);

        setRequesterDirection(ctx, next);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (!isRequesterFace(ctx, direction)) {
            return null;
        }
        String suffix = ctx.isInventoryConnection(direction) ? "_feature_extended" : "_feature";
        return com.logistics.LogisticsPipe.model("requester_logistics_pipe" + suffix);
    }

    /**
     * Process configured requests - check network availability and create ItemRequest objects.
     */
    private void processRequests(PipeContext ctx) {
        PipeNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
        if (network == null) {
            return;
        }

        // TODO(Phase 11): Check energy availability
        // if (ctx.getEnergy() < RF_PER_REQUEST_CYCLE) return;

        List<RequestConfig> configs = getRequestConfigs(ctx);
        if (configs.isEmpty()) {
            return;
        }

        // Check each configured request
        for (RequestConfig config : configs) {
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
            long available = network.getAvailableAmount(stack);

            // If network has sufficient items, create request
            if (available >= config.amount()) {
                ItemRequest request = new ItemRequest(
                    ctx.pos(),
                    stack,
                    config.amount(),
                    ctx.world().getGameTime()
                );
                network.addRequest(request);

                // TODO(Phase 11): Consume energy
                // ctx.setEnergy(ctx.getEnergy() - RF_PER_REQUEST_CYCLE);

                // Only process one request per cycle
                break;
            }
        }
    }

    /**
     * Get all configured requests from NBT.
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
     */
    public void setRequestConfig(PipeContext ctx, int slotIndex, String itemId, int amount) {
        if (slotIndex < 0 || slotIndex >= MAX_REQUEST_SLOTS) {
            throw new IllegalArgumentException("Slot must be 0-" + (MAX_REQUEST_SLOTS - 1));
        }

        CompoundTag requests = ctx.getCompoundTag(this, REQUESTS);
        String key = String.valueOf(slotIndex);

        if (itemId.isEmpty() || amount <= 0) {
            // Clear slot
            requests.remove(key);
        } else {
            // Set slot
            CompoundTag slotTag = new CompoundTag();
            slotTag.putString("item", itemId);
            slotTag.putInt("amount", amount);
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
        CompoundTag state = ctx.moduleState(getStateKey());
        String directionStr = NbtCompat.getString(state, REQUESTER_DIRECTION, "");
        if (directionStr.isEmpty()) {
            return null;
        }
        try {
            return Direction.from3DDataValue(Integer.parseInt(directionStr));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setRequesterDirection(PipeContext ctx, @Nullable Direction direction) {
        Direction current = getRequesterDirection(ctx);
        if (current == direction) {
            return;
        }

        if (direction == null) {
            ctx.remove(this, REQUESTER_DIRECTION);
        } else {
            ctx.saveString(this, REQUESTER_DIRECTION, String.valueOf(direction.get3DDataValue()));
        }

        ctx.markDirtyAndSync();
    }

    private Direction nextInCycle(List<Direction> ordered, @Nullable Direction current) {
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("ordered directions must not be empty");
        }

        int idx = (current == null) ? -1 : ordered.indexOf(current);
        return (idx < 0) ? ordered.getFirst() : ordered.get((idx + 1) % ordered.size());
    }

    private boolean isRequesterFace(PipeContext ctx, Direction direction) {
        return getRequesterDirection(ctx) == direction;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        // TODO(Phase 11): Accept energy for request costs
        return false; // For now, no energy required
    }

    /**
     * Configuration for a single request slot.
     */
    public record RequestConfig(String itemId, int amount) {}
}
