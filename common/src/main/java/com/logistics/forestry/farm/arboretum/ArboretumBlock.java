package com.logistics.forestry.farm.arboretum;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;

/** The Arboretum block: fells and replants trees grown on Humus in its Reach; see {@link ArboretumTendLogic}. */
public class ArboretumBlock extends FarmMachineBlock {

    private static final TendLogic LOGIC = new ArboretumTendLogic();

    public ArboretumBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TendLogic logic() {
        return LOGIC;
    }
}
