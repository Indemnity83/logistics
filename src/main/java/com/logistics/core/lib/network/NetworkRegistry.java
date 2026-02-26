package com.logistics.core.lib.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

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

        // Check if position already belongs to a network
        UUID existingId = levelPositions.get(pos);
        if (existingId != null) {
            PipeNetwork existing = levelNetworks.get(existingId);
            if (existing != null) {
                return existing;
            }
        }

        // Scan neighbors for existing networks
        // Also recursively scan any pipe neighbors that aren't in a network yet
        Set<UUID> neighborNetworks = new HashSet<>();
        Set<BlockPos> unmappedPipes = new HashSet<>();

        for (BlockPos neighbor : getNeighbors(level, pos)) {
            // Check if neighbor is already in a network
            UUID neighborId = levelPositions.get(neighbor);
            if (neighborId != null) {
                neighborNetworks.add(neighborId);
            } else if (isPipe(level, neighbor)) {
                // Found a pipe that's not in any network yet
                unmappedPipes.add(neighbor);
            }
        }

        // Recursively scan unmapped pipes to find networks they connect to
        if (!unmappedPipes.isEmpty()) {
            Set<BlockPos> allUnmappedPipes = findConnectedUnmappedPipes(level, unmappedPipes, levelPositions);
            unmappedPipes = allUnmappedPipes;

            // Check if any of these unmapped pipes have neighbors in existing networks
            for (BlockPos unmappedPipe : allUnmappedPipes) {
                for (BlockPos unmappedNeighbor : getNeighbors(level, unmappedPipe)) {
                    UUID unmappedNeighborId = levelPositions.get(unmappedNeighbor);
                    if (unmappedNeighborId != null) {
                        neighborNetworks.add(unmappedNeighborId);
                    }
                }
            }
        }

        PipeNetwork network;
        UUID networkId;

        if (neighborNetworks.isEmpty()) {
            // Create new network
            networkId = UUID.randomUUID();
            network = new PipeNetwork(networkId);
            network.addPipe(pos);
            levelNetworks.put(networkId, network);
            levelPositions.put(pos, networkId);
        } else if (neighborNetworks.size() == 1) {
            // Join existing network
            networkId = neighborNetworks.iterator().next();
            network = levelNetworks.get(networkId);
            network.addPipe(pos);
            levelPositions.put(pos, networkId);
        } else {
            // Merge multiple networks
            network = mergeNetworks(level, pos, neighborNetworks, levelNetworks, levelPositions);
            networkId = levelPositions.get(pos);
        }

        // Add any unmapped pipe neighbors to this network
        for (BlockPos unmappedPipe : unmappedPipes) {
            network.addPipe(unmappedPipe);
            levelPositions.put(unmappedPipe, networkId);
        }

        return network;
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
        for (PipeNetwork network : levelNetworks.values()) {
            network.tick(gameTime);
        }
    }

    /**
     * Merge multiple networks into one.
     */
    private static PipeNetwork mergeNetworks(
        Level level,
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
            UUID otherId = iterator.next();
            PipeNetwork other = levelNetworks.get(otherId);

            if (other != null) {
                baseNetwork.merge(other);
                levelNetworks.remove(otherId);

                // Update position lookup
                for (BlockPos pos : other.getMembers()) {
                    levelPositions.put(pos, baseId);
                }
            }
        }

        // Add new pipe
        baseNetwork.addPipe(newPos);
        levelPositions.put(newPos, baseId);

        return baseNetwork;
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
        // Create network for first component
        UUID firstId = UUID.randomUUID();
        PipeNetwork firstNetwork = new PipeNetwork(firstId);
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
            PipeNetwork network = new PipeNetwork(id);
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

        // Only include neighbors where pipes are actually connected
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof com.logistics.pipe.block.PipeBlock pipeBlock) {
                com.logistics.core.lib.block.capability.PipeConnection.Type connectionType =
                    pipeBlock.getConnectionType(level, pos, direction);

                if (connectionType != com.logistics.core.lib.block.capability.PipeConnection.Type.NONE) {
                    // Check if neighbor is also a pipe
                    if (isPipe(level, neighborPos)) {
                        neighbors.add(neighborPos);
                    }
                }
            }
        }

        return neighbors;
    }

    /**
     * Check if a block at the given position is a pipe.
     */
    private static boolean isPipe(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof com.logistics.pipe.block.PipeBlock;
    }

    /**
     * Clear all networks (for testing).
     */
    public static void clear() {
        networks.clear();
        positionLookup.clear();
    }
}
