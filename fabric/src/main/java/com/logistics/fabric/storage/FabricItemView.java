package com.logistics.fabric.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemView;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;

/**
 * Fabric adapter: wraps {@link StorageView}{@code <ItemVariant>} as an {@link IItemView}.
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
