package com.logistics.test;

import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.BeforeAll;

import java.util.Optional;
import java.util.stream.Stream;

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

                // MC 26.1: bind data components to all registry holders.
                // Bootstrap alone no longer completes item initialization — components
                // must be explicitly applied before ItemStack can be constructed.
                // Mirrors RegistryDataCollector.updateComponents() in normal gameplay.
                //
                // Two complications in the test environment:
                // 1. Some component initializers (e.g. fire-resistant items) look up tags in
                //    dynamic registries (like damage_type) that are NOT in BuiltInRegistries —
                //    they're only loaded from data packs at runtime. We handle this by returning
                //    a phantom lookup for any unknown registry that answers all tag queries with
                //    empty named sets.
                // 2. Tags that ARE in built-in registries may also not be bound during bootstrap
                //    (they're loaded from data packs). We apply the same empty-tag fallback there.
                RegistryAccess.Frozen base = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
                HolderLookup.Provider tagSafeRegistries = new HolderLookup.Provider() {
                    @Override
                    public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                        return base.listRegistryKeys();
                    }

                    @Override
                    public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(
                            ResourceKey<? extends Registry<? extends T>> key) {
                        return base.lookup(key)
                                .<HolderLookup.RegistryLookup<T>>map(inner ->
                                        new HolderLookup.RegistryLookup.Delegate<T>() {
                                            @Override
                                            public HolderLookup.RegistryLookup<T> parent() {
                                                return inner;
                                            }

                                            @Override
                                            public Optional<HolderSet.Named<T>> get(TagKey<T> tagKey) {
                                                return inner.get(tagKey)
                                                        .or(() -> Optional.of(HolderSet.emptyNamed(this, tagKey)));
                                            }
                                        })
                                // Dynamic registries (e.g. damage_type) are not in BuiltInRegistries;
                                // return a phantom that answers tag queries with empty sets.
                                .or(() -> Optional.of(phantomLookup(key)));
                    }
                };

                BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                        .build(tagSafeRegistries)
                        .forEach(DataComponentInitializers.PendingComponents::apply);

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

    private static <T> HolderLookup.RegistryLookup<T> phantomLookup(ResourceKey<? extends Registry<? extends T>> key) {
        return new HolderLookup.RegistryLookup<T>() {
            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return key;
            }

            @Override
            public Lifecycle registryLifecycle() {
                return Lifecycle.stable();
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
                // Return a stand-alone stub holder — value() would throw if accessed,
                // but component initializers only need to store the reference, not read it.
                return Optional.of(Holder.Reference.createStandAlone(this, id));
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return Stream.empty();
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> tagKey) {
                return Optional.of(HolderSet.emptyNamed(this, tagKey));
            }

            @Override
            public Stream<HolderSet.Named<T>> listTags() {
                return Stream.empty();
            }
        };
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
