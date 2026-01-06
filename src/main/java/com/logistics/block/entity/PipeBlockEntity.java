package com.logistics.block.entity;

import com.logistics.block.IronPipeBlock;
import com.logistics.block.PipeBlock;
import com.logistics.block.WoodenPipeBlock;
import com.logistics.block.VoidPipeBlock;
import com.logistics.item.TravelingItem;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
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
    public static final int VIRTUAL_CAPACITY = 5 * 64;
    private final List<TravelingItem> travelingItems = new ArrayList<>();
    @Nullable
    private Direction activeInputFace;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsBlockEntities.PIPE_BLOCK_ENTITY, pos, state);
    }

    /**
     * Add an item to this pipe
     * @param item The item to add
     * @param fromDirection The direction the item is coming from
     * @return true if accepted, false if rejected
     */
    public boolean addItem(TravelingItem item, Direction fromDirection) {
        long accepted = getInsertableAmount(item.getStack().getCount(), fromDirection);
        if (accepted <= 0) {
            dropItem(world, pos, item);
            return false;
        }

        TravelingItem remainder = null;
        if (accepted < item.getStack().getCount()) {
            ItemStack remainderStack = item.getStack().copy();
            remainderStack.setCount(item.getStack().getCount() - (int) accepted);
            remainder = new TravelingItem(remainderStack, fromDirection.getOpposite(), item.getSpeed());
        }

        ItemStack acceptedStack = item.getStack().copy();
        acceptedStack.setCount((int) accepted);
        acceptInsertedStack(acceptedStack, fromDirection, item.getSpeed());

        if (remainder != null) {
            dropItem(world, pos, remainder);
            return false;
        }

        return true;
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

        if (activeInputFace != null) {
            nbt.putInt("ActiveInputFace", activeInputFace.getId());
        }
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

        if (nbt.contains("ActiveInputFace", 99)) {
            int faceId = nbt.getInt("ActiveInputFace");
            activeInputFace = faceId >= 0 ? Direction.byId(faceId) : null;
        } else {
            activeInputFace = null;
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
        List<TravelingItem> itemsToDiscard = new ArrayList<>();
        boolean needsSync = false;

        for (TravelingItem item : blockEntity.travelingItems) {
            float progressBefore = item.getProgress();

            if (item.tick(targetSpeed, accelerationRate, canAccelerate)) {
                // Item reached the end of this pipe segment
                itemsToRoute.add(item);
            }

            // Void pipes discard items at the pipe center.
            if (state.getBlock() instanceof VoidPipeBlock) {
                if (item.getProgress() >= 0.5f) {
                    itemsToDiscard.add(item);
                    continue;
                }
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
            blockEntity.travelingItems.removeAll(itemsToDiscard);
        } else {
            // Server: remove items that reached the end (for routing)
            blockEntity.travelingItems.removeAll(itemsToRoute);
            blockEntity.travelingItems.removeAll(itemsToDiscard);
        }

        // Sync direction changes to clients
        if (!world.isClient && needsSync) {
            blockEntity.markDirty();
            world.updateListeners(pos, state, state, 3);
        }

        // Only handle routing on server side
        if (!world.isClient && (!itemsToRoute.isEmpty() || !itemsToDiscard.isEmpty())) {
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

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, targetPos, direction.getOpposite());
        if (storage != null) {
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted;
                if (storage instanceof PipeItemStorage pipeStorage) {
                    inserted = pipeStorage.insertTravelingItem(item, transaction);
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
        // Special handling for iron pipes - always route to output direction
        if (state.getBlock() instanceof IronPipeBlock ironPipe) {
            Direction outputDirection = ironPipe.getOutputDirection(state);
            // Check if output direction is valid (connected and not the direction we came from)
            if (outputDirection != currentDirection.getOpposite()) {
                BooleanProperty property = getPropertyForDirection(outputDirection);
                if (property != null && state.get(property)) {
                    return outputDirection;
                }
            }
            // If output direction is invalid, return null to drop the item
            return null;
        }

        List<Direction> validDirections = new ArrayList<>();
        Direction oppositeDirection = currentDirection.getOpposite();

        // Check all connected directions (from the pipe's blockstate)
        // Dumb routing: if there's a connection, it's a valid direction
        for (Direction direction : Direction.values()) {
            // Don't go back the way we came
            if (direction == oppositeDirection) {
                continue;
            }

            // Check if this direction is connected on the pipe
            BooleanProperty property = getPropertyForDirection(direction);
            if (property != null && state.get(property)) {
                validDirections.add(direction);
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

    @Nullable
    public Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        if (side == null || world == null) {
            return null;
        }

        BlockState state = getCachedState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return null;
        }

        BlockPos neighborPos = pos.offset(side);
        BlockState neighborState = world.getBlockState(neighborPos);
        boolean neighborIsPipe = neighborState.getBlock() instanceof PipeBlock;

        if (neighborIsPipe) {
            return new PipeItemStorage(this, side);
        }

        if (pipeBlock.canAcceptFromInventory(world, pos, state, side)) {
            return new PipeItemStorage(this, side);
        }

        return null;
    }

    long getInsertableAmount(long maxAmount, Direction fromDirection) {
        if (maxAmount <= 0) {
            return 0;
        }

        boolean fromPipe = isNeighborPipe(fromDirection);
        if (!canAcceptFrom(fromDirection, fromPipe)) {
            return 0;
        }

        int remaining = getRemainingCapacity();
        if (remaining <= 0) {
            return 0;
        }

        return Math.min(maxAmount, remaining);
    }

    void acceptInsertedStack(ItemStack stack, Direction fromDirection, @Nullable Float speedOverride) {
        if (stack.isEmpty()) {
            return;
        }

        float speed = speedOverride != null ? speedOverride : getInitialSpeed();
        TravelingItem newItem = new TravelingItem(stack, fromDirection.getOpposite(), speed);
        travelingItems.add(newItem);
        markDirty();

        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Nullable
    public Direction getActiveInputFace() {
        return activeInputFace;
    }

    public void setActiveInputFace(@Nullable Direction direction) {
        boolean changed = activeInputFace != direction;
        activeInputFace = direction;
        if (changed) {
            markDirty();
        }

        if (world != null && !world.isClient) {
            BlockState state = getCachedState();
            if (state.getBlock() instanceof PipeBlock pipeBlock) {
                BlockState updated = pipeBlock.refreshInventoryConnections(world, pos, state);
                if (pipeBlock instanceof WoodenPipeBlock) {
                    updated = updated.with(WoodenPipeBlock.ACTIVE_FACE, WoodenPipeBlock.toActiveFace(activeInputFace));
                }
                if (updated != state) {
                    world.setBlockState(pos, updated, 3);
                } else if (changed) {
                    world.updateListeners(pos, state, state, 3);
                }
            } else if (changed) {
                world.updateListeners(pos, getCachedState(), getCachedState(), 3);
            }
        }
    }

    private float getInitialSpeed() {
        if (world == null) {
            return PipeBlock.BASE_PIPE_SPEED;
        }

        BlockState state = getCachedState();
        if (state.getBlock() instanceof PipeBlock pipeBlock) {
            return pipeBlock.getPipeSpeed(world, pos, state);
        }

        return PipeBlock.BASE_PIPE_SPEED;
    }

    private boolean isNeighborPipe(Direction fromDirection) {
        if (world == null) {
            return false;
        }

        BlockPos sourcePos = pos.offset(fromDirection);
        BlockState sourceState = world.getBlockState(sourcePos);
        return sourceState.getBlock() instanceof PipeBlock;
    }

    private boolean canAcceptFrom(Direction fromDirection, boolean fromPipe) {
        if (world == null) {
            return false;
        }

        BlockState state = getCachedState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return false;
        }

        if (fromPipe && !pipeBlock.canAcceptFromPipe(world, pos, state, fromDirection)) {
            return false;
        }

        if (!fromPipe && !pipeBlock.canAcceptFromInventory(world, pos, state, fromDirection)) {
            return false;
        }

        if (pipeBlock instanceof IronPipeBlock ironPipe) {
            if (!ironPipe.canAcceptFromDirection(state, fromDirection)) {
                return false;
            }
        }

        return true;
    }

    private int getRemainingCapacity() {
        return VIRTUAL_CAPACITY - getTotalItemCount();
    }

    private int getTotalItemCount() {
        int total = 0;
        for (TravelingItem item : travelingItems) {
            total += item.getStack().getCount();
        }
        return total;
    }
}
