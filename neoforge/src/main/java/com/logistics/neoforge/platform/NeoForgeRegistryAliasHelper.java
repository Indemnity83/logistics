package com.logistics.neoforge.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.IRegistryExtension;

final class NeoForgeRegistryAliasHelper {
    private NeoForgeRegistryAliasHelper() {}

    @SuppressWarnings("unchecked")
    static <T> void addAlias(Registry<T> registry, Identifier from, Identifier to) {
        ((IRegistryExtension<T>) registry).addAlias(from, to);
    }
}
