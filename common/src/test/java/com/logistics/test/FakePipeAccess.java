package com.logistics.test;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.IPipeAccess;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory stub implementation of {@link IPipeAccess} for unit tests.
 *
 * <p>Stores module state in plain {@link CompoundTag} maps. All connection types default
 * to {@link PipeConnection.Type#NONE}; use {@link #setConnection} to configure specific
 * directions. The network defaults to {@code null}; use {@link #setNetwork} to inject one.
 *
 * <p>The pipe holds {@link PipeBlockEntity#VIRTUAL_CAPACITY} items unless {@link #setCapacity}
 * says otherwise, so a module that hands more than that to {@link #forceAddItem} spills the excess
 * into {@link #getDroppedItems()} exactly as the real block entity drops it in world.
 *
 * <p>Usage:
 * <pre>{@code
 * FakePipeAccess access = new FakePipeAccess();
 * PipeContext ctx = new PipeContext(null, BlockPos.ZERO, Blocks.STONE.defaultBlockState(), access);
 * }</pre>
 */
public class FakePipeAccess implements IPipeAccess {

    private final Map<String, CompoundTag> states = new HashMap<>();
    private final Map<Direction, PipeConnection.Type> connections = new EnumMap<>(Direction.class);
    private final List<TravelingItem> travelingItems = new ArrayList<>();
    private final List<TravelingItem> droppedItems = new ArrayList<>();
    private int capacity = PipeBlockEntity.VIRTUAL_CAPACITY;
    private @Nullable ILogisticsNetwork network = null;
    private boolean powered = false;
    private int poweredArmMask = 0;
    private @Nullable EnergyComponent energy = null;

    // ==================== IPipeAccess ====================

    @Override
    public CompoundTag moduleState(String key) {
        return states.computeIfAbsent(key, k -> new CompoundTag());
    }

    @Override
    public void clearModuleState(String key) {
        states.remove(key);
    }

    @Override
    public @Nullable EnergyComponent getEnergy() {
        return energy;
    }

    @Override
    public void markDirty() {
        // no-op in tests
    }

    @Override
    public void invalidateConnectionCache() {
        // no-op in tests
    }

    @Override
    public PipeConnection.Type getCachedConnectionType(Direction direction) {
        return connections.getOrDefault(direction, PipeConnection.Type.NONE);
    }

    @Override
    public PipeConnection.Type getConnectionType(Level world, BlockPos pos, Direction direction) {
        return getCachedConnectionType(direction);
    }

    @Override
    public boolean isNeighborPipe(Level world, BlockPos pos, Direction direction) {
        return getCachedConnectionType(direction) == PipeConnection.Type.PIPE;
    }

    @Override
    public boolean isPowered() {
        return powered;
    }

    @Override
    public @Nullable ILogisticsNetwork getNetwork() {
        return network;
    }

    @Override
    public List<TravelingItem> getTravelingItems() {
        return travelingItems;
    }

    /**
     * Mirrors {@code PipeBlockEntity.forceAddItem}: capacity is still enforced when ingress checks
     * are bypassed, so the pipe takes only what fits and everything it could not take is dropped in
     * world (recorded in {@link #getDroppedItems()}) with {@code false} returned.
     *
     * <p>An item accepted in full is stored as-is, so its delivery id, TTL and destination survive.
     * A partial accept is re-created at the accepted count, matching the real block entity.
     */
    @Override
    public boolean forceAddItem(TravelingItem item, Direction fromDirection) {
        int count = item.getStack().getCount();
        int accepted = Math.max(0, Math.min(count, capacity - getTotalItemCount()));

        if (accepted <= 0) {
            droppedItems.add(item);
            return false;
        }
        if (accepted == count) {
            travelingItems.add(item);
            return true;
        }

        travelingItems.add(portion(item, accepted, fromDirection));
        droppedItems.add(portion(item, count - accepted, fromDirection));
        return false;
    }

    private static TravelingItem portion(TravelingItem source, int count, Direction fromDirection) {
        ItemStack stack = source.getStack().copy();
        stack.setCount(count);
        TravelingItem portion =
                new TravelingItem(stack, fromDirection.getOpposite(), source.getSpeed(), source.getDestination());
        portion.setDeliveryId(source.getDeliveryId());
        portion.setRemainingTtl(source.getRemainingTtl());
        return portion;
    }

    private int getTotalItemCount() {
        int total = 0;
        for (TravelingItem item : travelingItems) total += item.getStack().getCount();
        return total;
    }

    // ==================== Test helpers ====================

    /** Configure the connection type for a direction. */
    public FakePipeAccess setConnection(Direction direction, PipeConnection.Type type) {
        connections.put(direction, type);
        return this;
    }

    /** Set all directions to the given connection type. */
    public FakePipeAccess setAllConnections(PipeConnection.Type type) {
        for (Direction d : Direction.values()) connections.put(d, type);
        return this;
    }

    /** Inject a logistics network. */
    public FakePipeAccess setNetwork(ILogisticsNetwork network) {
        this.network = network;
        return this;
    }

    @Override
    public int getPoweredArmMask() {
        return poweredArmMask;
    }

    /** Set the per-arm powered bitmask used for arm tinting. */
    public FakePipeAccess setPoweredArmMask(int mask) {
        this.poweredArmMask = mask;
        return this;
    }

    /** Set the redstone powered state. */
    public FakePipeAccess setPowered(boolean powered) {
        this.powered = powered;
        return this;
    }

    /** Set an energy component (for energy-consuming modules). */
    public FakePipeAccess setEnergy(EnergyComponent energy) {
        this.energy = energy;
        return this;
    }

    /** Return items that were injected via {@link #forceAddItem}. */
    public List<TravelingItem> getInjectedItems() {
        return List.copyOf(travelingItems);
    }

    /**
     * Shrink the pipe's item capacity, which defaults to the real
     * {@link PipeBlockEntity#VIRTUAL_CAPACITY}.
     */
    public FakePipeAccess setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * Items {@link #forceAddItem} could not fit and therefore spilled on the floor. Anything here
     * is an item a module handed to a full pipe after already taking it out of an inventory.
     */
    public List<TravelingItem> getDroppedItems() {
        return List.copyOf(droppedItems);
    }

    /** Return the raw module state tag for direct inspection in tests. */
    public CompoundTag getRawState(String key) {
        return states.computeIfAbsent(key, k -> new CompoundTag());
    }
}
