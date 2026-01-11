package com.logistics.client.render;

import com.logistics.block.PipeBlock;
import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.runtime.PipeConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders traveling items inside pipes
 */
public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity, PipeBlockEntityRenderer.PipeRenderState> {
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

        // Clear previous items
        state.travelingItems.clear();

        // Get pipe properties for speed calculations
        BlockState blockState = entity.getCachedState();
        float targetSpeed = PipeConfig.BASE_PIPE_SPEED;
        float accelerationRate = 0f;
        boolean canAccelerate = false;

        if (blockState.getBlock() instanceof PipeBlock pipeBlock) {
            if (pipeBlock.getPipe() != null && entity.getWorld() != null) {
                PipeContext context = new PipeContext(entity.getWorld(), entity.getPos(), blockState, entity);
                targetSpeed = pipeBlock.getPipe().getTargetSpeed(context);
                accelerationRate = pipeBlock.getPipe().getAccelerationRate(context);
                canAccelerate = pipeBlock.getPipe().canAccelerate(context);
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
            itemState.targetSpeed = targetSpeed;
            itemState.accelerationRate = accelerationRate;
            itemState.canAccelerate = canAccelerate;

            state.travelingItems.add(itemState);
        }
    }

    @Override
    public void render(
        PipeRenderState state,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        CameraRenderState cameraState
    ) {
        for (TravelingItemRenderState itemState : state.travelingItems) {
            matrices.push();

            // Calculate speed change during this partial tick
            float speedChange;
            if (itemState.currentSpeed < itemState.targetSpeed) {
                // Only accelerate if allowed
                if (itemState.canAccelerate) {
                    speedChange = Math.min(
                        itemState.accelerationRate * state.tickDelta,
                        itemState.targetSpeed - itemState.currentSpeed
                    );
                } else {
                    speedChange = 0; // Maintain speed (no acceleration)
                }
            } else if (itemState.currentSpeed > itemState.targetSpeed) {
                // Always decelerate (drag)
                speedChange = Math.max(
                    -itemState.accelerationRate * state.tickDelta,
                    itemState.targetSpeed - itemState.currentSpeed
                );
            } else {
                speedChange = 0;
            }

            // Speed at the end of this partial tick
            float interpolatedSpeed = itemState.currentSpeed + speedChange;

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
                0, // overlay
                0  // outlineColors
            );

            matrices.pop();
        }
    }

    // Render state classes
    public static class PipeRenderState extends BlockEntityRenderState {
        public final List<TravelingItemRenderState> travelingItems = new ArrayList<>();
        public float tickDelta;
    }

    public static class TravelingItemRenderState {
        public final ItemRenderState itemRenderState = new ItemRenderState();
        public Direction direction;
        public float progress;
        public float currentSpeed;
        public float targetSpeed;
        public float accelerationRate;
        public boolean canAccelerate;
    }
}
