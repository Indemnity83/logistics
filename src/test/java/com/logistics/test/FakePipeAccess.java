package com.logistics.test;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.IPipeAccess;
import com.logistics.core.lib.pipe.TravelingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
    private @Nullable ILogisticsNetwork network = null;
    private boolean powered = false;
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

    @Override
    public boolean forceAddItem(TravelingItem item, Direction fromDirection) {
        travelingItems.add(item);
        return true;
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

    /** Return the raw module state tag for direct inspection in tests. */
    public CompoundTag getRawState(String key) {
        return states.getOrDefault(key, new CompoundTag());
    }
}
