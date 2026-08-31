package com.logistics.compat.jade;

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
import com.logistics.pipe.block.FluidPipeBlock;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.power.block.CreativeSinkBlock;
import com.logistics.power.cable.CableBlock;
import snownee.jade.api.IWailaClientRegistration;

/** Registers the tooltip halves of the Jade integration; discovered by {@link JadeLogisticsPlugin}. */
public final class JadeLogisticsClientPlugin implements JadeClientPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EngineComponentProvider.INSTANCE, AbstractEngineBlock.class);
        registration.registerBlockComponent(PowerInfraComponentProvider.INSTANCE, CableBlock.class);
        registration.registerBlockComponent(PowerInfraComponentProvider.INSTANCE, CreativeSinkBlock.class);
        registration.registerBlockComponent(QuarryComponentProvider.INSTANCE, LaserQuarryBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, MaceratorBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, KilnBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, SawmillBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, AlloySmelterBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, CrucibleBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, RefineryBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, SequentialFabricatorBlock.class);
        registration.registerBlockComponent(MachineComponentProvider.INSTANCE, TransposerBlock.class);
        registration.registerBlockComponent(PipeComponentProvider.INSTANCE, PipeBlock.class);
        registration.registerBlockComponent(FluidPipeComponentProvider.INSTANCE, FluidPipeBlock.class);
    }
}
