package com.logistics.pipe.network;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * A single unit of work within a {@link NetworkJob}.
 * Sealed hierarchy — exactly three concrete forms.
 */
public sealed interface WorkOrder
        permits WorkOrder.ExtractWorkOrder, WorkOrder.CraftWorkOrder, WorkOrder.DeliverWorkOrder {

    CommitmentLevel level();
    WorkOrder withLevel(CommitmentLevel newLevel);

    record ExtractWorkOrder(
            @Nullable ReservationId reservation,
            BlockPos provider,
            IItemKey item,
            long amount,
            CommitmentLevel level) implements WorkOrder {

        @Override
        public WorkOrder withLevel(CommitmentLevel newLevel) {
            return new ExtractWorkOrder(reservation, provider, item, amount, newLevel);
        }
    }

    record CraftWorkOrder(
            BlockPos crafter,
            IItemKey output,
            long batches,
            CommitmentLevel level) implements WorkOrder {

        @Override
        public WorkOrder withLevel(CommitmentLevel newLevel) {
            return new CraftWorkOrder(crafter, output, batches, newLevel);
        }
    }

    record DeliverWorkOrder(
            BlockPos destination,
            IItemKey item,
            long amount,
            CommitmentLevel level) implements WorkOrder {

        @Override
        public WorkOrder withLevel(CommitmentLevel newLevel) {
            return new DeliverWorkOrder(destination, item, amount, newLevel);
        }
    }
}
