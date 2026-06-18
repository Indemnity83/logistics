package com.logistics.pipe.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;

public class FluidPumpRenderState extends BlockEntityRenderState {
    public BlockPos pumpPos = BlockPos.ZERO;
    public float armY;
    public int tubeLight;
}
