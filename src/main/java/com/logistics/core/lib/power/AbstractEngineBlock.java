package com.logistics.core.lib.power;

import static com.logistics.core.lib.power.AbstractEngineBlockEntity.STAGE;

import com.logistics.core.lib.block.Probeable;
import com.logistics.core.lib.block.Wrenchable;
import com.logistics.core.lib.power.AbstractEngineBlockEntity.HeatStage;
import com.logistics.core.lib.support.ProbeResult;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Abstract base class for all engine blocks.
 * Provides common functionality for FACING, POWERED, STAGE properties and redstone handling.
 *
 * @param <E> The type of engine block entity this block creates
 */
public abstract class AbstractEngineBlock<E extends AbstractEngineBlockEntity> extends BlockWithEntity
        implements Probeable, Wrenchable {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    protected AbstractEngineBlock(Settings settings, BlockSoundGroup soundGroup) {
        super(settings.strength(3.5f)
                .sounds(soundGroup)
                .nonOpaque()
                .solidBlock((state, world, pos) -> false)
                .suffocates((state, world, pos) -> false)
                .blockVision((state, world, pos) -> false));
        setDefaultState(getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false)
                .with(STAGE, HeatStage.COLD));
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
    protected BlockState applyAdditionalPlacementState(BlockState base, ItemPlacementContext ctx) {
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
    protected boolean handleSpecialWrench(World world, BlockPos pos, PlayerEntity player, BlockState state) {
        return false;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, STAGE);
        for (Property<?> property : getAdditionalProperties()) {
            builder.add(property);
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Output faces an adjacent EnergyStorage if available, otherwise player look direction
        Direction defaultFacing = ctx.getPlayerLookDirection();
        Direction facing = findBestOutputDirection(ctx.getWorld(), ctx.getBlockPos(), defaultFacing);
        boolean powered = hasDirectRedstonePower(ctx.getWorld(), ctx.getBlockPos());

        BlockState base =
                getDefaultState().with(FACING, facing).with(POWERED, powered).with(STAGE, HeatStage.COLD);

        return applyAdditionalPlacementState(base, ctx);
    }

    @Override
    public ProbeResult onProbe(World world, BlockPos pos, PlayerEntity player) {
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
    public ActionResult onWrench(World world, BlockPos pos, PlayerEntity player) {
        BlockState state = world.getBlockState(pos);

        // Let subclasses handle special wrench behavior first
        if (handleSpecialWrench(world, pos, player, state)) {
            return ActionResult.SUCCESS;
        }

        // Default behavior: snap to next EnergyStorage or rotate through all directions
        if (!world.isClient()) {
            Direction currentFacing = state.get(FACING);
            Direction newFacing = findNextOutputDirection(world, pos, currentFacing);
            world.setBlockState(pos, state.with(FACING, newFacing), Block.NOTIFY_ALL);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    protected void neighborUpdate(
            BlockState state,
            World world,
            BlockPos pos,
            Block block,
            @Nullable WireOrientation wireOrientation,
            boolean notify) {
        if (!world.isClient()) {
            boolean powered = hasDirectRedstonePower(world, pos);
            if (powered != state.get(POWERED)) {
                world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_LISTENERS);
            }
        }
        super.neighborUpdate(state, world, pos, block, wireOrientation, notify);
    }

    /**
     * Check if the block has direct redstone power (from levers, buttons, etc.)
     * but not from redstone dust passing by.
     */
    private boolean hasDirectRedstonePower(World world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            // Get emitted redstone power directly from neighbors
            // This excludes weak power from dust and only counts strong power sources
            int power = world.getEmittedRedstonePower(pos.offset(direction), direction);
            if (power > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    /**
     * Gets the direction this engine outputs energy to.
     */
    public static Direction getOutputDirection(BlockState state) {
        return state.get(FACING);
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
    public static Direction findBestOutputDirection(World world, BlockPos pos, Direction defaultDirection) {
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
    public static Direction findNextOutputDirection(World world, BlockPos pos, Direction current) {
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
    private static boolean hasEnergyStorage(World world, BlockPos pos, Direction direction) {
        BlockPos targetPos = pos.offset(direction);
        EnergyStorage storage = EnergyStorage.SIDED.find(world, targetPos, direction.getOpposite());
        return storage != null && storage.supportsInsertion();
    }
}
