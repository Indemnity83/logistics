package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.block.CreativeSinkBlock;
import com.logistics.power.cable.CableBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade HUD integration. Compiled against Jade's API only (clientCompileOnly) and loaded via the
 * {@code "jade"} entrypoint in fabric.mod.json — so it only initializes when Jade is actually installed.
 *
 * <p>Per-block content is added in stacked passes; see {@code EngineServerDataProvider} /
 * {@code EngineComponentProvider} for engines and {@code PowerInfra*Provider} for cables and the sink.
 */
@WailaPlugin(LogisticsMod.MOD_ID)
public class JadeLogisticsPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EngineServerDataProvider.INSTANCE, AbstractEngineBlock.class);
        registration.registerBlockDataProvider(PowerInfraServerDataProvider.INSTANCE, CreativeSinkBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EngineComponentProvider.INSTANCE, AbstractEngineBlock.class);
        registration.registerBlockComponent(PowerInfraComponentProvider.INSTANCE, CableBlock.class);
        registration.registerBlockComponent(PowerInfraComponentProvider.INSTANCE, CreativeSinkBlock.class);
    }
}
