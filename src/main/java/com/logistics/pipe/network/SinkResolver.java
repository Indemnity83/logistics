package com.logistics.pipe.network;

import com.logistics.core.lib.network.INetworkGraph;
import com.logistics.core.lib.network.IWorldView;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Manages sink registration and resolution for a pipe network.
 *
 * <p>Tracks which positions are registered as sinks (and at what priority),
 * maintains per-item and generic interest indices to avoid scanning every pipe
 * on every routing decision, and resolves a destination for a given item using
 * the interest-index strategy from OG LogisticsPipes.
 *
 * <p>Owned by {@link PipeNetwork}; the public {@link com.logistics.core.lib.network.ILogisticsNetwork}
 * registration/resolution methods delegate here.
 */
class SinkResolver {
    // Maps sink position → priority (0 = catch-all/default-route, higher = preferred)
    private final Map<BlockPos, Integer> sinkRegistry = new HashMap<>();
    // Per-item interest index: item → sinks that declared interest in it
    private final Map<Item, Set<BlockPos>> specificInterests = new HashMap<>();
    // Sinks that accept any item (no specific filter)
    private final Set<BlockPos> genericInterests = new HashSet<>();

    private final INetworkGraph graph;
    private final IWorldView worldView;

    SinkResolver(INetworkGraph graph, IWorldView worldView) {
        this.graph = graph;
        this.worldView = worldView;
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    void registerSink(BlockPos pos, int priority) {
        sinkRegistry.put(pos, priority);
    }

    void unregisterSink(BlockPos pos) {
        sinkRegistry.remove(pos);
        specificInterests.values().forEach(set -> set.remove(pos));
        genericInterests.remove(pos);
    }

    void registerSinkInterest(BlockPos pos, Item item) {
        specificInterests.computeIfAbsent(item, k -> new HashSet<>()).add(pos);
    }

    void unregisterSinkInterests(BlockPos pos) {
        specificInterests.values().forEach(set -> set.remove(pos));
    }

    void registerGenericSinkInterest(BlockPos pos) {
        genericInterests.add(pos);
    }

    void unregisterGenericSinkInterest(BlockPos pos) {
        genericInterests.remove(pos);
    }

    /**
     * Remove all sink state for a pipe that has left the network
     * (equivalent to unregisterSink + clearing interest indices in one call).
     */
    void remove(BlockPos pos) {
        sinkRegistry.remove(pos);
        specificInterests.values().forEach(set -> set.remove(pos));
        genericInterests.remove(pos);
    }

    // -------------------------------------------------------------------------
    // Resolution
    // -------------------------------------------------------------------------

    int registeredSinkCount() {
        return sinkRegistry.size();
    }

    /**
     * Find the highest-priority registered sink that accepts the given item.
     *
     * @param stack item to route
     * @return best sink position, or null if none accepts
     */
    @Nullable
    BlockPos findSinkFor(ItemStack stack) {
        return findSink(stack, false);
    }

    /**
     * Find the highest-priority filtered (non-catch-all) sink that accepts the item.
     * Skips priority-0 default-route sinks.
     *
     * @param stack item to route
     * @return best filtered sink position, or null if none accepts
     */
    @Nullable
    BlockPos findFilteredSinkFor(ItemStack stack) {
        return findSink(stack, true);
    }

    /**
     * Core resolution: build candidate set from interest indices, filter to registered/reachable
     * sinks, sort by priority, then ask the world view which first accepts the item.
     *
     * <p>Uses the interest-index strategy (OG LogisticsPipes) to skip pipes that declared no
     * interest in this item type, avoiding expensive inventory scans for every network member.
     *
     * @param stack        item to route
     * @param filteredOnly if true, skip priority-0 (catch-all) sinks
     */
    @Nullable
    private BlockPos findSink(ItemStack stack, boolean filteredOnly) {
        // Candidate set = generic-interest pipes ∪ pipes interested in this specific item
        Set<BlockPos> candidates = new HashSet<>(genericInterests);
        Set<BlockPos> specific = specificInterests.get(stack.getItem());
        if (specific != null) candidates.addAll(specific);

        return candidates.stream()
                .filter(pos -> sinkRegistry.containsKey(pos) && graph.contains(pos))
                .filter(pos -> !filteredOnly || sinkRegistry.get(pos) > 0)
                .sorted(Comparator.comparingInt(pos -> -sinkRegistry.getOrDefault(pos, 0)))
                .filter(pos -> worldView.matchesSinkFilter(pos, stack))
                .findFirst()
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // Merge (called when two networks join)
    // -------------------------------------------------------------------------

    void merge(SinkResolver other) {
        other.sinkRegistry.forEach((pos, priority) ->
                sinkRegistry.merge(pos, priority, Math::max));
        genericInterests.addAll(other.genericInterests);
        other.specificInterests.forEach((item, positions) ->
                specificInterests.computeIfAbsent(item, k -> new HashSet<>()).addAll(positions));
    }
}
