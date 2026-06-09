package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade HUD integration. Compiled against Jade's API only (clientCompileOnly) and loaded via the
 * {@code "jade"} entrypoint in fabric.mod.json — so it only initializes when Jade is actually installed.
 *
 * <p>Per-block content is added in stacked passes; see {@code QuarryServerDataProvider} /
 * {@code QuarryComponentProvider} for the laser quarry.
 */
@WailaPlugin(LogisticsMod.MOD_ID)
public class JadeLogisticsPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(QuarryServerDataProvider.INSTANCE, LaserQuarryBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(QuarryComponentProvider.INSTANCE, LaserQuarryBlock.class);
    }
}
