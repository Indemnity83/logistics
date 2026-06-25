package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsCore.BLOCK.QUARTZ_CRYSTAL, RenderType.translucent());
    }

    @Override
    public int order() {
        return -100; // Initialize core first
    }
}
