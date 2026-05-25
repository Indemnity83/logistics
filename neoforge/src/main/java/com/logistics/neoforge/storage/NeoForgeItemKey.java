package com.logistics.neoforge.storage;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge 21.1 implementation of {@link IItemKey}.
 *
 * <p>Wraps an {@link ItemStack} stripped to a count-of-one for identity purposes.
 * Two keys are equal when they represent the same item type and components.
 */
public final class NeoForgeItemKey implements IItemKey {
    private final ItemStack stack;

    private NeoForgeItemKey(ItemStack stack) {
        if (stack == null) {
            throw new NullPointerException("stack must not be null");
        }
        // Normalize to count 1 for identity comparisons
        this.stack = stack.copyWithCount(1);
    }

    /**
     * Create a {@link NeoForgeItemKey} from an {@link ItemStack}.
     *
     * @param stack the stack whose type to capture; count is ignored
     * @return the key
     */
    public static NeoForgeItemKey of(ItemStack stack) {
        return new NeoForgeItemKey(stack);
    }

    /**
     * Return the underlying identity stack (count == 1).
     *
     * @return the identity stack
     */
    public ItemStack identityStack() {
        return stack;
    }

    @Override
    public ItemStack toStack(int count) {
        return stack.copyWithCount(count);
    }

    @Override
    public boolean matches(ItemStack other) {
        return ItemStack.isSameItemSameComponents(stack, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NeoForgeItemKey other)) return false;
        return ItemStack.isSameItemSameComponents(stack, other.stack);
    }

    @Override
    public int hashCode() {
        int result = stack.getItem().hashCode();
        result = 31 * result + stack.getComponentsPatch().hashCode();
        return result;
    }
}
