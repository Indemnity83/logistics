package com.logistics.pipe.modules;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.network.IWorldView;
import com.logistics.core.lib.network.NetworkGraph;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.storage.ContainerItemStorage;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.network.PipeNetwork;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies what a Provider does to its source inventory when the network can't pay.
 *
 * <p>The dispatch is priced from a simulated extraction, so an unpowered network must leave the
 * source untouched rather than pull a stack out and then try to give it back — {@code insert} is a
 * partial-transfer API and an output-only face such as a furnace's bottom refuses the refund.
 */
@DisplayName("ProviderModule dispatch on an unpowered network")
class ProviderModuleRefundTest extends MinecraftTestEnvironment {

    private static final BlockPos PIPE_POS = BlockPos.ZERO;
    private static final BlockPos FURNACE_POS = PIPE_POS.relative(Direction.NORTH);
    private static final BlockPos CHEST_POS = PIPE_POS.relative(Direction.EAST);
    private static final BlockPos REQUESTER = new BlockPos(10, 1, 0);

    private OutputOnlyContainer furnace;
    private SimpleContainer chest;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        furnace = new OutputOnlyContainer(3);
        chest = new SimpleContainer(27);
        ItemStorageLookup.register((world, pos, dir) -> {
            if (FURNACE_POS.equals(pos)) return new ContainerItemStorage(furnace, dir);
            if (CHEST_POS.equals(pos)) return new ContainerItemStorage(chest, dir);
            return null;
        });

        access = new FakePipeAccess()
                .setConnection(Direction.NORTH, PipeConnection.Type.INVENTORY)
                .setConnection(Direction.EAST, PipeConnection.Type.INVENTORY)
                .setNetwork(drainedNetwork());
        ctx = new PipeContext(null, PIPE_POS, null, access);
    }

    @AfterEach
    void tearDown() {
        ItemStorageLookup.register((world, pos, dir) -> null);
    }

    @Test
    @DisplayName("a source that would refuse a refund is never drained in the first place")
    void unpoweredNetwork_outputOnlySource_isLeftUntouched() {
        furnace.setItem(0, new ItemStack(Items.IRON_INGOT, 64));

        // Repeated cycles: the queue entry survives an unaffordable dispatch, so a drain that
        // extracts before charging empties the machine one stack at a time.
        drainCycles(5);

        assertThat(access.getInjectedItems())
                .as("the network can't pay, so nothing may be shipped")
                .isEmpty();
        assertThat(furnace.countItem(Items.IRON_INGOT))
                .as("the items must stay in the machine, not be pulled out and dropped")
                .isEqualTo(64);
        assertThat(chest.countItem(Items.IRON_INGOT))
                .as("nor relocated into another connected inventory")
                .isZero();
    }

    @Test
    @DisplayName("a source that would accept a refund is left untouched too")
    void unpoweredNetwork_insertableSource_isLeftUntouched() {
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 64));

        drainCycles(5);

        assertThat(access.getInjectedItems()).isEmpty();
        assertThat(chest.countItem(Items.IRON_INGOT)).isEqualTo(64);
        assertThat(furnace.countItem(Items.IRON_INGOT)).isZero();
    }

    @Test
    @DisplayName("a funded network still dispatches, extracting exactly once")
    void poweredNetwork_dispatchesWithoutDoubleExtracting() {
        access.setNetwork(fundedNetwork());
        furnace.setItem(0, new ItemStack(Items.IRON_INGOT, 64));

        drainCycles(1);

        assertThat(access.getInjectedItems())
                .as("pricing the dispatch from a simulated pass must not stop a paid one")
                .hasSize(1);
        assertThat(furnace.countItem(Items.IRON_INGOT))
                .as("the simulated pass must not remove anything, so exactly 32 leaves the furnace")
                .isEqualTo(32);
    }

    // ==================== Helpers ====================

    private void drainCycles(int cycles) {
        ProviderModule provider = new ProviderModule(64, 4);
        ProviderDispatchQueue queue = new ProviderDispatchQueue();
        queue.enqueue("minecraft:iron_ingot", 32, REQUESTER, UUID.randomUUID());
        provider.saveQueue(ctx, queue);

        for (int i = 0; i < cycles; i++) {
            provider.processDispatchQueue(ctx);
        }
    }

    /** A network whose only battery has run dry, so every {@code consumeEnergy} call fails. */
    private static PipeNetwork drainedNetwork() {
        return networkWithCharge(0);
    }

    /** A network with enough stored energy to pay for the whole dispatch. */
    private static PipeNetwork fundedNetwork() {
        return networkWithCharge(1000);
    }

    private static PipeNetwork networkWithCharge(long charge) {
        BlockPos batteryPos = new BlockPos(0, 5, 0);
        EnergyComponent battery = new EnergyComponent(1000, 1000, 1000, () -> {});
        battery.setAmount(charge);

        IWorldView view = new IWorldView() {
            @Override public boolean isPipe(BlockPos pos) { return false; }
            @Override public List<BlockPos> getConnectedNeighbors(BlockPos pos) { return List.of(); }
            @Override public boolean matchesSinkFilter(BlockPos pos, ItemStack stack) { return false; }
            @Override public long dispatch(BlockPos p, BlockPos r, IItemKey i, long a, UUID d) { return 0; }
            @Override public boolean isClientSide() { return false; }
            @Override public void broadcastAlert(BlockPos pos, Component message) {}
            @Override public IEnergyStorage energyStorageAt(BlockPos pos) {
                return batteryPos.equals(pos) ? battery : null;
            }
            @Override public long gameTime() { return 0L; }
        };

        PipeNetwork network = new PipeNetwork(UUID.randomUUID(), new NetworkGraph(), view);
        network.registerEnergySource(batteryPos);
        return network;
    }

    /** Models a furnace output face: items can be taken out, nothing can be put back in. */
    private static final class OutputOnlyContainer extends SimpleContainer implements WorldlyContainer {

        OutputOnlyContainer(int size) {
            super(size);
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            int[] slots = new int[getContainerSize()];
            for (int i = 0; i < slots.length; i++) slots[i] = i;
            return slots;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return true;
        }
    }
}
