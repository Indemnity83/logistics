package com.logistics.forestry.farm.gourd;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** The Gourd Farm block: harvests the fruit of pumpkin and melon stems in its Reach; see {@link GourdTendLogic}. */
public class GourdFarmBlock extends FarmMachineBlock {
    public static final MapCodec<GourdFarmBlock> CODEC = simpleCodec(GourdFarmBlock::new);

    private static final TendLogic LOGIC = new GourdTendLogic();

    public GourdFarmBlock(Properties settings) {
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
