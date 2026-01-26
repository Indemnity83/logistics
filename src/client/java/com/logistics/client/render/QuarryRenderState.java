package com.logistics.client.render;

import com.logistics.quarry.QuarryConfig;
import com.logistics.quarry.entity.QuarryBlockEntity;

import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Render state for the quarry arm visualization.
 */
public class QuarryRenderState extends BlockEntityRenderState {
    public boolean shouldRenderArm = false;
    public BlockPos quarryPos = BlockPos.ORIGIN;
    public Direction facing = Direction.NORTH;

    // Frame bounds
    public int frameStartX;
    public int frameStartZ;
    public int frameEndX;
    public int frameEndZ;
    public int frameTopY;

    // Server-synced arm position (absolute world coordinates)
    public float serverArmX;
    public float serverArmY;
    public float serverArmZ;

    // Client-side interpolated position for smooth rendering (populated from persistent cache)
    public float renderArmX;
    public float renderArmY;
    public float renderArmZ;

    // Current phase and arm state
    public QuarryBlockEntity.Phase phase = QuarryBlockEntity.Phase.CLEARING;
    public QuarryBlockEntity.ArmState armState = QuarryBlockEntity.ArmState.MOVING;

    // Light level sampled at the frame top (where horizontal beams are)
    public int frameTopLight;

    // Client-side interpolation speed (blocks per second, derived from server ARM_SPEED * 20 ticks/sec)
    private static final float CLIENT_ARM_SPEED_PER_SECOND = QuarryConfig.ARM_SPEED * 20f;

    // Persistent interpolation state stored per quarry position (survives render state recreation)
    private static final Map<BlockPos, InterpolationState> INTERPOLATION_CACHE = new ConcurrentHashMap<>();

    private static class InterpolationState {
        float renderArmX, renderArmY, renderArmZ;
        long lastUpdateTimeNanos;
        boolean initialized;
    }

    /**
     * Update client-side interpolated position to smoothly move towards server position.
     * Uses real time for frame-rate independent movement.
     * State is persisted in a static cache to survive render state recreation.
     */
    public void updateClientInterpolation() {
        InterpolationState interp = INTERPOLATION_CACHE.computeIfAbsent(quarryPos, k -> new InterpolationState());

        long currentTime = System.nanoTime();

        if (!interp.initialized || interp.lastUpdateTimeNanos == 0) {
            // First time - snap to server position
            interp.renderArmX = serverArmX;
            interp.renderArmY = serverArmY;
            interp.renderArmZ = serverArmZ;
            interp.initialized = true;
            interp.lastUpdateTimeNanos = currentTime;

            renderArmX = interp.renderArmX;
            renderArmY = interp.renderArmY;
            renderArmZ = interp.renderArmZ;
            return;
        }

        // Calculate delta time in seconds
        float deltaSeconds = (currentTime - interp.lastUpdateTimeNanos) / 1_000_000_000f;
        interp.lastUpdateTimeNanos = currentTime;

        // Clamp delta to avoid huge jumps after pauses
        deltaSeconds = Math.min(deltaSeconds, 0.1f);

        float moveDistance = CLIENT_ARM_SPEED_PER_SECOND * deltaSeconds;

        // Smoothly interpolate towards server position
        float dx = serverArmX - interp.renderArmX;
        float dy = serverArmY - interp.renderArmY;
        float dz = serverArmZ - interp.renderArmZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= moveDistance) {
            // Close enough, snap to server position
            interp.renderArmX = serverArmX;
            interp.renderArmY = serverArmY;
            interp.renderArmZ = serverArmZ;
        } else {
            // Move towards server position at constant speed
            float factor = moveDistance / distance;
            interp.renderArmX += dx * factor;
            interp.renderArmY += dy * factor;
            interp.renderArmZ += dz * factor;
        }

        // Copy to render state for use in rendering
        renderArmX = interp.renderArmX;
        renderArmY = interp.renderArmY;
        renderArmZ = interp.renderArmZ;
    }

    /**
     * Clear interpolation cache for a specific quarry (call when quarry is removed).
     */
    public static void clearInterpolationCache(BlockPos pos) {
        INTERPOLATION_CACHE.remove(pos);
    }

    /**
     * Clear all interpolation caches (call on world unload).
     */
    public static void clearAllInterpolationCaches() {
        INTERPOLATION_CACHE.clear();
    }
}
