package com.logistics.core.lib.pipe;

import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.storage.ItemStorageLookup;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** Delivery bookkeeping shared by the pipe runtime and the routing modules. */
public final class Deliveries {

    private Deliveries() {}

    /**
     * Give an in-flight item's delivery back to the network and clear its routing fields, so the item
     * falls back to being unrouted.
     *
     * <p>The order must go back before the fields are cleared. Every delivery notification keys off
     * the delivery id, so an item that loses its id first can never release its in-transit order, its
     * requester accounting or the provider's reservation — they would leak for the life of the world.
     * Clearing afterwards is also what makes this safe to call again: a second abandon, or a later
     * arrival at an inventory, finds no id and releases nothing twice.
     */
    public static void abandon(@Nullable ILogisticsNetwork network, TravelingItem item) {
        UUID deliveryId = item.getDeliveryId();
        BlockPos destination = item.getDestination();

        item.setDestination(null);
        item.setDeliveryId(null);

        if (network == null || deliveryId == null || destination == null) return;
        network.notifyDeliveryFailed(
                deliveryId, destination, ItemStorageLookup.of(item.getStack()), item.getStack().getCount());
    }
}
