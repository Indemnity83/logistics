package com.logistics.power.render;

import com.logistics.core.lib.power.AbstractEngineBlockEntity.HeatStage;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Render state for engine block entities.
 * Holds all the data needed to render an engine with animation.
 *
 * <p>Animation is computed client-side based on the synced block state stage.
 * This avoids depending on infrequent network updates for smooth animation.
 */
public class EngineRenderState extends BlockEntityRenderState {
    public BlockPos pos = BlockPos.ZERO;
    public Direction facing = Direction.UP;
    public HeatStage stage = HeatStage.COLD;
    public boolean isRunning = false;
    public float pistonSpeed = 0.0f;
    public boolean canOverheat = true;

    // Engine type for texture selection
    public EngineType engineType = EngineType.REDSTONE;

    // Animation progress (set from renderer's persistent cache)
    private float animationProgress = 0f;

    public enum EngineType {
        REDSTONE,
        STIRLING,
        CREATIVE
    }

    /**
     * Sets the animation progress from the renderer's persistent cache.
     */
    public void setAnimationProgress(float progress) {
        this.animationProgress = Math.max(0f, Math.min(1f, progress));
    }

    /**
     * Gets the progress value to use for rendering (0-1).
     */
    public float getRenderProgress() {
        return animationProgress;
    }

    /**
     * Calculates the piston offset based on animation progress.
     * Returns a value from 0 (fully retracted) to ~8 pixels (fully extended).
     */
    public float getPistonOffset() {
        float p = animationProgress;
        // Piston extends from 0-0.5, retracts from 0.5-1
        if (p > 0.5f) {
            p = 1.0f - p;
        }
        // Scale to max extension of 0.5 blocks (8 pixels)
        return p;
    }
}
