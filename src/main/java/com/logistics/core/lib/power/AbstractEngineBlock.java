package com.logistics.core.lib.power;

import static com.logistics.core.lib.power.AbstractEngineBlockEntity.STAGE;

import com.logistics.core.lib.block.Probeable;
import com.logistics.core.lib.block.Wrenchable;
import com.logistics.core.lib.power.AbstractEngineBlockEntity.HeatStage;
import com.logistics.core.lib.support.ProbeResult;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Abstract base class for all engine blocks.
 * Provides common functionality for FACING, POWERED, STAGE properties and redstone handling.
 *
 * @param <E> The type of engine block entity this block creates
 */
public abstract class AbstractEngineBlock<E extends AbstractEngineBlockEntity> extends BaseEntityBlock
        implements Probeable, Wrenchable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected AbstractEngineBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(STAGE, HeatStage.COLD));
    }

    /**
     * Returns additional properties that subclasses want to add to the block state.
     * Base implementation returns empty list. Subclasses override to add properties like LIT.
     */
    protected List<Property<?>> getAdditionalProperties() {
        return Collections.emptyList();
    }

    /**
     * Applies additional placement state that subclasses want to add.
     * Base implementation returns the state unchanged.
     *
     * @param base the base placement state with FACING, POWERED, STAGE already set
     * @param ctx the placement context
     * @return the state with additional properties applied
     */
    protected BlockState applyAdditionalPlacementState(BlockState base, BlockPlaceContext ctx) {
        return base;
    }

    /**
     * Type-safe getter for the engine block entity.
     * Subclasses implement this to cast to their specific block entity type.
     */
    protected abstract E getEngineBlockEntity(BlockEntity be);

    /**
     * Handles special wrench behavior before the default rotation.
     * Return true to skip the default rotation behavior.
     * Base implementation returns false (always perform rotation).
     */
    protected boolean handleSpecialWrench(Level world, BlockPos pos, Player player, BlockState state) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, STAGE);
        for (Property<?> property : getAdditionalProperties()) {
            builder.add(property);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Output faces an adjacent EnergyStorage if available, otherwise player look direction
        Direction defaultFacing = ctx.getHorizontalDirection();
        Direction facing = findBestOutputDirection(ctx.getLevel(), ctx.getClickedPos(), defaultFacing);
        boolean powered = hasDirectRedstonePower(ctx.getLevel(), ctx.getClickedPos());

        BlockState base =
                defaultBlockState().setValue(FACING, facing).setValue(POWERED, powered).setValue(STAGE, HeatStage.COLD);

        return applyAdditionalPlacementState(base, ctx);
    }

    @Override
    public ProbeResult onProbe(Level world, BlockPos pos, Player player) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be != null) {
            E engine = getEngineBlockEntity(be);
            if (engine != null) {
                return engine.getProbeResult();
            }
        }
        return null;
    }

    @Override
    public InteractionResult onWrench(Level world, BlockPos pos, Player player) {
        BlockState state = world.getBlockState(pos);

        // Let subclasses handle special wrench behavior first
        if (handleSpecialWrench(world, pos, player, state)) {
            return InteractionResult.SUCCESS;
        }

        // Default behavior: snap to next EnergyStorage or rotate through all directions
        if (!world.isClientSide()) {
            Direction currentFacing = state.getValue(FACING);
            Direction newFacing = findNextOutputDirection(world, pos, currentFacing);
            world.setBlock(pos, state.setValue(FACING, newFacing), Block.UPDATE_ALL);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level world,
            BlockPos pos,
            Block block,
            @Nullable Orientation wireOrientation,
            boolean notify) {
        if (!world.isClientSide()) {
            boolean powered = hasDirectRedstonePower(world, pos);
            if (powered != state.getValue(POWERED)) {
                world.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            }
        }
        super.neighborChanged(state, world, pos, block, wireOrientation, notify);
    }

    /**
     * Check if the block has direct redstone power (from levers, buttons, etc.)
     * but not from redstone dust passing by.
     */
    private boolean hasDirectRedstonePower(Level world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            // Get emitted redstone power directly from neighbors
            // This excludes weak power from dust and only counts strong power sources
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            int power = neighborState.getSignal(world, neighborPos, direction);
            if (power > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * Gets the direction this engine outputs energy to.
     */
    public static Direction getOutputDirection(BlockState state) {
        return state.getValue(FACING);
    }

    /**
     * Finds the best direction for an engine to face based on adjacent EnergyStorage blocks.
     * Prefers the given default direction if it points to an EnergyStorage, otherwise returns
     * the first adjacent direction with an EnergyStorage, or the default if none found.
     *
     * @param world the world
     * @param pos the engine's position
     * @param defaultDirection the preferred direction (usually player look direction)
     * @return the direction to face
     */
    public static Direction findBestOutputDirection(Level world, BlockPos pos, Direction defaultDirection) {
        // First check if the default direction has an EnergyStorage
        if (hasEnergyStorage(world, pos, defaultDirection)) {
            return defaultDirection;
        }

        // Check all other directions
        for (Direction dir : Direction.values()) {
            if (dir != defaultDirection && hasEnergyStorage(world, pos, dir)) {
                return dir;
            }
        }

        // No EnergyStorage found, use default
        return defaultDirection;
    }

    /**
     * Finds the next direction with an EnergyStorage when rotating from the current direction.
     * If no EnergyStorage is found in any direction, cycles to the next sequential direction.
     *
     * @param world the world
     * @param pos the engine's position
     * @param current the current facing direction
     * @return the next direction to face
     */
    public static Direction findNextOutputDirection(Level world, BlockPos pos, Direction current) {
        // Try to find an EnergyStorage in the remaining directions (cycling from current)
        Direction[] directions = Direction.values();
        int startIdx = (current.ordinal() + 1) % directions.length;

        // First pass: look for EnergyStorage
        for (int i = 0; i < directions.length; i++) {
            Direction dir = directions[(startIdx + i) % directions.length];
            if (hasEnergyStorage(world, pos, dir)) {
                return dir;
            }
        }

        // No EnergyStorage found, just cycle to next direction
        return directions[startIdx];
    }

    /**
     * Checks if there's a block with EnergyStorage capability in the given direction.
     *
     * @param world the world
     * @param pos the engine's position
     * @param direction the direction to check
     * @return true if an EnergyStorage exists in that direction
     */
    private static boolean hasEnergyStorage(Level world, BlockPos pos, Direction direction) {
        BlockPos targetPos = pos.relative(direction);
        EnergyStorage storage = EnergyStorage.SIDED.find(world, targetPos, direction.getOpposite());
        return storage != null && storage.supportsInsertion();
    }
}
