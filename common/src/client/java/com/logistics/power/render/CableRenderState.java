package com.logistics.power.render;

import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jetbrains.annotations.Nullable;

/** Render state for {@link CableBlockEntityRenderer}: the pre-built model for this cable's connection mask. */
public class CableRenderState extends BlockEntityRenderState {
    @Nullable
    public BlockStateModel model;
}
