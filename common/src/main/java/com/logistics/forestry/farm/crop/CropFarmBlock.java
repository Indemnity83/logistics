package com.logistics.forestry.farm.crop;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** The Crop Farm block: harvests and replants ripe field crops in its Reach; see {@link CropTendLogic}. */
public class CropFarmBlock extends FarmMachineBlock {
    public static final MapCodec<CropFarmBlock> CODEC = simpleCodec(CropFarmBlock::new);

    private static final TendLogic LOGIC = new CropTendLogic();

    public CropFarmBlock(Properties settings) {
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
