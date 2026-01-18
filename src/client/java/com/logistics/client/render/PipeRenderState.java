package com.logistics.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

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
        public final @Nullable Direction armDirection;

        /** Constructor for cores/decorations (no rotation needed). */
        public ModelRenderInfo(Identifier modelId, int color) {
            this(modelId, color, null);
        }

        /** Constructor for arm models that need rotation based on direction. */
        public ModelRenderInfo(Identifier modelId, int color, @Nullable Direction armDirection) {
            this.modelId = modelId;
            this.color = color;
            this.armDirection = armDirection;
        }
    }
}
