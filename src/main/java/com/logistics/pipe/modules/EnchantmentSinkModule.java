package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.DirectionSerializer;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.network.ILogisticsNetwork;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Enchantment sink module — routes any enchanted item into an adjacent inventory.
 * Accepts items with applied enchantments ({@link DataComponents#ENCHANTMENTS}) as well as
 * enchanted books ({@link DataComponents#STORED_ENCHANTMENTS}). No configuration needed.
 */
public class EnchantmentSinkModule implements Module {
    private static final String SINK_DIRECTION = "sink_direction";
    private final int priority;

    public EnchantmentSinkModule(int priority) {
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
                network.registerGenericSinkInterest(ctx.pos()); // enchantment check is dynamic
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
        if (matchesItem(item.getStack())) {
            return RoutePlan.reroute(sinkDir);
        }
        return RoutePlan.pass();
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (getSinkDirection(ctx) != direction) return null;
        String suffix = ctx.isInventoryConnection(direction) ? "_arm_extended" : "_arm";
        return LogisticsPipe.model("basic_logistics_pipe" + suffix);
    }

    public boolean matchesItem(ItemStack stack) {
        var enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) return true;
        var stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        return stored != null && !stored.isEmpty();
    }

    @Nullable
    private Direction getSinkDirection(PipeContext ctx) {
        return DirectionSerializer.load(ctx, this, SINK_DIRECTION);
    }

    private void setSinkDirection(PipeContext ctx, @Nullable Direction direction) {
        DirectionSerializer.save(ctx, this, SINK_DIRECTION, direction);
    }
}
