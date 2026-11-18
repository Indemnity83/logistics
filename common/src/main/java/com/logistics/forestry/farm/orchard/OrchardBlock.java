package com.logistics.forestry.farm.orchard;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** The Orchard block: picks apples off living trees in its Reach; see {@link OrchardTendLogic}. */
public class OrchardBlock extends FarmMachineBlock {
    public static final MapCodec<OrchardBlock> CODEC = simpleCodec(OrchardBlock::new);

    private static final TendLogic LOGIC = new OrchardTendLogic();

    public OrchardBlock(Properties settings) {
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
