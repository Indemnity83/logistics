package com.logistics.forestry.farm.arboretum;

import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmMachineBlock;

/** The tree farm block; see {@link ArboretumLogic}. */
public class ArboretumBlock extends FarmMachineBlock {

    private static final FarmLogic LOGIC = new ArboretumLogic();

    public ArboretumBlock(Properties settings) {
        super(settings);
    }

    @Override
    public FarmLogic logic() {
        return LOGIC;
    }
}
