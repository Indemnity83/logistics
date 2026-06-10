package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.RoutingModule;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.*;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.serialization.DirectionSerializer;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.ui.ModSinkScreenHandler;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Mod-Based Item Sink module — routes items from the network into an adjacent inventory,
 * filtering by the mod (namespace) of the item.
 *
 * <p>Configuration: up to 10 mod filter entries shown in a scrollable list.
 * <p>GUI: Accessible with wrench. Each filter entry stores the representative item ID;
 * the mod namespace is derived from it for matching.
 */
public class ModSinkModule implements Module, TickingModule, RoutingModule, ItemAcceptingModule {
    public static final String MOD_IDS = "mod_ids";
    public static final int MAX_FILTERS = 10;
    private static final String SINK_DIRECTION = "sink_direction";
    private static final String TICKS_SINCE_SYNC = "ticks_since_sync";
    private static final int SYNC_INTERVAL = 20;

    private final int priority;

    public ModSinkModule(int priority) {
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
        registerWithNetwork(ctx);
    }

    @Override
    public void onDetach(PipeContext ctx) {
        if (!ctx.world().isClientSide()) {
            ILogisticsNetwork network = ctx.network();
            if (network != null) network.unregisterSink(ctx.pos());
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
        if (!ctx.world().isClientSide()) {
            registerWithNetwork(ctx);
        }
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        Level world = ctx.world();
        BlockPos pos = ctx.pos();
        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, inventory, p) -> new ModSinkScreenHandler(
                        syncId, inventory,
                        world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null),
                Component.translatable("screen.logistics.mod_sink")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult openItemConfig(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ModSinkScreenHandler(syncId, inv, player, hand),
                Component.translatable("screen.logistics.mod_sink")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        Direction sinkDir = getSinkDirection(ctx);
        if (sinkDir == null || !options.contains(sinkDir)) return RoutePlan.pass();

        if (matchesFilter(ctx, item.getStack())) {
            NetDbg.out("[ModSink @ {}] Item {} matched mod filter, routing to inventory ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
            return RoutePlan.reroute(sinkDir);
        }

        if (item.getDestination() != null && item.getDestination().equals(ctx.pos())) {
            NetDbg.out("[ModSink @ {}] Item {} arrived at destination, routing to inventory ({})",
                    ctx.pos(), item.getStack().getItem(), sinkDir);
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

    @Override
    public boolean acceptsItem(PipeContext ctx, ItemStack stack) {
        return matchesFilter(ctx, stack);
    }

    /**
     * Check if the item's mod namespace matches any stored filter.
     *
     * @param ctx   Pipe context
     * @param stack Item to check
     * @return true if the item's mod matches a filter entry
     */
    public boolean matchesFilter(PipeContext ctx, ItemStack stack) {
        String travelingNamespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        for (int i = 0; i < MAX_FILTERS; i++) {
            String storedItemId = ctx.getString(this, MOD_IDS + "_" + i, "");
            if (storedItemId.isEmpty()) continue;
            ResourceId rid = ResourceId.tryParse(storedItemId);
            if (rid == null) continue;
            if (rid.getNamespace().equals(travelingNamespace)) return true;
        }
        return false;
    }

    /**
     * Get all stored filter item IDs (including empty strings for unused slots).
     *
     * @param ctx Pipe context
     * @return Array of item ID strings, length MAX_FILTERS
     */
    public String[] getFilterItemIds(PipeContext ctx) {
        String[] result = new String[MAX_FILTERS];
        for (int i = 0; i < MAX_FILTERS; i++) {
            result[i] = ctx.getString(this, MOD_IDS + "_" + i, "");
        }
        return result;
    }

    @Override
    public void appendHud(PipeContext ctx, PipeHud hud) {
        // Stored as example item ids; the accepted "mod" is each id's namespace.
        LinkedHashSet<String> mods = new LinkedHashSet<>();
        for (String itemId : getFilterItemIds(ctx)) {
            if (itemId == null || itemId.isEmpty()) {
                continue;
            }
            int colon = itemId.indexOf(':');
            mods.add(colon > 0 ? itemId.substring(0, colon) : itemId);
        }
        if (!mods.isEmpty()) {
            hud.line(ModuleHud.summary("jade.logistics.pipe.mods", Component.literal(String.join(", ", mods))));
        }
    }

    /**
     * Add a mod filter by full item ID. No-op if the mod namespace is already filtered
     * or if the filter list is full.
     *
     * @param ctx    Pipe context
     * @param itemId Full item resource ID (e.g., "create:iron_sheet")
     */
    public void addModId(PipeContext ctx, String itemId) {
        ResourceId rid = ResourceId.tryParse(itemId);
        if (rid == null) return;
        String namespace = rid.getNamespace();

        for (int i = 0; i < MAX_FILTERS; i++) {
            String existing = ctx.getString(this, MOD_IDS + "_" + i, "");
            if (existing.isEmpty()) continue;
            ResourceId existRid = ResourceId.tryParse(existing);
            if (existRid != null && existRid.getNamespace().equals(namespace)) return;
        }

        for (int i = 0; i < MAX_FILTERS; i++) {
            if (ctx.getString(this, MOD_IDS + "_" + i, "").isEmpty()) {
                ctx.saveString(this, MOD_IDS + "_" + i, itemId);
                ctx.markDirtyAndSync();
                if (!ctx.world().isClientSide()) registerWithNetwork(ctx);
                return;
            }
        }
    }

    /**
     * Remove a mod filter by 0-based index, compacting remaining entries.
     *
     * @param ctx   Pipe context
     * @param index 0-based index of the filter to remove
     */
    public void removeModId(PipeContext ctx, int index) {
        List<String> current = new ArrayList<>();
        for (int i = 0; i < MAX_FILTERS; i++) {
            current.add(ctx.getString(this, MOD_IDS + "_" + i, ""));
        }
        if (index < 0 || index >= current.size()) return;
        current.remove(index);
        for (int i = 0; i < MAX_FILTERS; i++) {
            String value = i < current.size() ? current.get(i) : "";
            ctx.saveString(this, MOD_IDS + "_" + i, value);
        }
        ctx.markDirtyAndSync();
        if (!ctx.world().isClientSide()) registerWithNetwork(ctx);
    }

    private void registerWithNetwork(PipeContext ctx) {
        ILogisticsNetwork network = ctx.network();
        if (network != null) {
            network.registerSink(ctx.pos(), priority);
            network.registerGenericSinkInterest(ctx.pos());
        }
    }

    @Nullable
    private Direction getSinkDirection(PipeContext ctx) {
        return DirectionSerializer.load(ctx, this, SINK_DIRECTION);
    }

    private void setSinkDirection(PipeContext ctx, @Nullable Direction direction) {
        DirectionSerializer.save(ctx, this, SINK_DIRECTION, direction);
    }
}
