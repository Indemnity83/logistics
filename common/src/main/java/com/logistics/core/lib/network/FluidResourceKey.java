package com.logistics.core.lib.network;

import net.minecraft.world.level.material.Fluid;

import java.util.Objects;

/**
 * Logical identity of a fluid for network order/supply bookkeeping — identity is the {@link Fluid}
 * alone, independent of any physically transported amount.
 *
 * <p>Deliberately distinct from {@link com.logistics.core.lib.fluids.IFluidKey}/{@code SimpleFluidKey}
 * (a different concern: fluid-*storage-handler* identity, used for tank insert/extract matching). This
 * type exists only so the fluid order/supply network ({@code FluidOrderBook}) never needs a fabricated
 * physical packet to key its bookkeeping — see {@code FluidPacket}, whose {@code amountMb} always
 * carries a real, positive payload and is never used as an identity key.
 */
public record FluidResourceKey(Fluid fluid) {
    public FluidResourceKey {
        Objects.requireNonNull(fluid, "fluid");
    }
}
