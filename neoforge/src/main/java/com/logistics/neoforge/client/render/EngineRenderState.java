package com.logistics.neoforge.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class EngineRenderState extends BlockEntityRenderState {
    public BlockPos pos = BlockPos.ZERO;
    public Direction facing = Direction.UP;
    public boolean isRunning = false;
    public float pistonSpeed = 0.0f;
    public EngineType engineType = EngineType.REDSTONE;
    private float animationProgress = 0f;

    public enum EngineType {
        REDSTONE,
        STIRLING,
        MAGMATIC,
        STEAM,
        FUEL,
        CREATIVE
    }

    public void setAnimationProgress(float progress) {
        this.animationProgress = Math.max(0f, Math.min(1f, progress));
    }

    public float getPistonOffset() {
        float p = animationProgress;
        if (p > 0.5f) {
            p = 1.0f - p;
        }
        return p;
    }
}
