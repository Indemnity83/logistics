package com.logistics.pipe.runtime;

import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.block.entity.PipeItemStorage;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class PipeRuntime {
    private PipeRuntime() {}

    /**
     * Holds pipe configuration and context for the current tick.
     */
    private record TickContext(
            World world,
            BlockPos pos,
            BlockState state,
            PipeBlockEntity blockEntity,
            @Nullable Pipe pipe,
            @Nullable PipeContext pipeContext,
            float maxSpeed,
            float accelerationRate,
            float dragCoefficient) {

        static TickContext create(World world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
            float maxSpeed = PipeConfig.PIPE_MAX_SPEED;
            float accelerationRate = 0f;
            float dragCoefficient = PipeConfig.DRAG_COEFFICIENT;
            Pipe pipe = null;
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
            return world.isClient();
        }

        boolean isServer() {
            return !world.isClient();
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
     * <p>Items make routing decisions when crossing the pipe center (0.5 progress), allowing
     * modules to influence direction before the item commits to an exit. Final delivery to
     * adjacent inventories or pipes happens when items reach progress 1.0.
     */
    public static void tick(World world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        TickContext ctx = TickContext.create(world, pos, state, blockEntity);
        ItemTickState itemState = new ItemTickState();

        // Update connection cache and notify modules of topology changes
        if (ctx.hasPipe()) {
            updateConnectionCache(ctx);
            if (handleConnectionChanges(ctx)) {
                itemState.markNeedsSync();
            }
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

    private static void updateConnectionCache(TickContext ctx) {
        if (ctx.state().getBlock() instanceof PipeBlock pipeBlock) {
            for (Direction direction : Direction.values()) {
                PipeBlock.ConnectionType type = pipeBlock.getDynamicConnectionType(ctx.world(), ctx.pos(), direction);
                ctx.blockEntity().setConnectionType(direction, type);
            }
        }
    }

    private static boolean handleConnectionChanges(TickContext ctx) {
        List<Direction> connected = getAllConnectedDirections(ctx.world(), ctx.pos(), ctx.state());
        int mask = 0;
        for (Direction d : connected) {
            mask |= (1 << d.getIndex());
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
        if (progressBefore < 0.5f && item.getProgress() >= 0.5f && !item.isRouted()) {
            routeItem(ctx, item, itemState);
        }

        // Client-side removal buffer (prevents flicker during handoff)
        if (ctx.isClient() && item.getProgress() > 1.3f) {
            itemState.markForRemoval(item);
        }
    }

    /**
     * Determine and execute the routing decision for an item.
     *
     * <p>Called when an item crosses the pipe center (0.5 progress). The item's exit direction
     * is determined and locked in, allowing rendering to show the item moving toward its
     * destination during the second half of travel, and ensuring client/server agreement
     * on routing using deterministic randomness.
     */
    private static void routeItem(TickContext ctx, TravelingItem item, ItemTickState itemState) {
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
        RoutePlan defaultPlan = validDirections.isEmpty() ? RoutePlan.drop() : RoutePlan.reroute(validDirections);

        if (!ctx.hasPipe()) {
            return defaultPlan;
        }

        RoutePlan customPlan = ctx.pipe().route(ctx.pipeContext(), item, validDirections);

        customPlan = switch (customPlan.getType()) {
            case REROUTE -> customPlan.getDirections().isEmpty() ? RoutePlan.drop() : customPlan;
            case SPLIT -> customPlan.getItems().isEmpty() ? RoutePlan.discard() : customPlan;
            default -> customPlan;
        };

        return customPlan.getType() == RoutePlan.Type.PASS ? defaultPlan : customPlan;
    }

    private static void executeRoutePlan(TickContext ctx, TravelingItem item, RoutePlan plan, ItemTickState itemState) {
        switch (plan.getType()) {
            case DROP -> dropItem(ctx, item, itemState);
            case DISCARD -> discardItem(item, itemState);
            case REROUTE -> rerouteItem(ctx, item, plan, itemState);
            case SPLIT -> splitItem(ctx, item, plan, itemState);
            default -> {} // PASS should have been converted in resolveRoutePlan
        }
    }

    private static void dropItem(TickContext ctx, TravelingItem item, ItemTickState itemState) {
        if (ctx.isServer()) {
            PipeBlockEntity.dropItem(ctx.world(), ctx.pos(), item);
        }
        itemState.markForDiscard(item);
    }

    private static void discardItem(TravelingItem item, ItemTickState itemState) {
        itemState.markForDiscard(item);
    }

    private static void rerouteItem(TickContext ctx, TravelingItem item, RoutePlan plan, ItemTickState itemState) {
        List<Direction> candidates = plan.getDirections();

        Direction chosen = candidates.size() == 1
                ? candidates.getFirst()
                : chooseRandomDirection(ctx.world(), ctx.pos(), item.getDirection(), candidates);

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
     * <p>Client and server handle removals differently: the server removes items at progress 1.0
     * for routing, while the client keeps them slightly longer (until 1.3) to prevent visual
     * flicker during the handoff to the next pipe.
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
            ctx.blockEntity().markDirty();
            ctx.world().updateListeners(ctx.pos(), ctx.state(), ctx.state(), 3);
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
            transferItem(ctx.world(), ctx.pos(), item);
        }

        ctx.blockEntity().markDirty();
        ctx.world().updateListeners(ctx.pos(), ctx.state(), ctx.state(), 3);
    }

    /**
     * Transfer an item to the next pipe or inventory at the end of this segment.
     * Direction was already determined at the pipe center (0.5 progress).
     */
    private static void transferItem(World world, BlockPos pos, TravelingItem item) {
        Direction direction = item.getDirection();
        BlockPos targetPos = pos.offset(direction);

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, targetPos, direction.getOpposite());
        if (storage != null) {
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted;
                if (storage instanceof PipeItemStorage pipeStorage) {
                    inserted = pipeStorage.insert(item, transaction);
                } else {
                    ItemVariant variant = ItemVariant.of(item.getStack());
                    inserted = storage.insert(variant, item.getStack().getCount(), transaction);
                }

                if (inserted > 0) {
                    transaction.commit();
                    if (inserted < item.getStack().getCount()) {
                        item.getStack().decrement((int) inserted);
                        PipeBlockEntity.dropItem(world, pos, item);
                    }
                    return;
                }
            }
        }

        PipeBlockEntity.dropItem(world, pos, item);
    }

    private static List<Direction> getValidDirections(
            World world, BlockPos pos, BlockState state, Direction currentDirection) {
        List<Direction> validDirections = new ArrayList<>();
        Direction oppositeDirection = currentDirection.getOpposite();

        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return validDirections;
        }

        for (Direction direction : Direction.values()) {
            if (direction == oppositeDirection) {
                continue;
            }

            PipeBlock.ConnectionType type = pipeBlock.getConnectionType(world, pos, direction);
            if (type != PipeBlock.ConnectionType.NONE) {
                validDirections.add(direction);
            }
        }

        return validDirections;
    }

    private static Direction chooseRandomDirection(
            World world, BlockPos pos, Direction currentDirection, List<Direction> options) {
        long seed = mixHash(pos.asLong(), world.getTime(), currentDirection.getIndex());
        java.util.Random random = new java.util.Random(seed);
        return options.get(random.nextInt(options.size()));
    }

    private static List<Direction> getAllConnectedDirections(World world, BlockPos pos, BlockState state) {
        List<Direction> connected = new ArrayList<>();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return connected;
        }

        for (Direction direction : Direction.values()) {
            PipeBlock.ConnectionType type = pipeBlock.getConnectionType(world, pos, direction);
            if (type != PipeBlock.ConnectionType.NONE) {
                connected.add(direction);
            }
        }
        return connected;
    }

    private static long mixHash(long a, long b, long c) {
        long hash = a;
        hash = hash * 31 + b;
        hash = hash * 31 + c;

        hash ^= (hash >>> 33);
        hash *= 0xff51afd7ed558ccdL;
        hash ^= (hash >>> 33);
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= (hash >>> 33);

        return hash;
    }
}
