package com.logistics.fabric.storage;

import com.logistics.core.lib.storage.IItemKey;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric adapter: wraps {@link ItemVariant} as an {@link IItemKey}.
 *
 * <p>Equality and hash-code delegate to {@link ItemVariant}, so instances can be used as
 * {@link java.util.Map} keys with the same semantics as native Fabric code.
 */
public final class FabricItemKey implements IItemKey {

    private final ItemVariant variant;

    public FabricItemKey(ItemVariant variant) {
        this.variant = variant;
    }

    /** Convenience factory: create a {@code FabricItemKey} from a vanilla stack. */
    public static FabricItemKey of(ItemStack stack) {
        return new FabricItemKey(ItemVariant.of(stack));
    }

    /** Returns the underlying {@link ItemVariant}. */
    public ItemVariant variant() {
        return variant;
    }

    @Override
    public ItemStack toStack(int count) {
        return variant.toStack(count);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ItemVariant.of(stack).equals(variant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FabricItemKey other)) return false;
        return variant.equals(other.variant);
    }

    @Override
    public int hashCode() {
        return variant.hashCode();
    }

    @Override
    public String toString() {
        return "FabricItemKey{" + variant + "}";
    }
}
