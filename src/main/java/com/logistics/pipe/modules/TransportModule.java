package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;

public class TransportModule implements Module {
    private final float maxSpeed;
    private final float dragCoefficient;

    public TransportModule(float maxSpeed, float dragCoefficient) {
        this.maxSpeed = maxSpeed;
        this.dragCoefficient = dragCoefficient;
    }

    @Override
    public float getMaxSpeed(PipeContext ctx) {
        return maxSpeed;
    }

    @Override
    public float getDrag(PipeContext ctx) {
        return dragCoefficient;
    }
}
