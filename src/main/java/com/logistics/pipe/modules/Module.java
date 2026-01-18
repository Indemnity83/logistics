package com.logistics.pipe.modules;

import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.runtime.RoutePlan;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public interface Module {
    default void onTick(PipeContext ctx) {}

    default float getAcceleration(PipeContext ctx) {
        return 0f;
    }

    default float getDrag(PipeContext ctx) {
        return PipeConfig.DRAG_COEFFICIENT;
    }

    default float getMaxSpeed(PipeContext ctx) {
        return PipeConfig.PIPE_MAX_SPEED;
    }

    default RoutePlan route(PipeContext ctx, com.logistics.pipe.runtime.TravelingItem item, List<Direction> options) {
        return RoutePlan.pass();
    }

    default boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
        return true;
    }

    default void onConnectionsChanged(PipeContext ctx, List<Direction> options) {}

    default ActionResult onUseWithItem(PipeContext ctx, ItemUsageContext usage) {
        return ActionResult.PASS;
    }

    default int comparatorOutput(PipeContext ctx) {
        return 0;
    }

    default boolean hasComparatorOutput() {
        return false;
    }

    /**
     * Return false to prevent this pipe from connecting in the given direction.
     */
    default boolean allowsConnection(
            @Nullable PipeContext ctx, Direction direction, Pipe selfPipe, Block neighborBlock) {
        return true;
    }

    /**
     * Called randomly on the client for display effects like particles.
     * Modules can override this to add visual effects.
     */
    default void randomDisplayTick(PipeContext ctx, Random random) {}

    /**
     * Get the NBT state key for this module.
     * Defaults to lowercase class simple name (e.g., "mergermodule").
     * Override to provide custom key for backwards compatibility.
     */
    default String getStateKey() {
        return this.getClass().getSimpleName().toLowerCase();
    }

    /**
     * Override the base arm model for a specific direction.
     *
     * @param ctx the pipe context
     * @param direction the direction of the arm being rendered
     * @return the arm model identifier, or null to use the default arm model
     */
    @Nullable default Identifier getPipeArm(PipeContext ctx, Direction direction) {
        return null;
    }

    /**
     * Override the tint color for the arm model in a specific direction.
     * Used for models with tintindex to apply directional coloring.
     *
     * @param ctx the pipe context
     * @param direction the direction of the arm being rendered
     * @return the tint color (0xRRGGBB), or null to use no tint (white)
     */
    @Nullable default Integer getArmTint(PipeContext ctx, Direction direction) {
        return null;
    }

    /**
     * Append decoration models for a specific direction (feature faces, overlays, extensions, etc.).
     *
     * @param ctx the pipe context
     * @param direction the direction of the arm being rendered
     * @return decoration model identifiers to render for this arm direction
     */
    default List<Identifier> getPipeDecorations(PipeContext ctx, Direction direction) {
        return List.of();
    }

    /**
     * Append decoration models for the pipe core.
     *
     * @param ctx the pipe context
     * @return decoration model infos to render for the pipe core
     */
    default List<Pipe.CoreDecoration> getCoreDecorations(PipeContext ctx) {
        return List.of();
    }
}
