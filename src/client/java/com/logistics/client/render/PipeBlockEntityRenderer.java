package com.logistics.client.render;

import com.logistics.block.PipeBlock;
import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.pipe.runtime.PipeConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Renders traveling items inside pipes
 */
public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity> {
    private final ItemRenderer itemRenderer;

    public PipeBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(PipeBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {

        for (TravelingItem travelingItem : entity.getTravelingItems()) {
            matrices.push();

            // Calculate the item's current position along the pipe
            // Items travel from -0.5 to +0.5 in their direction
            Direction direction = travelingItem.getDirection();

            // Get pipe's target speed, acceleration, and whether it can accelerate
            BlockState state = entity.getCachedState();
            float targetSpeed = PipeConfig.BASE_PIPE_SPEED;
            float accelerationRate = 0f;
            boolean canAccelerate = false;

            if (state.getBlock() instanceof PipeBlock pipeBlock) {
                if (pipeBlock.getPipe() != null && entity.getWorld() != null) {
                    PipeContext context = new PipeContext(entity.getWorld(), entity.getPos(), state, entity);
                    targetSpeed = pipeBlock.getPipe().getTargetSpeed(context);
                    accelerationRate = pipeBlock.getPipe().getAccelerationRate(context);
                    canAccelerate = pipeBlock.getPipe().canAccelerate(context);
                }
            }

            // Simulate acceleration for this partial tick
            float progress = travelingItem.getProgress();
            float currentSpeed = travelingItem.getSpeed();

            // Calculate speed change during this partial tick
            float speedChange;
            if (currentSpeed < targetSpeed) {
                // Only accelerate if allowed
                if (canAccelerate) {
                    speedChange = Math.min(accelerationRate * tickDelta, targetSpeed - currentSpeed);
                } else {
                    speedChange = 0; // Maintain speed (no acceleration)
                }
            } else if (currentSpeed > targetSpeed) {
                // Always decelerate (drag)
                speedChange = Math.max(-accelerationRate * tickDelta, targetSpeed - currentSpeed);
            } else {
                speedChange = 0;
            }

            // Speed at the end of this partial tick
            float interpolatedSpeed = currentSpeed + speedChange;

            // Use average speed for progress calculation (trapezoidal integration)
            float avgSpeed = (currentSpeed + interpolatedSpeed) / 2.0f;
            float interpolatedProgress = progress + (avgSpeed * tickDelta);

            // Start at center of pipe (offset Y down slightly to account for ground item offset)
            matrices.translate(0.5, 0.375, 0.5);

            // Calculate position along the travel direction
            // Progress 0.0 = entering from opposite direction (-0.5)
            // Progress 1.0 = exiting in travel direction (+0.5)
            float travelDistance = interpolatedProgress - 0.5f;
            matrices.translate(
                direction.getOffsetX() * travelDistance,
                direction.getOffsetY() * travelDistance,
                direction.getOffsetZ() * travelDistance
            );

            // Keep items at ground scale (no scaling)
            // ItemDisplayContext.GROUND already handles proper item sizing
            // No rotation - items move straight through the pipe

            // Render the item
            itemRenderer.renderItem(
                travelingItem.getStack(),
                ItemDisplayContext.GROUND,
                light,
                overlay,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                0
            );

            matrices.pop();
        }
    }
}
