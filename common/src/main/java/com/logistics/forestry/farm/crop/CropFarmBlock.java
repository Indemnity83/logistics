package com.logistics.forestry.farm.crop;

import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** The Crop Farm block: tills, waters, plants and harvests field crops on its plot. */
public class CropFarmBlock extends FarmMachineBlock {
    public static final MapCodec<CropFarmBlock> CODEC = simpleCodec(CropFarmBlock::new);

    private static final FarmLogic LOGIC = new CropFarmLogic();

    public CropFarmBlock(Properties settings) {
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
