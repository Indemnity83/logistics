package com.logistics.neoforge.platform;

import com.logistics.core.lib.platform.PlatformService;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.Registry;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.IRegistryExtension;

import java.nio.file.Path;

/**
 * NeoForge implementation of {@link PlatformService}.
 */
public final class NeoForgePlatformService implements PlatformService {

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String getModName(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(c -> c.getModInfo().getDisplayName())
                .orElse(namespace);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void registerAlias(Registry<T> registry, ResourceId oldId, T currentEntry) {
        var currentId = registry.getKey(currentEntry);
        if (currentId != null) {
            ((IRegistryExtension<T>) registry).addAlias(oldId.toIdentifier(), currentId);
        }
    }
}
