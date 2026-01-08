package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;

public class BoostModule implements Module {
    private final float accelerationRate;

    public BoostModule(float accelerationRate) {
        this.accelerationRate = accelerationRate;
    }

    @Override
    public boolean applyAcceleration(PipeContext ctx) {
        return ctx.isPowered();
    }

    @Override
    public float getAcceleration(PipeContext ctx) {
        return this.accelerationRate;
    }
}
