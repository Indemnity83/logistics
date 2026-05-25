package com.logistics.neoforge.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.IRegistryExtension;

final class NeoForgeRegistryAliasHelper {
    private NeoForgeRegistryAliasHelper() {}

    @SuppressWarnings("unchecked")
    static <T> void addAlias(Registry<T> registry, ResourceLocation from, ResourceLocation to) {
        ((IRegistryExtension<T>) registry).addAlias(from, to);
    }
}
