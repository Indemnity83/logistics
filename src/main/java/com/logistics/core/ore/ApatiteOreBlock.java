package com.logistics.core.ore;

import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.DropExperienceBlock;

public class ApatiteOreBlock extends DropExperienceBlock {
    public ApatiteOreBlock(Properties properties) {
        super(ConstantInt.of(0), properties);
    }
}
