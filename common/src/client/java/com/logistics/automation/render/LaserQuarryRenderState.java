package com.logistics.automation.render;

import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Render state for the laser quarry arm visualization.
 */
public class LaserQuarryRenderState extends BlockEntityRenderState {
    public boolean shouldRenderArm = false;
    public boolean shouldRenderPreviewOutline = false;
    public BlockPos quarryPos = BlockPos.ZERO;
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
    public LaserQuarryBlockEntity.Phase phase = LaserQuarryBlockEntity.Phase.CLEARING;
    public LaserQuarryBlockEntity.ArmState armState = LaserQuarryBlockEntity.ArmState.MOVING;

    // Light level sampled at the frame top (where horizontal beams are)
    public int frameTopLight;

    // Synced arm speed from server (blocks per tick, scales with energy)
    public float syncedArmSpeed = 0.0f;

    // Persistent interpolation state stored per quarry position (survives render state recreation)
    private static final Map<BlockPos, InterpolationState> INTERPOLATION_CACHE = new ConcurrentHashMap<>();

    private static final class InterpolationState {
        float renderArmX;
        float renderArmY;
        float renderArmZ;
        long lastUpdateTimeNanos;
        boolean initialized;
    }

    /**
     * Update client-side interpolated position to smoothly move towards server position.
     * Uses real time scaled by current tick rate for frame-rate independent movement
     * that respects game speed changes (e.g., /tick rate command).
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

        // Get current tick rate (MC 1.21.11 always runs at 20 TPS)
        // TODO: Use getTickManager() when MC 26.1+ support is added
        float tickRate = 20f;

        // Speed in blocks per second = synced speed per tick * ticks per second
        float speedPerSecond = syncedArmSpeed * tickRate;
        float moveDistance = speedPerSecond * deltaSeconds;

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
     * Prune cache entries that no longer have a quarry block entity in the current world.
     */
    public static void pruneInterpolationCache(Level world) {
        INTERPOLATION_CACHE.keySet().removeIf(pos -> !(world.getBlockEntity(pos) instanceof LaserQuarryBlockEntity));
    }

    /**
     * Clear all interpolation caches (call on world unload).
     */
    public static void clearAllInterpolationCaches() {
        INTERPOLATION_CACHE.clear();
    }
}
