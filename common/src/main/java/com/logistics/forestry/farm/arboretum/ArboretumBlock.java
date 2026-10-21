package com.logistics.forestry.farm.arboretum;

import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;

/** The Arboretum block: fells and replants trees grown on Humus in its Reach; see {@link ArboretumTendLogic}. */
public class ArboretumBlock extends FarmMachineBlock {

    private static final FarmLogic LOGIC = new ArboretumLogic();
    private static final TendLogic TEND = new ArboretumTendLogic();

    public ArboretumBlock(Properties settings) {
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
