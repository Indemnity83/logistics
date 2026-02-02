package com.logistics.pipe.render;

import com.logistics.core.lib.pipe.PipeConnection;
import com.logistics.core.render.ModelRegistry;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.runtime.TravelingItem;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

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
            @Nullable net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
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

                Pipe pipe = pipeBlock.getPipe();
                state.models.add(new PipeRenderState.ModelRenderInfo(pipe.getCoreModelId(context), 0xFFFFFF));
                for (Pipe.CoreDecoration decoration : pipe.getCoreDecorations(context)) {
                    state.models.add(new PipeRenderState.ModelRenderInfo(decoration.modelId(), decoration.color()));
                }

                for (Direction direction : Direction.values()) {
                    PipeConnection.Type type = entity.getCachedConnectionType(direction);
                    if (type == PipeConnection.Type.NONE) {
                        continue;
                    }

                    // Get arm model (module override or base), rotate at render time
                    Identifier armModel = pipe.getPipeArm(context, direction);
                    Integer armTint = pipe.getArmTint(context, direction);
                    int armColor = armTint != null ? armTint : 0xFFFFFF;
                    state.models.add(new PipeRenderState.ModelRenderInfo(armModel, armColor, direction));

                    for (Identifier decoration : pipe.getPipeDecorations(context, direction)) {
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
            CameraRenderState cameraState) {
        if (!state.models.isEmpty()) {
            RenderLayer renderLayer = state.blockState == null
                    ? RenderLayers.cutout()
                    : BlockRenderLayers.getEntityBlockLayer(state.blockState);
            for (PipeRenderState.ModelRenderInfo modelInfo : state.models) {
                BlockStateModel model = ModelRegistry.getModel(modelInfo.modelId);
                if (model == null) {
                    continue;
                }

                // Apply rotation for arm models
                if (modelInfo.armDirection != null) {
                    matrices.push();
                    matrices.translate(0.5, 0.5, 0.5); // Rotate around block center
                    applyDirectionRotation(matrices, modelInfo.armDirection);
                    matrices.translate(-0.5, -0.5, -0.5);
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
                        0);

                if (modelInfo.armDirection != null) {
                    matrices.pop();
                }
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
                    itemState.direction.getOffsetZ() * travelDistance);

            // Keep items at ground scale (no scaling)
            // ItemDisplayContext.GROUND already handles proper item sizing
            // No rotation - items move straight through the pipe

            // Render the item using ItemRenderState.render()
            itemState.itemRenderState.render(
                    matrices, queue, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, 0 // outlineColors
                    );

            matrices.pop();
        }
    }

    /**
     * Applies rotation to the matrix stack for rendering arm models in the given direction.
     * The base arm model is oriented NORTH; this method rotates it to face other directions.
     */
    private static void applyDirectionRotation(MatrixStack matrices, Direction direction) {
        switch (direction) {
            case SOUTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            case EAST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
            case WEST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            case UP -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            case DOWN -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            default -> {} // NORTH: Base orientation, no rotation
        }
    }
}
