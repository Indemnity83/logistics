package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public interface Module {
    default void onTick(PipeContext ctx) {}

    default float getAcceleration(PipeContext ctx) {
        return 0f;
    }

    default float getDrag(PipeContext ctx) {
        return LogisticsPipe.CONFIG.DRAG_COEFFICIENT;
    }

    default float getMaxSpeed(PipeContext ctx) {
        return LogisticsPipe.CONFIG.PIPE_MAX_SPEED;
    }

    default RoutePlan route(PipeContext ctx, com.logistics.pipe.runtime.TravelingItem item, List<Direction> options) {
        return RoutePlan.pass();
    }

    default boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
        return true;
    }

    default void onConnectionsChanged(PipeContext ctx, List<Direction> options) {}

    default InteractionResult onUseWithItem(PipeContext ctx, UseOnContext usage) {
        return InteractionResult.PASS;
    }

    default InteractionResult onUseWithoutItem(PipeContext ctx, UseOnContext usage) {
        return InteractionResult.PASS;
    }

    default InteractionResult onWrench(PipeContext ctx, Player player) {
        return InteractionResult.PASS;
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
    default void randomDisplayTick(PipeContext ctx, RandomSource random) {}

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

    /**
     * Override the core model for this pipe.
     * Used for state-dependent core variants (e.g., powered gold pipe).
     *
     * @param ctx the pipe context
     * @return the core model identifier, or null to use the default core model
     */
    @Nullable default Identifier getCoreModel(PipeContext ctx) {
        return null;
    }

    /**
     * Return true if this module needs random ticks.
     * Modules that override randomTick() should also override this to return true.
     */
    default boolean hasRandomTicks() {
        return false;
    }

    default void randomTick(PipeContext ctx, RandomSource random) {}

    /**
     * Add components to the item stack when the block is broken.
     * Called for each module to allow adding custom components to dropped items.
     *
     * @param builder the component map builder
     * @param ctx the pipe context
     */
    default void addItemComponents(DataComponentMap.Builder builder, PipeContext ctx) {}

    /**
     * Read components from the item stack when the block is placed.
     * Called for each module to allow reading custom components from placed items.
     *
     * @param components the components from the item
     * @param ctx the pipe context
     */
    default void readItemComponents(DataComponentGetter components, PipeContext ctx) {}

    /**
     * Get custom model data strings for item model selection.
     * These strings are used with the minecraft:select model type.
     *
     * @param ctx the pipe context
     * @return list of model data strings (empty list for none)
     */
    default List<String> getCustomModelDataStrings(PipeContext ctx) {
        return List.of();
    }

    /**
     * Get the translation key suffix for the item name based on module state.
     * For example, ".exposed" or ".waxed.oxidized" for weathering states.
     *
     * @param ctx the pipe context
     * @return the translation key suffix, or empty string for default name
     */
    default String getItemNameSuffix(PipeContext ctx) {
        return "";
    }

    /**
     * Get the translation key suffix for the item name from item components.
     * Used for item display names when we don't have a block context.
     * For example, ".exposed" or ".waxed.oxidized" for weathering states.
     *
     * @param components the item components
     * @return the translation key suffix, or empty string for default name
     */
    default String getItemNameSuffixFromComponents(DataComponentGetter components) {
        return "";
    }

    /**
     * Append additional item stack variants for the creative menu.
     * Called for each module to allow adding variants with different component states.
     * The base stack (default state) is already included.
     *
     * @param stacks the list to append variants to
     * @param baseStack the base item stack to copy and modify
     */
    default void appendCreativeMenuVariants(List<ItemStack> stacks, ItemStack baseStack) {}

    /**
     * Whether this module accepts low-tier energy from the given direction.
     * Only relevant for pipes with energy storage (created via .withEnergy()).
     * Modules can access energy storage directly via ctx.blockEntity().energyStorage.
     *
     * @param ctx the pipe context
     * @param from direction energy is coming from
     * @return true if low-tier energy is accepted from this direction
     */
    default boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        return false; // Default: don't accept (only modules that need energy should override)
    }
}
