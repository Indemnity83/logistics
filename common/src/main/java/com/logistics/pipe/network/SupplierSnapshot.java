package com.logistics.pipe.network;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Immutable snapshot of a supplier pipe's full demand state.
 * Contains one {@link SupplySlotState} per configured supply slot.
 * Pure value object — no Minecraft world coupling.
 */
public record SupplierSnapshot(BlockPos pos, List<SupplySlotState> slots) {
    public SupplierSnapshot {
        if (pos == null) throw new NullPointerException("pos must not be null");
        slots = List.copyOf(slots); // defensive immutable copy
    }

    /** {@code true} if any slot needs restocking. */
    public boolean needsRestock() {
        return slots.stream().anyMatch(SupplySlotState::shouldRestock);
    }
}
