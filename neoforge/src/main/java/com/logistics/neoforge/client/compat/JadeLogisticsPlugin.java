package com.logistics.neoforge.client.compat;

import com.logistics.LogisticsMod;
import com.logistics.automation.kiln.KilnBlock;
import com.logistics.core.macerator.MaceratorBlock;
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
 * {@code MachineServerDataProvider} / {@code MachineComponentProvider} for the macerator and kiln.
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
