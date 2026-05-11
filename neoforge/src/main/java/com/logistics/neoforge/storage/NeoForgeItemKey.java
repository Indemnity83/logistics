package com.logistics.neoforge.storage;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class NeoForgeItemKey implements IItemKey {
    private final ItemResource resource;

    public NeoForgeItemKey(ItemResource resource) {
        if (resource == null) {
            throw new NullPointerException("resource must not be null");
        }
        this.resource = resource;
    }

    public static NeoForgeItemKey of(ItemStack stack) {
        return new NeoForgeItemKey(ItemResource.of(stack));
    }

    public ItemResource resource() {
        return resource;
    }

    @Override
    public ItemStack toStack(int count) {
        return resource.toStack(count);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return resource.matches(stack);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof NeoForgeItemKey other && resource.equals(other.resource));
    }

    @Override
    public int hashCode() {
        return resource.hashCode();
    }
}
