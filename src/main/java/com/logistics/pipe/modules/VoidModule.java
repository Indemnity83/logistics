package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import java.util.List;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

public class VoidModule implements Module {
    @Override
    public RoutePlan route(
            PipeContext ctx,
            com.logistics.pipe.runtime.TravelingItem item,
            List<net.minecraft.util.math.Direction> options) {
        return RoutePlan.discard();
    }

    @Override
    public boolean discardWhenNoRoute(PipeContext ctx) {
        return true;
    }

    @Override
    public void randomDisplayTick(PipeContext ctx, Random random) {
        // Only show particles occasionally (5% of the time)
        if (random.nextInt(20) != 0) {
            return;
        }

        // Spawn portal particles near the center of the pipe
        double x = ctx.pos().getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double y = ctx.pos().getY() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double z = ctx.pos().getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;

        ctx.world().addParticleClient(ParticleTypes.PORTAL, x, y, z, 0.0, 0.02, 0.0);
    }
}
