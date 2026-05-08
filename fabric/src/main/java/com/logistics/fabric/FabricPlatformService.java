package com.logistics.fabric;

import com.logistics.core.lib.platform.PlatformService;
import net.fabricmc.loader.api.FabricLoader;

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
}
