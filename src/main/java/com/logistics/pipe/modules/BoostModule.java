package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.PipeConfig;

public class BoostModule implements Module {
    private final float accelerationRate;
    private final float maxSpeed;

    public BoostModule(float accelerationRate) {
        this.accelerationRate = accelerationRate;
        this.maxSpeed = PipeConfig.PIPE_MAX_SPEED * 4.0f;
    }

    @Override
    public float getAcceleration(PipeContext ctx) {
        return ctx.isPowered() ? this.accelerationRate : 0f;
    }

    @Override
    public float getMaxSpeed(PipeContext ctx) {
        return maxSpeed;
    }
}
