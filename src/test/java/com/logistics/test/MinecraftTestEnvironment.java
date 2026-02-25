package com.logistics.test;

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
    private static boolean bootstrapped = false;

    @BeforeAll
    public static void bootstrapMinecraft() {
        if (bootstrapped) {
            return;
        }

        try {
            // Initialize Minecraft's shared constants (game version, protocol version, etc.)
            SharedConstants.tryDetectVersion();

            // Bootstrap registries (blocks, items, entities, etc.)
            // This is equivalent to what Minecraft does on startup
            Bootstrap.bootStrap();

            // Verify registries are initialized
            if (BuiltInRegistries.ITEM.size() == 0) {
                throw new IllegalStateException("Item registry not initialized");
            }

            bootstrapped = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to bootstrap Minecraft test environment", e);
        }
    }

    /**
     * Verifies that a registry has been populated.
     * Useful for debugging test setup issues.
     */
    protected static void assertRegistryPopulated(Registry<?> registry, String name) {
        if (registry.size() == 0) {
            throw new IllegalStateException(name + " registry is empty - bootstrap may have failed");
        }
    }
}
