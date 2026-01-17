package com.logistics.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.util.Identifier;

public class PipeRenderState extends BlockEntityRenderState {
    public final List<TravelingItemRenderState> travelingItems = new ArrayList<>();
    public final List<ModelRenderInfo> models = new ArrayList<>();
    public BlockState blockState;
    public float tickDelta;
    public float accelerationRate;
    public float dragCoefficient;
    public float maxSpeed;

    public static final class ModelRenderInfo {
        public final Identifier modelId;
        public final int color;

        public ModelRenderInfo(Identifier modelId, int color) {
            this.modelId = modelId;
            this.color = color;
        }
    }
}
