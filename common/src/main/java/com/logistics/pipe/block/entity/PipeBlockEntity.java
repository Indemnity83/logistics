package com.logistics.pipe.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.IPipeAccess;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.core.lib.power.DirectEnergyReceiver;
import com.logistics.pipe.ChassisPipe;
import com.logistics.pipe.ItemPipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.data.PipeDataComponents.WeatheringState;
import com.logistics.pipe.modules.WeatheringModule;
import com.logistics.pipe.item.ModuleItem;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.runtime.PipeRuntime;
import com.logistics.core.lib.pipe.TravelingItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.logistics.core.lib.storage.IItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PipeBlockEntity extends BaseBlockEntity
        implements PipeConnection, AcceptsLowTierEnergy, DirectEnergyReceiver, HasItemStorage, HasEnergyStorage, IPipeAccess {
    public static final int VIRTUAL_CAPACITY = 5 * 64;
    private final List<TravelingItem> travelingItems = new ArrayList<>();
    private final CompoundTag moduleState = new CompoundTag();

    // Tracks changes in connected sides so modules can react deterministically.
    private int lastConnectionsMask = -1;
    private int lastConnectionSignature = -1;

    // Per-arm power-status bitmask (one bit per direction; set = that arm is "powered"/green).
    // Computed on the server tick and synced to the client for arm tinting.
    private int poweredArmMask = 0;

    // Connection type cache for rendering (updated when connections change)
    private final PipeConnection.Type[] connectionTypes = new PipeConnection.Type[6];
    private boolean connectionCacheDirty = true;

    // Energy storage (only created for pipes with energy capability)
    @Nullable
    private final EnergyComponent energy;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY, pos, state);

        // Initialize connection types to NONE
        for (int i = 0; i < 6; i++) {
            connectionTypes[i] = PipeConnection.Type.NONE;
        }

        // Create energy storage only if pipe has energy capability
        ItemPipe pipe = state.getBlock() instanceof PipeBlock pipeBlock ? pipeBlock.getPipe() : null;
        if (pipe != null && pipe.hasEnergy()) {
            // TODO: Configure capacity/maxInsert/maxExtract based on pipe type
            this.energy = new EnergyComponent(1000, 100, 0, this::setChanged);
        } else {
            this.energy = null;
        }
    }

    /**
     * Gets the energy storage for this pipe, or null if the pipe doesn't support energy.
     */
    @Nullable
    public EnergyComponent getEnergy() {
        return energy;
    }

    // ==================== HasItemStorage & HasEnergyStorage ====================

    @Override
    @Nullable
    public IItemStorage itemStorage(@Nullable Direction side) {
        return getItemStorage(side);
    }

    @Override
    @Nullable
    public com.logistics.core.lib.energy.IEnergyStorage energyStorage(@Nullable Direction side) {
        // Pipes accept energy from all sides (if they support energy at all)
        return getEnergy();
    }

    // ==================== PipeConnection ====================

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

    /** Packed form of all six cached connection types — see {@link PipeConnection.Type#signature}. */
    public int getConnectionSignature() {
        return PipeConnection.Type.signature(connectionTypes);
    }

    public int getLastConnectionSignature() {
        return lastConnectionSignature;
    }

    public void setLastConnectionSignature(int signature) {
        this.lastConnectionSignature = signature;
    }

    /**
     * Mark the connection cache as dirty, forcing recalculation on next tick.
     * Called when a neighbor block changes.
     */
    public void invalidateConnectionCache() {
        connectionCacheDirty = true;
    }

    /**
     * Check if the connection cache needs recalculation.
     */
    public boolean isConnectionCacheDirty() {
        return connectionCacheDirty;
    }

    /**
     * Mark the connection cache as clean after recalculation.
     */
    public void markConnectionCacheClean() {
        connectionCacheDirty = false;
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
            dropItemInWorld(item);
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
        acceptInsertedStack(acceptedStack, fromDirection, item);

        if (remainder != null) {
            dropItemInWorld(remainder);
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
        TravelingItem item = new TravelingItem(stack, from.getOpposite(), LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED));
        return addItem(item, from, false);
    }

    @Override
    public boolean forceAddItem(TravelingItem item, Direction fromDirection) {
        return addItem(item, fromDirection, true);
    }

    /**
     * Get all traveling items (for rendering)
     */
    @Override
    public List<TravelingItem> getTravelingItems() {
        return travelingItems;
    }

    @Override
    protected void saveLogisticsData(CompoundTag pipeData, HolderLookup.Provider registries) {
        super.saveLogisticsData(pipeData, registries);

        // Save traveling items
        if (!travelingItems.isEmpty()) {
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            ListTag itemsList = new ListTag();
            for (TravelingItem item : travelingItems) {
                CompoundTag itemTag = (CompoundTag) TravelingItem.CODEC
                        .encodeStart(ops, item)
                        .getOrThrow();
                itemsList.add(itemTag);
            }
            pipeData.put("ItemsInTransit", itemsList);
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
            pipeData.put("ConnectionTypes", connectionsNbt);
        }

        // Save power-status mask (for client arm tinting)
        if (poweredArmMask != 0) {
            pipeData.putInt("PoweredArmMask", poweredArmMask);
        }

        // Save energy (if this pipe has energy capability)
        if (energy != null) {
            energy.writeNbt(pipeData, "Energy");
        }
    }

    @Override
    protected void loadLogisticsData(CompoundTag pipeData, HolderLookup.Provider registries) {
        super.loadLogisticsData(pipeData, registries);

        long readStart = System.nanoTime();

        // Load traveling items
        travelingItems.clear();
        NbtCompat.ifHasList(pipeData, "ItemsInTransit", itemsList -> {
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            for (int i = 0; i < itemsList.size(); i++) {
                NbtCompat.ifHasCompoundAt(itemsList, i, itemTag ->
                    TravelingItem.CODEC.parse(ops, itemTag).result().ifPresentOrElse(
                        travelingItems::add,
                        () -> LogisticsMod.LOGGER.warn(
                                "[Logistics] Skipping unreadable TravelingItem at {}", getBlockPos())));
            }
        });

        // Load module state
        // CompoundTag lacks clear() method, so copy keys then remove each
        new ArrayList<>(moduleState.getAllKeys()).forEach(moduleState::remove);
        NbtCompat.ifHasCompound(pipeData, "ModuleState", stored -> {
            for (String key : stored.getAllKeys()) {
                moduleState.put(key, Objects.requireNonNull(stored.get(key)).copy());
            }
        });

        // Load connection types
        // Reset all to NONE first
        for (int i = 0; i < 6; i++) {
            connectionTypes[i] = PipeConnection.Type.NONE;
        }
        NbtCompat.ifHasCompound(pipeData, "ConnectionTypes", connectionsNbt -> {
            // Load saved connections
            for (Direction direction : Direction.values()) {
                String typeName = NbtCompat.getString(connectionsNbt, direction.name().toLowerCase(), "none");
                connectionTypes[direction.ordinal()] = PipeConnection.Type.fromSerializedName(typeName);
            }
        });

        // Load power-status mask (client arm tinting)
        poweredArmMask = NbtCompat.getInt(pipeData, "PoweredArmMask", 0);

        // Load energy (if this pipe has energy capability)
        if (energy != null) {
            energy.readNbt(pipeData, "Energy");
        }

        long durationMs = (System.nanoTime() - readStart) / 1_000_000L;
        if (durationMs >= 2L && Boolean.getBoolean("logistics.timing")) {
            com.logistics.LogisticsMod.LOGGER.debug(
                    "[timing] PipeBlockEntity loadLogisticsData at {} took {} ms (items={})",
                    getBlockPos(),
                    durationMs,
                    travelingItems.size());
        }
    }

    public static void tick(
            Level world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        PipeRuntime.tick(world, pos, state, blockEntity);
    }

    /**
     * Drop an item into the world at this pipe's position.
     * Convenience method for internal use within PipeBlockEntity.
     */
    private void dropItemInWorld(TravelingItem item) {
        dropItem(level, getBlockPos(), item);
    }

    /**
     * Drop an item entity at the specified pipe position.
     * Static method for external callers (PipeBlock, PipeRuntime).
     *
     * <p>Undeliverable fluid packets are voided instead of spawning a ground-item entity.
     */
    public static void dropItem(Level level, BlockPos pos, TravelingItem item) {
        if (item.getStack().getItem() == LogisticsPipe.ITEM.FLUID_PACKET) {
            return;
        }

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
        // Dropping is handled in PipeBlock.onRemove(); setRemoved() also fires on chunk unload.
    }

    /**
     * Drop in-transit items and installed chassis modules, then detach from the network, when the
     * pipe is removed by any means (player break, explosion, /setblock). Runs while the block entity
     * is still present -- unlike affectNeighborsAfterRemoval -- and is not called on chunk unload.
     */
    // Called from PipeBlock.onRemove while the block entity is still present, for any removal
    // (player break, explosion, /setblock).
    public void onPipeRemoved(BlockPos pos) {
        if (level == null || level.isClientSide()) {
            return;
        }
        for (TravelingItem item : travelingItems) {
            dropItem(level, pos, item);
        }
        dropChassisModules(pos);
        NetworkRegistry.removePipe(level, pos);
    }

    /** Drop installed chassis module items, each carrying its saved configuration. */
    private void dropChassisModules(BlockPos pos) {
        CompoundTag chassisState = getOrCreateModuleState(ChassisPipe.STATE_KEY);
        if (chassisState.isEmpty()) {
            return;
        }

        // ItemStack.CODEC needs registry-aware ops for registry-backed components such as enchantments.
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        PipeContext ctx = createContext();

        for (int slot = 0; slot < ChassisPipe.MAX_SLOTS; slot++) {
            Tag tag = chassisState.get(String.valueOf(slot));
            if (tag == null) {
                continue;
            }

            ItemStack.CODEC.parse(ops, tag).result().ifPresent(stack -> {
                applyModuleStateToStack(ctx, stack);
                ItemEntity entity = new ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
            });
        }
    }

    private void applyModuleStateToStack(PipeContext ctx, ItemStack stack) {
        if (!(stack.getItem() instanceof ModuleItem moduleItem)) {
            return;
        }
        String stateKey = ChassisPipe.moduleStateKey(stack, moduleItem.createModule());
        CompoundTag moduleData = ctx.moduleState(stateKey);
        stack.set(DataComponents.CUSTOM_DATA, ModuleItem.customDataWithModuleState(stack, moduleData));
    }

    public CompoundTag getOrCreateModuleState(String key) {
        if (!moduleState.contains(key)) {
            moduleState.put(key, new CompoundTag());
        }
        return NbtCompat.getCompoundOrEmpty(moduleState, key);
    }

    public void clearModuleState(String key) {
        moduleState.remove(key);
    }

    // ==================== IPipeAccess ====================

    @Override
    public void markDirty() {
        setChanged();
    }

    @Override
    public CompoundTag moduleState(String key) {
        return getOrCreateModuleState(key);
    }

    @Override
    public @Nullable CompoundTag existingModuleState(String key) {
        if (!moduleState.contains(key)) {
            return null;
        }
        return NbtCompat.getCompoundOrEmpty(moduleState, key);
    }

    @Override
    public PipeConnection.Type getConnectionType(Level world, BlockPos pos, Direction direction) {
        BlockState blockState = getBlockState();
        if (blockState.getBlock() instanceof PipeBlock pipeBlock) {
            return pipeBlock.getConnectionType(world, pos, direction);
        }
        return PipeConnection.Type.NONE;
    }

    @Override
    public boolean isNeighborPipe(Level world, BlockPos pos, Direction direction) {
        return world.getBlockState(pos.relative(direction)).getBlock() instanceof PipeBlock;
    }

    @Override
    public boolean isPowered() {
        BlockState blockState = getBlockState();
        return blockState.hasProperty(PipeBlock.POWERED) && blockState.getValue(PipeBlock.POWERED);
    }

    @Override
    public @Nullable ILogisticsNetwork getNetwork() {
        // Use getOrCreateNetwork so that pipes loaded from disk (world load) self-register
        // on their first tick rather than requiring a neighborChanged event.
        // getOrCreateNetwork returns immediately if the pipe is already mapped (O(1) fast path).
        return NetworkRegistry.getOrCreateNetwork(getLevel(), getBlockPos());
    }

    public PipeContext createContext() {
        return new PipeContext(level, worldPosition, getBlockState(), this);
    }

    // --- Component handling for item drops ---

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) return;

        ItemPipe pipe = pipeBlock.getPipe();
        if (pipe == null) return;

        pipe.addItemComponents(builder, createContext());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        super.applyImplicitComponents(components);

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) return;

        ItemPipe pipe = pipeBlock.getPipe();
        if (pipe == null) return;

        // Apply weathering state directly - DataComponentInput has protected access
        // so we handle it here in the BlockEntity subclass rather than in module code.
        WeatheringState ws = components.get(LogisticsPipe.DATA.WEATHERING_STATE);
        if (ws != null && !ws.isDefault()) {
            WeatheringModule wm = pipe.getModule(WeatheringModule.class, this);
            if (wm != null) {
                wm.applyWeatheringState(ws, createContext());
            }
        }
    }

    public int getLastConnectionsMask() {
        return lastConnectionsMask;
    }

    public void setLastConnectionsMask(int lastConnectionsMask) {
        this.lastConnectionsMask = lastConnectionsMask;
    }

    public int getPoweredArmMask() {
        return poweredArmMask;
    }

    public void setPoweredArmMask(int poweredArmMask) {
        this.poweredArmMask = poweredArmMask;
    }

    @Nullable public PipeItemStorage getItemStorage(@Nullable Direction side) {
        if (side == null || level == null) {
            return null;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) {
            return null;
        }

        if (pipeBlock.getPipe() != null) {
            PipeContext context = new PipeContext(level, worldPosition, state, this);
            ItemPipe modulePipe = pipeBlock.getPipe();
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

    public boolean canAcceptEntireStack(ItemStack stack, Direction fromDirection, boolean bypassIngress) {
        if (stack.isEmpty()) {
            return true;
        }

        return getInsertableAmount(stack.getCount(), fromDirection, stack, bypassIngress) >= stack.getCount();
    }

    void acceptInsertedStack(ItemStack stack, Direction fromDirection, @Nullable Float speedOverride) {
        acceptInsertedStack(stack, fromDirection, speedOverride, null);
    }

    void acceptInsertedStack(ItemStack stack, Direction fromDirection, @Nullable Float speedOverride, @Nullable BlockPos destination) {
        if (stack.isEmpty()) {
            return;
        }

        // Give modules the first chance to handle external insertions (e.g., split a craft result).
        BlockState insertState = getBlockState();
        if (insertState.getBlock() instanceof PipeBlock insertPipeBlock
                && insertPipeBlock.getPipe() != null
                && level != null
                && !level.isClientSide()) {
            PipeContext ctx = createContext();
            if (insertPipeBlock.getPipe().onExternalInsert(ctx, stack, fromDirection)) {
                return;
            }
        }

        float speed = speedOverride != null ? speedOverride : getInitialSpeed();
        TravelingItem newItem = new TravelingItem(stack, fromDirection.getOpposite(), speed, destination);
        travelingItems.add(newItem);
        // Note: Per-item sync matches pre-refactoring behavior. Could potentially be
        // batched at tick boundaries for high-throughput pipes, but unchanged for now.
        markDirtyAndSync();
    }

    /**
     * Accept an item from a pipe-to-pipe transfer, preserving transient state (deliveryId, remainingTtl).
     * Used when routing items between pipes so that in-transit accounting and TTL are not lost.
     */
    void acceptInsertedStack(ItemStack stack, Direction fromDirection, TravelingItem source) {
        if (stack.isEmpty()) {
            return;
        }

        float speed = source.getSpeed();
        TravelingItem newItem = new TravelingItem(stack, fromDirection.getOpposite(), speed, source.getDestination());
        newItem.setDeliveryId(source.getDeliveryId());
        newItem.setRemainingTtl(source.getRemainingTtl());
        travelingItems.add(newItem);
        markDirtyAndSync();
    }

    private float getInitialSpeed() {
        return LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED);
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
        ItemPipe pipe = pipeBlock.getPipe();
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

    // --- Energy Storage Access ---

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

    // MC 1.21.1: Component system methods don't exist in this version.
    // Copper pipes lose oxidation state when picked up.
    // These methods are present in MC 1.21.11+ only.
}
