package com.logistics.neoforge.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges the NeoForge 21.1 {@link IItemHandler} to the common {@link IItemStorage}
 * abstraction and vice versa.
 *
 * <p>NeoForge 21.1's {@link IItemHandler} is slot-based and uses a simulate boolean.
 * The common interface is key-based and uses a simulate boolean. This class translates
 * between both directions.
 */
public final class NeoForgeItemStorage implements IItemStorage {
    private final IItemHandler handler;

    private NeoForgeItemStorage(IItemHandler handler) {
        this.handler = handler;
    }

    /**
     * Wrap a NeoForge {@link IItemHandler} as the common {@link IItemStorage}.
     *
     * @param handler the NeoForge handler to wrap; may be {@code null}
     * @return the wrapped storage, or {@code null} if handler is {@code null}
     */
    @Nullable
    public static IItemStorage wrap(@Nullable IItemHandler handler) {
        return handler == null ? null : new NeoForgeItemStorage(handler);
    }

    /**
     * Adapt a common {@link IItemStorage} to a NeoForge {@link IItemHandler}.
     *
     * <p>The resulting handler reports a single slot that reflects the first non-empty
     * item in the common storage. This is a "unified view" adapter — callers should prefer
     * the key-based insert/extract methods for correct multi-item interaction.
     *
     * @param storage the common storage to adapt; may be {@code null}
     * @return the NeoForge adapter, or {@code null} if storage is {@code null}
     */
    @Nullable
    public static IItemHandler asNeoForge(@Nullable IItemStorage storage) {
        return storage == null ? null : new CommonItemHandler(storage);
    }

    @Override
    public long insert(IItemKey item, long maxAmount, boolean simulate) {
        if (maxAmount <= 0) {
            return 0;
        }
        ItemStack toInsert = item.toStack(clampToInt(maxAmount));
        int remaining = toInsert.getCount();
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack leftover = handler.insertItem(slot, toInsert.copyWithCount(remaining), simulate);
            remaining = leftover.getCount();
        }
        return maxAmount - remaining;
    }

    @Override
    public long extract(IItemKey item, long maxAmount, boolean simulate) {
        if (maxAmount <= 0) {
            return 0;
        }
        long extracted = 0;
        int remaining = clampToInt(maxAmount);
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty() || !item.matches(inSlot)) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, remaining, simulate);
            if (!taken.isEmpty()) {
                extracted += taken.getCount();
                remaining -= taken.getCount();
            }
        }
        return extracted;
    }

    @Override
    public Iterable<IItemView> contents() {
        List<IItemView> views = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            final NeoForgeItemKey key = NeoForgeItemKey.of(stack);
            final long amount = stack.getCount();
            views.add(new IItemView() {
                @Override public IItemKey resource() { return key; }
                @Override public long amount() { return amount; }
            });
        }
        return views;
    }

    private static int clampToInt(long amount) {
        return (int) Math.max(0, Math.min(amount, Integer.MAX_VALUE));
    }

    /**
     * Adapts a common {@link IItemStorage} to NeoForge's slot-based {@link IItemHandler}.
     *
     * <p>Reports {@link #getSlots()}{@code == 1} regardless of how many distinct item types
     * the underlying storage holds. {@link #getStackInSlot(int)} returns the first non-empty
     * item. This is an intentional "unified slot" view — the common abstraction has no fixed
     * slot count, so we expose it as a single virtual slot.
     */
    private static final class CommonItemHandler implements IItemHandler {
        private final IItemStorage storage;

        private CommonItemHandler(IItemStorage storage) {
            this.storage = storage;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            checkIndex(slot);
            for (IItemView view : storage.contents()) {
                if (view.amount() > 0) {
                    return view.resource().toStack(clampToInt(view.amount()));
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            checkIndex(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            IItemKey key = NeoForgeItemKey.of(stack);
            long inserted = storage.insert(key, stack.getCount(), simulate);
            int leftover = stack.getCount() - clampToInt(inserted);
            return leftover <= 0 ? ItemStack.EMPTY : stack.copyWithCount(leftover);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            checkIndex(slot);
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }
            // Extract the first available item type
            for (IItemView view : storage.contents()) {
                if (view.amount() > 0) {
                    IItemKey key = view.resource();
                    long extracted = storage.extract(key, amount, simulate);
                    if (extracted > 0) {
                        return key.toStack(clampToInt(extracted));
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            checkIndex(slot);
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            checkIndex(slot);
            if (stack.isEmpty()) {
                return false;
            }
            return storage.insert(NeoForgeItemKey.of(stack), 1, true) > 0;
        }

        private static void checkIndex(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException(index);
            }
        }
    }
}
