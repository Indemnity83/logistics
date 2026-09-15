package com.logistics.pipe.network;

import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.network.NetworkGraph;
import com.logistics.core.lib.network.StrayClaim;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.test.MinecraftTestEnvironment;
import com.logistics.test.TestItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The network-level view of re-homing a stray packet: which requesters
 * {@link PipeNetwork#claimStrayDelivery} will even consider.
 *
 * <p>A claim re-addresses a packet that is physically sitting in a pipe, so it is only allowed to
 * name somewhere that packet can actually travel to. Everything about ranking and accounting is
 * covered by {@link NetworkControllerStrayClaimTest}; this fixes the graph filter on top of it.
 */
@DisplayName("Stray claim through the network")
class PipeNetworkStrayClaimTest extends MinecraftTestEnvironment {

    private static final BlockPos STRANDED = new BlockPos(0, 0, 0);
    private static final BlockPos REQUESTER = new BlockPos(3, 0, 0);
    private static final BlockPos ACROSS_THE_BREAK = new BlockPos(6, 0, 0);

    private NetworkGraph graph;
    private PipeNetwork network;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        // A straight run of pipes from x=0 to x=6.
        for (int x = 0; x <= 6; x++) graph.addNode(new BlockPos(x, 0, 0));
        network = new PipeNetwork(UUID.randomUUID(), graph, stubWorldView());
    }

    private static IItemKey diamond() {
        return new TestItemKey(Items.DIAMOND);
    }

    private static ItemStack stack() {
        return new ItemStack(Items.DIAMOND, 16);
    }

    @Test
    @DisplayName("a stranded stack is re-homed onto a reachable requester's order")
    void claimStrayDelivery_findsAnOrderNoSinkCouldOffer() {
        UUID orderId = network.placeOrder(diamond(), 16L, REQUESTER);

        // The requester registered no sink — an order is the only thing that can save this stack.
        assertThat(network.findSinkFor(stack(), STRANDED)).isNull();

        StrayClaim claim = network.claimStrayDelivery(stack(), STRANDED, null);
        assertThat(claim).isNotNull();
        assertThat(claim.destination()).isEqualTo(REQUESTER);
        assertThat(claim.deliveryId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("the claimed delivery settles the requester's accounting when it arrives")
    void claimStrayDelivery_isAcknowledgeableOnArrival() {
        network.placeOrder(diamond(), 16L, REQUESTER);
        StrayClaim claim = network.claimStrayDelivery(stack(), STRANDED, null);

        network.notifyDelivery(claim.deliveryId(), claim.destination(), diamond(), 16L);

        assertThat(network.getOrderedAmountFor(REQUESTER, stack())).isZero();
    }

    @Test
    @DisplayName("a requester on the far side of a break is not claimed")
    void claimStrayDelivery_skipsRequestersTheStackCannotReach() {
        network.placeOrder(diamond(), 16L, ACROSS_THE_BREAK);

        // Pull out a middle pipe: the order survives (only the removed pipe's orders are cancelled)
        // but its requester is no longer routable from where the stack is stuck.
        network.removePipe(new BlockPos(3, 0, 0));
        assertThat(graph.getNextHop(STRANDED, ACROSS_THE_BREAK)).isNull();

        assertThat(network.claimStrayDelivery(stack(), STRANDED, null)).isNull();
    }

    @Test
    @DisplayName("a requester that has left the network is not claimed")
    void claimStrayDelivery_skipsRequestersOutsideTheGraph() {
        BlockPos offNetwork = new BlockPos(0, 10, 0);
        network.placeOrder(diamond(), 16L, offNetwork);

        assertThat(network.claimStrayDelivery(stack(), STRANDED, null)).isNull();
    }

    @Test
    @DisplayName("the destination that just failed is excluded from the retry")
    void claimStrayDelivery_honoursTheExclusion() {
        network.placeOrder(diamond(), 16L, REQUESTER);

        assertThat(network.claimStrayDelivery(stack(), STRANDED, REQUESTER)).isNull();
        assertThat(network.claimStrayDelivery(stack(), STRANDED, null)).isNotNull();
    }

    /** A world that accepts nothing, so no sink can ever compete with the order book. */
    private static com.logistics.core.lib.network.IWorldView stubWorldView() {
        return new com.logistics.core.lib.network.IWorldView() {
            @Override public boolean isPipe(BlockPos pos) { return false; }
            @Override public List<BlockPos> getConnectedNeighbors(BlockPos pos) { return List.of(); }
            @Override public boolean matchesSinkFilter(BlockPos pos, ItemStack stack) { return false; }
            @Override public long dispatch(BlockPos provider, BlockPos requester, IItemKey item,
                                           long amount, UUID deliveryId) { return 0L; }
            @Override public boolean isClientSide() { return false; }
            @Override public void broadcastAlert(BlockPos pos, Component message) {}
            @Override public IEnergyStorage energyStorageAt(BlockPos pos) { return null; }
            @Override public long gameTime() { return 0L; }
        };
    }
}
