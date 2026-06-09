package com.logistics.fabric;

import com.logistics.core.lib.platform.PlatformService;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;

import java.nio.file.Path;

public final class FabricPlatformService implements PlatformService {
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String getModName(String namespace) {
        return FabricLoader.getInstance()
                .getModContainer(namespace)
                .map(c -> c.getMetadata().getName())
                .orElse(namespace);
    }

    @Override
    public <T> void registerAlias(Registry<T> registry, ResourceId oldId, T currentEntry) {
        var currentId = registry.getKey(currentEntry); // Fabric API extension
        if (currentId != null) {
            registry.addAlias(oldId.toIdentifier(), currentId); // Fabric API extension
        }
    }
}
