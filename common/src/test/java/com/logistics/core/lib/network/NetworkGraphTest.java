package com.logistics.core.lib.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for NetworkGraph.
 * Pure Java tests - no Minecraft bootstrap required.
 */
class NetworkGraphTest {
    private NetworkGraph graph;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
    }

    @Test
    void testAddNode() {
        BlockPos pos = new BlockPos(0, 64, 0);

        graph.addNode(pos);

        assertTrue(graph.contains(pos));
        assertEquals(1, graph.size());
    }

    @Test
    void testAddDuplicateNode() {
        BlockPos pos = new BlockPos(0, 64, 0);

        graph.addNode(pos);
        graph.addNode(pos);

        assertEquals(1, graph.size(), "Adding duplicate should not increase size");
    }

    @Test
    void testRemoveNode() {
        BlockPos pos = new BlockPos(0, 64, 0);
        graph.addNode(pos);

        graph.removeNode(pos);

        assertFalse(graph.contains(pos));
        assertEquals(0, graph.size());
    }

    @Test
    void testRemoveNonexistentNode() {
        BlockPos pos = new BlockPos(0, 64, 0);

        graph.removeNode(pos);

        assertEquals(0, graph.size(), "Removing nonexistent node should not affect size");
    }

    @Test
    void testContains() {
        BlockPos pos1 = new BlockPos(0, 64, 0);
        BlockPos pos2 = new BlockPos(1, 64, 0);

        graph.addNode(pos1);

        assertTrue(graph.contains(pos1));
        assertFalse(graph.contains(pos2));
    }

    @Test
    void testGetNodes() {
        BlockPos pos1 = new BlockPos(0, 64, 0);
        BlockPos pos2 = new BlockPos(1, 64, 0);

        graph.addNode(pos1);
        graph.addNode(pos2);

        Set<BlockPos> nodes = graph.getNodes();
        assertEquals(2, nodes.size());
        assertTrue(nodes.contains(pos1));
        assertTrue(nodes.contains(pos2));
    }

    @Test
    void testGetNodesReturnsUnmodifiableSet() {
        BlockPos pos = new BlockPos(0, 64, 0);
        graph.addNode(pos);

        Set<BlockPos> nodes = graph.getNodes();

        assertThrows(UnsupportedOperationException.class, () -> {
            nodes.add(new BlockPos(1, 64, 0));
        });
    }

    @Test
    void testFindPathSimpleLine() {
        // Create a line: 0,0,0 -> 1,0,0 -> 2,0,0
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos mid = new BlockPos(1, 0, 0);
        BlockPos end = new BlockPos(2, 0, 0);

        graph.addNode(start);
        graph.addNode(mid);
        graph.addNode(end);

        List<BlockPos> path = graph.findPath(start, end);

        assertNotNull(path);
        assertEquals(3, path.size());
        assertEquals(start, path.get(0));
        assertEquals(mid, path.get(1));
        assertEquals(end, path.get(2));
    }

    @Test
    void testFindPathNoPath() {
        // Two disconnected nodes
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos end = new BlockPos(10, 10, 10);

        graph.addNode(start);
        graph.addNode(end);

        List<BlockPos> path = graph.findPath(start, end);

        assertNull(path, "Should return null when no path exists");
    }

    @Test
    void testFindPathStartNotInGraph() {
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos end = new BlockPos(1, 0, 0);

        graph.addNode(end);

        List<BlockPos> path = graph.findPath(start, end);

        assertNull(path, "Should return null when start not in graph");
    }

    @Test
    void testFindPathEndNotInGraph() {
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos end = new BlockPos(1, 0, 0);

        graph.addNode(start);

        List<BlockPos> path = graph.findPath(start, end);

        assertNull(path, "Should return null when end not in graph");
    }

    @Test
    void testGetNextHopEast() {
        // Create line going east: 0,0,0 -> 1,0,0
        BlockPos current = new BlockPos(0, 0, 0);
        BlockPos next = new BlockPos(1, 0, 0);

        graph.addNode(current);
        graph.addNode(next);

        Direction direction = graph.getNextHop(current, next);

        assertEquals(Direction.EAST, direction);
    }

    @Test
    void testGetNextHopWest() {
        BlockPos current = new BlockPos(1, 0, 0);
        BlockPos next = new BlockPos(0, 0, 0);

        graph.addNode(current);
        graph.addNode(next);

        Direction direction = graph.getNextHop(current, next);

        assertEquals(Direction.WEST, direction);
    }

    @Test
    void testGetNextHopUp() {
        BlockPos current = new BlockPos(0, 0, 0);
        BlockPos next = new BlockPos(0, 1, 0);

        graph.addNode(current);
        graph.addNode(next);

        Direction direction = graph.getNextHop(current, next);

        assertEquals(Direction.UP, direction);
    }

    @Test
    void testGetNextHopDown() {
        BlockPos current = new BlockPos(0, 1, 0);
        BlockPos next = new BlockPos(0, 0, 0);

        graph.addNode(current);
        graph.addNode(next);

        Direction direction = graph.getNextHop(current, next);

        assertEquals(Direction.DOWN, direction);
    }

    @Test
    void testGetNextHopSouth() {
        BlockPos current = new BlockPos(0, 0, 0);
        BlockPos next = new BlockPos(0, 0, 1);

        graph.addNode(current);
        graph.addNode(next);

        Direction direction = graph.getNextHop(current, next);

        assertEquals(Direction.SOUTH, direction);
    }

    @Test
    void testGetNextHopNorth() {
        BlockPos current = new BlockPos(0, 0, 1);
        BlockPos next = new BlockPos(0, 0, 0);

        graph.addNode(current);
        graph.addNode(next);

        Direction direction = graph.getNextHop(current, next);

        assertEquals(Direction.NORTH, direction);
    }

    @Test
    void testGetNextHopNoPath() {
        BlockPos current = new BlockPos(0, 0, 0);
        BlockPos destination = new BlockPos(10, 10, 10);

        graph.addNode(current);
        graph.addNode(destination);

        Direction direction = graph.getNextHop(current, destination);

        assertNull(direction, "Should return null when no path exists");
    }

    @Test
    void testGetNextHopMultiStep() {
        // Create path: 0,0,0 -> 1,0,0 -> 2,0,0
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos mid = new BlockPos(1, 0, 0);
        BlockPos end = new BlockPos(2, 0, 0);

        graph.addNode(start);
        graph.addNode(mid);
        graph.addNode(end);

        // From start to end, next hop should be EAST (toward mid)
        Direction direction = graph.getNextHop(start, end);

        assertEquals(Direction.EAST, direction);
    }

    @Test
    void testMerge() {
        BlockPos pos1 = new BlockPos(0, 0, 0);
        BlockPos pos2 = new BlockPos(1, 0, 0);
        BlockPos pos3 = new BlockPos(2, 0, 0);

        graph.addNode(pos1);
        graph.addNode(pos2);

        NetworkGraph other = new NetworkGraph();
        other.addNode(pos2); // Overlapping node
        other.addNode(pos3);

        graph.merge(other);

        assertEquals(3, graph.size());
        assertTrue(graph.contains(pos1));
        assertTrue(graph.contains(pos2));
        assertTrue(graph.contains(pos3));
    }

    @Test
    void testSize() {
        assertEquals(0, graph.size());

        graph.addNode(new BlockPos(0, 0, 0));
        assertEquals(1, graph.size());

        graph.addNode(new BlockPos(1, 0, 0));
        assertEquals(2, graph.size());

        graph.removeNode(new BlockPos(0, 0, 0));
        assertEquals(1, graph.size());
    }
}
