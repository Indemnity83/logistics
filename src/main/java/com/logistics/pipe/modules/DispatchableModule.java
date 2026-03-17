package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * Role interface for modules that can extract and dispatch items to a requester.
 *
 * <p>Modules implementing this interface are eligible to fulfill network dispatch requests.
 * The pipe dispatcher guarantees this is only called server-side.
 */
public interface DispatchableModule extends Module {
    /**
     * Extract and dispatch items toward {@code requester}.
     *
     * <p>The pipe dispatcher guarantees this is server-side only.
     *
     * @param ctx        the pipe context
     * @param requester  destination position
     * @param item       item variant to extract
     * @param amount     requested amount
     * @param deliveryId UUID to attach to the TravelingItem for delivery tracking
     * @return actual amount dispatched (&gt;0), -1 if temporarily busy (deferred), or 0 if unable
     */
    long onDispatch(PipeContext ctx, BlockPos requester, ItemVariant item, long amount, UUID deliveryId);
}
