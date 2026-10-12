package com.logistics.forestry.farm.arboretum;

import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** The tree farm block; see {@link ArboretumLogic}. */
public class ArboretumBlock extends FarmMachineBlock {
    public static final MapCodec<ArboretumBlock> CODEC = simpleCodec(ArboretumBlock::new);

    private static final FarmLogic LOGIC = new ArboretumLogic();

    public ArboretumBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public FarmLogic logic() {
        return LOGIC;
    }
}
