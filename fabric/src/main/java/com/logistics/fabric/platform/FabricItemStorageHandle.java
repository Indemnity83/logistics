package com.logistics.fabric.platform;

import com.logistics.block.entity.PipeItemStorage;
import com.logistics.platform.storage.ItemStorageHandle;
import com.logistics.platform.storage.TransactionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.item.ItemStack;

/**
 * Fabric implementation of ItemStorageHandle wrapping Storage&lt;ItemVariant&gt;.
 */
public class FabricItemStorageHandle implements ItemStorageHandle {

    private final Storage<ItemVariant> storage;

    public FabricItemStorageHandle(Storage<ItemVariant> storage) {
        this.storage = storage;
    }

    /**
     * Get the underlying Fabric storage for direct access when needed.
     */
    public Storage<ItemVariant> getStorage() {
        return storage;
    }

    @Override
    public long insert(ItemStack stack, long maxAmount, TransactionContext transaction) {
        if (stack.isEmpty() || maxAmount <= 0) {
            return 0;
        }

        Transaction fabricTx = unwrapTransaction(transaction);
        ItemVariant variant = ItemVariant.of(stack);
        return storage.insert(variant, maxAmount, fabricTx);
    }

    @Override
    public ItemStack extract(Predicate<ItemStack> filter, long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0) {
            return ItemStack.EMPTY;
        }

        Transaction fabricTx = unwrapTransaction(transaction);

        for (StorageView<ItemVariant> view : storage) {
            ItemVariant variant = view.getResource();
            if (variant.isBlank()) {
                continue;
            }

            ItemStack testStack = variant.toStack(1);
            if (!filter.test(testStack)) {
                continue;
            }

            long extracted = view.extract(variant, maxAmount, fabricTx);
            if (extracted > 0) {
                return variant.toStack((int) extracted);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public long extractExact(ItemStack resource, long maxAmount, TransactionContext transaction) {
        if (resource.isEmpty() || maxAmount <= 0) {
            return 0;
        }

        Transaction fabricTx = unwrapTransaction(transaction);
        ItemVariant variant = ItemVariant.of(resource);

        long totalExtracted = 0;
        for (StorageView<ItemVariant> view : storage) {
            if (!view.getResource().equals(variant)) {
                continue;
            }

            long toExtract = maxAmount - totalExtracted;
            long extracted = view.extract(variant, toExtract, fabricTx);
            totalExtracted += extracted;

            if (totalExtracted >= maxAmount) {
                break;
            }
        }

        return totalExtracted;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        try (Transaction tx = Transaction.openOuter()) {
            ItemVariant variant = ItemVariant.of(stack);
            long inserted = storage.insert(variant, 1, tx);
            // Don't commit - this is just a simulation
            return inserted > 0;
        }
    }

    @Override
    public Iterable<ItemStack> getContents() {
        List<ItemStack> contents = new ArrayList<>();
        for (StorageView<ItemVariant> view : storage) {
            ItemVariant variant = view.getResource();
            if (!variant.isBlank() && view.getAmount() > 0) {
                contents.add(variant.toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE)));
            }
        }
        return contents;
    }

    @Override
    public boolean isPipeStorage() {
        return storage instanceof PipeItemStorage;
    }

    private Transaction unwrapTransaction(TransactionContext context) {
        if (context instanceof FabricTransactionContext fabricContext) {
            return fabricContext.getTransaction();
        }
        throw new IllegalArgumentException("Expected FabricTransactionContext but got " + context.getClass());
    }
}
