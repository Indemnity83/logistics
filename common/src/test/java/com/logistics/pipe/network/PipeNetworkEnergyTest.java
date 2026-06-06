package com.logistics.pipe.network;

import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.network.IWorldView;
import com.logistics.core.lib.network.NetworkGraph;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PipeNetwork}'s energy sourcing — the all-or-nothing
 * {@link com.logistics.core.lib.network.ILogisticsNetwork#consumeEnergy} contract and the
 * pos-based source registration that re-resolves storage from the world on each call.
 */
class PipeNetworkEnergyTest extends MinecraftTestEnvironment {

    private static final BlockPos BAT_A = new BlockPos(0, 0, 0);
    private static final BlockPos BAT_B = new BlockPos(5, 0, 0);
    private static final BlockPos BAT_C = new BlockPos(10, 0, 0);

    // Stub world view resolves a registered battery position to its live storage (or null if gone).
    private final Map<BlockPos, IEnergyStorage> storages = new HashMap<>();

    // Controllable game time so isPowered()'s per-tick cache can be exercised.
    private long stubTick = 0;

    private final IWorldView stubView = new IWorldView() {
        @Override public boolean isPipe(BlockPos pos) { return false; }
        @Override public List<BlockPos> getConnectedNeighbors(BlockPos pos) { return List.of(); }
        @Override public boolean matchesSinkFilter(BlockPos pos, ItemStack stack) { return false; }
        @Override public long dispatch(BlockPos p, BlockPos r, IItemKey i, long a, UUID d) { return 0; }
        @Override public boolean isClientSide() { return false; }
        @Override public void broadcastAlert(BlockPos pos, Component message) {}
        @Override public IEnergyStorage energyStorageAt(BlockPos pos) { return storages.get(pos); }
        @Override public long gameTime() { return stubTick; }
    };

    private PipeNetwork network;

    private static EnergyComponent battery(long amount) {
        EnergyComponent e = new EnergyComponent(1_000_000, 1_000_000, 1_000_000, () -> {});
        e.setAmount(amount);
        return e;
    }

    @BeforeEach
    void setUp() {
        storages.clear();
        stubTick = 0;
        network = new PipeNetwork(UUID.randomUUID(), new NetworkGraph(), stubView);
    }

    @Test
    void consumeEnergy_noSources_returnsFalse() {
        assertFalse(network.consumeEnergy(10));
    }

    @Test
    @SuppressWarnings("deprecation")
    void consumeEnergy_noWorldView_returnsFalse() {
        // Legacy constructor leaves worldView null — sources can't be resolved, so consumption fails.
        PipeNetwork legacy = new PipeNetwork(UUID.randomUUID());
        legacy.registerEnergySource(BAT_A);
        assertFalse(legacy.consumeEnergy(10));
    }

    @Test
    void consumeEnergy_nonPositive_isNoOpTrue() {
        assertTrue(network.consumeEnergy(0));
        assertTrue(network.consumeEnergy(-5));
    }

    @Test
    void consumeEnergy_sufficient_drawsAndReturnsTrue() {
        EnergyComponent a = battery(100);
        storages.put(BAT_A, a);
        network.registerEnergySource(BAT_A);

        assertTrue(network.consumeEnergy(60));
        assertEquals(40, a.getAmount());
    }

    @Test
    void consumeEnergy_insufficient_drawsNothing() {
        EnergyComponent a = battery(50);
        storages.put(BAT_A, a);
        network.registerEnergySource(BAT_A);

        assertFalse(network.consumeEnergy(60));
        assertEquals(50, a.getAmount(), "all-or-nothing: nothing consumed when insufficient");
    }

    @Test
    void consumeEnergy_multipleSources_drawsInRegistrationOrder() {
        EnergyComponent a = battery(40);
        EnergyComponent b = battery(40);
        storages.put(BAT_A, a);
        storages.put(BAT_B, b);
        network.registerEnergySource(BAT_A);
        network.registerEnergySource(BAT_B);

        assertTrue(network.consumeEnergy(60));
        assertEquals(0, a.getAmount(), "first registered source drained first");
        assertEquals(20, b.getAmount(), "remainder taken from second source");
    }

    @Test
    void consumeEnergy_stopsOncePlanSatisfied_leavingLaterSourcesUntouched() {
        EnergyComponent a = battery(40);
        EnergyComponent b = battery(40);
        EnergyComponent c = battery(40);
        storages.put(BAT_A, a);
        storages.put(BAT_B, b);
        storages.put(BAT_C, c);
        network.registerEnergySource(BAT_A);
        network.registerEnergySource(BAT_B);
        network.registerEnergySource(BAT_C);

        assertTrue(network.consumeEnergy(60));
        assertEquals(0, a.getAmount());
        assertEquals(20, b.getAmount());
        assertEquals(40, c.getAmount(), "planning stops once the amount is met; third source untouched");
    }

    @Test
    void consumeEnergy_skipsEmptySource() {
        EnergyComponent empty = battery(0);
        EnergyComponent full = battery(100);
        storages.put(BAT_A, empty);
        storages.put(BAT_B, full);
        network.registerEnergySource(BAT_A);
        network.registerEnergySource(BAT_B);

        assertTrue(network.consumeEnergy(70));
        assertEquals(0, empty.getAmount(), "empty source contributes nothing");
        assertEquals(30, full.getAmount());
    }

    @Test
    void consumeEnergy_skipsUnresolvedSource() {
        // A registered position whose battery was removed/unloaded resolves to null and is skipped.
        network.registerEnergySource(BAT_A);
        EnergyComponent b = battery(100);
        storages.put(BAT_B, b);
        network.registerEnergySource(BAT_B);

        assertTrue(network.consumeEnergy(70));
        assertEquals(30, b.getAmount());
    }

    @Test
    void unregisterEnergySource_removesIt() {
        EnergyComponent a = battery(100);
        storages.put(BAT_A, a);
        network.registerEnergySource(BAT_A);
        network.unregisterEnergySource(BAT_A);

        assertFalse(network.consumeEnergy(10));
        assertEquals(100, a.getAmount());
    }

    @Test
    void merge_unionsEnergySources() {
        EnergyComponent a = battery(50);
        EnergyComponent b = battery(50);
        storages.put(BAT_A, a);
        storages.put(BAT_B, b);
        network.registerEnergySource(BAT_A);

        PipeNetwork other = new PipeNetwork(UUID.randomUUID(), new NetworkGraph(), stubView);
        other.registerEnergySource(BAT_B);

        network.merge(other);

        assertTrue(network.consumeEnergy(80), "merged network draws from both batteries");
        assertEquals(0, a.getAmount());
        assertEquals(20, b.getAmount());
    }

    // ===== isPowered() =====

    @Test
    void isPowered_noSources_returnsFalse() {
        assertFalse(network.isPowered());
    }

    @Test
    @SuppressWarnings("deprecation")
    void isPowered_noWorldView_returnsFalse() {
        PipeNetwork legacy = new PipeNetwork(UUID.randomUUID());
        legacy.registerEnergySource(BAT_A);
        assertFalse(legacy.isPowered());
    }

    @Test
    void isPowered_sourceWithEnergy_returnsTrue() {
        storages.put(BAT_A, battery(100));
        network.registerEnergySource(BAT_A);
        assertTrue(network.isPowered());
    }

    @Test
    void isPowered_allSourcesEmpty_returnsFalse() {
        storages.put(BAT_A, battery(0));
        network.registerEnergySource(BAT_A);
        assertFalse(network.isPowered());
    }

    @Test
    void isPowered_unresolvedSource_returnsFalse() {
        // Registered position whose battery was removed/unloaded resolves to null and contributes nothing.
        network.registerEnergySource(BAT_A);
        assertFalse(network.isPowered());
    }

    @Test
    void isPowered_secondSourceHasEnergy_returnsTrue() {
        storages.put(BAT_A, battery(0));
        storages.put(BAT_B, battery(100));
        network.registerEnergySource(BAT_A);
        network.registerEnergySource(BAT_B);
        assertTrue(network.isPowered());
    }

    @Test
    void isPowered_cachesResultWithinSameTick() {
        EnergyComponent a = battery(100);
        storages.put(BAT_A, a);
        network.registerEnergySource(BAT_A);

        assertTrue(network.isPowered(), "computes powered on first call");

        // Drain the battery but stay on the same tick: the cached result is reused.
        a.setAmount(0);
        assertTrue(network.isPowered(), "same-tick call returns the cached result, not a fresh scan");

        // Advancing the game tick invalidates the cache and recomputes.
        stubTick++;
        assertFalse(network.isPowered(), "next tick rescans and sees the drained battery");
    }
}
