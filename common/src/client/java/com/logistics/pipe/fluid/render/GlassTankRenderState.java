package com.logistics.pipe.fluid.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

/** Frame-local render data for a glass tank: the contained fluid's sprite, tint, and fill level. */
public class GlassTankRenderState extends BlockEntityRenderState {
    public boolean hasFluid;
    public float fillRatio;
    public boolean renderTop;
    public int tintColor = 0xFFFFFFFF;
    @Nullable public TextureAtlasSprite sprite;
}
