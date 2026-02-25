package com.logistics.test;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for tests that require Minecraft's registry system.
 * Bootstraps the game environment once before all tests run.
 */
public abstract class MinecraftTestEnvironment {
    private static volatile boolean bootstrapped = false;

    @BeforeAll
    public static void bootstrapMinecraft() {
        // Double-checked locking with synchronization to prevent race conditions
        if (bootstrapped) {
            return;
        }

        synchronized (MinecraftTestEnvironment.class) {
            // Re-check inside synchronized block (double-checked locking pattern)
            if (bootstrapped) {
                return;
            }

            try {
                // Initialize Minecraft's shared constants (game version, protocol version, etc.)
                SharedConstants.tryDetectVersion();

                // Bootstrap registries (blocks, items, entities, etc.)
                // This is equivalent to what Minecraft does on startup
                Bootstrap.bootStrap();

                // MC 26.1+: Bind data components to registry holders
                // In MC 26.1, the component system requires components to be explicitly
                // bound to each item's holder before ItemStacks can be created.
                // This is normally done during full game startup but must be done manually in tests.
                bindComponentsToItems();

                bootstrapped = true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to bootstrap Minecraft test environment", e);
            }

            // Verify registries are initialized (outside try block so IllegalStateException isn't wrapped)
            if (BuiltInRegistries.ITEM.size() == 0) {
                throw new IllegalStateException("Item registry not initialized");
            }
        }
    }

    /**
     * Verifies that a registry has been populated.
     * Useful for debugging test setup issues.
     */
    protected static void assertRegistryPopulated(Registry<?> registry, String name) {
        if (registry.size() == 0) {
            throw new AssertionError(name + " registry is empty - bootstrap may have failed");
        }
    }

    /**
     * Binds data components to all items in the registry.
     * Required for MC 26.1+ where ItemStack creation requires bound components.
     */
    private static void bindComponentsToItems() {
        // In MC 26.1, items require components to be bound before ItemStacks can be created.
        // For unit tests, we bind empty component maps as a workaround since we don't have
        // a full HolderLookup.Provider available. This is sufficient for most test scenarios.
        for (Item item : BuiltInRegistries.ITEM) {
            // Get the identifier for this item
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);

            // Get the holder reference for this item
            Holder.Reference<Item> holder =
                BuiltInRegistries.ITEM.get(itemId).orElseThrow();

            // Bind an empty component map to avoid NPE during ItemStack creation
            // This is a test-only workaround; real game initialization uses proper component data
            holder.bindComponents(DataComponentMap.EMPTY);
        }
    }
}
