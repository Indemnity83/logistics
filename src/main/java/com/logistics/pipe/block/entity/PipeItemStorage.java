package com.logistics.pipe.block.entity;

import com.logistics.pipe.runtime.TravelingItem;
import java.util.Collections;
import java.util.Iterator;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext.Result;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

public class PipeItemStorage implements Storage<ItemVariant> {
    private final PipeBlockEntity pipe;
    private final Direction fromDirection;

    public PipeItemStorage(PipeBlockEntity pipe, Direction fromDirection) {
        this.pipe = pipe;
        this.fromDirection = fromDirection;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0) {
            return 0;
        }

        ItemStack previewStack = resource.toStack(1);
        long accepted = pipe.getInsertableAmount(maxAmount, fromDirection, previewStack);
        if (accepted <= 0) {
            return 0;
        }

        transaction.addCloseCallback((context, result) -> {
            if (result == Result.COMMITTED) {
                ItemStack stack = resource.toStack((int) accepted);
                pipe.acceptInsertedStack(stack, fromDirection, null);
            }
        });

        return accepted;
    }

    public long insert(TravelingItem item, TransactionContext transaction) {
        if (item.getStack().isEmpty()) {
            return 0;
        }

        long accepted = pipe.getInsertableAmount(item.getStack().getCount(), fromDirection, item.getStack());
        if (accepted <= 0) {
            return 0;
        }

        float speed = item.getSpeed();
        ItemStack stack = item.getStack().copy();
        stack.setCount((int) accepted);

        transaction.addCloseCallback((context, result) -> {
            if (result == Result.COMMITTED) {
                pipe.acceptInsertedStack(stack, fromDirection, speed);
            }
        });

        return accepted;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return Collections.emptyIterator();
    }
}
