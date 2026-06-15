package com.logistics.core.lib.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-agnostic entry point for filling/draining a fluid container item (bucket, etc.) against a
 * block's exposed fluid storage. Each loader registers an implementation that delegates to its built-in
 * helper (Fabric {@code FluidStorageUtil}, NeoForge {@code FluidUtil}), which operates on the registered
 * fluid capability — so the interaction acts on whatever the block exposes from {@code fluidStorage}.
 */
public final class FluidContainerInteraction {

    @FunctionalInterface
    public interface Interaction {
        InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand, @Nullable Direction side);
    }

    private static volatile Interaction impl;

    private FluidContainerInteraction() {}

    /** Register the platform-specific interaction. Called once during loader initialization. */
    public static void register(Interaction interaction) {
        impl = Objects.requireNonNull(interaction, "interaction");
    }

    /** Try to fill/drain the player's held container item against the block at {@code pos}. */
    public static InteractionResult interact(
            Level level, BlockPos pos, Player player, InteractionHand hand, @Nullable Direction side) {
        Interaction current = impl;
        return current != null ? current.interact(level, pos, player, hand, side) : InteractionResult.PASS;
    }
}
