package com.logistics.fabric.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ISlottedItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric adapter for a storage whose slots are addressable.
 *
 * <p>Most Minecraft inventories reach us as a {@link SlottedStorage} — the resource-scoped
 * {@code Storage} contract alone cannot say <em>which</em> slot an extraction came out of, which
 * callers that reserve particular slots (the Provider's crop modes) depend on.
 */
final class SlottedFabricItemStorage extends FabricItemStorage implements ISlottedItemStorage {

    private final SlottedStorage<ItemVariant> slotted;

    SlottedFabricItemStorage(SlottedStorage<ItemVariant> slotted) {
        super(slotted);
        this.slotted = slotted;
    }

    @Override
    public int slotCount() {
        return slotted.getSlotCount();
    }

    @Override
    @Nullable
    public IItemView slotView(int slot) {
        SingleSlotStorage<ItemVariant> view = slotted.getSlot(slot);
        if (view.isResourceBlank() || view.getAmount() <= 0) {
            return null;
        }
        return new FabricItemView(view);
    }

    @Override
    public long slotCapacity(int slot) {
        return slotted.getSlot(slot).getCapacity();
    }

    @Override
    public long insert(int slot, IItemKey item, long maxAmount, boolean simulate) {
        return transfer(slot, item, maxAmount, simulate, true);
    }

    @Override
    public long extract(int slot, IItemKey item, long maxAmount, boolean simulate) {
        return transfer(slot, item, maxAmount, simulate, false);
    }

    private long transfer(int slot, IItemKey item, long maxAmount, boolean simulate, boolean insert) {
        if (maxAmount <= 0) {
            return 0;
        }
        ItemVariant variant = variantOf(item);
        SingleSlotStorage<ItemVariant> target = slotted.getSlot(slot);
        try (Transaction t = Transaction.openOuter()) {
            long moved = insert ? target.insert(variant, maxAmount, t) : target.extract(variant, maxAmount, t);
            if (!simulate) {
                t.commit();
            }
            return moved;
        }
    }
}
