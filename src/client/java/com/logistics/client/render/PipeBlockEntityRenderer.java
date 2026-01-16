package com.logistics.client.render;

import com.logistics.LogisticsMod;
import com.logistics.block.PipeBlock;
import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.DykemModule;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.runtime.PipeConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders traveling items inside pipes
 */
public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity, PipeRenderState> {
    private final ItemModelManager itemModelManager;

    public PipeBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.itemModelManager = ctx.itemModelManager();
    }

    @Override
    public PipeRenderState createRenderState() {
        return new PipeRenderState();
    }

    @Override
    public void updateRenderState(
        PipeBlockEntity entity,
        PipeRenderState state,
        float tickDelta,
        Vec3d cameraPos,
        @Nullable net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay
    ) {
        // Update base block entity render state
        BlockEntityRenderState.updateBlockEntityRenderState(entity, state, crumblingOverlay);

        // Store tickDelta for use in render()
        state.tickDelta = tickDelta;

        state.models.clear();

        // Clear previous items
        state.travelingItems.clear();

        // Get pipe properties for speed calculations
        BlockState blockState = entity.getCachedState();
        state.blockState = blockState;
        float maxSpeed = PipeConfig.PIPE_MAX_SPEED;
        float accelerationRate = 0f;
        float dragCoefficient = PipeConfig.DRAG_COEFFICIENT;

        if (blockState.getBlock() instanceof PipeBlock pipeBlock) {
            if (pipeBlock.getPipe() != null && entity.getWorld() != null) {
                PipeContext context = new PipeContext(entity.getWorld(), entity.getPos(), blockState, entity);
                maxSpeed = pipeBlock.getPipe().getMaxSpeed(context);
                accelerationRate = pipeBlock.getPipe().getAccelerationRate(context);
                dragCoefficient = pipeBlock.getPipe().getDrag(context);

                state.models.add(new PipeRenderState.ModelRenderInfo(pipeBlock.getPipe().getCoreModelId(), 0xFFFFFF));

                DykemModule dykemModule = pipeBlock.getPipe().getModule(DykemModule.class);
                if (dykemModule != null) {
                    var color = dykemModule.getStoredColor(context);
                    if (color != null) {
                    Identifier dyedCore = Identifier.of(LogisticsMod.MOD_ID,
                        "block/" + pipeBlock.getPipe().getPipeName() + "_core_dyed");
                    state.models.add(new PipeRenderState.ModelRenderInfo(dyedCore, color.getFireworkColor()));
                    }
                }

                for (Direction direction : Direction.values()) {
                    PipeBlock.ConnectionType type = entity.getConnectionType(direction);
                    if (type == PipeBlock.ConnectionType.NONE) {
                        continue;
                    }
                    state.models.add(new PipeRenderState.ModelRenderInfo(
                        pipeBlock.getPipe().getPipeArm(context, direction),
                        0xFFFFFF
                    ));
                    for (Identifier decoration : pipeBlock.getPipe().getPipeDecorations(context, direction)) {
                        state.models.add(new PipeRenderState.ModelRenderInfo(decoration, 0xFFFFFF));
                    }
                }
            }
        }

        // Extract each traveling item
        for (TravelingItem travelingItem : entity.getTravelingItems()) {
            TravelingItemRenderState itemState = new TravelingItemRenderState();

            // Update the ItemRenderState using ItemModelManager
            this.itemModelManager.update(
                itemState.itemRenderState,
                travelingItem.getStack(),
                ItemDisplayContext.GROUND,
                entity.getWorld(),
                null, // heldItemContext - not held by entity
                0 // seed
            );

            // Store item data
            itemState.direction = travelingItem.getDirection();
            itemState.progress = travelingItem.getProgress();
            itemState.currentSpeed = travelingItem.getSpeed();

            state.travelingItems.add(itemState);
        }

        state.accelerationRate = accelerationRate;
        state.dragCoefficient = dragCoefficient;
        state.maxSpeed = maxSpeed;
    }

    @Override
    public void render(
        PipeRenderState state,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        CameraRenderState cameraState
    ) {
        if (!state.models.isEmpty()) {
            RenderLayer renderLayer = state.blockState == null
                ? RenderLayers.cutout()
                : BlockRenderLayers.getEntityBlockLayer(state.blockState);
            for (PipeRenderState.ModelRenderInfo modelInfo : state.models) {
                BlockStateModel model = PipeModelRegistry.getModel(modelInfo.modelId);
                if (model == null) {
                    continue;
                }
                int color = modelInfo.color;
                float red = ((color >> 16) & 0xFF) / 255.0f;
                float green = ((color >> 8) & 0xFF) / 255.0f;
                float blue = (color & 0xFF) / 255.0f;
                queue.submitBlockStateModel(
                    matrices,
                    renderLayer,
                    model,
                    red,
                    green,
                    blue,
                    state.lightmapCoordinates,
                    OverlayTexture.DEFAULT_UV,
                    0
                );
            }
        }

        for (TravelingItemRenderState itemState : state.travelingItems) {
            matrices.push();

            // Calculate speed change during this partial tick
            float speedChange = 0f;
            boolean deceleratingToMax = itemState.currentSpeed > state.maxSpeed;
            if (deceleratingToMax) {
                float remaining = Math.max(1.0e-4f, 1.0f - itemState.progress);
                float targetSquared = state.maxSpeed * state.maxSpeed;
                float currentSquared = itemState.currentSpeed * itemState.currentSpeed;
                float decel = (targetSquared - currentSquared) / (2.0f * remaining);
                speedChange = decel * state.tickDelta;
            } else if (state.accelerationRate != 0f) {
                speedChange = state.accelerationRate * state.tickDelta;
            } else if (state.dragCoefficient != 0f) {
                speedChange = -(itemState.currentSpeed * state.dragCoefficient) * state.tickDelta;
            }

            // Speed at the end of this partial tick
            float interpolatedSpeed = itemState.currentSpeed + speedChange;
            if (interpolatedSpeed < PipeConfig.ITEM_MIN_SPEED) {
                interpolatedSpeed = PipeConfig.ITEM_MIN_SPEED;
            } else if (!deceleratingToMax && interpolatedSpeed > state.maxSpeed) {
                interpolatedSpeed = state.maxSpeed;
            }

            // Use average speed for progress calculation (trapezoidal integration)
            float avgSpeed = (itemState.currentSpeed + interpolatedSpeed) / 2.0f;
            float interpolatedProgress = itemState.progress + (avgSpeed * state.tickDelta);

            // Start at center of pipe (offset Y down slightly to account for ground item offset)
            matrices.translate(0.5, 0.375, 0.5);

            // Calculate position along the travel direction
            // Progress 0.0 = entering from opposite direction (-0.5)
            // Progress 1.0 = exiting in travel direction (+0.5)
            float travelDistance = interpolatedProgress - 0.5f;
            matrices.translate(
                itemState.direction.getOffsetX() * travelDistance,
                itemState.direction.getOffsetY() * travelDistance,
                itemState.direction.getOffsetZ() * travelDistance
            );

            // Keep items at ground scale (no scaling)
            // ItemDisplayContext.GROUND already handles proper item sizing
            // No rotation - items move straight through the pipe

            // Render the item using ItemRenderState.render()
            itemState.itemRenderState.render(
                matrices,
                queue,
                state.lightmapCoordinates,
                OverlayTexture.DEFAULT_UV,
                0  // outlineColors
            );

            matrices.pop();
        }
    }

    // Render state classes
}
