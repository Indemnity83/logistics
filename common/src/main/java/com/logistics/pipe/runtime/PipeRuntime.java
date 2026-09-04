package com.logistics.pipe.runtime;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.network.NetDbg;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.pipe.ItemPipe;
import com.logistics.pipe.modules.NetworkRouterModule;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.block.entity.PipeItemStorage;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PipeRuntime {
    private PipeRuntime() {}

    /**
     * Holds pipe configuration and context for the current tick.
     */
    private record TickContext(
            Level world,
            BlockPos pos,
            BlockState state,
            PipeBlockEntity blockEntity,
            @Nullable ItemPipe pipe,
            @Nullable PipeContext pipeContext,
            float maxSpeed,
            float accelerationRate,
            float dragCoefficient) {

        static TickContext create(Level world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
            float maxSpeed = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MAX_SPEED);
            float accelerationRate = 0f;
            float dragCoefficient = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_DRAG);
            ItemPipe pipe = null;
            PipeContext pipeContext = null;

            if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
                pipe = pipeBlock.getPipe();
                pipeContext = new PipeContext(world, pos, state, blockEntity);
                maxSpeed = pipe.getMaxSpeed(pipeContext);
                accelerationRate = pipe.getAccelerationRate(pipeContext);
                dragCoefficient = pipe.getDrag(pipeContext);
            }

            return new TickContext(
                    world, pos, state, blockEntity, pipe, pipeContext, maxSpeed, accelerationRate, dragCoefficient);
        }

        boolean hasPipe() {
            return pipe != null && pipeContext != null;
        }

        boolean isClient() {
            return world.isClientSide();
        }

        boolean isServer() {
            return !world.isClientSide();
        }
    }

    /**
     * Tracks items to add/remove during tick processing.
     */
    private static final class ItemTickState {
        final List<TravelingItem> toRoute = new ArrayList<>();
        final List<TravelingItem> toRemove = new ArrayList<>();
        final List<TravelingItem> toDiscard = new ArrayList<>();
        final List<TravelingItem> toReplace = new ArrayList<>();
        final List<TravelingItem> toAdd = new ArrayList<>();
        boolean needsSync = false;

        void markForRouting(TravelingItem item) {
            toRoute.add(item);
        }

        void markForRemoval(TravelingItem item) {
            toRemove.add(item);
        }

        void markForDiscard(TravelingItem item) {
            toDiscard.add(item);
            toRoute.remove(item);
            needsSync = true;
        }

        void markForReplacement(TravelingItem item, List<TravelingItem> replacements) {
            toReplace.add(item);
            toRoute.remove(item);
            toAdd.addAll(replacements);
            needsSync = true;
        }

        void markNeedsSync() {
            needsSync = true;
        }
    }

    /**
     * Main tick handler for pipe block entities. Processes item movement, routing decisions,
     * and synchronization between client and server.
     *
     * <p>Items make routing decisions when crossing the pipe center (ROUTE_POINT), allowing
     * modules to influence direction before the item commits to an exit. Final delivery to
     * adjacent inventories or pipes happens when items reach SERVER_EXIT_THRESHOLD.
     */
    public static void tick(Level world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        TickContext ctx = TickContext.create(world, pos, state, blockEntity);
        ItemTickState itemState = new ItemTickState();

        // Update connection cache and notify modules of topology changes
        if (ctx.hasPipe()) {
            updateConnections(ctx, itemState);
            updatePoweredArmMask(ctx, itemState);
            ctx.pipe().onTick(ctx.pipeContext());
        }

        // Process all traveling items
        processItems(ctx, itemState);

        // Apply item list changes
        applyItemChanges(ctx, itemState);

        // Sync to clients if needed
        syncIfNeeded(ctx, itemState);

        // Route completed items (server only)
        transferCompletedItems(ctx, itemState);
    }

    private static void updateConnections(TickContext ctx, ItemTickState itemState) {
        if (!ctx.blockEntity().isConnectionCacheDirty()) {
            return;
        }

        updateConnectionCache(ctx);
        if (handleConnectionChanges(ctx)) itemState.markNeedsSync();
    }

    private static void updateConnectionCache(TickContext ctx) {
        if (ctx.state().getBlock() instanceof PipeBlock pipeBlock) {
            for (Direction direction : Direction.values()) {
                PipeConnection.Type type = pipeBlock.getDynamicConnectionType(ctx.world(), ctx.pos(), direction);
                ctx.blockEntity().setConnectionType(direction, type);
            }
            ctx.blockEntity().markConnectionCacheClean();
        }
    }

    /**
     * Recompute the per-arm power-status mask for smart (logistics) pipes and sync it to clients
     * when it changes. An arm is "powered" (green) when the pipe's network has stored energy AND the
     * arm links into that network — a battery ({@code POWER}) or another actual pipe block, since
     * power flows network-wide (including through transport pipes that bridge logistics pipes).
     * Blocks that merely speak the {@code PIPE} connection type for item I/O (e.g. the laser quarry)
     * are deliberately excluded — they are item endpoints, not power links. Computed every server
     * tick because power can change (battery draining) without any topology change.
     */
    private static void updatePoweredArmMask(TickContext ctx, ItemTickState itemState) {
        if (!ctx.isServer()) return;
        // Only logistics (smart) pipes carry a power indicator; others never tint their arms.
        if (ctx.pipe().getModule(NetworkRouterModule.class, ctx.blockEntity()) == null) return;

        int mask = computePoweredArmMask(ctx);
        if (mask != ctx.blockEntity().getPoweredArmMask()) {
            ctx.blockEntity().setPoweredArmMask(mask);
            itemState.markNeedsSync();
        }
    }

    /** Build the per-arm powered bitmask, or {@code 0} when the pipe's network has no stored power. */
    private static int computePoweredArmMask(TickContext ctx) {
        ILogisticsNetwork network = ctx.blockEntity().getNetwork();
        if (network == null || !network.isPowered()) return 0;

        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (isArmPowered(ctx, dir)) {
                mask |= (1 << dir.get3DDataValue());
            }
        }
        return mask;
    }

    /**
     * True if the arm toward {@code dir} links into the (powered) network — a battery
     * ({@code POWER}) or another actual pipe block. Inventory/endpoint connections (e.g. the laser
     * quarry, which speaks {@code PIPE} only for item I/O) are not power links and stay unpowered.
     */
    private static boolean isArmPowered(TickContext ctx, Direction dir) {
        return switch (ctx.blockEntity().getCachedConnectionType(dir)) {
            case POWER -> true;
            case PIPE -> isPipeNeighbor(ctx.world(), ctx.pos().relative(dir));
            default -> false;
        };
    }

    /** True if the block at {@code pos} is an actual pipe (so power flows through it). */
    private static boolean isPipeNeighbor(Level world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof PipeBlock;
    }

    private static boolean handleConnectionChanges(TickContext ctx) {
        List<Direction> connected = getAllConnectedDirections(ctx.world(), ctx.pos(), ctx.state());
        int mask = 0;
        for (Direction d : connected) {
            mask |= (1 << d.get3DDataValue());
        }

        if (mask != ctx.blockEntity().getLastConnectionsMask()) {
            ctx.blockEntity().setLastConnectionsMask(mask);
            ctx.pipe().onConnectionsChanged(ctx.pipeContext(), connected);
            return ctx.isServer();
        }
        return false;
    }

    private static void processItems(TickContext ctx, ItemTickState itemState) {
        for (TravelingItem item : ctx.blockEntity().getTravelingItems()) {
            processItem(ctx, item, itemState);
        }
    }

    private static void processItem(TickContext ctx, TravelingItem item, ItemTickState itemState) {
        float progressBefore = item.getProgress();

        // Advance item progress
        if (item.tick(ctx.accelerationRate(), ctx.dragCoefficient(), ctx.maxSpeed())) {
            itemState.markForRouting(item);
        }

        // Handle routing decision at pipe center
        if (progressBefore < TravelingItem.ROUTE_POINT
                && item.getProgress() >= TravelingItem.ROUTE_POINT
                && !item.isRouted()) {
            routeItem(ctx, item, itemState);
        }

        // Client-side removal buffer (prevents flicker during handoff)
        if (ctx.isClient() && item.getProgress() > TravelingItem.CLIENT_EXIT_THRESHOLD) {
            itemState.markForRemoval(item);
        }
    }

    /**
     * Determine and execute the routing decision for an item.
     *
     * <p>Called when an item crosses the pipe center (ROUTE_POINT). The item's exit direction
     * is determined and locked in, allowing rendering to show the item moving toward its
     * destination during the second half of travel, and ensuring client/server agreement
     * on routing using deterministic randomness.
     */
    private static void routeItem(TickContext ctx, TravelingItem item, ItemTickState itemState) {
        // TTL expiry: if the item has been traveling too long, release its destination so it
        // falls back to default routing rather than being stuck forever.
        if (ctx.isServer() && item.isExpired() && item.getDestination() != null) {
            item.setDestination(null);
            item.setDeliveryId(null); // release ref; orderedForRequester accounting is best-effort
        }
        RoutePlan plan = resolveRoutePlan(ctx, item);
        executeRoutePlan(ctx, item, plan, itemState);
    }

    /**
     * Consult modules for a routing plan, or fall back to default routing.
     *
     * <p>Always returns an executable plan. Normalizes edge cases:
     * empty REROUTE → DROP, empty SPLIT → DISCARD.
     */
    private static RoutePlan resolveRoutePlan(TickContext ctx, TravelingItem item) {
        List<Direction> validDirections = getValidDirections(ctx.world(), ctx.pos(), ctx.state(), item.getDirection());
        RoutePlan defaultPlan = PipeRoutingPlanner.defaultPlan(validDirections);

        if (!ctx.hasPipe()) {
            return defaultPlan;
        }

        RoutePlan customPlan = ctx.pipe().route(ctx.pipeContext(), item, validDirections);
        return PipeRoutingPlanner.normalize(customPlan, defaultPlan);
    }

    private static void executeRoutePlan(TickContext ctx, TravelingItem item, RoutePlan plan, ItemTickState itemState) {
        switch (plan.getType()) {
            case DROP -> dropItem(ctx, item, itemState);
            case DISCARD -> discardItem(ctx, item, itemState);
            case REROUTE -> rerouteItem(ctx, item, plan, itemState);
            case SPLIT -> splitItem(ctx, item, plan, itemState);
            default -> {} // PASS should have been converted in resolveRoutePlan
        }
    }

    private static void dropItem(TickContext ctx, TravelingItem item, ItemTickState itemState) {
        if (ctx.isServer()) {
            if (hasArrived(item, ctx.pos())) {
                // Dropping at the recorded destination fulfills the delivery.
                notifyDelivered(ctx.world(), ctx.pos(), item, item.getStack().getCount());
            } else {
                notifyDeliveryFailed(ctx.world(), ctx.pos(), item, item.getStack().getCount());
            }
            PipeBlockEntity.dropItem(ctx.world(), ctx.pos(), item);
        }
        itemState.markForDiscard(item);
    }

    /** {@code true} once the item has reached the destination its order was placed for. */
    private static boolean hasArrived(TravelingItem item, BlockPos pos) {
        return item.getDestination() != null && item.getDestination().equals(pos);
    }

    private static void discardItem(TickContext ctx, TravelingItem item, ItemTickState itemState) {
        if (ctx.isServer()) {
            notifyDeliveryFailed(ctx.world(), ctx.pos(), item, item.getStack().getCount());
        }
        itemState.markForDiscard(item);
    }

    private static void rerouteItem(TickContext ctx, TravelingItem item, RoutePlan plan, ItemTickState itemState) {
        List<Direction> candidates = plan.getDirections();

        Direction chosen = candidates.size() == 1
                ? candidates.getFirst()
                : PipeRoutingPlanner.chooseDirection(
                        ctx.pos(), ctx.world().getGameTime(), item.getDirection(), candidates);

        item.setDirection(chosen);
        item.setRouted(true);

        if (ctx.isServer()) {
            itemState.markNeedsSync();
        }
    }

    private static void splitItem(TickContext ctx, TravelingItem item, RoutePlan plan, ItemTickState itemState) {
        List<TravelingItem> routed = plan.getItems();

        // Single item returned (no actual split)
        if (routed.size() == 1 && routed.getFirst() == item) {
            item.setRouted(true);
            if (ctx.isServer()) {
                itemState.markNeedsSync();
            }
            return;
        }

        // Actual split - replace original with new items
        List<TravelingItem> replacements = new ArrayList<>();
        for (TravelingItem routedItem : routed) {
            if (routedItem != item) {
                routedItem.setProgress(item.getProgress());
                routedItem.setSpeed(item.getSpeed());
                routedItem.setRouted(true);
            }
            replacements.add(routedItem);
        }
        itemState.markForReplacement(item, replacements);
    }

    /**
     * Apply pending item list modifications after processing.
     *
     * <p>Client and server handle removals differently: the server removes items at
     * SERVER_EXIT_THRESHOLD for routing, while the client keeps them slightly longer
     * (until CLIENT_EXIT_THRESHOLD) to prevent visual flicker during the handoff to the next pipe.
     */
    private static void applyItemChanges(TickContext ctx, ItemTickState itemState) {
        List<TravelingItem> items = ctx.blockEntity().getTravelingItems();

        if (ctx.isClient()) {
            items.removeAll(itemState.toRemove);
            items.removeAll(itemState.toDiscard);
            items.removeAll(itemState.toReplace);
            items.addAll(itemState.toAdd);
        } else {
            items.removeAll(itemState.toRoute);
            items.removeAll(itemState.toDiscard);
            items.removeAll(itemState.toReplace);
            items.addAll(itemState.toAdd);
        }
    }

    private static void syncIfNeeded(TickContext ctx, ItemTickState itemState) {
        if (ctx.isServer() && itemState.needsSync) {
            ctx.blockEntity().setChanged();
            ctx.world().sendBlockUpdated(ctx.pos(), ctx.state(), ctx.state(), 3);
        }
    }

    private static void transferCompletedItems(TickContext ctx, ItemTickState itemState) {
        if (ctx.isClient()) {
            return;
        }

        boolean hasChanges =
                !itemState.toRoute.isEmpty() || !itemState.toDiscard.isEmpty() || !itemState.toAdd.isEmpty();

        if (!hasChanges) {
            return;
        }

        for (TravelingItem item : itemState.toRoute) {
            transferItem(ctx, item);
        }

        ctx.blockEntity().setChanged();
        ctx.world().sendBlockUpdated(ctx.pos(), ctx.state(), ctx.state(), 3);
    }

    /**
     * Transfer an item to the next pipe or inventory at the end of this segment.
     * Direction was already determined at the pipe center (ROUTE_POINT).
     *
     * <p>For non-pipe storages, modules are consulted first via
     * {@link com.logistics.pipe.Pipe#handleTransfer} before the default generic insertion.
     * This allows modules like {@link com.logistics.pipe.modules.CraftingModule} to perform
     * slot-specific insertion rather than relying on the storage's default ordering.
     */
    private static void transferItem(TickContext ctx, TravelingItem item) {
        Level world = ctx.world();
        BlockPos pos = ctx.pos();
        Direction direction = item.getDirection();
        BlockPos targetPos = pos.relative(direction);

        // Pipe-to-pipe: bypass the capability system entirely and hand off directly.
        // This preserves TravelingItem metadata (speed, delivery ID, destination).
        if (world.getBlockEntity(targetPos) instanceof PipeBlockEntity targetPipe) {
            PipeItemStorage pipeStorage = targetPipe.getItemStorage(direction.getOpposite());
            if (pipeStorage != null) {
                long inserted = pipeStorage.insertTravelingItem(item);
                if (unacceptedCount(item, inserted) > 0) {
                    // Only the accepted portion continues in the target pipe; the source item is
                    // shrunk to what was left over and that remainder is reported and dropped.
                    item.getStack().shrink((int) inserted);
                    notifyDropAndDrop(world, pos, item);
                }
            } else {
                notifyDropAndDrop(world, pos, item);
            }
            return;
        }

        // Non-pipe target: use the loader-agnostic storage lookup.
        IItemStorage storage = ItemStorageLookup.find(world, targetPos, direction.getOpposite());

        if (storage != null) {
            // Give modules a chance to handle insertion (e.g., slot-specific insertion into an autocrafter).
            if (ctx.hasPipe()) {
                TravelingItem remaining = ctx.pipe().handleTransfer(ctx.pipeContext(), item, direction);
                if (remaining == null) return; // module fully handled it
                item = remaining;
            }

            long inserted = storage.insert(ItemStorageLookup.of(item.getStack()), item.getStack().getCount(), false);

            if (inserted > 0) {
                // Notify delivery when item fully enters a real inventory.
                // Skip on partial insertion — orderedForRequester accounting is best-effort.
                if (item.getDeliveryId() != null
                        && item.getDestination() != null
                        && inserted == item.getStack().getCount()) {
                    PipeNetwork network = NetworkRegistry.getNetwork(world, pos);
                    if (network != null) {
                        network.notifyDelivery(
                                item.getDeliveryId(),
                                item.getDestination(),
                                ItemStorageLookup.of(item.getStack()),
                                inserted);
                    } else {
                        NetDbg.out("[PipeRuntime] notifyDelivery skipped at {}: no network found for {} ({})",
                                pos, item.getStack().getItem(), item.getDeliveryId());
                    }
                }
                if (inserted < item.getStack().getCount()) {
                    long failed = item.getStack().getCount() - inserted;
                    if (item.getDeliveryId() != null && item.getDestination() != null) {
                        PipeNetwork network = NetworkRegistry.getNetwork(world, pos);
                        if (network != null) {
                            network.notifyDelivery(
                                    item.getDeliveryId(),
                                    item.getDestination(),
                                    ItemStorageLookup.of(item.getStack()),
                                    inserted);
                            network.notifyDeliveryFailed(
                                    item.getDeliveryId(),
                                    item.getDestination(),
                                    ItemStorageLookup.of(item.getStack()),
                                    failed);
                        }
                    }
                    item.getStack().shrink((int) inserted);
                    PipeBlockEntity.dropItem(world, pos, item);
                }
                return;
            }
        }

        // No item storage, but the target may expose FLUID storage (a tank or a fluid-only machine like
        // the magmatic engine). Give modules first refusal — the fluid supplier drains the packet into its
        // buffer here — before dropping. Return null = fully handled; the module calls notifyDelivery itself.
        if (ctx.hasPipe() && FluidStorageLookup.find(world, targetPos, direction.getOpposite()) != null) {
            TravelingItem remaining = ctx.pipe().handleTransfer(ctx.pipeContext(), item, direction);
            if (remaining == null) return;
            item = remaining;
        }

        // Item could not enter any storage — drop it.
        notifyDropAndDrop(world, pos, item);
    }

    /**
     * How much of {@code item} a hand-off left behind, given the {@code inserted} count the target
     * reported. The source pipe discards the whole traveling item after a hand-off, so anything the
     * target did not take must be accounted for here or it is destroyed.
     */
    static long unacceptedCount(TravelingItem item, long inserted) {
        return Math.max(0, item.getStack().getCount() - Math.max(0, inserted));
    }

    private static void notifyDropAndDrop(Level world, BlockPos pos, TravelingItem item) {
        notifyDeliveryFailed(world, pos, item, item.getStack().getCount());
        PipeBlockEntity.dropItem(world, pos, item);
    }

    private static void notifyDeliveryFailed(Level world, BlockPos pos, TravelingItem item, long amount) {
        if (item.getDeliveryId() == null || item.getDestination() == null || amount <= 0) {
            return;
        }
        PipeNetwork network = NetworkRegistry.getNetwork(world, pos);
        if (network != null) {
            network.notifyDeliveryFailed(
                    item.getDeliveryId(),
                    item.getDestination(),
                    ItemStorageLookup.of(item.getStack()),
                    amount);
        }
    }

    /** Marks an order fulfilled without the item actually entering an inventory (dropped as loot at its destination). */
    private static void notifyDelivered(Level world, BlockPos pos, TravelingItem item, long amount) {
        if (item.getDeliveryId() == null || item.getDestination() == null || amount <= 0) {
            return;
        }
        PipeNetwork network = NetworkRegistry.getNetwork(world, pos);
        if (network != null) {
            network.notifyDelivery(
                    item.getDeliveryId(),
                    item.getDestination(),
                    ItemStorageLookup.of(item.getStack()),
                    amount);
        }
    }

    private static List<Direction> getValidDirections(
            Level world, BlockPos pos, BlockState state, Direction currentDirection) {
        List<Direction> validDirections = new ArrayList<>();
        Direction oppositeDirection = currentDirection.getOpposite();

        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return validDirections;
        }

        for (Direction direction : Direction.values()) {
            if (direction == oppositeDirection) {
                continue;
            }

            PipeConnection.Type type = pipeBlock.getConnectionType(world, pos, direction);
            // POWER connections (batteries) are rendered as arms but never carry items.
            if (type != PipeConnection.Type.NONE && type != PipeConnection.Type.POWER) {
                validDirections.add(direction);
            }
        }

        return validDirections;
    }

    private static List<Direction> getAllConnectedDirections(Level world, BlockPos pos, BlockState state) {
        List<Direction> connected = new ArrayList<>();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return connected;
        }

        for (Direction direction : Direction.values()) {
            PipeConnection.Type type = pipeBlock.getConnectionType(world, pos, direction);
            if (type != PipeConnection.Type.NONE) {
                connected.add(direction);
            }
        }
        return connected;
    }

}
