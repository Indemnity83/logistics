package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.DirectionSerializer;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.ui.SinkScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.logistics.LogisticsMod.LOGGER;

/**
 * Sink module - routes items from the network into an adjacent inventory.
 * Can filter specific items OR accept any item without a destination (default route).
 *
 * <p>Configuration: 9 filter slots + "Default Route" toggle
 * <p>GUI: Accessible with wrench
 *
 * <p>This is the Basic Logistics Pipe from LogisticsPipes - pulls items out of the network.
 */
public class SinkModule implements Module {
    private static final String FILTERS = "filters";
    private static final String DEFAULT_ROUTE = "default_route";
    private static final String SINK_DIRECTION = "sink_direction";
    private static final String TICKS_SINCE_SYNC = "ticks_since_sync";
    private static final int SYNC_INTERVAL = 20; // Re-register with network every second to recover from splits
    public static final int MAX_FILTER_SLOTS = 9;

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) {
            return;
        }

        int ticks = ctx.getInt(this, TICKS_SINCE_SYNC, 0) + 1;
        if (ticks < SYNC_INTERVAL) {
            ctx.saveInt(this, TICKS_SINCE_SYNC, ticks);
            return;
        }
        ctx.saveInt(this, TICKS_SINCE_SYNC, 0);

        // Re-register with the network periodically to recover after network splits/merges.
        // setDefaultRoute() handles immediate registration on change; this is just the recovery path.
        ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
        if (network != null && isDefaultRoute(ctx)) {
            network.registerDefaultRouteSink(ctx.pos(), 0);
        }
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            setSinkDirection(ctx, null);
            return;
        }

        Direction current = getSinkDirection(ctx);
        if (current == null || !inventoryFaces.contains(current)) {
            setSinkDirection(ctx, inventoryFaces.getFirst());
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
                (syncId, inventory, p) -> new SinkScreenHandler(
                        syncId,
                        inventory,
                        world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null
                ),
                Component.translatable("screen.logistics.sink.requested_items")
        ));
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (!isSinkFace(ctx, direction)) {
            return null;
        }
        String suffix = ctx.isInventoryConnection(direction) ? "_arm_extended" : "_arm";
        return LogisticsPipe.model("basic_logistics_pipe" + suffix);
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        Direction sinkDir = getSinkDirection(ctx);
        if (sinkDir == null || !options.contains(sinkDir)) {
            return RoutePlan.pass();
        }

        // Priority 1: Filter match
        if (matchesFilter(ctx, item.getStack())) {
            LOGGER.debug("[Sink @ {}] Item {} matches filter, routing to inventory ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
            return RoutePlan.reroute(sinkDir);
        }

        // Priority 2: Destination match
        if (item.getDestination() != null && item.getDestination().equals(ctx.pos())) {
            LOGGER.debug("[Sink @ {}] Item {} arrived at destination, routing to inventory ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
            return RoutePlan.reroute(sinkDir);
        }

        // Priority 3: Default route (only if no other routing options)
        if (isDefaultRoute(ctx) && item.getDestination() == null) {
            boolean hasOtherOptions = options.stream().anyMatch(dir -> !dir.equals(sinkDir));
            if (!hasOtherOptions) {
                LOGGER.debug("[Sink @ {}] Default route accepting {} (no other options), routing to inventory ({})",
                        ctx.pos(), item.getStack().getItem(), sinkDir);
                return RoutePlan.reroute(sinkDir);
            }
            LOGGER.debug("[Sink @ {}] Default route passing on {} (other directions available)",
                    ctx.pos(), item.getStack().getItem());
        }

        return RoutePlan.pass();
    }

    /**
     * Check if item matches any configured filter.
     * @param ctx Pipe context
     * @param stack Item to check
     * @return true if the item matches any filter slot
     */
    public boolean matchesFilter(PipeContext ctx, ItemStack stack) {
        CompoundTag filters = ctx.getCompoundTag(this, FILTERS);

        for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
            String key = String.valueOf(i);
            if (!filters.contains(key)) {
                continue;
            }

            String itemId = NbtCompat.getString(filters, key, "");
            if (itemId.isEmpty()) {
                continue;
            }

            ResourceId resourceId = ResourceId.tryParse(itemId);
            if (resourceId == null) {
                continue;
            }

            var itemHolder = BuiltInRegistries.ITEM.get(resourceId.toIdentifier());
            if (itemHolder.isEmpty()) {
                continue;
            }

            Item filterItem = itemHolder.get().value();
            if (stack.getItem() == filterItem) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get all configured filter items.
     * @param ctx Pipe context
     * @return Array of filter item IDs (empty strings for unused slots)
     */
    public String[] getFilters(PipeContext ctx) {
        CompoundTag filters = ctx.getCompoundTag(this, FILTERS);
        String[] result = new String[MAX_FILTER_SLOTS];

        for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
            String key = String.valueOf(i);
            result[i] = NbtCompat.getString(filters, key, "");
        }

        return result;
    }

    /**
     * Set a filter slot.
     * @param ctx Pipe context
     * @param slotIndex Filter slot index (0-8)
     * @param itemId Item resource ID (empty string to clear slot)
     */
    public void setFilter(PipeContext ctx, int slotIndex, String itemId) {
        if (slotIndex < 0 || slotIndex >= MAX_FILTER_SLOTS) {
            throw new IllegalArgumentException("Slot must be 0-" + (MAX_FILTER_SLOTS - 1));
        }

        CompoundTag filters = ctx.getCompoundTag(this, FILTERS);

        if (itemId.isEmpty()) {
            filters.remove(String.valueOf(slotIndex));
        } else {
            filters.putString(String.valueOf(slotIndex), itemId);
        }

        if (!filters.isEmpty()) {
            ctx.putCompoundTag(this, FILTERS, filters);
        } else {
            ctx.remove(this, FILTERS);
        }

        ctx.markDirtyAndSync();
    }

    /**
     * Get default route setting.
     * @param ctx Pipe context
     * @return true if this sink accepts items without destinations (default route)
     */
    public boolean isDefaultRoute(PipeContext ctx) {
        return ctx.getInt(this, DEFAULT_ROUTE, 0) == 1;
    }

    /**
     * Set default route setting and register/unregister with network.
     * @param ctx Pipe context
     * @param enabled true to accept items without destinations
     */
    public void setDefaultRoute(PipeContext ctx, boolean enabled) {
        ctx.saveInt(this, DEFAULT_ROUTE, enabled ? 1 : 0);
        ctx.markDirtyAndSync();

        if (!ctx.world().isClientSide()) {
            ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
            if (network != null) {
                if (enabled) {
                    network.registerDefaultRouteSink(ctx.pos(), 0);
                    LOGGER.debug("[Sink @ {}] Default route enabled and registered with network", ctx.pos());
                } else {
                    network.unregisterDefaultRouteSink(ctx.pos());
                    LOGGER.debug("[Sink @ {}] Default route disabled and unregistered from network", ctx.pos());
                }
            }
        }
    }

    @Nullable
    private Direction getSinkDirection(PipeContext ctx) {
        return DirectionSerializer.load(ctx, this, SINK_DIRECTION);
    }

    private void setSinkDirection(PipeContext ctx, @Nullable Direction direction) {
        DirectionSerializer.save(ctx, this, SINK_DIRECTION, direction);
    }

    private boolean isSinkFace(PipeContext ctx, Direction direction) {
        return getSinkDirection(ctx) == direction;
    }
}
