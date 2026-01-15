package com.logistics.pipe.runtime;

import com.logistics.block.PipeBlock;
import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.block.entity.PipeItemStorage;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class PipeRuntime {
    private PipeRuntime() {}

    public static void tick(World world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        // Get pipe's speed bounds and acceleration/drag behavior
        float maxSpeed = PipeConfig.PIPE_MAX_SPEED;
        float accelerationRate = 0f;
        float dragCoefficient = PipeConfig.DRAG_COEFFICIENT;
        Pipe pipe = null;
        PipeContext pipeContext = null;

        if (state.getBlock() instanceof PipeBlock pipeBlock) {
            if (pipeBlock.getPipe() != null) {
                pipe = pipeBlock.getPipe();
                pipeContext = new PipeContext(world, pos, state, blockEntity);
                maxSpeed = pipe.getMaxSpeed(pipeContext);
                accelerationRate = pipe.getAccelerationRate(pipeContext);
                dragCoefficient = pipe.getDrag(pipeContext);
            }
        }

        // Notify modules when the pipe's connection topology changes.
        // This is intentionally based on blockstate connections (not per-item options) so modules can reset state
        // deterministically when neighbors are added/removed.
        boolean needsSync = false;
        if (pipe != null && pipeContext != null) {
            // Cache connection types in block entity for rendering
            if (state.getBlock() instanceof PipeBlock pipeBlock) {
                for (Direction direction : Direction.values()) {
                    PipeBlock.ConnectionType type = pipeBlock.getDynamicConnectionType(world, pos, direction);
                    blockEntity.setConnectionType(direction, type);
                }
            }

            List<Direction> connected = getAllConnectedDirections(world, pos, state);
            int mask = 0;
            for (Direction d : connected) {
                mask |= (1 << d.getIndex());
            }

            if (mask != blockEntity.getLastConnectionsMask()) {
                blockEntity.setLastConnectionsMask(mask);
                pipe.onConnectionsChanged(pipeContext, connected);
                if (!world.isClient()) {
                    needsSync = true;
                }
            }

            pipe.onTick(pipeContext);
        }

        // Tick all traveling items (both client and server for smooth rendering)
        List<TravelingItem> itemsToRoute = new ArrayList<>();
        List<TravelingItem> itemsToRemove = new ArrayList<>();
        List<TravelingItem> itemsToDiscard = new ArrayList<>();
        List<TravelingItem> itemsToReplace = new ArrayList<>();
        List<TravelingItem> itemsToAdd = new ArrayList<>();

        for (TravelingItem item : blockEntity.getTravelingItems()) {
            float progressBefore = item.getProgress();

            if (item.tick(accelerationRate, dragCoefficient, maxSpeed)) {
                // Item reached the end of this pipe segment
                itemsToRoute.add(item);
            }

            // Determine exit direction when item reaches pipe center (both client and server)
            // Using deterministic random ensures client and server make the same choice
            if (progressBefore < 0.5f && item.getProgress() >= 0.5f) {
                if (pipe != null && pipeContext != null && !item.isRouted()) {
                    List<Direction> validDirections = getValidDirections(world, pos, state, item.getDirection());
                    if (validDirections.isEmpty()) {
                        if (pipe.discardWhenNoRoute(pipeContext)) {
                            itemsToDiscard.add(item);
                        } else if (!world.isClient()) {
                            PipeBlockEntity.dropItem(world, pos, item);
                        }
                        itemsToRoute.remove(item);
                        needsSync = true;
                        continue;
                    }
                    RoutePlan plan = pipe.route(pipeContext, item, validDirections);
                    RoutePlan.Type type = plan.getType();
                    if (type == RoutePlan.Type.PASS) {
                        plan = RoutePlan.reroute(validDirections);
                        type = RoutePlan.Type.REROUTE;
                    }
                    if (type == RoutePlan.Type.DROP) {
                        if (!world.isClient()) {
                            PipeBlockEntity.dropItem(world, pos, item);
                        }
                        itemsToDiscard.add(item);
                        itemsToRoute.remove(item);
                        needsSync = true;
                        continue;
                    } else if (type == RoutePlan.Type.DISCARD) {
                        itemsToDiscard.add(item);
                        itemsToRoute.remove(item);
                        needsSync = true;
                        continue;
                    } else if (type == RoutePlan.Type.REROUTE) {
                        List<Direction> candidates = plan.getDirections();
                        if (candidates.isEmpty()) {
                            if (!world.isClient()) {
                                PipeBlockEntity.dropItem(world, pos, item);
                            }
                            itemsToDiscard.add(item);
                            itemsToRoute.remove(item);
                            needsSync = true;
                            continue;
                        }
                        Direction chosen = candidates.size() == 1
                            ? candidates.getFirst()
                            : chooseRandomDirection(pipeContext, item.getDirection(), candidates);
                        item.setDirection(chosen);
                        item.setRouted(true);
                        if (!world.isClient()) {
                            needsSync = true;
                        }
                    } else if (type == RoutePlan.Type.SPLIT) {
                        List<TravelingItem> routed = plan.getItems();
                        if (routed.isEmpty()) {
                            itemsToDiscard.add(item);
                            itemsToRoute.remove(item);
                            needsSync = true;
                            continue;
                        }

                        if (routed.size() == 1 && routed.get(0) == item) {
                            item.setRouted(true);
                            if (!world.isClient()) {
                                needsSync = true;
                            }
                        } else {
                            itemsToReplace.add(item);
                            itemsToRoute.remove(item);
                            for (TravelingItem routedItem : routed) {
                                if (routedItem != item) {
                                    routedItem.setProgress(item.getProgress());
                                    routedItem.setSpeed(item.getSpeed());
                                    routedItem.setRouted(true);
                                }
                                itemsToAdd.add(routedItem);
                            }
                            needsSync = true;
                            continue;
                        }
                    }
                } else if (pipe == null) {
                    Direction newDirection = chooseDirection(world, pos, state, item.getDirection());
                    if (newDirection != null && newDirection != item.getDirection()) {
                        item.setDirection(newDirection);
                        if (!world.isClient()) {
                            needsSync = true;
                        }
                    }
                }
            }

            // On client, keep rendering items a bit longer to prevent flicker during handoff
            // On server, remove immediately for routing
            if (world.isClient()) {
                // Remove when progress exceeds 1.3 (gives ~6 frames buffer for server sync)
                if (item.getProgress() > 1.3f) {
                    itemsToRemove.add(item);
                }
            }
        }

        // Remove items based on client/server logic
        if (world.isClient()) {
            // Client: only remove items that have exceeded the buffer
            blockEntity.getTravelingItems().removeAll(itemsToRemove);
            blockEntity.getTravelingItems().removeAll(itemsToDiscard);
            blockEntity.getTravelingItems().removeAll(itemsToReplace);
            blockEntity.getTravelingItems().addAll(itemsToAdd);
        } else {
            // Server: remove items that reached the end (for routing)
            blockEntity.getTravelingItems().removeAll(itemsToRoute);
            blockEntity.getTravelingItems().removeAll(itemsToDiscard);
            blockEntity.getTravelingItems().removeAll(itemsToReplace);
            blockEntity.getTravelingItems().addAll(itemsToAdd);
        }

        // Sync direction changes to clients
        if (!world.isClient() && needsSync) {
            blockEntity.markDirty();
            world.updateListeners(pos, state, state, 3);
        }

        // Only handle routing on server side
        if (!world.isClient() && (!itemsToRoute.isEmpty() || !itemsToDiscard.isEmpty() || !itemsToAdd.isEmpty())) {
            for (TravelingItem item : itemsToRoute) {
                routeItem(world, pos, state, blockEntity, item);
            }

            blockEntity.markDirty();
            // Sync to clients when items are removed/added
            world.updateListeners(pos, state, state, 3);
        }
    }

    /**
     * Route an item that has reached the end of this pipe segment.
     * Direction has already been determined at the pipe center (0.5 progress).
     */
    private static void routeItem(World world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity,
                                  TravelingItem item) {
        // Use the item's current direction (already set at 0.5 progress)
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
                    // Item was fully or partially inserted
                    if (inserted < item.getStack().getCount()) {
                        // Partial insertion - drop remainder
                        item.getStack().decrement((int) inserted);
                        PipeBlockEntity.dropItem(world, pos, item);
                    }
                    return;
                }
            }
        }

        // Routing failed - drop item
        PipeBlockEntity.dropItem(world, pos, item);
    }

    private static List<Direction> getValidDirections(World world, BlockPos pos, BlockState state, Direction currentDirection) {
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

    private static Direction chooseRandomDirection(PipeContext ctx, Direction currentDirection, List<Direction> options) {
        long seed = mixHash(ctx.pos().asLong(), ctx.world().getTime(), currentDirection.getIndex());
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

    /**
     * Choose a random direction for an item to travel.
     * Never routes back the way it came.
     */
    private static Direction chooseDirection(World world, BlockPos pos, BlockState state, Direction currentDirection) {
        List<Direction> validDirections = getValidDirections(world, pos, state, currentDirection);
        if (validDirections.isEmpty()) {
            return null;
        }

        if (validDirections.size() == 1) {
            return validDirections.getFirst();
        }

        long seed = mixHash(pos.asLong(), world.getTime(), currentDirection.getIndex());
        java.util.Random random = new java.util.Random(seed);
        return validDirections.get(random.nextInt(validDirections.size()));
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
