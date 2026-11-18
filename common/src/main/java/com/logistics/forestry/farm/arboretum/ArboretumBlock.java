package com.logistics.forestry.farm.arboretum;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** The Arboretum block: fells and replants trees grown on Humus in its Reach; see {@link ArboretumTendLogic}. */
public class ArboretumBlock extends FarmMachineBlock {
    public static final MapCodec<ArboretumBlock> CODEC = simpleCodec(ArboretumBlock::new);

    private static final TendLogic LOGIC = new ArboretumTendLogic();

    public ArboretumBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public TendLogic logic() {
        return LOGIC;
    }
}
