package com.logistics.compat.jade;

import com.logistics.LogisticsMod;
import com.logistics.automation.kiln.KilnBlock;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.macerator.MaceratorBlock;
import com.logistics.pipe.block.FluidPipeBlock;
import com.logistics.pipe.block.PipeBlock;
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
 * {@code EngineComponentProvider} for engines, {@code PowerInfra*Provider} for cables and the sink,
 * {@code QuarryServerDataProvider} / {@code QuarryComponentProvider} for the laser quarry,
 * {@code MachineServerDataProvider} / {@code MachineComponentProvider} for the macerator and kiln, and
 * {@code PipeComponentProvider} for pipes.
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
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EngineComponentProvider.INSTANCE, AbstractEngineBlock.class);
        registration.registerBlockComponent(PowerInfraComponentProvider.INSTANCE, CableBlock.class);
        registration.registerBlockComponent(PowerInfraComponentProvider.INSTANCE, CreativeSinkBlock.class);
        registration.registerBlockComponent(QuarryComponentProvider.INSTANCE, LaserQuarryBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, MaceratorBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, KilnBlock.class);
        registration.registerBlockComponent(PipeComponentProvider.INSTANCE, PipeBlock.class);
        registration.registerBlockComponent(PipeComponentProvider.INSTANCE, FluidPipeBlock.class);
    }
}
