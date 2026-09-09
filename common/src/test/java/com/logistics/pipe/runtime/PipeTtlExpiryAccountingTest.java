package com.logistics.pipe.runtime;

import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.network.IWorldView;
import com.logistics.core.lib.network.NetworkGraph;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.pipe.network.PipeNetwork;
import com.logistics.test.MinecraftTestEnvironment;
import com.logistics.test.TestItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Network accounting for an item whose delivery expires in transit.
 *
 * <p>An item that circulates until its TTL runs out gives up its destination and falls back to
 * default routing, so it will never arrive where the order said. The order it was carrying has to
 * go back to the network at that moment — nothing later can do it, because the delivery id the
 * notification paths key off is exactly what expiry clears.
 */
@DisplayName("TTL-expired delivery accounting")
class PipeTtlExpiryAccountingTest extends MinecraftTestEnvironment {

    private static final BlockPos PROVIDER = new BlockPos(0, 0, 0);
    private static final BlockPos REQUESTER = new BlockPos(10, 0, 0);
    private static final long ORDERED = 32L;

    private final List<Dispatch> dispatches = new ArrayList<>();
    private PipeNetwork network;

    /** One call the network made into the world to ship an order. */
    private record Dispatch(BlockPos requester, long amount, UUID deliveryId) {}

    @BeforeEach
    void setUp() {
        dispatches.clear();
        network = new PipeNetwork(UUID.randomUUID(), new NetworkGraph(), recordingWorldView());
    }

    @Test
    @DisplayName("an expired delivery is handed back so the requester's order is dispatched again")
    void expiredDelivery_returnsTheOrderToTheNetwork() {
        TravelingItem item = shipOneOrder();

        PipeRuntime.expireDelivery(network, item);

        // The provider still holds the stock it "shipped": it re-advertises on its next scan.
        network.registerSupply(PROVIDER, Map.of(diamond(), ORDERED), 1);
        network.tick(0L);

        assertThat(dispatches)
                .as("the requester is still short a stack, so the network must ship it again")
                .hasSize(2);
        assertThat(dispatches.getLast().amount())
                .as("the provider's stock must not stay reserved for the item that never arrived")
                .isEqualTo(ORDERED);
    }

    @Test
    @DisplayName("an expired item stops carrying its delivery, so default routing takes over")
    void expiredDelivery_clearsTheItemsRouting() {
        TravelingItem item = shipOneOrder();

        PipeRuntime.expireDelivery(network, item);

        assertThat(item.getDestination()).isNull();
        assertThat(item.getDeliveryId()).isNull();
    }

    @Test
    @DisplayName("expiring the same item twice hands the order back only once")
    void expiringTwice_doesNotReleaseTheOrderTwice() {
        TravelingItem item = shipOneOrder();

        PipeRuntime.expireDelivery(network, item);
        PipeRuntime.expireDelivery(network, item);

        network.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        network.tick(0L);

        assertThat(dispatches)
                .as("one lost stack must produce one replacement shipment, not two")
                .hasSize(2);
        assertThat(dispatches.getLast().amount()).isEqualTo(ORDERED);
        assertThat(network.getOrderedAmountFor(REQUESTER, stack()))
                .as("the requester wants one stack, not two")
                .isEqualTo(ORDERED);
    }

    @Test
    @DisplayName("a replacement delivery clears the accounting exactly once")
    void expiredThenRedelivered_settlesTheAccountingOnce() {
        TravelingItem item = shipOneOrder();

        PipeRuntime.expireDelivery(network, item);
        network.registerSupply(PROVIDER, Map.of(diamond(), ORDERED), 1);
        network.tick(0L);

        Dispatch replacement = dispatches.getLast();
        network.notifyDelivery(replacement.deliveryId(), REQUESTER, diamond(), replacement.amount());

        assertThat(network.getOrderedAmountFor(REQUESTER, stack()))
                .as("the replacement arrived, so nothing is outstanding")
                .isZero();
        // The expired item is still physically traveling. It carries no delivery id, so wherever it
        // ends up it cannot release this requester's accounting a second time.
        assertThat(item.getDeliveryId()).isNull();

        network.registerSupply(PROVIDER, Map.of(diamond(), ORDERED), 1);
        network.tick(0L);
        assertThat(dispatches)
                .as("a settled order must not keep shipping")
                .hasSize(2);
    }

    // ==================== Helpers ====================

    /** Places an order and lets the network ship it, returning the item that is now in transit. */
    private TravelingItem shipOneOrder() {
        network.registerSupply(PROVIDER, Map.of(diamond(), ORDERED), 1);
        network.placeOrder(diamond(), ORDERED, REQUESTER, FulfillmentMode.PARTIAL);
        network.tick(0L);

        assertThat(dispatches).as("the order should have shipped").hasSize(1);
        Dispatch shipped = dispatches.getFirst();

        TravelingItem item = new TravelingItem(stack(), Direction.NORTH, 0.05f);
        item.setDestination(shipped.requester());
        item.setDeliveryId(shipped.deliveryId());
        item.setRemainingTtl(0);
        assertThat(item.isExpired()).isTrue();
        return item;
    }

    private static IItemKey diamond() {
        return new TestItemKey(Items.DIAMOND);
    }

    private static ItemStack stack() {
        return new ItemStack(Items.DIAMOND, (int) ORDERED);
    }

    /** A world that ships whatever the network asks for and records the dispatch. */
    private IWorldView recordingWorldView() {
        return new IWorldView() {
            @Override public boolean isPipe(BlockPos pos) { return false; }
            @Override public List<BlockPos> getConnectedNeighbors(BlockPos pos) { return List.of(); }
            @Override public boolean matchesSinkFilter(BlockPos pos, ItemStack stack) { return false; }
            @Override public long dispatch(BlockPos provider, BlockPos requester, IItemKey item,
                                           long amount, UUID deliveryId) {
                dispatches.add(new Dispatch(requester, amount, deliveryId));
                return amount;
            }
            @Override public boolean isClientSide() { return false; }
            @Override public void broadcastAlert(BlockPos pos, Component message) {}
            @Override public IEnergyStorage energyStorageAt(BlockPos pos) { return null; }
            @Override public long gameTime() { return 0L; }
        };
    }
}
