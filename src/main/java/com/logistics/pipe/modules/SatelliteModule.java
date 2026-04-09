package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;

import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.SatelliteScreenHandler;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Satellite pipe module — registers a named insertion endpoint so ProcessModule can route
 * machine inputs to specific faces/sides.
 *
 * <p>The satellite pipe is placed adjacent to a machine face. It registers a logical ID
 * (e.g. "furnace_input") with the network. ProcessModule resolves that ID to this pipe's
 * block position when placing input orders.
 *
 * <p>When items destined for this pipe arrive, the module redirects them into the
 * adjacent machine inventory.
 */
public class SatelliteModule implements Module, TickingModule, RoutingModule {
    public static final String KEY_ID = "satellite_id";
    private static final String TICKS_KEY = "sync_ticks";
    private static final String SINK_DIR_KEY = "sink_dir";
    private static final int SYNC_INTERVAL = 20;

    @Override
    public void onTick(PipeContext ctx) {
        int ticks = ctx.getInt(this, TICKS_KEY, 0) + 1;
        ctx.saveInt(this, TICKS_KEY, ticks);
        if (ticks < SYNC_INTERVAL) return;

        ctx.saveInt(this, TICKS_KEY, 0);
        String id = ctx.getString(this, KEY_ID, "");
        if (!id.isEmpty()) {
            var network = ctx.network();
            if (network != null) {
                NetDbg.out("[Satellite @ {}] Registered as satellite '{}' with network", ctx.pos(), id);
                network.registerSatellite(id, ctx.pos());
            }
        }
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> options) {
        List<Direction> inventoryFaces = ctx.getInventoryConnections();
        if (inventoryFaces.isEmpty()) {
            ctx.remove(this, SINK_DIR_KEY);
        } else {
            // Prefer the current direction if still valid; otherwise take the first
            int current = ctx.getInt(this, SINK_DIR_KEY, -1);
            boolean currentStillValid = current >= 0
                    && inventoryFaces.contains(Direction.from3DDataValue(current));
            if (!currentStillValid) {
                ctx.saveInt(this, SINK_DIR_KEY, inventoryFaces.getFirst().get3DDataValue());
            }
        }
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        // Route items whose destination is this satellite pipe into the adjacent machine
        if (item.getDestination() != null && item.getDestination().equals(ctx.pos())) {
            int dirVal = ctx.getInt(this, SINK_DIR_KEY, -1);
            if (dirVal >= 0) {
                Direction sinkDir = Direction.from3DDataValue(dirVal);
                if (options.contains(sinkDir)) {
                    NetDbg.out("[Satellite @ {}] Routing {} → {}", ctx.pos(), item.getStack().getItem(), sinkDir);
                    return RoutePlan.reroute(sinkDir);
                }
            }
            // Fallback: try any inventory connection
            List<Direction> inventoryFaces = ctx.getInventoryConnections();
            for (Direction d : inventoryFaces) {
                if (options.contains(d)) {
                    NetDbg.out("[Satellite @ {}] Routing {} → {} (fallback)", ctx.pos(), item.getStack().getItem(), d);
                    return RoutePlan.reroute(d);
                }
            }
            NetDbg.out("[Satellite @ {}] No sink direction, dropping {}", ctx.pos(), item.getStack().getItem());
        }
        return RoutePlan.pass();
    }

    @Override
    public void onDetach(PipeContext ctx) {
        String id = ctx.getString(this, KEY_ID, "");
        if (!id.isEmpty()) {
            var network = ctx.network();
            if (network != null) {
                NetDbg.out("[Satellite @ {}] Unregistered satellite '{}' from network", ctx.pos(), id);
                network.unregisterSatellite(id, ctx.pos());
            }
        }
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        Level world = ctx.world();
        var pos = ctx.pos();
        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new SatelliteScreenHandler(
                        syncId, inv,
                        world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null),
                Component.translatable("screen.logistics.satellite")));
        return InteractionResult.SUCCESS;
    }

    public String getSatelliteId(PipeContext ctx) {
        return ctx.getString(this, KEY_ID, "");
    }

    public int getSatelliteIdInt(PipeContext ctx) {
        String s = ctx.getString(this, KEY_ID, "");
        if (s.isEmpty()) return 0;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    public void setSatelliteId(PipeContext ctx, int id) {
        setSatelliteId(ctx, id > 0 ? String.valueOf(id) : "");
    }

    public void setSatelliteId(PipeContext ctx, String id) {
        String trimmed = id == null ? "" : id.trim();
        // Unregister old ID first
        String oldId = ctx.getString(this, KEY_ID, "");
        if (!oldId.isEmpty()) {
            var network = ctx.network();
            if (network != null) {
                NetDbg.out("[Satellite @ {}] Unregistered satellite '{}' from network", ctx.pos(), oldId);
                network.unregisterSatellite(oldId, ctx.pos());
            }
        }
        ctx.saveString(this, KEY_ID, trimmed);
        if (!trimmed.isEmpty()) {
            var network = ctx.network();
            if (network != null) {
                NetDbg.out("[Satellite @ {}] Registered as satellite '{}' with network", ctx.pos(), trimmed);
                network.registerSatellite(trimmed, ctx.pos());
            }
        }
        ctx.markDirtyAndSync();
    }
}
