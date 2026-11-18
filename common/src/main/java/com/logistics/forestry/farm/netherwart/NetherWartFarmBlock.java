package com.logistics.forestry.farm.netherwart;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;

/** The Nether Wart Farm block: harvests and replants ripe nether wart in its Reach; see {@link NetherWartTendLogic}. */
public class NetherWartFarmBlock extends FarmMachineBlock {

    private static final TendLogic LOGIC = new NetherWartTendLogic();

    public NetherWartFarmBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TendLogic logic() {
        return LOGIC;
    }
}
