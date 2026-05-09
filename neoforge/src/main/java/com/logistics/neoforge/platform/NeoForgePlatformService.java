package com.logistics.neoforge.platform;

import com.logistics.core.lib.platform.PlatformService;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.ModList;

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
}
