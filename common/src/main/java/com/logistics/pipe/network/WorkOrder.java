package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * A single unit of work within a {@link NetworkJob}.
 * Sealed hierarchy — exactly three concrete forms are possible:
 * <ul>
 *   <li>{@link ExtractWorkOrder} — pull items from a provider</li>
 *   <li>{@link CraftWorkOrder}   — trigger an autocrafter to produce items</li>
 *   <li>{@link DeliverWorkOrder} — route items to the requester</li>
 * </ul>
 * Pure value objects. Use {@link #withLevel(CommitmentLevel)} to produce an updated copy.
 */
public sealed interface WorkOrder
        permits WorkOrder.ExtractWorkOrder, WorkOrder.CraftWorkOrder, WorkOrder.DeliverWorkOrder {

    CommitmentLevel level();

    /** Return a copy of this work order with the given {@link CommitmentLevel}. */
    WorkOrder withLevel(CommitmentLevel newLevel);

    // ───────────────────────────────────────────────────────────────────────

    /**
     * Pull {@code amount} of {@code item} from a specific provider.
     *
     * @param reservation the reservation backing this extraction, or {@code null} if not yet allocated
     */
    record ExtractWorkOrder(
            @Nullable ReservationId reservation,
            BlockPos provider,
            ItemVariant item,
            long amount,
            CommitmentLevel level) implements WorkOrder {

        @Override
        public WorkOrder withLevel(CommitmentLevel newLevel) {
            return new ExtractWorkOrder(reservation, provider, item, amount, newLevel);
        }
    }

    // ───────────────────────────────────────────────────────────────────────

    /**
     * Submit {@code batches} crafting jobs to an autocrafter.
     */
    record CraftWorkOrder(
            BlockPos crafter,
            ItemVariant output,
            long batches,
            CommitmentLevel level) implements WorkOrder {

        @Override
        public WorkOrder withLevel(CommitmentLevel newLevel) {
            return new CraftWorkOrder(crafter, output, batches, newLevel);
        }
    }

    // ───────────────────────────────────────────────────────────────────────

    /**
     * Deliver {@code amount} of {@code item} to {@code destination}.
     */
    record DeliverWorkOrder(
            BlockPos destination,
            ItemVariant item,
            long amount,
            CommitmentLevel level) implements WorkOrder {

        @Override
        public WorkOrder withLevel(CommitmentLevel newLevel) {
            return new DeliverWorkOrder(destination, item, amount, newLevel);
        }
    }
}
