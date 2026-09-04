package com.logistics.pipe.network;

import com.logistics.core.LogisticsProfiler;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.network.INetworkGraph;
import com.logistics.core.lib.network.IWorldView;
import com.logistics.core.lib.network.NetworkGraph;
import com.logistics.pipe.block.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Global registry for pipe networks.
 * Handles network discovery, merging, and splitting.
 */
public class NetworkRegistry {
    private static final Map<Level, Map<UUID, PipeNetwork>> networks = new HashMap<>();
    private static final Map<Level, Map<BlockPos, UUID>> positionLookup = new HashMap<>();

    /**
     * Get or create network for a pipe at the given position.
     * Scans neighbors and merges networks if necessary.
     */
    public static PipeNetwork getOrCreateNetwork(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return null; // Networks only exist on server
        }

        Map<UUID, PipeNetwork> levelNetworks = networks.computeIfAbsent(level, k -> new HashMap<>());
        Map<BlockPos, UUID> levelPositions = positionLookup.computeIfAbsent(level, k -> new HashMap<>());

        // Return existing network if position is already mapped
        PipeNetwork existing = getExistingNetworkForPosition(pos, levelNetworks, levelPositions);
        if (existing != null) {
            return existing;
        }

        // Find neighbor networks and unmapped pipes
        NetworkScanResult scanResult = findNeighborNetworks(level, pos, levelPositions);

        // Create, join, or merge networks as appropriate
        PipeNetwork network = createOrJoinNetwork(
            level, pos, scanResult.neighborNetworks(), levelNetworks, levelPositions
        );

        // Add any unmapped pipes to the network
        addUnmappedPipesToNetwork(network, scanResult.unmappedPipes(), levelPositions);

