package com.logistics.fabric.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemView;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;

/**
 * Fabric adapter: wraps {@link StorageView}{@code <ItemVariant>} as an {@link IItemView}.
 *
 * <p><b>This deliberately does not override {@code capacity()}</b>, even though the wrapped
 * {@code StorageView} can answer it via {@code view.getCapacity()}. It therefore inherits
 * {@link IItemView}'s default, which returns {@code amount()} — so this view and
 * {@link SlottedFabricItemStorage#slotCapacity(int)} report different numbers for the same slot
 * whenever that slot is not exactly full. See {@link IItemView#capacity()} for why that default
 * stands and what it costs; the short version is that a partially-filled non-slotted storage reads
 * as full to a hopper on NeoForge. Kept as-is by decision, not by oversight.
 *
 * <p>Note the fluid adapter beside this one <i>does</i> override capacity
 * ({@code FabricFluidStorage}), so the item side is the odd one out. That asymmetry is the thing
 * most likely to look accidental to a later reader.
 */
public final class FabricItemView implements IItemView {

    private final StorageView<ItemVariant> view;

    public FabricItemView(StorageView<ItemVariant> view) {
        this.view = view;
    }

    @Override
    public IItemKey resource() {
        return new FabricItemKey(view.getResource());
    }

    @Override
    public long amount() {
        return view.getAmount();
    }
}
