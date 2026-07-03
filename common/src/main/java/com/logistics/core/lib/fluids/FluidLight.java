package com.logistics.core.lib.fluids;

import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;

/** Drives a block's light-level property from the fluid it holds — shared by the fluid pipe and glass tank. */
public final class FluidLight {

    private FluidLight() {}

    /**
     * Set {@code property} to the held fluid's luminance while the block contains fluid, else 0 — writing to
     * the level only when the value changes. No-op if the block lacks {@code property}. {@code luminance}
     * resolves a fluid's block-light level, injected so this stays independent of the mod's fluid table.
     */
    public static void update(
            Level level,
            BlockPos pos,
            BlockState state,
            IntegerProperty property,
            Fluid fluid,
            long amount,
            ToIntFunction<Fluid> luminance) {
        if (!state.hasProperty(property)) {
            return;
        }
        int light = amount > 0 ? luminance.applyAsInt(fluid) : 0;
        if (state.getValue(property) != light) {
            level.setBlock(pos, state.setValue(property, light), Block.UPDATE_CLIENTS);
        }
    }
}
