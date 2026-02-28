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
import com.logistics.pipe.ui.SinkScreenHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
    private static final String FILTERS = "filters"; // NBT key for 9 filter items
    private static final String DEFAULT_ROUTE = "default_route"; // Accept any item without destination
    private static final String SINK_DIRECTION = "sink_direction"; // Connected inventory face
    public static final int MAX_FILTER_SLOTS = 9;

    @Override
    public void onTick(PipeContext ctx) {
        // Ensure this sink is registered/unregistered with the network based on default route setting
        if (ctx.world().isClientSide()) {
            return;
        }

        ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
        if (network != null && isDefaultRoute(ctx)) {
            // Register as default route sink
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

        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        net.minecraft.world.level.Level world = ctx.world();
        net.minecraft.core.BlockPos pos = ctx.pos();
        serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (syncId, inventory, playerEntity) -> {
                    PipeBlockEntity pipeEntity =
                            world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null;
                    return new SinkScreenHandler(syncId, inventory, pipeEntity);
                },
                net.minecraft.network.chat.Component.translatable("screen.logistics.sink.requested_items")));
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

        // Check if item matches any filter (HIGHEST PRIORITY)
        if (matchesFilter(ctx, item.getStack())) {
            LOGGER.debug("[Sink @ {}] Item {} matches filter, routing to inventory ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
            return RoutePlan.reroute(sinkDir);
        }

        // Check if item's destination is this sink (network routed it here)
        if (item.getDestination() != null && item.getDestination().equals(ctx.pos())) {
            LOGGER.info("[Sink @ {}] Item {} arrived at destination, routing to inventory ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
            return RoutePlan.reroute(sinkDir);
        }

        // Check if this is a default route sink (FALLBACK - lowest priority)
        if (isDefaultRoute(ctx)) {
            // Only accept items without a destination if there are NO other routing options
            // (i.e., this is the last chance before the item would be dropped)
            if (item.getDestination() == null) {
                // Check if there are other directions besides the sink direction
                long otherDirections = options.stream()
                        .filter(dir -> !dir.equals(sinkDir))
                        .count();

                // Only accept if this is the last option (no other pipes to try)
                if (otherDirections == 0) {
                    LOGGER.info("[Sink @ {}] Default route accepting {} (no other options), routing to inventory ({})",
                            ctx.pos(), item.getStack().getItem(), sinkDir);
                    return RoutePlan.reroute(sinkDir);
                } else {
                    LOGGER.debug("[Sink @ {}] Default route passing on {} ({} other directions available)",
                            ctx.pos(), item.getStack().getItem(), otherDirections);
                    return RoutePlan.pass();
                }
            }
        }

        return RoutePlan.pass();
    }

    /**
     * Check if item matches any configured filter.
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

            // Parse item from ID
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
     */
    public boolean isDefaultRoute(PipeContext ctx) {
        return ctx.getInt(this, DEFAULT_ROUTE, 0) == 1;
    }

    /**
     * Set default route setting.
     */
    public void setDefaultRoute(PipeContext ctx, boolean enabled) {
        ctx.saveInt(this, DEFAULT_ROUTE, enabled ? 1 : 0);
        ctx.markDirtyAndSync();

        // Register/unregister with network
        if (!ctx.world().isClientSide()) {
            ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
            if (network != null) {
                if (enabled) {
                    network.registerDefaultRouteSink(ctx.pos(), 0);
                    LOGGER.info("[Sink @ {}] Default route enabled and registered with network", ctx.pos());
                } else {
                    network.unregisterDefaultRouteSink(ctx.pos());
                    LOGGER.info("[Sink @ {}] Default route disabled and unregistered from network", ctx.pos());
                }
            }
        }
    }

    @Nullable
    private Direction getSinkDirection(PipeContext ctx) {
        return com.logistics.core.lib.storage.DirectionSerializer.load(ctx, this, SINK_DIRECTION);
    }

    private void setSinkDirection(PipeContext ctx, @Nullable Direction direction) {
        com.logistics.core.lib.storage.DirectionSerializer.save(ctx, this, SINK_DIRECTION, direction);
    }

    private boolean isSinkFace(PipeContext ctx, Direction direction) {
        return getSinkDirection(ctx) == direction;
    }
}
