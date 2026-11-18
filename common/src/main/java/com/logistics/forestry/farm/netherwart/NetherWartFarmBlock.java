package com.logistics.forestry.farm.netherwart;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** The Nether Wart Farm block: harvests and replants ripe nether wart in its Reach; see {@link NetherWartTendLogic}. */
public class NetherWartFarmBlock extends FarmMachineBlock {
    public static final MapCodec<NetherWartFarmBlock> CODEC = simpleCodec(NetherWartFarmBlock::new);

    private static final TendLogic LOGIC = new NetherWartTendLogic();

    public NetherWartFarmBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public TendLogic logic() {
        return LOGIC;
    }
}
