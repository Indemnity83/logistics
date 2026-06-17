package com.logistics.core.lib.pipe;

import net.minecraft.core.Direction;

import java.util.List;

/**
 * Abstraction over {@code PipeBlockEntity} exposed to {@link PipeContext}.
 *
 * <p>Keeps {@code core.lib.pipe} free of concrete pipe-domain imports while still
 * giving modules access to the operations they need (module state, energy, connections).
 *
 * <p>Extends {@link IModuleHost} with the item-transport-specific operations that only the
 * item pipe block entity provides. Fluid pipes host modules via {@link IModuleHost} directly.
 */
public interface IPipeAccess extends IModuleHost {
    /**
     * Per-arm power-status bitmask (one bit per {@link Direction#get3DDataValue()}; set = that arm
     * is "powered"/green). Synced from the server; used for client-side arm tinting.
     */
    default int getPoweredArmMask() {
        return 0;
    }

    /** Return all items currently traveling through this pipe segment. */
    List<TravelingItem> getTravelingItems();

    /**
     * Inject a new {@link TravelingItem} into this pipe, bypassing capacity checks.
     * Returns {@code true} if the item was accepted.
     */
    boolean forceAddItem(TravelingItem item, Direction fromDirection);
}
