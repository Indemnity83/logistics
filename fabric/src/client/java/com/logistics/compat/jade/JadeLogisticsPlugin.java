package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.automation.kiln.KilnBlock;
import com.logistics.core.macerator.MaceratorBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade HUD integration. Compiled against Jade's API only (clientCompileOnly) and loaded via the
 * {@code "jade"} entrypoint in fabric.mod.json — so it only initializes when Jade is actually installed.
 *
 * <p>Per-block content is added in stacked passes; see {@code MachineServerDataProvider} /
 * {@code MachineComponentProvider} for the macerator and kiln.
 */
@WailaPlugin(LogisticsMod.MOD_ID)
public class JadeLogisticsPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, MaceratorBlock.class);
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, KilnBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, MaceratorBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, KilnBlock.class);
    }
}
