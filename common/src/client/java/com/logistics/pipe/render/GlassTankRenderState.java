package com.logistics.pipe.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

/** Frame-local render data for a glass tank: the contained fluid's sprite, tint, and fill level. */
public class GlassTankRenderState extends BlockEntityRenderState {
    public boolean hasFluid;
    public float fillRatio;
    /** True when this cell holds a gas, which settles against the ceiling rather than the floor. */
    public boolean gas;
    /** Whether this cell owns the free surface — the top face for a liquid, the bottom for a gas. */
    public boolean renderSurface;
    public int tintColor = 0xFFFFFFFF;
    @Nullable public TextureAtlasSprite sprite;
}
