package com.logistics.pipe.modules;

import com.logistics.LogisticsMod;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.PipeConfig;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public @Nullable Identifier getCoreModel(PipeContext ctx) {
        if (ctx.isPowered()) {
            return Identifier.of(LogisticsMod.MOD_ID, "block/pipe/gold_transport_pipe_core_powered");
        }
        return null;
    }

    @Override
    public @Nullable Identifier getPipeArm(PipeContext ctx, Direction direction) {
        if (ctx.isPowered()) {
            String suffix = ctx.isInventoryConnection(direction) ? "_arm_extended_powered" : "_arm_powered";
            return Identifier.of(LogisticsMod.MOD_ID, "block/pipe/gold_transport_pipe" + suffix);
        }
        return null;
    }
}
