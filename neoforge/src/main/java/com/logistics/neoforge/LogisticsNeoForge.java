package com.logistics.neoforge;

import com.logistics.core.bootstrap.LogisticsCommonBootstrap;
import com.logistics.neoforge.platform.NeoForgeCreativeTabRegistrar;
import com.logistics.neoforge.platform.NeoForgeResourceReloadRegistrar;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod("logistics")
public final class LogisticsNeoForge {

    private static final LogisticsCommonBootstrap COMMON_BOOTSTRAP = new LogisticsCommonBootstrap();

    public LogisticsNeoForge(IEventBus modBus) {
        COMMON_BOOTSTRAP.initialize();

        // Wire deferred event registrations on the creative tab and reload registrars
        // (The SPI INSTANCE is the NeoForge impl because it's the only implementation on classpath)
        var creativeTabRegistrar = com.logistics.core.lib.platform.CreativeTabRegistrar.INSTANCE;
        if (creativeTabRegistrar instanceof NeoForgeCreativeTabRegistrar nfRegistrar) {
            nfRegistrar.init(modBus);
        }
        var reloadRegistrar = com.logistics.core.lib.platform.ResourceReloadRegistrar.INSTANCE;
        if (reloadRegistrar instanceof NeoForgeResourceReloadRegistrar nfRegistrar) {
            nfRegistrar.init(net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
        }

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // TODO(neoforge): capability registration via RegisterCapabilitiesEvent
    }
}
