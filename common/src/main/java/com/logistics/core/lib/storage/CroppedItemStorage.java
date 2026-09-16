package com.logistics.core.lib.storage;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A slotted storage narrowed to a sub-range of its slots: the first {@code cropStart} and the last
 * {@code cropEnd} slots are hidden from every query.
 *
 * <p>The crop is by <em>slot index</em>, not by occupancy — an empty slot still consumes its place
 * in the range, exactly as the delegate reports it. Hiding a slot hides it from {@link #contents()},
 * from the slot-addressed methods, and from the resource-scoped {@link #insert} / {@link #extract},
 * so a caller holding this view has no way to reach a cropped slot.
 *
 * <p>Sided access layers underneath: a {@link ContainerItemStorage} built for a face already
 * addresses only the face-accessible slots, so "slot 0" here means the first slot that face exposes,
 * not the container's slot 0.
 *
 * <p>When the crop is wider than the storage the view is simply empty — it advertises nothing and
 * yields nothing, rather than clamping to some surviving slot.
 */
public final class CroppedItemStorage implements ISlottedItemStorage {

    private final ISlottedItemStorage delegate;
    private final int cropStart;
    private final int cropEnd;

    private CroppedItemStorage(ISlottedItemStorage delegate, int cropStart, int cropEnd) {
        this.delegate = delegate;
        this.cropStart = cropStart;
        this.cropEnd = cropEnd;
    }

    /**
     * Narrow {@code storage} to the slots outside the crop.
     *
     * <p>Returns {@code storage} unchanged when there is nothing to crop, and — deliberately — when
     * the storage is not slot-addressable. A resource-scoped storage has no slot indices to count,
     * so a crop over it could only mean something different from what it means everywhere else;
     * disabling it is the honest answer.
     */
    public static IItemStorage of(IItemStorage storage, int cropStart, int cropEnd) {
        if (cropStart <= 0 && cropEnd <= 0) return storage;
        if (!(storage instanceof ISlottedItemStorage slotted)) return storage;
        return new CroppedItemStorage(slotted, Math.max(0, cropStart), Math.max(0, cropEnd));
    }

    @Override
    public int slotCount() {
        return Math.max(0, delegate.slotCount() - cropStart - cropEnd);
    }

    @Override
    @Nullable
    public IItemView slotView(int slot) {
        return delegate.slotView(delegateSlot(slot));
    }

    @Override
    public long slotCapacity(int slot) {
        return delegate.slotCapacity(delegateSlot(slot));
    }

    @Override
    public long insert(int slot, IItemKey item, long maxAmount, boolean simulate) {
        return delegate.insert(delegateSlot(slot), item, maxAmount, simulate);
    }

    @Override
    public long extract(int slot, IItemKey item, long maxAmount, boolean simulate) {
        return delegate.extract(delegateSlot(slot), item, maxAmount, simulate);
    }

    @Override
    public long insert(IItemKey item, long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        // Top up slots already holding the item before opening an empty one, as a container would.
        long inserted = insertInto(item, maxAmount, 0, simulate, true);
        inserted += insertInto(item, maxAmount, inserted, simulate, false);
        return inserted;
    }

    @Override
    public long extract(IItemKey item, long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        long extracted = 0;
        for (int slot = 0; slot < slotCount() && extracted < maxAmount; slot++) {
            IItemView view = slotView(slot);
            if (view == null || !view.resource().equals(item)) continue;
            extracted += extract(slot, item, maxAmount - extracted, simulate);
        }
        return extracted;
    }

    @Override
    public Iterable<IItemView> contents() {
        List<IItemView> views = new ArrayList<>();
        for (int slot = 0; slot < slotCount(); slot++) {
            IItemView view = slotView(slot);
            if (view != null && view.amount() > 0) views.add(view);
        }
        return views;
    }

    /** Fill either the slots already holding {@code item} ({@code matching}) or the empty ones. */
    private long insertInto(IItemKey item, long maxAmount, long already, boolean simulate, boolean matching) {
        long inserted = 0;
        for (int slot = 0; slot < slotCount() && already + inserted < maxAmount; slot++) {
            IItemView view = slotView(slot);
            boolean matches = view != null && view.resource().equals(item);
            if (matching ? !matches : view != null) continue;
            inserted += insert(slot, item, maxAmount - already - inserted, simulate);
        }
        return inserted;
    }

    private int delegateSlot(int slot) {
        if (slot < 0 || slot >= slotCount()) throw new IndexOutOfBoundsException(slot);
        return slot + cropStart;
    }
}
