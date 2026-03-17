package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.ItemAcceptingModule;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.DirectionSerializer;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.pipe.network.NetDbg;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Polymorphic Sink module - dynamically routes items into an adjacent inventory
 * only if that inventory already contains at least one of that item type.
 *
 * <p>No static filter configuration - the inventory contents are checked live at routing time.
 * <p>No GUI needed - no wrench interaction.
 */
public class PolymorphicSinkModule implements Module, RoutingModule, ItemAcceptingModule {
    private static final String SINK_DIRECTION = "sink_direction";
    private final int priority;

    public PolymorphicSinkModule(int priority) {
        this.priority = priority;
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

        if (!ctx.world().isClientSide()) {
            ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
            if (network != null) {
                network.registerSink(ctx.pos(), priority);
                network.registerGenericSinkInterest(ctx.pos()); // inventory check is dynamic
            }
        }
    }

    @Override
    public void onDetach(PipeContext ctx) {
        if (!ctx.world().isClientSide()) {
            ILogisticsNetwork network = NetworkRegistry.getNetwork(ctx.world(), ctx.pos());
            if (network != null) network.unregisterSink(ctx.pos()); // also clears interests
        }
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        Direction sinkDir = getSinkDirection(ctx);
        if (sinkDir == null || !options.contains(sinkDir)) {
            return RoutePlan.pass();
        }

        // Priority 1: Dynamic filter match (inventory already contains this item type)
        if (matchesFilter(ctx, item.getStack())) {
            NetDbg.out("[PolymorphicSink @ {}] Item {} matched inventory contents, routing to ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
            return RoutePlan.reroute(sinkDir);
        }

        // Priority 2: Destination match
        if (item.getDestination() != null && item.getDestination().equals(ctx.pos())) {
            NetDbg.out("[PolymorphicSink @ {}] Item {} arrived at destination, routing to inventory ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
            return RoutePlan.reroute(sinkDir);
        }

        return RoutePlan.pass();
    }

    /**
     * Check if the adjacent inventory already contains at least one of this item type.
     *
     * @param ctx   Pipe context
     * @param stack Item to check
     * @return true if the adjacent inventory contains ≥1 of this item type
     */
    public boolean matchesFilter(PipeContext ctx, ItemStack stack) {
        Direction sinkDir = getSinkDirection(ctx);
        if (sinkDir == null) {
            return false;
        }

        BlockPos targetPos = ctx.pos().relative(sinkDir);
        var storage = ItemStorage.SIDED.find(ctx.world(), targetPos, sinkDir.getOpposite());
        if (storage == null) {
            return false;
        }

        for (StorageView<ItemVariant> view : storage) {
            if (!view.getResource().isBlank() && view.getAmount() > 0
                    && view.getResource().toStack().getItem() == stack.getItem()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (getSinkDirection(ctx) != direction) {
            return null;
        }
        String suffix = ctx.isInventoryConnection(direction) ? "_arm_extended" : "_arm";
        return LogisticsPipe.model("basic_logistics_pipe" + suffix);
    }

    @Override
    public boolean acceptsItem(PipeContext ctx, ItemStack stack) {
        return matchesFilter(ctx, stack);
    }

    @Nullable
    private Direction getSinkDirection(PipeContext ctx) {
        return DirectionSerializer.load(ctx, this, SINK_DIRECTION);
    }

    private void setSinkDirection(PipeContext ctx, @Nullable Direction direction) {
        DirectionSerializer.save(ctx, this, SINK_DIRECTION, direction);
    }
}
