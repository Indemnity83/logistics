package com.logistics.neoforge.platform;

import com.logistics.core.lib.platform.PlatformService;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
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
        return SharedConstants.getCurrentVersion().id();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
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
