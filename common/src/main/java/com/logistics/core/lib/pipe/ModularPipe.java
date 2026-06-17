package com.logistics.core.lib.pipe;

import org.jetbrains.annotations.Nullable;

/**
 * A pipe definition that composes {@link Module}s. Implemented by both the item pipe ({@code Pipe})
 * and the fluid pipe module host, so family-aware modules can look up a neighbour's module of a
 * given type without depending on a concrete pipe type.
 */
public interface ModularPipe {
    /**
     * Return this pipe's module of the given type, or {@code null} if none is present.
     *
     * <p>The {@code host} lets implementations that support runtime-installed modules (e.g. chassis
     * pipes) consult the block entity. Implementations with a fixed module list may ignore it.
     */
    @Nullable <T extends Module> T getModule(Class<T> moduleClass, IModuleHost host);
}
