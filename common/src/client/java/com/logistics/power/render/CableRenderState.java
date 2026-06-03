package com.logistics.power.render;

import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jetbrains.annotations.Nullable;

/** Render state for {@link CableBlockEntityRenderer}: the pre-built geometry for this cable's connection mask. */
public class CableRenderState extends BlockEntityRenderState {
    @Nullable
    public List<BlockStateModelPart> parts;
}
