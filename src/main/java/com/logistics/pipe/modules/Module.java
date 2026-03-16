package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public interface Module {
    /**
     * @deprecated Implement {@link TickingModule} instead. The pipe dispatcher only calls
     *     {@code onTick} on modules that implement {@link TickingModule}.
     */
    @Deprecated
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

    /**
     * @deprecated Implement {@link RoutingModule} instead. The pipe dispatcher only calls
     *     {@code route} on modules that implement {@link RoutingModule}.
     */
    @Deprecated
    default RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        return RoutePlan.pass();
    }

    /**
     * Called when a TravelingItem is about to exit this pipe into an adjacent non-pipe storage
     * (at SERVER_EXIT_THRESHOLD), before the default generic {@code storage.insert()} runs.
     *
     * <p>Return {@code null} if the module fully handled the transfer (item consumed). PipeRuntime
     * will not perform any further insertion. The module is responsible for calling
     * {@code network.confirmDelivery()} if needed.
     *
     * <p>Return the item (or a modified copy with a reduced count) if the module did not handle
     * it — PipeRuntime will perform the default generic insertion on whatever is returned.
     *
     * @param ctx       the pipe context
     * @param item      the traveling item being transferred
     * @param direction the direction the item is exiting (toward the storage)
     * @return null if fully handled, or the item (possibly with reduced count) to fall through
     */
    @Nullable
    default TravelingItem onTransferToStorage(PipeContext ctx, TravelingItem item, Direction direction) {
        return item;
    }

    default boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
        return true;
    }

    /**
     * Return true to allow item insertion from a non-pipe neighbor in the given direction.
     * By default, Pipe.canAcceptFrom blocks all non-pipe insertion.
     * Override this to allow specific non-pipe neighbors (e.g., autocrafters) to push items in.
     *
     * @param ctx the pipe context
     * @param from direction items are coming from
     * @return true if non-pipe insertion from this direction should be allowed
     */
    default boolean canAcceptFromNonPipe(PipeContext ctx, Direction from) {
        return false;
    }

    /**
     * Called when an item is inserted into the pipe from an external (non-pipe) source via the
     * Fabric Transfer API, before the default single TravelingItem is created.
     *
     * <p>Return {@code true} if the module fully handled the insertion (e.g., split the stack into
     * multiple TravelingItems). Return {@code false} to proceed with the default behavior.
     *
     * @param ctx           the pipe context
     * @param stack         the item stack being inserted
     * @param fromDirection the direction the item arrived from
     * @return true if the module handled the insertion, false to use default behaviour
     */
    default boolean onExternalInsert(PipeContext ctx, ItemStack stack, Direction fromDirection) {
        return false;
    }

    default void onConnectionsChanged(PipeContext ctx, List<Direction> options) {}

    /**
     * Called when this module is removed from a chassis slot (or the pipe is broken).
     * Modules that register network resources should clean them up here.
     */
    default void onDetach(PipeContext ctx) {}

    default InteractionResult onUseWithItem(PipeContext ctx, UseOnContext usage) {
        return InteractionResult.PASS;
    }

    default InteractionResult onUseWithoutItem(PipeContext ctx, UseOnContext usage) {
        return InteractionResult.PASS;
    }

    default InteractionResult onWrench(PipeContext ctx, Player player) {
        return InteractionResult.PASS;
    }

    default InteractionResult openItemConfig(ServerPlayer player, InteractionHand hand, ItemStack stack) {
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
     *
     * <p><strong>WARNING — serialization contract:</strong> this key is persisted in world NBT.
     * Changing the return value (including by renaming the class) will silently discard all
     * existing module state in saved worlds. If you rename a module class, override this method
     * to keep returning the original stable string:
     * <pre>{@code
     *   @Override
     *   public String getStateKey() {
     *       return "oldmodulename"; // DO NOT RENAME — changing this corrupts existing worlds
     *   }
     * }</pre>
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
    @Nullable default ResourceId getPipeArm(PipeContext ctx, Direction direction) {
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
    default List<ResourceId> getPipeDecorations(PipeContext ctx, Direction direction) {
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
    @Nullable default ResourceId getCoreModel(PipeContext ctx) {
        return null;
    }

    /**
     * @deprecated Implement {@link RandomTickModule} instead. {@code hasRandomTicks()} is no
     *     longer consulted by the pipe dispatcher — implement {@link RandomTickModule} to opt in.
     */
    @Deprecated
    default boolean hasRandomTicks() {
        return false;
    }

    /**
     * @deprecated Implement {@link RandomTickModule} instead. The pipe dispatcher only calls
     *     {@code randomTick} on modules that implement {@link RandomTickModule}.
     */
    @Deprecated
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
     * @deprecated Implement {@link DispatchableModule} instead. The pipe dispatcher only calls
     *     {@code onDispatch} on modules that implement {@link DispatchableModule}.
     */
    @Deprecated
    default long onDispatch(PipeContext ctx, BlockPos requester, ItemVariant item, long amount, UUID deliveryId) {
        return 0;
    }

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
