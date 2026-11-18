package com.logistics.forestry.farm.plantation;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;

/**
 * The Plantation block: harvests sugar cane, bamboo and cactus above their base in its Reach; see
 * {@link PlantationTendLogic}.
 */
public class PlantationBlock extends FarmMachineBlock {

    private static final TendLogic LOGIC = new PlantationTendLogic();

    public PlantationBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TendLogic logic() {
        return LOGIC;
    }
}
