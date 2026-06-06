package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;
import com.logistics.core.LogisticsConfig;

import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * Module that enables network-aware routing for pipes.
 * Routes items with explicit destinations using A* pathfinding through the pipe network.
 * For items without destinations, attempts to find a suitable sink in the network.
 * Drops items if no destination can be found.
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
        boolean powered = (ctx.blockEntity().getPoweredArmMask() & (1 << direction.get3DDataValue())) != 0;
        return powered ? ARM_TINT_POWERED : ARM_TINT_UNPOWERED;
    }

    /**
     * Tints this smart pipe's core by power status: green when the pipe is linked into a powered
     * network (it has at least one powered power-link arm), red otherwise. Reuses the per-arm mask
     * the server syncs — any set bit means this pipe reaches power through a battery or pipe link.
     */
    @Override
    public Integer getCoreTint(PipeContext ctx) {
        return ctx.blockEntity().getPoweredArmMask() != 0 ? ARM_TINT_POWERED : ARM_TINT_UNPOWERED;
    }

    @Override
    public float getMaxSpeed(PipeContext ctx) {
        return LogisticsConfig.get().pipe.injectSpeed;
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        if (ctx.world().isClientSide()) return RoutePlan.pass();

        ILogisticsNetwork network = ctx.network();
        if (network == null) return RoutePlan.pass();

        if (item.getDestination() == null) {
            if (!network.consumeEnergy(RF_PER_ROUTE)) return RoutePlan.drop();
            BlockPos destination = network.findSinkFor(item.getStack());
            if (destination == null) {
                NetDbg.out("[NetworkRouter @ {}] No sink found for {}, dropping", ctx.pos(), item.getStack().getItem());
                return RoutePlan.drop();
            }
            item.setDestination(destination);
            NetDbg.out("[NetworkRouter @ {}] Assigned destination {} for {}", ctx.pos(), destination, item.getStack().getItem());
        }

        if (item.getDestination().equals(ctx.pos())) return RoutePlan.pass();

        Direction nextHop = network.getNextHop(ctx.pos(), item.getDestination());
        if (nextHop != null && options.contains(nextHop)) {
            NetDbg.out("[NetworkRouter @ {}] Routing {} → {} via {}", ctx.pos(), item.getStack().getItem(), item.getDestination(), nextHop);
            return RoutePlan.reroute(nextHop);
        }

        NetDbg.out("[NetworkRouter @ {}] No valid hop for {} → {} (nextHop={}, options={}), dropping", ctx.pos(), item.getStack().getItem(), item.getDestination(), nextHop, options);
        return RoutePlan.drop();
    }

}