        return network;
    }

    /**
     * Get existing network for a position, or null if not mapped.
     */
    private static PipeNetwork getExistingNetworkForPosition(
        BlockPos pos,
        Map<UUID, PipeNetwork> levelNetworks,
        Map<BlockPos, UUID> levelPositions
    ) {
        UUID existingId = levelPositions.get(pos);
        return existingId != null ? levelNetworks.get(existingId) : null;
    }

    /**
     * Result of scanning neighbors for networks and unmapped pipes.
     */
    private record NetworkScanResult(Set<UUID> neighborNetworks, Set<BlockPos> unmappedPipes) {}

    /**
     * Scan neighbors to find existing networks and unmapped pipes.
     */
    private static NetworkScanResult findNeighborNetworks(
        Level level,
        BlockPos pos,
        Map<BlockPos, UUID> levelPositions
    ) {
        Set<UUID> neighborNetworks = new HashSet<>();
        Set<BlockPos> unmappedPipes = new HashSet<>();

        // Check immediate neighbors
        scanImmediateNeighbors(level, pos, levelPositions, neighborNetworks, unmappedPipes);

        // Recursively find all connected unmapped pipes and their adjacent networks
        if (!unmappedPipes.isEmpty()) {
            unmappedPipes = findConnectedUnmappedPipes(level, unmappedPipes, levelPositions);
            addNetworksAdjacentToUnmappedPipes(level, unmappedPipes, levelPositions, neighborNetworks);
        }

        return new NetworkScanResult(neighborNetworks, unmappedPipes);
    }

    /**
     * Scan immediate neighbors and categorize them into networks or unmapped pipes.
     */
    private static void scanImmediateNeighbors(
        Level level,
        BlockPos pos,
        Map<BlockPos, UUID> levelPositions,
        Set<UUID> neighborNetworks,
        Set<BlockPos> unmappedPipes
    ) {
        for (BlockPos neighbor : getNeighbors(level, pos)) {
            UUID neighborId = levelPositions.get(neighbor);
            if (neighborId != null) {
                neighborNetworks.add(neighborId);
            } else if (isPipe(level, neighbor)) {
                unmappedPipes.add(neighbor);
            }
        }
    }

    /**
     * Create a new network, join an existing one, or merge multiple networks.
     */
    private static PipeNetwork createOrJoinNetwork(
        Level level,
        BlockPos pos,
        Set<UUID> neighborNetworks,
        Map<UUID, PipeNetwork> levelNetworks,
        Map<BlockPos, UUID> levelPositions
    ) {
        if (neighborNetworks.isEmpty()) {
            return createNewNetwork(level, pos, levelNetworks, levelPositions);
        } else if (neighborNetworks.size() == 1) {
            return joinExistingNetwork(pos, neighborNetworks.iterator().next(), levelNetworks, levelPositions);
        } else {
            NetDbg.out("[Network] Merging {} networks at {}", neighborNetworks.size(), pos);
            return mergeNetworks(pos, neighborNetworks, levelNetworks, levelPositions);
        }
    }

    /**
     * Create a new network for a single pipe.
     */
    private static PipeNetwork createNewNetwork(
        Level level,
        BlockPos pos,
        Map<UUID, PipeNetwork> levelNetworks,
        Map<BlockPos, UUID> levelPositions
    ) {
        UUID networkId = UUID.randomUUID();
        IWorldView worldView = new MinecraftWorldView(level);
        INetworkGraph graph = new NetworkGraph();
        PipeNetwork network = new PipeNetwork(networkId, graph, worldView);
        network.addPipe(pos);
        levelNetworks.put(networkId, network);
        levelPositions.put(pos, networkId);
        NetDbg.out("[Network] Created new network {} with pipe at {}",
            PipeNetwork.getNetworkIdShort(networkId), pos);
        return network;
    }

    /**
     * Join a pipe to an existing network.
     */
    private static PipeNetwork joinExistingNetwork(
        BlockPos pos,
        UUID networkId,
        Map<UUID, PipeNetwork> levelNetworks,
        Map<BlockPos, UUID> levelPositions
    ) {
        PipeNetwork network = levelNetworks.get(networkId);
        network.addPipe(pos);
        levelPositions.put(pos, networkId);
        NetDbg.out("[Network {}] Pipe joined at {} (total pipes: {})",
            PipeNetwork.getNetworkIdShort(networkId), pos, network.size());
        return network;
    }

    /**
     * Add unmapped pipes to a network.
     */
    private static void addUnmappedPipesToNetwork(
        PipeNetwork network,
        Set<BlockPos> unmappedPipes,
        Map<BlockPos, UUID> levelPositions
    ) {
        if (unmappedPipes.isEmpty()) {
            return;
        }

        UUID networkId = network.getId();
        for (BlockPos unmappedPipe : unmappedPipes) {
            network.addPipe(unmappedPipe);
            levelPositions.put(unmappedPipe, networkId);
        }
        NetDbg.out("[Network {}] Added {} unmapped pipes (total pipes: {})",
            PipeNetwork.getNetworkIdShort(networkId), unmappedPipes.size(), network.size());
    }

    /**
     * Remove a pipe from its network.
     * May split network if pipe was a bridge.
     */
    public static void removePipe(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }

        Map<UUID, PipeNetwork> levelNetworks = networks.get(level);
        Map<BlockPos, UUID> levelPositions = positionLookup.get(level);

        if (levelNetworks == null || levelPositions == null) {
            return;
        }

        UUID networkId = levelPositions.remove(pos);
        if (networkId == null) {
            return;
        }

        PipeNetwork network = levelNetworks.get(networkId);
        if (network == null) {
            return;
        }

        network.removePipe(pos);

        // Check if network needs to be split
        if (network.size() > 0) {
            checkNetworkSplit(level, pos, network, levelNetworks, levelPositions);
        } else {
            levelNetworks.remove(networkId);
        }
    }

    /**
     * Get network for a specific position.
     */
    public static PipeNetwork getNetwork(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return null;
        }

        Map<UUID, PipeNetwork> levelNetworks = networks.get(level);
        Map<BlockPos, UUID> levelPositions = positionLookup.get(level);

        if (levelNetworks == null || levelPositions == null) {
            return null;
        }

        UUID networkId = levelPositions.get(pos);
        return networkId != null ? levelNetworks.get(networkId) : null;
    }

    /**
     * Tick all networks across all levels on a server.
     */
    public static void tickNetworks(MinecraftServer server) {
        for (var level : server.getAllLevels()) {
            tickNetworks(level);
        }
    }

    /**
     * Tick all networks in a level.
     */
    public static void tickNetworks(Level level) {
        if (level.isClientSide()) {
            return;
        }

        Map<UUID, PipeNetwork> levelNetworks = networks.get(level);
        if (levelNetworks == null) {
            return;
        }

        long gameTime = level.getGameTime();
        LogisticsProfiler.push("item_networks");
        try {
            for (PipeNetwork network : levelNetworks.values()) {
                network.tick(gameTime);
            }
        } finally {
            LogisticsProfiler.pop();
        }
    }

    /**
     * Merge multiple networks into one.
     */
    private static PipeNetwork mergeNetworks(
        BlockPos newPos,
        Set<UUID> networkIds,
        Map<UUID, PipeNetwork> levelNetworks,
        Map<BlockPos, UUID> levelPositions
    ) {
        // Pick first network as base
        Iterator<UUID> iterator = networkIds.iterator();
        UUID baseId = iterator.next();
        PipeNetwork baseNetwork = levelNetworks.get(baseId);

        // Merge others into base
        while (iterator.hasNext()) {
            mergeOneNetworkIntoBase(iterator.next(), baseId, baseNetwork, levelNetworks, levelPositions);
        }

        // Add new pipe
        baseNetwork.addPipe(newPos);
        levelPositions.put(newPos, baseId);

        return baseNetwork;
    }

    /**
     * Merge a single network into the base network and update lookups.
     */
    private static void mergeOneNetworkIntoBase(
        UUID otherId,
        UUID baseId,
        PipeNetwork baseNetwork,
        Map<UUID, PipeNetwork> levelNetworks,
        Map<BlockPos, UUID> levelPositions
    ) {
        PipeNetwork other = levelNetworks.get(otherId);
        if (other == null) {
            return;
        }

        baseNetwork.merge(other);
        levelNetworks.remove(otherId);

        // Update position lookup for all members of merged network
        for (BlockPos pos : other.getMembers()) {
            levelPositions.put(pos, baseId);
        }
    }

    /**
     * Check if removing a pipe split the network.
     * Uses flood-fill to detect disconnected components.
     */
    private static void checkNetworkSplit(
        Level level,
        BlockPos removedPos,
        PipeNetwork network,
        Map<UUID, PipeNetwork> levelNetworks,
        Map<BlockPos, UUID> levelPositions
    ) {
        Set<BlockPos> members = new HashSet<>(network.getMembers());
        if (members.isEmpty()) {
            return;
        }

        // Pick arbitrary starting point
        BlockPos start = members.iterator().next();
        Set<BlockPos> reachable = floodFill(level, start, members);

        if (reachable.size() == members.size()) {
            // Network still connected
            return;
        }

        // Network split - create new networks for each component
        levelNetworks.remove(network.getId());
        for (BlockPos pos : members) {
            levelPositions.remove(pos);
        }

        createNetworksFromComponents(level, members, reachable, levelNetworks, levelPositions);
    }

    /**
     * Find all unmapped pipes connected to the given set of unmapped pipes.
     * Uses flood fill to traverse through unmapped pipes only.
     */
    private static Set<BlockPos> findConnectedUnmappedPipes(
        Level level,
        Set<BlockPos> startingPipes,
        Map<BlockPos, UUID> levelPositions
    ) {
        Set<BlockPos> visited = new HashSet<>(startingPipes);
        Queue<BlockPos> queue = new LinkedList<>(startingPipes);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (BlockPos neighbor : getNeighbors(level, current)) {
                // Only traverse through unmapped pipes
                if (!visited.contains(neighbor) &&
                    levelPositions.get(neighbor) == null &&
                    isPipe(level, neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }

    /**
     * Find all networks that are adjacent to the given unmapped pipes.
     * Adds found network IDs to the neighborNetworks set.
     */
    private static void addNetworksAdjacentToUnmappedPipes(
        Level level,
        Set<BlockPos> unmappedPipes,
        Map<BlockPos, UUID> levelPositions,
        Set<UUID> neighborNetworks
    ) {
        for (BlockPos unmappedPipe : unmappedPipes) {
            for (BlockPos neighbor : getNeighbors(level, unmappedPipe)) {
                UUID neighborId = levelPositions.get(neighbor);
                if (neighborId != null) {
                    neighborNetworks.add(neighborId);
                }
            }
        }
    }

    /**
     * Flood-fill to find connected component.
     */
    private static Set<BlockPos> floodFill(Level level, BlockPos start, Set<BlockPos> available) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (BlockPos neighbor : getNeighbors(level, current)) {
                if (available.contains(neighbor) && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }

    /**
     * Create new networks from disconnected components.
     */
    private static void createNetworksFromComponents(
        Level level,
        Set<BlockPos> allMembers,
        Set<BlockPos> firstComponent,
        Map<UUID, PipeNetwork> levelNetworks,
        Map<BlockPos, UUID> levelPositions
    ) {
        IWorldView worldView = new MinecraftWorldView(level);

        // Create network for first component
        UUID firstId = UUID.randomUUID();
        INetworkGraph firstGraph = new NetworkGraph();
        PipeNetwork firstNetwork = new PipeNetwork(firstId, firstGraph, worldView);
        for (BlockPos pos : firstComponent) {
            firstNetwork.addPipe(pos);
            levelPositions.put(pos, firstId);
        }
        levelNetworks.put(firstId, firstNetwork);

        // Find remaining components
        Set<BlockPos> remaining = new HashSet<>(allMembers);
        remaining.removeAll(firstComponent);

        while (!remaining.isEmpty()) {
            BlockPos start = remaining.iterator().next();
            Set<BlockPos> component = floodFill(level, start, remaining);

            UUID id = UUID.randomUUID();
            INetworkGraph graph = new NetworkGraph();
            PipeNetwork network = new PipeNetwork(id, graph, worldView);
            for (BlockPos pos : component) {
                network.addPipe(pos);
                levelPositions.put(pos, id);
            }
            levelNetworks.put(id, network);

            remaining.removeAll(component);
        }
    }

    private static List<BlockPos> getNeighbors(Level level, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();

        // Ask *this* pipe whether it connects each way. Reading the block out of the neighbour's
        // state would evaluate this pipe's coordinates against the neighbour's module policy.
        if (!(level.getBlockState(pos).getBlock() instanceof PipeBlock pipeBlock)) {
            return neighbors;
        }

        // Only include neighbors where pipes are actually connected
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            if (pipeBlock.getConnectionType(level, pos, direction) != PipeConnection.Type.NONE
                    && isPipe(level, neighborPos)) {
                neighbors.add(neighborPos);
            }
        }

        return neighbors;
    }

    /**
     * Check if a block at the given position is a pipe.
     */
    private static boolean isPipe(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof PipeBlock;
    }

    /**
     * Clear all networks for a level when it is unloaded.
     */
    public static void clearLevel(Level level) {
        networks.remove(level);
        positionLookup.remove(level);
    }

    /**
     * Clear all networks (for testing).
     */
    public static void clear() {
        networks.clear();
        positionLookup.clear();
    }
}
