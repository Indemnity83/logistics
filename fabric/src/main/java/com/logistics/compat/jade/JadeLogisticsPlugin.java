package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.automation.alloysmelter.AlloySmelterBlock;
import com.logistics.automation.crucible.CrucibleBlock;
import com.logistics.automation.fabricator.SequentialFabricatorBlock;
import com.logistics.automation.kiln.KilnBlock;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.macerator.MaceratorBlock;
import com.logistics.automation.refinery.RefineryBlock;
import com.logistics.automation.sawmill.SawmillBlock;
import com.logistics.automation.transposer.TransposerBlock;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.block.CreativeSinkBlock;
import java.util.ServiceLoader;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade HUD integration. Compiled against Jade's API only (compileOnly) and loaded via the
 * {@code "jade"} entrypoint in fabric.mod.json — so it only initializes when Jade is actually installed.
 *
 * <p>Must stay in the common source set — Jade resolves this class on dedicated servers too. Client-only
 * registrations are reached through {@link JadeClientPlugin}.
 */
@WailaPlugin(LogisticsMod.MOD_ID)
public class JadeLogisticsPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EngineServerDataProvider.INSTANCE, AbstractEngineBlock.class);
        registration.registerBlockDataProvider(PowerInfraServerDataProvider.INSTANCE, CreativeSinkBlock.class);
        registration.registerBlockDataProvider(QuarryServerDataProvider.INSTANCE, LaserQuarryBlock.class);
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, MaceratorBlock.class);
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, KilnBlock.class);
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, SawmillBlock.class);
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, AlloySmelterBlock.class);
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, CrucibleBlock.class);
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, RefineryBlock.class);
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, SequentialFabricatorBlock.class);
        registration.registerBlockDataProvider(MachineServerDataProvider.INSTANCE, TransposerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        ServiceLoader.load(JadeClientPlugin.class, getClass().getClassLoader())
                .forEach(plugin -> plugin.registerClient(registration));
    }
}
