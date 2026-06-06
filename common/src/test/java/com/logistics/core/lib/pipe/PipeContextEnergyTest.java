package com.logistics.core.lib.pipe;

import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.network.IWorldView;
import com.logistics.core.lib.network.NetworkGraph;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.pipe.network.PipeNetwork;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link PipeContext#consumeEnergy(long)}. */
class PipeContextEnergyTest extends MinecraftTestEnvironment {

    private static PipeContext context(FakePipeAccess access) {
        return new PipeContext(null, BlockPos.ZERO, Blocks.STONE.defaultBlockState(), access);
    }

    @Test
    void consumeEnergy_noNetwork_returnsTrue() {
        // No network attached yet (bootstrap window) — modules must not stall.
        assertTrue(context(new FakePipeAccess()).consumeEnergy(100));
    }

    @Test
    void consumeEnergy_delegatesToNetwork() {
        BlockPos batPos = new BlockPos(0, 0, 0);
        EnergyComponent battery = new EnergyComponent(1000, 1000, 1000, () -> {});
        battery.setAmount(100);
        Map<BlockPos, IEnergyStorage> storages = new HashMap<>();
        storages.put(batPos, battery);

        IWorldView view = new IWorldView() {
            @Override public boolean isPipe(BlockPos pos) { return false; }
            @Override public List<BlockPos> getConnectedNeighbors(BlockPos pos) { return List.of(); }
            @Override public boolean matchesSinkFilter(BlockPos pos, ItemStack stack) { return false; }
            @Override public long dispatch(BlockPos p, BlockPos r, IItemKey i, long a, UUID d) { return 0; }
            @Override public boolean isClientSide() { return false; }
            @Override public void broadcastAlert(BlockPos pos, Component message) {}
            @Override public IEnergyStorage energyStorageAt(BlockPos pos) { return storages.get(pos); }
            @Override public long gameTime() { return 0L; }
        };
        PipeNetwork network = new PipeNetwork(UUID.randomUUID(), new NetworkGraph(), view);
        network.registerEnergySource(batPos);

        PipeContext ctx = context(new FakePipeAccess().setNetwork(network));
        assertTrue(ctx.consumeEnergy(60), "battery has 100 RF, should cover 60");
        assertFalse(ctx.consumeEnergy(60), "only 40 RF left, should gate");
        assertEquals(40, battery.getAmount());
    }
}
