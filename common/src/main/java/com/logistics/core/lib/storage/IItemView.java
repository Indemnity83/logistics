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

    /**
     * The total this slot or resource group could hold, not the room left in it. Always
     * {@code >= amount()}. Mirrors {@code IFluidView#capacity()}.
     *
     * <p>Both loaders' storage APIs ask for capacity independently of the current contents, so
     * deriving it from an insert simulation reports a full slot as having zero capacity — which
     * reads as "nothing fits here ever" rather than "this is full right now".
     *
     * <p>Defaults to {@link #amount()} for storages that track no distinct capacity.
     *
     * <p><b>That default contradicts the paragraph above, and does so knowingly.</b> A view that
     * does not override this reports {@code capacity == amount}, i.e. "full", for any partially
     * filled slot. The consequence is real rather than theoretical: a non-slotted storage reaches
     * other mods through {@code NeoForgeItemStorage.getCapacityAsLong}, and
     * {@code ResourceHandlerUtil.isFull} reads {@code amount >= capacity} as full — so a hopper
     * will not top up a partially filled engine fuel slot on NeoForge. That is the same failure
     * mode #985 fixed for the *empty* case; the partially-filled case is knowingly left as-is.
     *
     * <p>Kept rather than fixed by decision (see #1165). Anyone tempted to "fix" the default should
     * know it is load-bearing for nothing and wrong for everything — the correct repair is for each
     * view to report its real capacity, which is a behavioural change that was considered and
     * declined, not an oversight waiting to be tidied.
     */
    default long capacity() {
        return amount();
    }
}
