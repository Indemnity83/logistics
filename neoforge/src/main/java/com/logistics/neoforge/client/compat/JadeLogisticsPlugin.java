package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.power.AbstractEngineBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade HUD integration. Compiled against Jade's API only (compileOnly) and discovered at runtime
 * via the {@link WailaPlugin} annotation scan — so it only initializes when Jade is actually installed.
 *
 * <p>Client-only by nature (Jade is a client mod), hence the {@code com.logistics.neoforge.client} package
 * required by the NeoForge source-isolation rules. Per-block content is added in stacked passes; see
 * {@link EngineServerDataProvider} and {@link EngineComponentProvider} for engines.
 */
@WailaPlugin(LogisticsMod.MOD_ID)
public class JadeLogisticsPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EngineServerDataProvider.INSTANCE, AbstractEngineBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EngineComponentProvider.INSTANCE, AbstractEngineBlock.class);
    }
}
