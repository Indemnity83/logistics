package com.logistics.core.fluid;

import com.logistics.LogisticsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/** Detects whether an entity is submerged in Crude Oil, the mod's one world-placeable custom fluid. */
public final class CrudeOilSubmersion {
    private static final String NAME = "crude_oil";

    private static Fluid source;
    private static Fluid flowing;
    private static boolean resolved;

    private CrudeOilSubmersion() {}

    /** {@code true} when the entity's eye is below the actual surface of the Crude Oil there. */
    public static boolean isEyeInCrudeOil(Entity entity) {
        Vec3 eyePos = entity.getEyePosition(1f);
        return isCameraSubmerged(entity.level(), BlockPos.containing(eyePos), eyePos.y);
    }

    /** {@code true} when {@code eyeY} is below the actual surface of the Crude Oil at {@code pos}. */
    public static boolean isCameraSubmerged(Level level, BlockPos pos, double eyeY) {
        resolveFluids();
        if (source == Fluids.EMPTY) {
            return false;
        }
        FluidState state = level.getFluidState(pos);
        Fluid fluid = state.getType();
        if (fluid != source && fluid != flowing) {
            return false;
        }
        return eyeY < (double) pos.getY() + state.getHeight(level, pos);
    }

    /** Resolves the registered source/flowing fluid instances once, mirroring {@code LogisticsCore.buildFluidLuminance}. */
    private static void resolveFluids() {
        if (resolved) {
            return;
        }
        source = BuiltInRegistries.FLUID.get(LogisticsCore.resource(NAME).toIdentifier());
        flowing = BuiltInRegistries.FLUID.get(LogisticsCore.resource("flowing_" + NAME).toIdentifier());
        resolved = source != Fluids.EMPTY;
    }
}
