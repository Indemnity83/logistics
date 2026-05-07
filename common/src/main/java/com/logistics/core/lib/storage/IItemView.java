package com.logistics.core.lib.storage;

/**
 * Read-only view of a slot or resource within an {@link IItemStorage}.
 *
 * <p>Equivalent to Fabric's {@code StorageView<ItemVariant>} in the loader-agnostic API.
 * Instances are obtained by iterating {@link IItemStorage#contents()}.
 *
 * <p>Contract: {@link #resource()} never returns {@code null} and {@link #amount()} is always
 * {@code > 0} for any view produced by {@link IItemStorage#contents()}. Implementations must
 * not produce zero-amount views; callers may rely on this invariant.
 */
public interface IItemView {

    /** The item type stored in this slot or resource group. Never {@code null}. */
    IItemKey resource();

    /** The number of items available in this view. Always {@code > 0}. */
    long amount();
}
