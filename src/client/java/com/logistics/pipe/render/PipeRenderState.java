package com.logistics.pipe.render;

import com.logistics.core.lib.resource.ResourceId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PipeRenderState extends BlockEntityRenderState {
    public final List<TravelingItemRenderState> travelingItems = new ArrayList<>();
    public final List<ModelRenderInfo> models = new ArrayList<>();
    public BlockState blockState;
    public float tickDelta;
    public float accelerationRate;
    public float dragCoefficient;
    public float maxSpeed;

    // Item render state cache by ItemVariant — avoids re-resolving models each frame
    public final Map<ItemVariant, ItemStackRenderState> itemRenderCache = new HashMap<>();

    public static final class ModelRenderInfo {
        public final ResourceId modelId;
        public final int color;
        public final @Nullable Direction armDirection;
        // Model populated from renderer-level modelsCache in extractRenderState(), reused in submit()
        @Nullable public BlockStateModel model = null;

        /** Constructor for cores/decorations (no rotation needed). */
        public ModelRenderInfo(ResourceId modelId, int color) {
            this(modelId, color, null);
        }

        /** Constructor for arm models that need rotation based on direction. */
        public ModelRenderInfo(ResourceId modelId, int color, @Nullable Direction armDirection) {
            this.modelId = modelId;
            this.color = color;
            this.armDirection = armDirection;
        }
    }
}
