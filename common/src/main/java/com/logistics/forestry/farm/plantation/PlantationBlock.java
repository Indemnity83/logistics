package com.logistics.forestry.farm.plantation;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/**
 * The Plantation block: harvests sugar cane, bamboo and cactus above their base in its Reach; see
 * {@link PlantationTendLogic}.
 */
public class PlantationBlock extends FarmMachineBlock {
    public static final MapCodec<PlantationBlock> CODEC = simpleCodec(PlantationBlock::new);

    private static final TendLogic LOGIC = new PlantationTendLogic();

    public PlantationBlock(Properties settings) {
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
