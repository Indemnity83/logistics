package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.core.lib.pipe.RoutingModule;

import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.network.StrayClaim;
import com.logistics.core.lib.pipe.Deliveries;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Module that enables network-aware routing for pipes.
 * Routes items with explicit destinations using A* pathfinding through the pipe network.
 * For items without destinations, attempts to find a suitable sink in the network, and failing that
 * a live standing order ({@link ILogisticsNetwork#claimStrayDelivery}) — suppliers and requesters
 * order but never register as sinks, so the order book is the only place they can be found.
 * Drops items only when neither lookup produces a reachable destination.
 *
 * <p>Add this module to pipes that should participate in smart routing (Provider, Requester, Routing pipes).
 * Regular transport pipes (copper, iron, etc.) should NOT have this module.
 */
public class NetworkRouterModule implements Module, RoutingModule {
    // Accelerate aggressively so items reach ITEM_NETWORK_SPEED within one pipe segment.
    // ITEM_NETWORK_SPEED (0.2) - ITEM_MIN_SPEED (0.02) = 0.18 over ~5 ticks → 0.036/tick.
    private static final float NETWORK_ACCELERATION = 0.04f;
    // Energy cost to assign a destination for an unrouted item.
    private static final long RF_PER_ROUTE = 2;
    // Power-status tint: green = powered network link, red = anything else (unpowered, inventory, machine).
    // Package-private so the arm-tint test can assert against them without duplicating the literals.
    static final int ARM_TINT_POWERED = 0x00FF00;
    static final int ARM_TINT_UNPOWERED = 0xFF0000;

    @Override
    public float getAcceleration(PipeContext ctx) {
        return NETWORK_ACCELERATION;
    }

    /**
     * Tints this smart pipe's arms by status: green when the arm links into a powered network (a
     * battery or another pipe carrying power), red otherwise (unpowered, or a non-power endpoint such
     * as an inventory, chest, or machine like the laser quarry). Driven by the per-arm mask the server
     * computes and syncs ({@link com.logistics.core.lib.pipe.IPipeAccess#getPoweredArmMask}).
     */
    @Override
    public Integer getArmTint(PipeContext ctx, Direction direction) {
        boolean powered = (ctx.pipeAccess().getPoweredArmMask() & (1 << direction.get3DDataValue())) != 0;
        return powered ? ARM_TINT_POWERED : ARM_TINT_UNPOWERED;
    }

    /**
     * Tints this smart pipe's core by power status: green when the pipe is linked into a powered
     * network (it has at least one powered power-link arm), red otherwise. Reuses the per-arm mask
     * the server syncs — any set bit means this pipe reaches power through a battery or pipe link.
     */
    @Override
    public Integer getCoreTint(PipeContext ctx) {
        return ctx.pipeAccess().getPoweredArmMask() != 0 ? ARM_TINT_POWERED : ARM_TINT_UNPOWERED;
    }

    @Override
    public float getMaxSpeed(PipeContext ctx) {
        return LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_INJECT_SPEED);
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (ctx.world().isClientSide()) return RoutePlan.pass();

        ILogisticsNetwork network = ctx.network();
        if (network == null) return RoutePlan.pass();

        if (item.getDestination() == null) {
            if (!network.consumeEnergy(RF_PER_ROUTE)) return RoutePlan.drop();
            if (!assignDestination(ctx, network, item, null)) {
                NetDbg.out("[NetworkRouter @ {}] No sink or order found for {}, dropping",
                        ctx.pos(), item.getStack().getItem());
                return RoutePlan.drop();
            }
        }

        if (item.getDestination().equals(ctx.pos())) return RoutePlan.pass();

        Direction nextHop = network.getNextHop(ctx.pos(), item.getDestination());
        if (nextHop != null && options.contains(nextHop)) {
            NetDbg.out("[NetworkRouter @ {}] Routing {} → {} via {}", ctx.pos(), item.getStack().getItem(), item.getDestination(), nextHop);
            return RoutePlan.reroute(nextHop);
        }

        // The destination became unreachable from here (a split, or an arm that no longer leads
        // anywhere). Hand the delivery back and look for somewhere else that wants this stack before
        // giving up on it — dropping is the last resort, not the first answer.
        BlockPos unreachable = item.getDestination();
        NetDbg.out("[NetworkRouter @ {}] No valid hop for {} → {} (nextHop={}, options={}), re-homing",
                ctx.pos(), item.getStack().getItem(), unreachable, nextHop, options);
        Deliveries.abandon(network, item);
        if (assignDestination(ctx, network, item, unreachable)) {
            Direction retry = network.getNextHop(ctx.pos(), item.getDestination());
            if (retry != null && options.contains(retry)) return RoutePlan.reroute(retry);
            Deliveries.abandon(network, item);
        }
        return RoutePlan.drop();
    }

    /**
     * Give an unrouted item a destination: a sink if one accepts it, otherwise a live standing order.
     *
     * <p>Sinks come first, so nothing about ordinary routing changes — the order book is consulted
     * only for a stack that would otherwise be dropped. That second lookup is what reaches suppliers
     * and requesters, which place orders but never register as sinks and so are invisible to
     * {@code findSinkFor}. A claimed order is already committed in the network's books by the time it
     * comes back, so the delivery id travels with the item and its TTL starts over.
     *
     * @param exclude a destination that just proved unreachable, or {@code null} for a fresh item;
     *                non-null also means the sink lookup is skipped, since sinks were already tried
     *                when this destination was first assigned
     * @return {@code true} if the item now carries a destination
     */
    private static boolean assignDestination(
            PipeContext ctx, ILogisticsNetwork network, TravelingItem item, @Nullable BlockPos exclude) {
        if (exclude == null) {
            BlockPos sink = network.findSinkFor(item.getStack(), ctx.pos());
            if (sink != null) {
                item.setDestination(sink);
                NetDbg.out("[NetworkRouter @ {}] Assigned destination {} for {}",
                        ctx.pos(), sink, item.getStack().getItem());
                return true;
            }
        }

        StrayClaim claim = network.claimStrayDelivery(item.getStack(), ctx.pos(), exclude);
        if (claim == null) return false;

        item.setDestination(claim.destination());
        item.setDeliveryId(claim.deliveryId());
        item.resetTtl();
        NetDbg.out("[NetworkRouter @ {}] Re-homed {} onto order {} → {}",
                ctx.pos(), item.getStack().getItem(),
                claim.deliveryId().toString().substring(0, 8), claim.destination());
        return true;
    }

}
