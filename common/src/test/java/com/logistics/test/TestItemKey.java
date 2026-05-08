package com.logistics.test;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Loader-agnostic {@link IItemKey} implementation for unit tests.
 *
 * <p>Identifies items by {@link Item} identity only (no NBT/components).
 * Register the factory via {@code ItemStorageLookup.registerKeyFactory(TestItemKey::of)}
 * in test setup.
 */
public final class TestItemKey implements IItemKey {

    private final Item item;

    public TestItemKey(Item item) {
        this.item = item;
    }

    public static TestItemKey of(ItemStack stack) {
        return new TestItemKey(stack.getItem());
    }

    @Override
    public ItemStack toStack(int count) {
        return new ItemStack(item, count);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.is(item);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestItemKey other)) return false;
        return Objects.equals(item, other.item);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(item);
    }

    @Override
    public String toString() {
        return "TestItemKey[" + item + "]";
    }
}
