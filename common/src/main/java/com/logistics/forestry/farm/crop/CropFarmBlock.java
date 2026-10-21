package com.logistics.forestry.farm.crop;

import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;

/** The Crop Farm block: harvests and replants ripe field crops in its Reach. */
public class CropFarmBlock extends FarmMachineBlock {

    private static final FarmLogic LOGIC = new CropFarmLogic();
    private static final TendLogic TEND = new CropTendLogic();

    public CropFarmBlock(Properties settings) {
        super(settings);
    }

    @Override
    public FarmLogic logic() {
        return LOGIC;
    }

    @Override
    public TendLogic tendLogic() {
        return TEND;
    }
}
