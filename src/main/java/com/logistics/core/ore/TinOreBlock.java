package com.logistics.core.ore;

import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.DropExperienceBlock;

public class TinOreBlock extends DropExperienceBlock {
    public TinOreBlock(Properties properties) {
        super(ConstantInt.of(0), properties);
    }
}
