package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.*;
import com.logistics.core.lib.pipe.Module;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.DirectionSerializer;
import com.logistics.core.lib.storage.FilterSlots;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.SinkScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sink module - routes items from the network into an adjacent inventory.
 * Can filter specific items OR accept any item without a destination (default route).
 *
 * <p>Configuration: 9 filter slots + "Default Route" toggle
 * <p>GUI: Accessible with wrench
 *
 * <p>This is the Basic Logistics Pipe from LogisticsPipes - pulls items out of the network.
 */
public class SinkModule implements Module, TickingModule, RoutingModule, ItemAcceptingModule {
    public static final String FILTERS = "filters";
    public static final String DEFAULT_ROUTE = "default_route";
    private static final String SINK_DIRECTION = "sink_direction";
    private static final String TICKS_SINCE_SYNC = "ticks_since_sync";
    private static final int SYNC_INTERVAL = 20; // Re-register with network every second to recover from splits
    public static final int MAX_FILTER_SLOTS = 9;

    private final int priority;

    public SinkModule(int priority) {
        this.priority = priority;
    }

    @Override
    public void onTick(PipeContext ctx) {
        int ticks = ctx.getInt(this, TICKS_SINCE_SYNC, 0) + 1;
        if (ticks < SYNC_INTERVAL) {
            ctx.saveInt(this, TICKS_SINCE_SYNC, ticks);
            return;
        }
        ctx.saveInt(this, TICKS_SINCE_SYNC, 0);

        // Re-register with the network periodically to recover after network splits/merges.
        // setDefaultRoute() handles immediate registration on change; this is just the recovery path.
        ILogisticsNetwork network = ctx.network();
        if (network != null) {
            // Default-route sinks use priority 0 so filtered sinks (ModSink, etc.) can outbid them.
            int effectivePriority = isDefaultRoute(ctx) ? 0 : priority;
            network.registerSink(ctx.pos(), effectivePriority);
            // Rebuild specific-item interests from current filter slots
            network.unregisterSinkInterests(ctx.pos());
            for (String itemId : getFilters(ctx).asList()) {
                if (itemId.isEmpty()) continue;
                ResourceId rid = ResourceId.tryParse(itemId);
                if (rid == null) continue;
                BuiltInRegistries.ITEM.get(rid.toIdentifier()).ifPresent(holder ->
                        network.registerSinkInterest(ctx.pos(), holder.value()));
            }
            if (isDefaultRoute(ctx)) {
                network.registerGenericSinkInterest(ctx.pos());
            }
        }
    }

    @Override
    public void onDetach(PipeContext ctx) {
        ILogisticsNetwork network = ctx.network();
        if (network != null) {
            network.unregisterSink(ctx.pos()); // also clears both interest maps
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
    public InteractionResult openItemConfig(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new SinkScreenHandler(syncId, inv, player, hand),
                Component.translatable("screen.logistics.sink.requested_items")));
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
            NetDbg.out("[Sink @ {}] Item {} matches filter, routing to inventory ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
            return RoutePlan.reroute(sinkDir);
        }

        // Priority 2: Destination match
        if (item.getDestination() != null && item.getDestination().equals(ctx.pos())) {
            NetDbg.out("[Sink @ {}] Item {} arrived at destination, routing to inventory ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
            return RoutePlan.reroute(sinkDir);
        }

        // Priority 3: Default route (only if no other routing options)
        if (isDefaultRoute(ctx) && item.getDestination() == null) {
            boolean hasOtherOptions = options.stream().anyMatch(dir -> !dir.equals(sinkDir));
            if (!hasOtherOptions) {
                NetDbg.out("[Sink @ {}] Default route accepting {} (no other options), routing to inventory ({})",
                        ctx.pos(), item.getStack().getItem(), sinkDir);
                return RoutePlan.reroute(sinkDir);
            }
            NetDbg.out("[Sink @ {}] Default route passing on {} (other directions available)",
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
        for (String itemId : getFilters(ctx).asList()) {
            if (itemId.isEmpty()) continue;
            ResourceId resourceId = ResourceId.tryParse(itemId);
            if (resourceId == null) continue;
            var itemHolder = BuiltInRegistries.ITEM.get(resourceId.toIdentifier());
            if (itemHolder.isEmpty()) continue;
            if (stack.getItem() == itemHolder.get().value()) return true;
        }
        return false;
    }

    /**
     * Get all configured filter slots (including empty slots).
     * @param ctx Pipe context
     * @return FilterSlots with one entry per slot; empty slots hold {@code ""}
     */
    public FilterSlots getFilters(PipeContext ctx) {
        return FilterSlots.load(ctx.getCompoundTag(this, FILTERS), MAX_FILTER_SLOTS);
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
        FilterSlots updated = getFilters(ctx).with(slotIndex, itemId);
        if (updated.isEmpty()) {
            ctx.remove(this, FILTERS);
        } else {
            ctx.putCompoundTag(this, FILTERS, updated.toTag());
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

    @Override
    public boolean acceptsItem(PipeContext ctx, ItemStack stack) {
        return matchesFilter(ctx, stack) || isDefaultRoute(ctx);
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
            ILogisticsNetwork network = ctx.network();
            if (network != null) {
                if (enabled) {
                    network.registerSink(ctx.pos(), 0);
                    network.registerGenericSinkInterest(ctx.pos());
                    NetDbg.out("[Sink @ {}] Default route enabled and registered with network", ctx.pos());
                } else {
                    network.unregisterSink(ctx.pos());
                    network.unregisterGenericSinkInterest(ctx.pos());
                    NetDbg.out("[Sink @ {}] Default route disabled and unregistered from network", ctx.pos());
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
