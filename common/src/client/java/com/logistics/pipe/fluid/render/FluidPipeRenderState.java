package com.logistics.pipe.fluid.render;

import com.logistics.core.lib.resource.ResourceId;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Frame-local render data for a fluid pipe: its body parts plus the contained fluid's sprite/level. */
public class FluidPipeRenderState extends BlockEntityRenderState {
    public final List<ModelRenderInfo> models = new ArrayList<>();

    public boolean hasFluid;
    public float fillRatio;
    public int tintColor = 0xFFFFFFFF;
    @Nullable public TextureAtlasSprite sprite;
    /** Which sides have a fluid connection (indexed by {@link net.minecraft.core.Direction#get3DDataValue()}). */
    public final boolean[] connectedArms = new boolean[6];

    /** A body model part (core or directional arm), with parts cached at the renderer level. */
    public static final class ModelRenderInfo {
        public final ResourceId modelId;
        /** ARGB tint applied to this part; opaque white for the pipe body, set for module overlays (e.g. markings). */
        public final int color;
        public final @Nullable Direction armDirection;
        @Nullable public List<BlockStateModelPart> parts = null;

        public ModelRenderInfo(ResourceId modelId, @Nullable Direction armDirection) {
            this(modelId, 0xFFFFFFFF, armDirection);
        }

        public ModelRenderInfo(ResourceId modelId, int color, @Nullable Direction armDirection) {
            this.modelId = modelId;
            this.color = color;
            this.armDirection = armDirection;
        }
    }
}
