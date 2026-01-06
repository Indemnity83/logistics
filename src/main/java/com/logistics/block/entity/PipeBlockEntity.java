package com.logistics.block.entity;

import com.logistics.LogisticsMod;
import com.logistics.block.PipeBlock;
import com.logistics.item.TravelingItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PipeBlockEntity extends BlockEntity {
    private final List<TravelingItem> travelingItems = new ArrayList<>();

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsBlockEntities.PIPE_BLOCK_ENTITY, pos, state);
    }

    /**
     * Add an item to this pipe
     */
    public void addItem(TravelingItem item) {
        travelingItems.add(item);
        markDirty();
        // Sync to clients
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    /**
     * Get all traveling items (for rendering)
     */
    public List<TravelingItem> getTravelingItems() {
        return travelingItems;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        // Save traveling items
        NbtList itemsList = new NbtList();
        for (TravelingItem item : travelingItems) {
            itemsList.add(item.writeNbt(new NbtCompound(), registryLookup));
        }
        nbt.put("TravelingItems", itemsList);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        // Load traveling items
        travelingItems.clear();
        NbtList itemsList = nbt.getList("TravelingItems", 10); // 10 = compound type
        for (int i = 0; i < itemsList.size(); i++) {
            travelingItems.add(TravelingItem.fromNbt(itemsList.getCompound(i), registryLookup));
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    public static void tick(net.minecraft.world.World world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        // Get pipe's target speed, acceleration rate, and whether it can accelerate
        float targetSpeed = PipeBlock.BASE_PIPE_SPEED;
        float accelerationRate = PipeBlock.ACCELERATION_RATE;
        boolean canAccelerate = false;

        if (state.getBlock() instanceof PipeBlock pipeBlock) {
            targetSpeed = pipeBlock.getPipeSpeed(world, pos, state);
            accelerationRate = pipeBlock.getAccelerationRate();
            canAccelerate = pipeBlock.canAccelerate(world, pos, state);
        }

        // Tick all traveling items (both client and server for smooth rendering)
        List<TravelingItem> itemsToRoute = new ArrayList<>();
        List<TravelingItem> itemsToRemove = new ArrayList<>();
        boolean needsSync = false;

        for (TravelingItem item : blockEntity.travelingItems) {
            float progressBefore = item.getProgress();

            if (item.tick(targetSpeed, accelerationRate, canAccelerate)) {
                // Item reached the end of this pipe segment
                itemsToRoute.add(item);
            }

            // Determine exit direction when item reaches pipe center (both client and server)
            // Using deterministic random ensures client and server make the same choice
            if (progressBefore < 0.5f && item.getProgress() >= 0.5f) {
                Direction newDirection = blockEntity.chooseDirection(world, pos, state, item.getDirection());
                if (newDirection != null && newDirection != item.getDirection()) {
                    // Update direction without resetting progress
                    item.setDirectionOnly(newDirection);
                    // Server still syncs to ensure consistency
                    if (!world.isClient) {
                        needsSync = true;
                    }
                }
            }

            // On client, keep rendering items a bit longer to prevent flicker during handoff
            // On server, remove immediately for routing
            if (world.isClient) {
                // Remove when progress exceeds 1.3 (gives ~6 frames buffer for server sync)
                if (item.getProgress() > 1.3f) {
                    itemsToRemove.add(item);
                }
            }
        }

        // Remove items based on client/server logic
        if (world.isClient) {
            // Client: only remove items that have exceeded the buffer
            blockEntity.travelingItems.removeAll(itemsToRemove);
        } else {
            // Server: remove items that reached the end (for routing)
            blockEntity.travelingItems.removeAll(itemsToRoute);
        }

        // Sync direction changes to clients
        if (!world.isClient && needsSync) {
            blockEntity.markDirty();
            world.updateListeners(pos, state, state, 3);
        }

        // Only handle routing on server side
        if (!world.isClient && !itemsToRoute.isEmpty()) {
            for (TravelingItem item : itemsToRoute) {
                blockEntity.routeItem(world, pos, state, item);
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
    private void routeItem(net.minecraft.world.World world, BlockPos pos, BlockState state, TravelingItem item) {
        // Use the item's current direction (already set at 0.5 progress)
        Direction direction = item.getDirection();
        BlockPos targetPos = pos.offset(direction);
        BlockState targetState = world.getBlockState(targetPos);

        // Try to pass to next pipe
        if (targetState.getBlock() instanceof PipeBlock pipeBlock) {
            // Check if the target pipe accepts items from other pipes
            if (pipeBlock.canAcceptFromPipe(world, targetPos, targetState, direction)) {
                BlockEntity targetEntity = world.getBlockEntity(targetPos);
                if (targetEntity instanceof PipeBlockEntity targetPipe) {
                    // Reset item progress (speed will gradually adjust to new pipe's speed)
                    item.setDirection(direction);  // This resets progress to 0.0
                    targetPipe.addItem(item);
                    return;
                }
            }
        }

        // Try to insert into inventory
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, targetPos, direction.getOpposite());
        if (storage != null) {
            try (Transaction transaction = Transaction.openOuter()) {
                ItemVariant variant = ItemVariant.of(item.getStack());
                long inserted = storage.insert(variant, item.getStack().getCount(), transaction);

                if (inserted > 0) {
                    transaction.commit();
                    // Item was fully or partially inserted
                    if (inserted < item.getStack().getCount()) {
                        // Partial insertion - drop remainder
                        item.getStack().decrement((int) inserted);
                        dropItem(world, pos, item);
                    }
                    return;
                }
            }
        }

        // Routing failed - drop item
        dropItem(world, pos, item);
    }

    /**
     * Choose the best direction for an item to travel.
     * Prefers continuing straight, but will turn if necessary.
     * Randomly chooses between multiple valid options.
     * Never routes back the way it came.
     */
    private Direction chooseDirection(net.minecraft.world.World world, BlockPos pos, BlockState state, Direction currentDirection) {
        List<Direction> validDirections = new ArrayList<>();
        Direction oppositeDirection = currentDirection.getOpposite();

        // Check all connected directions (from the pipe's blockstate)
        for (Direction direction : Direction.values()) {
            // Don't go back the way we came
            if (direction == oppositeDirection) {
                continue;
            }

            // Check if this direction is connected on the pipe
            BooleanProperty property = getPropertyForDirection(direction);
            if (property != null && state.get(property)) {
                // Check if there's a valid destination (pipe or inventory)
                BlockPos targetPos = pos.offset(direction);
                BlockState targetState = world.getBlockState(targetPos);

                // Valid if it's another pipe that accepts items from pipes
                if (targetState.getBlock() instanceof PipeBlock pipeBlock) {
                    // Check if the target pipe accepts items from other pipes (excludes wooden pipes)
                    if (pipeBlock.canAcceptFromPipe(world, targetPos, targetState, direction)) {
                        validDirections.add(direction);
                    }
                    continue;
                }

                // Valid if it's an inventory that can accept items
                Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, targetPos, direction.getOpposite());
                if (storage != null) {
                    validDirections.add(direction);
                }
            }
        }

        // No valid directions
        if (validDirections.isEmpty()) {
            return null;
        }

        // Only one option - use it
        if (validDirections.size() == 1) {
            return validDirections.get(0);
        }

        // Multiple options - choose randomly using deterministic seed
        // This ensures client and server make the same choice for smooth rendering
        // Use proper hash mixing for better randomness
        long seed = mixHash(pos.asLong(), world.getTime(), currentDirection.getId());
        java.util.Random random = new java.util.Random(seed);
        return validDirections.get(random.nextInt(validDirections.size()));
    }

    private static BooleanProperty getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> PipeBlock.NORTH;
            case SOUTH -> PipeBlock.SOUTH;
            case EAST -> PipeBlock.EAST;
            case WEST -> PipeBlock.WEST;
            case UP -> PipeBlock.UP;
            case DOWN -> PipeBlock.DOWN;
        };
    }

    /**
     * Drop an item entity at the pipe's position
     */
    private void dropItem(net.minecraft.world.World world, BlockPos pos, TravelingItem item) {
        // Create item entity at center of pipe
        Vec3d spawnPos = Vec3d.ofCenter(pos);

        ItemEntity itemEntity = new ItemEntity(
            world,
            spawnPos.x,
            spawnPos.y,
            spawnPos.z,
            item.getStack().copy()
        );

        // Prevent immediate pickup
        itemEntity.setToDefaultPickupDelay();

        world.spawnEntity(itemEntity);
    }

    /**
     * Mix multiple values into a well-distributed hash for better randomness.
     * Uses techniques from MurmurHash to ensure good distribution.
     */
    private static long mixHash(long a, long b, long c) {
        // Mix the inputs
        long hash = a;
        hash = hash * 31 + b;
        hash = hash * 31 + c;

        // MurmurHash-style bit mixing for better distribution
        hash ^= (hash >>> 33);
        hash *= 0xff51afd7ed558ccdL;
        hash ^= (hash >>> 33);
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= (hash >>> 33);

        return hash;
    }

    // TODO: Implement ItemStorage for pipes
    // - Pipes should REJECT inserts from hoppers (prevent free automation, preserve extraction energy cost)
    // - Pipes should ALLOW inserts from other pipes (normal pipe network behavior)
    // - Pipes should ALLOW extraction into hoppers/inventories
    // - Use Fabric Transfer API (ItemStorage.SIDED) to control this behavior
}
