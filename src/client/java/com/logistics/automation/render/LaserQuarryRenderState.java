package com.logistics.automation.render;

import com.logistics.automation.laserquarry.LaserQuarryConfig;
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

    // LED state - always rendered regardless of arm visibility
    public Direction blockFacing = Direction.NORTH; // For LED orientation
    public float energyLevel = 0f; // 0.0-1.0 for red LED brightness
    public boolean isWorking = false; // True if quarry is actively working (green LED)
    public boolean isFinished = false; // True if quarry has completed mining
    public int quarryLight = 0; // Light level at quarry position for display overlay

    // Top hatch - rendered when pipe connected above
    public boolean hasPipeAbove = false;
    public int aboveLight = 0; // Light level above quarry for top hatch

    // Synced arm speed from server (blocks per tick, scales with energy)
    public float syncedArmSpeed = LaserQuarryConfig.ARM_SPEED;

    // Persistent interpolation state stored per quarry position (survives render state recreation)
    private static final Map<BlockPos, InterpolationState> INTERPOLATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, LedFadeState> LED_FADE_CACHE = new ConcurrentHashMap<>();

    // Green LED fade duration in ticks
    private static final int LED_FADE_TICKS = 12;

    // Calculated green LED brightness (0.0-1.0) after applying fade
    public float greenLedBrightness = 0f;

    private static final class InterpolationState {
        float renderArmX;
        float renderArmY;
        float renderArmZ;
        long lastUpdateTimeNanos;
        boolean initialized;
    }

    private static final class LedFadeState {
        boolean wasWorking;
        long fadeStartTimeNanos;
        boolean isFading;
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
     * Update green LED brightness with fade-out effect.
     * Instant on (0→100%), gradual fade off over 12 ticks.
     */
    public void updateGreenLedBrightness() {
        LedFadeState fade = LED_FADE_CACHE.computeIfAbsent(quarryPos, k -> new LedFadeState());

        long currentTime = System.nanoTime();

        if (isWorking) {
            // Instant on - full brightness
            greenLedBrightness = 1.0f;
            fade.isFading = false;
            fade.wasWorking = true;
        } else if (fade.wasWorking && !fade.isFading) {
            // Just stopped working - start fade
            fade.isFading = true;
            fade.fadeStartTimeNanos = currentTime;
            fade.wasWorking = false;
            greenLedBrightness = 1.0f;
        } else if (fade.isFading) {
            // Currently fading - calculate brightness based on elapsed time
            // MC 1.21.11 always runs at 20 TPS
            // TODO: Use getTickManager() when MC 26.1+ support is added
            float tickRate = 20f;

            float elapsedSeconds = (currentTime - fade.fadeStartTimeNanos) / 1_000_000_000f;
            float elapsedTicks = elapsedSeconds * tickRate;

            if (elapsedTicks >= LED_FADE_TICKS) {
                // Fade complete
                greenLedBrightness = 0f;
                fade.isFading = false;
            } else {
                // Linear fade from 1.0 to 0.0
                greenLedBrightness = 1.0f - (elapsedTicks / LED_FADE_TICKS);
            }
        } else {
            // Not working, not fading - LED is off
            greenLedBrightness = 0f;
        }
    }

    /**
     * Clear interpolation cache for a specific quarry (call when quarry is removed).
     */
    public static void clearInterpolationCache(BlockPos pos) {
        INTERPOLATION_CACHE.remove(pos);
        LED_FADE_CACHE.remove(pos);
    }

    /**
     * Prune cache entries that no longer have a quarry block entity in the current world.
     */
    public static void pruneInterpolationCache(Level world) {
        INTERPOLATION_CACHE.keySet().removeIf(pos -> !(world.getBlockEntity(pos) instanceof LaserQuarryBlockEntity));
        LED_FADE_CACHE.keySet().removeIf(pos -> !(world.getBlockEntity(pos) instanceof LaserQuarryBlockEntity));
    }

    /**
     * Clear all interpolation caches (call on world unload).
     */
    public static void clearAllInterpolationCaches() {
        INTERPOLATION_CACHE.clear();
        LED_FADE_CACHE.clear();
    }
}
