package com.logistics.core.lib.network;

import com.logistics.pipe.network.PipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PipeNetwork.
 * Note: NetworkRegistry requires a Level instance and is tested via game tests.
 * These tests focus on PipeNetwork internals.
 */
class NetworkRegistryTest {
    @Test
    void testPipeNetworkCreation() {
        PipeNetwork network = new PipeNetwork(UUID.randomUUID());
        BlockPos pos = new BlockPos(0, 0, 0);

        network.addPipe(pos);

        assertTrue(network.contains(pos));
        assertEquals(1, network.size());
    }

    @Test
    void testPipeNetworkMerging() {
        PipeNetwork network1 = new PipeNetwork(UUID.randomUUID());
        PipeNetwork network2 = new PipeNetwork(UUID.randomUUID());

        BlockPos pos1 = new BlockPos(0, 0, 0);
        BlockPos pos2 = new BlockPos(1, 0, 0);

        network1.addPipe(pos1);
        network2.addPipe(pos2);

        network1.merge(network2);

        assertTrue(network1.contains(pos1));
        assertTrue(network1.contains(pos2));
        assertEquals(2, network1.size());
    }

    @Test
    void testPipeNetworkRemoval() {
        PipeNetwork network = new PipeNetwork(UUID.randomUUID());
        BlockPos pos = new BlockPos(0, 0, 0);

        network.addPipe(pos);
        assertEquals(1, network.size());

        network.removePipe(pos);
        assertEquals(0, network.size());
        assertFalse(network.contains(pos));
    }

    @Test
    void testPipeNetworkPathfinding() {
        PipeNetwork network = new PipeNetwork(UUID.randomUUID());

        // Create linear path
        BlockPos pos1 = new BlockPos(0, 0, 0);
        BlockPos pos2 = new BlockPos(1, 0, 0);
        BlockPos pos3 = new BlockPos(2, 0, 0);

        network.addPipe(pos1);
        network.addPipe(pos2);
        network.addPipe(pos3);

        var path = network.findPath(pos1, pos3);
        assertNotNull(path);
        assertEquals(3, path.size());
        assertEquals(pos1, path.get(0));
        assertEquals(pos3, path.get(2));
    }

    @Test
    void testPipeNetworkNextHop() {
        PipeNetwork network = new PipeNetwork(UUID.randomUUID());

        BlockPos pos1 = new BlockPos(0, 0, 0);
        BlockPos pos2 = new BlockPos(1, 0, 0);
        BlockPos pos3 = new BlockPos(2, 0, 0);

        network.addPipe(pos1);
        network.addPipe(pos2);
        network.addPipe(pos3);

        var direction = network.getNextHop(pos1, pos3);
        assertNotNull(direction);
        assertEquals(Direction.EAST, direction);
    }

    // Note: Provider cache tests require Minecraft bootstrap and are tested via game tests
}
