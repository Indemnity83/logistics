package com.logistics.power.cable;

import com.logistics.core.LogisticsProfiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Manages cable networks per-level. Handles network discovery, merging, and splitting
 * when cables are placed or removed.
 *
 * <p>Networks are rebuilt lazily: adding or removing a cable marks the manager dirty,
 * and the next tick rebuilds all affected networks via flood-fill.
 */
public class CableNetworkManager {
    private static final Map<Level, CableNetworkManager> INSTANCES = new WeakHashMap<>();

    private final Set<BlockPos> allCables = new HashSet<>();
    private final List<CableNetwork> networks = new ArrayList<>();
    private boolean dirty = true;

    private long loadedCheckGameTime = Long.MIN_VALUE;
    private boolean networksFullyLoaded = false;

    public static CableNetworkManager get(Level level) {
        return INSTANCES.computeIfAbsent(level, k -> new CableNetworkManager());
    }

    public static void clearLevel(Level level) {
        INSTANCES.remove(level);
    }

    public void addCable(BlockPos pos) {
        if (allCables.add(pos.immutable())) {
            dirty = true;
        }
    }

    public void removeCable(BlockPos pos) {
        if (allCables.remove(pos)) {
            dirty = true;
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public long insert(
            Level level, BlockPos cablePos, @Nullable Direction sourceSide,
            long maxAmount, boolean simulate) {
        if (maxAmount <= 0
                || !isPositionLoaded(level, cablePos)
                || !(level.getBlockEntity(cablePos) instanceof CableBlockEntity)) {
            return 0;
        }

        addCable(cablePos);
        if (dirty || hasUnloadedNetworkPositions(level)) {
            rebuildNetworks(level);
            dirty = false;
        }

        for (CableNetwork network : networks) {
            if (network.contains(cablePos)) {
                return network.insert(level, cablePos, sourceSide, maxAmount, simulate);
            }
        }

        CableNetwork network = CableNetwork.buildFrom(level, cablePos);
        networks.add(network);
        allCables.addAll(network.getCablePositions());
        return network.insert(level, cablePos, sourceSide, maxAmount, simulate);
    }

    /**
     * Ticks all cable networks in this level.
     * Called once per server tick from the power domain's tick handler.
     */
    public void tick(Level level) {
        LogisticsProfiler.push("power_cables");
        try {
            if (dirty || hasUnloadedNetworkPositions(level)) {
                rebuildNetworks(level);
                dirty = false;
            }

            for (CableNetwork network : networks) {
                network.tick(level);
            }
        } finally {
            LogisticsProfiler.pop();
        }
    }

    /**
     * Whether any network still holds a cable whose chunk has gone away, checked once per tick.
     *
     * <p>A cable in an unloading chunk is never handed back — the block entity is discarded
     * without the block being removed — so the networks are swept for orphans instead. Sweeping
     * every cable on every insert made the check scale with pushes rather than with cables, and
     * a chunk cannot unload between two pushes: unloads are processed before a level ticks its
     * block entities.
     */
    private boolean hasUnloadedNetworkPositions(Level level) {
        long gameTime = level.getGameTime();
        if (loadedCheckGameTime != gameTime) {
            loadedCheckGameTime = gameTime;
            networksFullyLoaded = allNetworksLoaded(level);
        }
        return !networksFullyLoaded;
    }

    private boolean allNetworksLoaded(Level level) {
        for (CableNetwork network : networks) {
            if (!network.allPositionsLoaded(level)) {
                return false;
            }
        }
        return true;
    }

    private void rebuildNetworks(Level level) {
        networks.clear();

        Set<BlockPos> unvisited = new HashSet<>(allCables);

        // Remove any positions that no longer have cable block entities
        unvisited.removeIf(pos -> !isPositionLoaded(level, pos)
                || !(level.getBlockEntity(pos) instanceof CableBlockEntity));
        allCables.retainAll(unvisited);

        while (!unvisited.isEmpty()) {
            BlockPos start = unvisited.iterator().next();
            CableNetwork network = CableNetwork.buildFrom(level, start);
            if (network.isEmpty()) {
                unvisited.remove(start);
                continue;
            }
            networks.add(network);
            allCables.addAll(network.getCablePositions());
            unvisited.removeAll(network.getCablePositions());
        }

        // Flood-fill only ever adds loaded positions, so the rebuild is itself a fresh answer to
        // the orphan check — without this the same tick's later inserts would rebuild again.
        loadedCheckGameTime = level.getGameTime();
        networksFullyLoaded = true;
    }

    private static boolean isPositionLoaded(Level level, BlockPos pos) {
        return level.isLoaded(pos);
    }

    /**
     * Ticks cable networks for all loaded levels.
     */
    public static void tickAll(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            CableNetworkManager manager = INSTANCES.get(level);
            if (manager != null) {
                manager.tick(level);
            }
        }
    }
}
