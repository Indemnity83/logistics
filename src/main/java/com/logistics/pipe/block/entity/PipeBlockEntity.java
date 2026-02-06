package com.logistics.pipe.block.entity;

import com.logistics.api.EnergyStorage;
import com.logistics.core.lib.pipe.PipeConnection;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.LogisticsPipe;
import com.logistics.pipe.runtime.PipeRuntime;
import com.logistics.pipe.runtime.TravelingItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class PipeBlockEntity extends BlockEntity implements PipeConnection, EnergyStorage, AcceptsLowTierEnergy {
    public static final int VIRTUAL_CAPACITY = 5 * 64;
    private final List<TravelingItem> travelingItems = new ArrayList<>();
    private final CompoundTag moduleState = new CompoundTag();

    // Tracks changes in connected sides so modules can react deterministically.
    private int lastConnectionsMask = -1;

    // Connection type cache for rendering (updated when connections change)
    private final PipeConnection.Type[] connectionTypes = new PipeConnection.Type[6];

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, pos, state);
        // Initialize connection types to NONE
        for (int i = 0; i < 6; i++) {
            connectionTypes[i] = PipeConnection.Type.NONE;
        }
    }

    /**
     * PipeConnection interface: Pipes always accept connections from all sides.
     * This is used by other blocks to query if they can connect to this pipe.
     */
    @Override
    public PipeConnection.Type getConnectionType(Direction direction) {
        return PipeConnection.Type.PIPE;
    }

    /**
     * Get the cached connection type for rendering.
     * This is updated by PipeRuntime and reflects what this pipe has connected to.
     */
    public PipeConnection.Type getCachedConnectionType(Direction direction) {
        return connectionTypes[direction.ordinal()];
    }

    public void setConnectionType(Direction direction, PipeConnection.Type type) {
        connectionTypes[direction.ordinal()] = type;
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
            dropItem(level, getBlockPos(), item);
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
            dropItem(level, getBlockPos(), remainder);
            return false;
        }

        return true;
    }

    /**
     * Add an item from the PipeConnection interface.
     * Creates a TravelingItem with default speed and delegates to the existing addItem logic.
     */
    @Override
    public boolean addItem(Direction from, ItemStack stack) {
        TravelingItem item = new TravelingItem(stack, from.getOpposite(), LogisticsPipe.CONFIG.ITEM_MIN_SPEED);
        return addItem(item, from, false);
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
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);

        // Save all data to a CompoundTag, then store it
        CompoundTag pipeData = new CompoundTag();

        // Save traveling items
        if (!travelingItems.isEmpty()) {
            ListTag itemsList = new ListTag();
            for (TravelingItem item : travelingItems) {
                CompoundTag itemTag = (CompoundTag) TravelingItem.CODEC
                        .encodeStart(NbtOps.INSTANCE, item)
                        .getOrThrow();
                itemsList.add(itemTag);
            }
            pipeData.put("TravelingItems", itemsList);
        }

        // Save module state
        if (!moduleState.isEmpty()) {
            pipeData.put("ModuleState", moduleState);
        }

        // Save connection types (for client rendering)
        CompoundTag connectionsNbt = new CompoundTag();
        for (Direction direction : Direction.values()) {
            PipeConnection.Type type = connectionTypes[direction.ordinal()];
            if (type != PipeConnection.Type.NONE) {
                connectionsNbt.putString(direction.name().toLowerCase(), type.getSerializedName());
            }
        }
        if (!connectionsNbt.isEmpty()) {
            pipeData.put("Connections", connectionsNbt);
        }

        view.store("PipeData", CompoundTag.CODEC, pipeData);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        long readStart = System.nanoTime();
        super.loadAdditional(view);

        view.read("PipeData", CompoundTag.CODEC).ifPresent(pipeData -> {
            // Load traveling items
            travelingItems.clear();
            if (pipeData.contains("TravelingItems")) {
                pipeData.getList("TravelingItems").ifPresent(itemsList -> {
                    for (int i = 0; i < itemsList.size(); i++) {
                        itemsList.getCompound(i)
                                .flatMap(itemTag -> TravelingItem.CODEC.parse(NbtOps.INSTANCE, itemTag).result())
                                .ifPresent(travelingItems::add);
                    }
                });
            }

            // Load module state
            if (!moduleState.isEmpty()) {
                for (String key : new java.util.ArrayList<>(moduleState.keySet())) {
                    moduleState.remove(key);
                }
            }
            if (pipeData.contains("ModuleState")) {
                pipeData.getCompound("ModuleState").ifPresent(stored -> {
                    for (String key : stored.keySet()) {
                        moduleState.put(key, java.util.Objects.requireNonNull(stored.get(key)).copy());
                    }
                });
            }

            // Load connection types
            if (pipeData.contains("Connections")) {
                CompoundTag connectionsNbt = pipeData.getCompound("Connections").orElse(new CompoundTag());
                // Reset all to NONE first
                for (int i = 0; i < 6; i++) {
                    connectionTypes[i] = PipeConnection.Type.NONE;
                }
                // Load saved connections
                for (Direction direction : Direction.values()) {
                    String typeName = connectionsNbt.getString(direction.name().toLowerCase()).orElse("none");
                    connectionTypes[direction.ordinal()] = PipeConnection.Type.fromSerializedName(typeName);
                }
            }
        });

        long durationMs = (System.nanoTime() - readStart) / 1_000_000L;
        if (durationMs >= 2L && Boolean.getBoolean("logistics.timing")) {
            com.logistics.LogisticsMod.LOGGER.info(
                    "[timing] PipeBlockEntity loadAdditional at {} took {} ms (items={})",
                    getBlockPos(),
                    durationMs,
                    travelingItems.size());
        }
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public static void tick(
            net.minecraft.world.level.Level world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        PipeRuntime.tick(world, pos, state, blockEntity);
    }

    /**
     * Drop an item entity at the pipe's position
     */
    public static void dropItem(net.minecraft.world.level.Level level, BlockPos pos, TravelingItem item) {
        // Create item entity at center of pipe
        Vec3 spawnPos = Vec3.atCenterOf(pos);

        ItemEntity itemEntity = new ItemEntity(
                level, spawnPos.x, spawnPos.y, spawnPos.z, item.getStack().copy());

        // Prevent immediate pickup
        itemEntity.setDefaultPickUpDelay();

        level.addFreshEntity(itemEntity);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Item dropping is handled in PipeBlock.onRemove() instead
    }

    public CompoundTag getOrCreateModuleState(String key) {
        if (!moduleState.contains(key)) {
            moduleState.put(key, new CompoundTag());
        }
        return moduleState.getCompound(key).orElseThrow();
    }

    public PipeContext createContext() {
        return new PipeContext(level, worldPosition, getBlockState(), this);
    }

    public int getLastConnectionsMask() {
        return lastConnectionsMask;
    }

    public void setLastConnectionsMask(int lastConnectionsMask) {
        this.lastConnectionsMask = lastConnectionsMask;
    }

    @Nullable public Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        if (side == null || level == null) {
            return null;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return null;
        }

        if (pipeBlock.getPipe() != null) {
            PipeContext context = new PipeContext(level, worldPosition, state, this);
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
        setChanged();

        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private float getInitialSpeed() {
        return LogisticsPipe.CONFIG.ITEM_MIN_SPEED;
    }

    private boolean isNeighborPipe(Direction fromDirection) {
        if (level == null) {
            return false;
        }

        BlockPos sourcePos = worldPosition.relative(fromDirection);
        BlockState sourceState = level.getBlockState(sourcePos);
        return sourceState.getBlock() instanceof PipeBlock;
    }

    private boolean canAcceptFrom(Direction fromDirection, boolean fromPipe, ItemStack stack) {
        if (level == null) {
            return false;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return false;
        }

        if (pipeBlock.getPipe() != null) {
            PipeContext context = new PipeContext(level, worldPosition, state, this);
            return pipeBlock.getPipe().canAcceptFrom(context, fromDirection, stack);
        }

        return false;
    }

    /**
     * Check if this pipe can accept an item from the given direction (PipeConnection interface).
     * Delegates to the pipe's module logic.
     */
    @Override
    public boolean canAcceptFrom(Direction from, ItemStack stack) {
        PipeBlock pipeBlock = (PipeBlock) getBlockState().getBlock();
        Pipe pipe = pipeBlock.getPipe();
        if (pipe == null) {
            return false;
        }
        PipeContext ctx = new PipeContext(level, getBlockPos(), getBlockState(), this);
        return pipe.canAcceptFrom(ctx, from, stack);
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

    // --- EnergyStorage implementation ---

    @Override
    public long getAmount() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            PipeContext ctx = createContext();
            return pipeBlock.getPipe().getEnergyAmount(ctx);
        }
        return 0;
    }

    @Override
    public long getCapacity() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            PipeContext ctx = createContext();
            return pipeBlock.getPipe().getEnergyCapacity(ctx);
        }
        return 0;
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            PipeContext ctx = createContext();
            return pipeBlock.getPipe().insertEnergy(ctx, maxAmount, simulate);
        }
        return 0;
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            PipeContext ctx = createContext();
            return pipeBlock.getPipe().extractEnergy(ctx, maxAmount, simulate);
        }
        return 0;
    }

    @Override
    public boolean canInsert() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            PipeContext ctx = createContext();
            return pipeBlock.getPipe().canInsertEnergy(ctx);
        }
        return false;
    }

    @Override
    public boolean canExtract() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            PipeContext ctx = createContext();
            return pipeBlock.getPipe().canExtractEnergy(ctx);
        }
        return false;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(Direction from) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            PipeContext ctx = createContext();
            return pipeBlock.getPipe().acceptsLowTierEnergyFrom(ctx, from);
        }
        return false;
    }

    // --- Component handling for item drops ---

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) return;

        Pipe pipe = pipeBlock.getPipe();
        if (pipe == null) return;

        PipeContext ctx = createContext();
        pipe.readItemComponents(components, ctx);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) return;

        Pipe pipe = pipeBlock.getPipe();
        if (pipe == null) return;

        PipeContext ctx = createContext();
        pipe.addItemComponents(builder, ctx);
    }
}
