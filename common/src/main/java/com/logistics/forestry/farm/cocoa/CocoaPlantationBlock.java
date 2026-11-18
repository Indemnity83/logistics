package com.logistics.forestry.farm.cocoa;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;

/** The Cocoa Plantation block: picks and replants ripe cocoa on jungle logs in its Reach; see {@link CocoaTendLogic}. */
public class CocoaPlantationBlock extends FarmMachineBlock {

    private static final TendLogic LOGIC = new CocoaTendLogic();

    public CocoaPlantationBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TendLogic logic() {
        return LOGIC;
    }
}
