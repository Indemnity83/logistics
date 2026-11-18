package com.logistics.forestry.farm.orchard;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;

/** The Orchard block: picks apples off living trees in its Reach; see {@link OrchardTendLogic}. */
public class OrchardBlock extends FarmMachineBlock {

    private static final TendLogic LOGIC = new OrchardTendLogic();

    public OrchardBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TendLogic logic() {
        return LOGIC;
    }
}
