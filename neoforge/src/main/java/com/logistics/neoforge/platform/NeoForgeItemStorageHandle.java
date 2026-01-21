package com.logistics.neoforge.platform;

import com.logistics.platform.storage.ItemStorageHandle;
import com.logistics.platform.storage.TransactionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * NeoForge implementation of ItemStorageHandle wrapping IItemHandler.
 */
public class NeoForgeItemStorageHandle implements ItemStorageHandle {
    private final IItemHandler handler;
    private final boolean isPipe;

    public NeoForgeItemStorageHandle(IItemHandler handler) {
        this(handler, false);
    }

    public NeoForgeItemStorageHandle(IItemHandler handler, boolean isPipe) {
        this.handler = handler;
        this.isPipe = isPipe;
    }

    public IItemHandler getHandler() {
        return handler;
    }

    @Override
    public long insert(ItemStack stack, long maxAmount, TransactionContext transaction) {
        if (stack.isEmpty() || maxAmount <= 0) {
            return 0;
        }

        // Determine simulation mode based on transaction state
        boolean simulate = !(transaction instanceof NeoForgeTransactionContext ctx) || ctx.shouldSimulate();

        int toInsert = (int) Math.min(maxAmount, stack.getCount());
        ItemStack insertStack = stack.copyWithCount(toInsert);
        int totalInserted = 0;

        // Try to insert into each slot
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack remaining = handler.insertItem(slot, insertStack, simulate);
            int inserted = insertStack.getCount() - remaining.getCount();
            totalInserted += inserted;

            if (remaining.isEmpty()) {
                break;
            }
            insertStack = remaining;
        }

        return totalInserted;
    }

    @Override
    public ItemStack extract(Predicate<ItemStack> filter, long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0) {
            return ItemStack.EMPTY;
        }

        boolean simulate = !(transaction instanceof NeoForgeTransactionContext ctx) || ctx.shouldSimulate();

        // Find first matching stack
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stackInSlot = handler.getStackInSlot(slot);
            if (!stackInSlot.isEmpty() && filter.test(stackInSlot.copyWithCount(1))) {
                int toExtract = (int) Math.min(maxAmount, stackInSlot.getCount());
                ItemStack extracted = handler.extractItem(slot, toExtract, simulate);
                if (!extracted.isEmpty()) {
                    return extracted;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public long extractExact(ItemStack resource, long maxAmount, TransactionContext transaction) {
        if (resource.isEmpty() || maxAmount <= 0) {
            return 0;
        }

        boolean simulate = !(transaction instanceof NeoForgeTransactionContext ctx) || ctx.shouldSimulate();

        long totalExtracted = 0;
        int remaining = (int) maxAmount;

        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack stackInSlot = handler.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(stackInSlot, resource)) {
                int toExtract = Math.min(remaining, stackInSlot.getCount());
                ItemStack extracted = handler.extractItem(slot, toExtract, simulate);
                totalExtracted += extracted.getCount();
                remaining -= extracted.getCount();
            }
        }

        return totalExtracted;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Test with simulation
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack remaining = handler.insertItem(slot, stack.copyWithCount(1), true);
            if (remaining.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterable<ItemStack> getContents() {
        List<ItemStack> contents = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                contents.add(stack.copy());
            }
        }
        return contents;
    }

    @Override
    public boolean isPipeStorage() {
        return isPipe;
    }
}
