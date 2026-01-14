package com.logistics.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class PipeRenderState extends BlockEntityRenderState {
    public final List<TravelingItemRenderState> travelingItems = new ArrayList<>();
    public final List<Identifier> modelIds = new ArrayList<>();
    public BlockState blockState;
    public float tickDelta;
}
