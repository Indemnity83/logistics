package com.logistics.block.entity;

import com.logistics.block.PipeBlock;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.PipeRuntime;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PipeBlockEntity extends BlockEntity {
    public static final int VIRTUAL_CAPACITY = 5 * 64;
    private final List<TravelingItem> travelingItems = new ArrayList<>();
    private final NbtCompound moduleState = new NbtCompound();

    // Tracks changes in connected sides so modules can react deterministically.
    private int lastConnectionsMask = -1;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsBlockEntities.PIPE_BLOCK_ENTITY, pos, state);
    }

    /**
     * Add an item to this pipe
     * @param item The item to add
     * @param fromDirection The direction the item is coming from
     * @param bypassIngress Bypass any ingress checks
     * @return true if accepted, false if rejected
     */
    public boolean addItem(TravelingItem item, Direction fromDirection, boolean bypassIngress) {
        long accepted = getInsertableAmount(item.getStack().getCount(), fromDirection, item.getStack(), bypassIngress);
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

    public boolean forceAddItem(TravelingItem item, Direction fromDirection) {
        return addItem(item, fromDirection, true);
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

        if (!moduleState.isEmpty()) {
            nbt.put("ModuleState", moduleState.copy());
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        long readStart = System.nanoTime();
        super.readNbt(nbt, registryLookup);

        // Load traveling items
        travelingItems.clear();
        NbtList itemsList = nbt.getListOrEmpty("TravelingItems");
        for (int i = 0; i < itemsList.size(); i++) {
            travelingItems.add(TravelingItem.fromNbt((NbtCompound) itemsList.get(i), registryLookup));
        }

        if (!moduleState.getKeys().isEmpty()) {
            for (String key : new ArrayList<>(moduleState.getKeys())) {
                moduleState.remove(key);
            }
        }
        if (nbt.contains("ModuleState")) {
            NbtCompound stored = nbt.getCompoundOrEmpty("ModuleState");
            for (String key : stored.getKeys()) {
                moduleState.put(key, Objects.requireNonNull(stored.get(key)).copy());
            }
        }

        long durationMs = (System.nanoTime() - readStart) / 1_000_000L;
        if (durationMs >= 2L && Boolean.getBoolean("logistics.timing")) {
            com.logistics.LogisticsMod.LOGGER.info(
                "[timing] PipeBlockEntity readNbt at {} took {} ms (items={})",
                getPos(),
                durationMs,
                travelingItems.size()
            );
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
        PipeRuntime.tick(world, pos, state, blockEntity);
    }

    /**
     * Drop an item entity at the pipe's position
     */
    public static void dropItem(net.minecraft.world.World world, BlockPos pos, TravelingItem item) {
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

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);

        if (world != null && !world.isClient) {
            // Drop all traveling items
            for (TravelingItem travelingItem : travelingItems) {
                ItemEntity itemEntity = new ItemEntity(
                        world,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        travelingItem.getStack().copy()
                );
                itemEntity.setToDefaultPickupDelay();
                world.spawnEntity(itemEntity);
            }
        }
    }

    public NbtCompound getOrCreateModuleState(String key) {
        if (!moduleState.contains(key)) {
            moduleState.put(key, new NbtCompound());
        }
        return moduleState.getCompound(key).orElseGet(NbtCompound::new);
    }

    public PipeContext createContext() {
        return new PipeContext(world, pos, getCachedState(), this);
    }

    public int getLastConnectionsMask() {
        return lastConnectionsMask;
    }

    public void setLastConnectionsMask(int lastConnectionsMask) {
        this.lastConnectionsMask = lastConnectionsMask;
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

        if (pipeBlock.getPipe() != null) {
            PipeContext context = new PipeContext(world, pos, state, this);
            Pipe modulePipe = pipeBlock.getPipe();
            if (modulePipe.canAcceptFrom(context, side, ItemStack.EMPTY)) {
                return new PipeItemStorage(this, side);
            }
            return null;
        }
        return null;
    }

    long getInsertableAmount(long maxAmount, Direction fromDirection, ItemStack stack) {
        return getInsertableAmount(maxAmount, fromDirection, stack, false);
    }

    long getInsertableAmount(long maxAmount, Direction fromDirection, ItemStack stack, boolean bypassIngress) {
        if (maxAmount <= 0) {
            return 0;
        }

        if (!bypassIngress) {
            boolean fromPipe = isNeighborPipe(fromDirection);
            if (!canAcceptFrom(fromDirection, fromPipe, stack)) {
                return 0;
            }
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

    private float getInitialSpeed() {
        if (world == null) {
            return PipeConfig.BASE_PIPE_SPEED;
        }

        BlockState state = getCachedState();
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            PipeContext context = new PipeContext(world, pos, state, this);
            return pipeBlock.getPipe().getTargetSpeed(context);
        }

        return PipeConfig.BASE_PIPE_SPEED;
    }

    private boolean isNeighborPipe(Direction fromDirection) {
        if (world == null) {
            return false;
        }

        BlockPos sourcePos = pos.offset(fromDirection);
        BlockState sourceState = world.getBlockState(sourcePos);
        return sourceState.getBlock() instanceof PipeBlock;
    }

    private boolean canAcceptFrom(Direction fromDirection, boolean fromPipe, ItemStack stack) {
        if (world == null) {
            return false;
        }

        BlockState state = getCachedState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return false;
        }

        if (pipeBlock.getPipe() != null) {
            PipeContext context = new PipeContext(world, pos, state, this);
            return pipeBlock.getPipe().canAcceptFrom(context, fromDirection, stack);
        }

        return false;
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

    public int getComparatorOutput() {
        int total = getTotalItemCount();
        if (total <= 0) {
            return 0;
        }
        int clamped = Math.min(total, VIRTUAL_CAPACITY);
        return Math.max(1, (clamped * 15) / VIRTUAL_CAPACITY);
    }

}
