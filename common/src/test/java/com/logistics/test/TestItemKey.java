package com.logistics.test;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Loader-agnostic {@link IItemKey} implementation for unit tests.
 *
 * <p>Identifies items by item <em>and</em> components, matching the loader keys it stands in for
 * ({@code FabricItemKey} wraps {@code ItemVariant}, {@code NeoForgeItemKey} wraps
 * {@code ItemResource} — both component-aware). A component-blind key would let a test pass while
 * the same code mixed up two differently-enchanted stacks in the game.
 *
 * <p>Register the factory via {@code ItemStorageLookup.registerKeyFactory(TestItemKey::of)}
 * in test setup.
 */
public final class TestItemKey implements IItemKey {

    /** Single-item template carrying the identity: item plus components. */
    private final ItemStack template;

    /** Key for a bare item with no components beyond the item's own defaults. */
    public TestItemKey(Item item) {
        this(new ItemStack(item));
    }

    public static TestItemKey of(ItemStack stack) {
        return new TestItemKey(stack);
    }

    private TestItemKey(ItemStack stack) {
        this.template = stack.copyWithCount(1);
    }

    @Override
    public ItemStack toStack(int count) {
        return template.copyWithCount(count);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(template, stack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestItemKey other)) return false;
        return ItemStack.isSameItemSameComponents(template, other.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(template.getItem(), template.getComponents());
    }

    @Override
    public String toString() {
        return "TestItemKey[" + template.getItem() + " " + template.getComponentsPatch() + "]";
    }
}
