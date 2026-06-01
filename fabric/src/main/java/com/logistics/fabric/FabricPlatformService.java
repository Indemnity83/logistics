package com.logistics.fabric;

import com.logistics.core.lib.platform.PlatformService;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

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
    public String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer("logistics")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public <T> void registerAlias(Registry<T> registry, Identifier oldId, T currentEntry) {
        Identifier currentId = registry.getKey(currentEntry); // Fabric API extension
        if (currentId != null) {
            registry.addAlias(oldId, currentId); // Fabric API extension
        }
    }
}
