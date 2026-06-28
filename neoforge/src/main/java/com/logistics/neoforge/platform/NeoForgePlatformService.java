package com.logistics.neoforge.platform;

import com.logistics.core.lib.platform.PlatformService;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
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
    public long fluidUnitsPerMillibucket() {
        // NeoForge measures fluids in millibuckets: 1_000 per bucket => 1 per mB.
        return 1L;
    }

    @Override
    public boolean isLighterThanAir(net.minecraft.world.level.material.Fluid fluid) {
        return fluid.getFluidType().isLighterThanAir();
    }

    @Override
    public String modVersion() {
        return ModList.get().getModContainerById("logistics")
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public String minecraftVersion() {
        return ModList.get().getModContainerById("minecraft")
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    @Override
    public String getModName(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(c -> c.getModInfo().getDisplayName())
                .orElse(namespace);
    }

    @Override
    public <T> void registerAlias(Registry<T> registry, ResourceLocation oldId, T currentEntry) {
        ResourceLocation currentId = registry.getKey(currentEntry);
        if (currentId != null) {
            NeoForgeRegistryAliasHelper.addAlias(registry, oldId, currentId);
        }
    }
}
