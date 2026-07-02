package com.logistics.core.lib.fluids;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * A trivial public subclass of {@link LiquidBlock}, whose constructor is protected. Used for the world
 * block of a placeable custom fluid (crude oil); behaviour is entirely vanilla.
 */
public class LogisticsLiquidBlock extends LiquidBlock {

    public LogisticsLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }
}
