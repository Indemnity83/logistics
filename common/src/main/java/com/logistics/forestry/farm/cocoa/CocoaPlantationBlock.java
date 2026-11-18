package com.logistics.forestry.farm.cocoa;

import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.TendLogic;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** The Cocoa Plantation block: picks and replants ripe cocoa on jungle logs in its Reach; see {@link CocoaTendLogic}. */
public class CocoaPlantationBlock extends FarmMachineBlock {
    public static final MapCodec<CocoaPlantationBlock> CODEC = simpleCodec(CocoaPlantationBlock::new);

    private static final TendLogic LOGIC = new CocoaTendLogic();

    public CocoaPlantationBlock(Properties settings) {
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
