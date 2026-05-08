package com.logistics.test;

import com.logistics.core.lib.storage.ItemStorageLookup;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
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

                // Register the test-environment key factory so ItemStorageLookup.of() works
                // without a loader-specific implementation (Fabric/NeoForge).
                ItemStorageLookup.registerKeyFactory(TestItemKey::of);

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
}
