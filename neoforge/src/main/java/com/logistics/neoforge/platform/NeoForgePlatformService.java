package com.logistics.neoforge.platform;

import com.logistics.core.lib.platform.PlatformService;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

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
    public <T> void registerAlias(Registry<T> registry, Identifier oldId, T currentEntry) {
        Identifier currentId = registry.getKey(currentEntry);
        if (currentId != null) {
            NeoForgeRegistryAliasHelper.addAlias(registry, oldId, currentId);
        }
    }
}
