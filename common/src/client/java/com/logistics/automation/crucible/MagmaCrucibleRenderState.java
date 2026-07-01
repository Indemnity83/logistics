package com.logistics.automation.crucible;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Frame-local render data for the crucible's face gauge: the tank fluid's sprite, tint, fill, and facing. */
public class MagmaCrucibleRenderState extends BlockEntityRenderState {
    public boolean hasFluid;
    public float fillRatio;
    public int tintColor = 0xFFFFFFFF;
    public Direction facing = Direction.NORTH;
    @Nullable public TextureAtlasSprite sprite;
}
