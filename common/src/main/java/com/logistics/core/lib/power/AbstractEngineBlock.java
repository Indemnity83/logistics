package com.logistics.core.lib.power;

import static com.logistics.core.lib.power.HeatStage.STAGE;

import com.logistics.core.lib.block.MachineBlock;
import com.logistics.core.lib.block.behavior.WrenchBehavior;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.IEnergyStorage;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for all engine blocks.
 * Provides common functionality for FACING, POWERED, STAGE properties and redstone handling.
 *
 * @param <E> The type of engine block entity this block creates
 */
public abstract class AbstractEngineBlock<E extends EngineEntity> extends MachineBlock
        implements WrenchBehavior.Wrenchable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SUPPORT_UP = Block.box(0, 15, 0, 16, 16, 16);
    private static final VoxelShape SUPPORT_DOWN = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape SUPPORT_NORTH = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape SUPPORT_SOUTH = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SUPPORT_WEST = Block.box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape SUPPORT_EAST = Block.box(15, 0, 0, 16, 16, 16);

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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, STAGE);
        for (Property<?> property : getAdditionalProperties()) {
            builder.add(property);
        }
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
    public InteractionResult onWrench(Level world, BlockPos pos, Player player) {
        if (!world.isClientSide()) {
            BlockState state = world.getBlockState(pos);
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
     * Only the base face (opposite of FACING) supports attachments like levers, buttons, and
     * redstone dust. All SupportType checks go through getBlockSupportShape, so we return a
     * thin slab on the base face side — giving that face full coverage while leaving all other
     * faces empty.
     */
    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return switch (state.getValue(FACING).getOpposite()) {
            case UP -> SUPPORT_UP;
            case DOWN -> SUPPORT_DOWN;
            case NORTH -> SUPPORT_NORTH;
            case SOUTH -> SUPPORT_SOUTH;
            case WEST -> SUPPORT_WEST;
            case EAST -> SUPPORT_EAST;
        };
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
     * Platform service for checking whether a block in a given direction can accept energy.
     * Set once during loader-specific initialization (Fabric or NeoForge bootstrap).
     */
    @FunctionalInterface
    public interface EnergyPresenceChecker {
        boolean hasEnergyStorage(Level world, BlockPos pos, Direction direction);
    }

    private static EnergyPresenceChecker energyPresenceChecker;

    public static void setEnergyPresenceChecker(EnergyPresenceChecker checker) {
        energyPresenceChecker = checker;
    }

    /**
     * Checks if there's a block with energy storage capability in the given direction.
     * Delegates to the loader-specific {@link EnergyPresenceChecker}.
     *
     * @param world the world
     * @param pos the engine's position
     * @param direction the direction to check
     * @return true if an energy-accepting storage exists in that direction
     */
    private static boolean hasEnergyStorage(Level world, BlockPos pos, Direction direction) {
        // Extraction pipes are off the loader grid; the presence checker can't see them. Only
        // count the face the receiver actually accepts power on.
        if (world.getBlockEntity(pos.relative(direction)) instanceof DirectEnergyReceiver receiver
                && receiver instanceof HasEnergyStorage hasStorage) {
            IEnergyStorage storage = hasStorage.energyStorage(direction.getOpposite());
            return storage != null && storage.acceptsEnergy();
        }
        if (energyPresenceChecker == null) return false;
        return energyPresenceChecker.hasEnergyStorage(world, pos, direction);
    }
}
