package com.logistics.test;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
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

                // MC 26.1+: Bind data components to registry holders.
                // Uses a lenient HolderLookup.Provider so that items referencing datapack
                // registries (e.g., armor trim materials) don't cause initialization failures
                // in unit tests that run without datapacks loaded.
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
     * Binds data components to all registry holders using the DataComponentInitializers system.
     * Required for MC 26.1+ where ItemStack creation requires bound components.
     *
     * <p>Uses a lenient HolderLookup.Provider that returns stand-alone dummy holders for
     * missing elements in dynamic/datapack registries (e.g., trim materials), allowing
     * items like armor to initialize without requiring a full server context with datapacks.
     */
    private static void bindComponentsToItems() {
        RegistryAccess access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        HolderLookup.Provider lenient = lenientProvider(access);
        for (var pending : BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lenient)) {
            pending.apply();
        }
    }

    /**
     * Wraps a RegistryAccess in a lenient HolderLookup.Provider that returns stand-alone
     * dummy holders for missing elements instead of throwing IllegalStateException.
     */
    private static HolderLookup.Provider lenientProvider(RegistryAccess access) {
        return new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                return access.listRegistryKeys();
            }

            @Override
            public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> key) {
                // Always return a lookup, even for missing registries (e.g., datapack registries
                // like trim_material that aren't loaded in unit tests).
                return Optional.of(access.lookup(key).map(inner -> lenientLookup(inner)).orElse(emptyLenientLookup(key)));
            }
        };
    }

    /**
     * Creates an empty lenient registry lookup for registries that don't exist (e.g., datapack
     * registries). Returns stand-alone dummy holders for any element lookup.
     */
    private static <T> HolderLookup.RegistryLookup<T> emptyLenientLookup(
            ResourceKey<? extends Registry<? extends T>> registryKey) {
        return new HolderLookup.RegistryLookup<T>() {
            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return registryKey;
            }

            @Override
            public Lifecycle registryLifecycle() {
                return Lifecycle.experimental();
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return Stream.empty();
            }

            @Override
            public Stream<HolderSet.Named<T>> listTags() {
                return Stream.empty();
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
                return Optional.of(Holder.Reference.createStandAlone(this, key));
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> tag) {
                return Optional.empty();
            }
        };
    }

    /**
     * Wraps a registry lookup to return stand-alone dummy holders for missing elements.
     */
    private static <T> HolderLookup.RegistryLookup<T> lenientLookup(
            HolderLookup.RegistryLookup<T> inner) {
        return new HolderLookup.RegistryLookup<T>() {
            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return inner.key();
            }

            @Override
            public Lifecycle registryLifecycle() {
                return inner.registryLifecycle();
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return inner.listElements();
            }

            @Override
            public Stream<HolderSet.Named<T>> listTags() {
                return inner.listTags();
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
                Optional<Holder.Reference<T>> result = inner.get(key);
                if (result.isPresent()) {
                    return result;
                }
                // Return a stand-alone unbound holder for missing elements (e.g., datapack entries)
                // so that item component initialization doesn't fail in unit tests
                return Optional.of(Holder.Reference.createStandAlone(this, key));
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> tag) {
                return inner.get(tag);
            }

            @Override
            public boolean canSerializeIn(HolderOwner<T> owner) {
                return inner.canSerializeIn(owner);
            }
        };
    }
}
