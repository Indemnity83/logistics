package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsConfigHost.Configs;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.pipe.PipeContext;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public class BoostModule implements Module {
    private final float accelerationRate;

    public BoostModule(float accelerationRate) {
        this.accelerationRate = accelerationRate;
    }

    @Override
    public float getAcceleration(PipeContext ctx) {
        return ctx.isPowered() ? this.accelerationRate : 0f;
    }

    @Override
    public float getMaxSpeed(PipeContext ctx) {
        return LogisticsConfigHost.get(Configs.PIPE_MAX_SPEED) * 4.0f;
    }

    @Override
    public @Nullable ResourceId getCoreModel(PipeContext ctx) {
        if (ctx.isPowered()) {
            return LogisticsPipe.model("gold_transport_pipe_core_powered");
        }
        return null;
    }

    @Override
    public @Nullable ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        if (ctx.isPowered()) {
            String suffix = ctx.isInventoryConnection(direction) ? "_arm_extended_powered" : "_arm_powered";
            return LogisticsPipe.model("gold_transport_pipe" + suffix);
        }
        return null;
    }
}
