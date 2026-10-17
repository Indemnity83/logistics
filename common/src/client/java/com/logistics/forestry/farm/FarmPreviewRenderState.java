package com.logistics.forestry.farm;

import java.util.List;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for a farm's area preview. */
public class FarmPreviewRenderState extends BlockEntityRenderState {
    public boolean visible;
    public List<FarmPreviewLines.Line> lines = List.of();
    public float lineWidth = 1.0f;
}
