package com.logistics.core.lib.network;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NetworkPathfinderTest {
    @Test
    void testFindShortestPath() {
        // Create linear 3-pipe path: (0,0,0) -> (1,0,0) -> (2,0,0)
        Set<BlockPos> network = Set.of(
            new BlockPos(0, 0, 0),
            new BlockPos(1, 0, 0),
            new BlockPos(2, 0, 0)
        );

        List<BlockPos> path = NetworkPathfinder.findPath(
            new BlockPos(0, 0, 0),
            new BlockPos(2, 0, 0),
            network
        );

        assertNotNull(path);
        assertEquals(3, path.size());
        assertEquals(new BlockPos(0, 0, 0), path.get(0));
        assertEquals(new BlockPos(1, 0, 0), path.get(1));
        assertEquals(new BlockPos(2, 0, 0), path.get(2));
    }

    @Test
    void testNoPathForDisconnected() {
        // Two disconnected pipes
        Set<BlockPos> network = Set.of(
            new BlockPos(0, 0, 0),
            new BlockPos(10, 10, 10)
        );

        List<BlockPos> path = NetworkPathfinder.findPath(
            new BlockPos(0, 0, 0),
            new BlockPos(10, 10, 10),
            network
        );

        assertNull(path);
    }

    @Test
    void testComplexNetwork() {
        // Create T-junction network
        //     (0,1,0)
        //        |
        // (0,0,0)-(1,0,0)-(2,0,0)
        Set<BlockPos> network = new HashSet<>();
        network.add(new BlockPos(0, 0, 0));
        network.add(new BlockPos(1, 0, 0));
        network.add(new BlockPos(2, 0, 0));
        network.add(new BlockPos(0, 1, 0));

        List<BlockPos> path = NetworkPathfinder.findPath(
            new BlockPos(0, 1, 0),
            new BlockPos(2, 0, 0),
            network
        );

        assertNotNull(path);
        assertEquals(4, path.size());
        assertEquals(new BlockPos(0, 1, 0), path.get(0));
        assertEquals(new BlockPos(2, 0, 0), path.get(3));
    }

    @Test
    void testSamePosReturnsPath() {
        Set<BlockPos> network = Set.of(new BlockPos(0, 0, 0));

        List<BlockPos> path = NetworkPathfinder.findPath(
            new BlockPos(0, 0, 0),
            new BlockPos(0, 0, 0),
            network
        );

        assertNotNull(path);
        assertEquals(1, path.size());
        assertEquals(new BlockPos(0, 0, 0), path.get(0));
    }

    @Test
    void testStartNotInNetwork() {
        Set<BlockPos> network = Set.of(new BlockPos(0, 0, 0));

        List<BlockPos> path = NetworkPathfinder.findPath(
            new BlockPos(10, 10, 10),
            new BlockPos(0, 0, 0),
            network
        );

        assertNull(path);
    }

    @Test
    void testGoalNotInNetwork() {
        Set<BlockPos> network = Set.of(new BlockPos(0, 0, 0));

        List<BlockPos> path = NetworkPathfinder.findPath(
            new BlockPos(0, 0, 0),
            new BlockPos(10, 10, 10),
            network
        );

        assertNull(path);
    }

    @Test
    void testLargeNetwork() {
        // Create 10x10 grid of pipes
        Set<BlockPos> network = new HashSet<>();
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 10; z++) {
                network.add(new BlockPos(x, 0, z));
            }
        }

        List<BlockPos> path = NetworkPathfinder.findPath(
            new BlockPos(0, 0, 0),
            new BlockPos(9, 0, 9),
            network
        );

        assertNotNull(path);
        // Manhattan distance is 18, optimal path length is 19 blocks (start + 18 moves)
        assertEquals(19, path.size());
        assertEquals(new BlockPos(0, 0, 0), path.get(0));
        assertEquals(new BlockPos(9, 0, 9), path.get(18));
    }
}
