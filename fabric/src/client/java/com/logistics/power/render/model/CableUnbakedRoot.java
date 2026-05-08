package com.logistics.power.render.model;

import com.logistics.power.cable.CableTier;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.block.state.BlockState;

public final class CableUnbakedRoot implements BlockStateModel.UnbakedRoot {
    private final CableModel model;
    private final Object equalityGroup = new Object();

    public CableUnbakedRoot(CableTier tier) {
        this.model = new CableModel(tier);
    }

    @Override
    public BlockStateModel bake(BlockState state, ModelBaker baker) {
        return model;
    }

    @Override
    public Object visualEqualityGroup(BlockState state) {
        return equalityGroup;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        resolver.markDependency(AtlasIds.BLOCKS);
    }
}
