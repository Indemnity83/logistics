package com.logistics.forestry.farm.crop;

import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmMachineBlock;

/** The Crop Farm block: tills, plants and harvests field crops on its plot. */
public class CropFarmBlock extends FarmMachineBlock {

    private static final FarmLogic LOGIC = new CropFarmLogic();

    public CropFarmBlock(Properties settings) {
        super(settings);
    }

    @Override
    public FarmLogic logic() {
        return LOGIC;
    }
}
