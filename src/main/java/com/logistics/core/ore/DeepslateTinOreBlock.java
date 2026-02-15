package com.logistics.core.ore;

import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.DropExperienceBlock;

public class DeepslateTinOreBlock extends DropExperienceBlock {
    public DeepslateTinOreBlock(Properties properties) {
        super(ConstantInt.of(0), properties);
    }
}
