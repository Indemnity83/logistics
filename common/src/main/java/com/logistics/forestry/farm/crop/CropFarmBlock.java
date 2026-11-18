package com.logistics.forestry.farm.crop;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;

/** The Crop Farm block: harvests and replants ripe field crops in its Reach; see {@link CropTendLogic}. */
public class CropFarmBlock extends FarmMachineBlock {

    private static final TendLogic LOGIC = new CropTendLogic();

    public CropFarmBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TendLogic logic() {
        return LOGIC;
    }
}
