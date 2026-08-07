package com.logistics.core.lib.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;

import java.util.UUID;

/**
 * Role interface for modules that can extract and dispatch fluid to a requester — the fluid analogue
 * of {@link DispatchableModule}. Fluid dispatch has its own path (see {@code FluidOrderBook},
 * {@code IWorldView#dispatchFluid}) parallel to, and never overlapping with, the item dispatch path.
 *
 * <p>Modules implementing this interface are eligible to fulfill fluid network dispatch requests.
 * The pipe dispatcher guarantees this is only called server-side.
 */
public interface FluidDispatchableModule extends Module {
    /**
     * Extract and dispatch fluid toward {@code requester}.
     *
     * <p>The pipe dispatcher guarantees this is server-side only.
     *
     * @param ctx        the pipe context
     * @param requester  destination position
     * @param fluid      fluid to extract
     * @param amountMb   requested mB
     * @param deliveryId UUID to attach to the minted packet(s) for delivery tracking
     * @return actual mB dispatched (0 if unable)
     */
    long onFluidDispatch(PipeContext ctx, BlockPos requester, Fluid fluid, long amountMb, UUID deliveryId);
}
