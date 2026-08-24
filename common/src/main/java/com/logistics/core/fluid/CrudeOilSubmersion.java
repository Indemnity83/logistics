package com.logistics.core.fluid;

import com.logistics.LogisticsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Detects whether an entity is submerged in Crude Oil — the mod's one world-placeable custom fluid
 * (see {@link LogisticsCore#CUSTOM_FLUIDS}). Pure vanilla API, no loader imports, so it's usable from
 * both server tick logic and client-side rendering.
 */
public final class CrudeOilSubmersion {
    private static final String NAME = "crude_oil";

    private static Fluid source;
    private static Fluid flowing;
    private static boolean resolved;

    private CrudeOilSubmersion() {}

    /**
     * {@code true} when the entity's eye position is inside Crude Oil (source or flowing). Drives the
     * screen-darkening overlay and Nausea/Poison — matches the issue's "camera submerged" wording.
     */
    public static boolean isEyeInCrudeOil(Entity entity) {
        return isCrudeOilAt(entity.level(), BlockPos.containing(entity.getEyePosition(1f)));
    }

    /**
     * {@code true} when either the entity's eye or feet position is inside Crude Oil. Broader than
     * {@link #isEyeInCrudeOil}, matching the issue's "submerged or swimming in it" wording — drives
     * movement damping.
     */
    public static boolean isBodyInCrudeOil(Entity entity) {
        Level level = entity.level();
        return isCrudeOilAt(level, BlockPos.containing(entity.getEyePosition(1f)))
                || isCrudeOilAt(level, entity.blockPosition());
    }

    private static boolean isCrudeOilAt(Level level, BlockPos pos) {
        resolveFluids();
        if (source == Fluids.EMPTY) {
            return false; // not registered yet (or registration failed) — never treat as crude oil
        }
        Fluid fluid = level.getFluidState(pos).getType();
        return fluid == source || fluid == flowing;
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
