package com.logistics.fabric;

import com.logistics.core.lib.platform.ResourceReloadRegistrar;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public final class FabricResourceReloadRegistrar implements ResourceReloadRegistrar {
    @Override
    public void register(ResourceId id, PreparableReloadListener listener) {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(id.toIdentifier(), listener);
    }
}
