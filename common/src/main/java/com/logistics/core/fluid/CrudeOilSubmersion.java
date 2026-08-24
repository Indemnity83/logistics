package com.logistics.core.fluid;

import com.logistics.LogisticsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** Detects whether an entity is submerged in Crude Oil, the mod's one world-placeable custom fluid. */
public final class CrudeOilSubmersion {
    private static final String NAME = "crude_oil";

    private static Fluid source;
    private static Fluid flowing;
    private static boolean resolved;

    private CrudeOilSubmersion() {}

    /** {@code true} when the entity's eye position is inside Crude Oil (source or flowing). */
    public static boolean isEyeInCrudeOil(Entity entity) {
        return isCrudeOilAt(entity.level(), BlockPos.containing(entity.getEyePosition(1f)));
    }

    private static boolean isCrudeOilAt(Level level, BlockPos pos) {
        resolveFluids();
        if (source == Fluids.EMPTY) {
            return false; // not registered yet (or registration failed) — never treat as crude oil
        }
        Fluid fluid = level.getFluidState(pos).getType();
        return fluid == source || fluid == flowing;
    }

    /**
     * {@code true} when {@code eyeY} is below the actual surface of the Crude Oil at {@code pos} — the
     * same "camera is really under the liquid's top, not just standing over a shallow flow" check
     * vanilla and NeoForge use to gate fog/overlay hooks ({@code Camera#getFluidInCamera},
     * {@code ClientHooks#getFogColor}). Used for fog manipulation, where a block-level check alone
     * would trigger a frame too early/late at the surface.
     */
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
        source = BuiltInRegistries.FLUID.getValue(LogisticsCore.resource(NAME).toIdentifier());
        flowing = BuiltInRegistries.FLUID.getValue(LogisticsCore.resource("flowing_" + NAME).toIdentifier());
        resolved = source != Fluids.EMPTY;
    }
}
