package com.logistics.block;

import com.logistics.block.entity.PipeBlockEntity;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BlockWithEntity implements Waterloggable {
    public static final MapCodec<PipeBlock> CODEC = createCodec(PipeBlock::new);

    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.of("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.of("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.of("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.of("west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> UP = EnumProperty.of("up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> DOWN = EnumProperty.of("down", ConnectionType.class);
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    // Base pipe speed - matches the speed after ~3 powered gold pipes
    // At 0.15 blocks/tick, takes ~6.67 ticks (0.33 seconds) to traverse one segment
    public static final float BASE_PIPE_SPEED = 3.0f / 20.0f; // Blocks per tick (0.15)

    // Acceleration rate - how quickly items adjust to pipe speed
    public static final float ACCELERATION_RATE = 0.004f; // Speed change per tick (doubled)

    // Uniform 8px thickness for both core and arms
    private static final double PIPE_SIZE = 8.0;

    private static final VoxelShape CORE_SHAPE = Block.createCuboidShape(
        8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2,
        8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2
    );

    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(
        8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 0,
        8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2
    );
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(
        8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2,
        8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 16
    );
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(
        8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2,
        16, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2
    );
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(
        0, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2,
        8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2
    );
    private static final VoxelShape UP_SHAPE = Block.createCuboidShape(
        8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2,
        8 + PIPE_SIZE / 2, 16, 8 + PIPE_SIZE / 2
    );
    private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(
        8 - PIPE_SIZE / 2, 0, 8 - PIPE_SIZE / 2,
        8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2
    );

    public PipeBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
            .with(NORTH, ConnectionType.NONE)
            .with(SOUTH, ConnectionType.NONE)
            .with(EAST, ConnectionType.NONE)
            .with(WEST, ConnectionType.NONE)
            .with(UP, ConnectionType.NONE)
            .with(DOWN, ConnectionType.NONE)
            .with(WATERLOGGED, false)
        );
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, com.logistics.block.entity.LogisticsBlockEntities.PIPE_BLOCK_ENTITY,
            (world1, pos, state1, blockEntity) -> PipeBlockEntity.tick(world1, pos, state1, blockEntity));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE_SHAPE;

        if (state.get(NORTH) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, NORTH_SHAPE);
        }
        if (state.get(SOUTH) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, SOUTH_SHAPE);
        }
        if (state.get(EAST) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, EAST_SHAPE);
        }
        if (state.get(WEST) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, WEST_SHAPE);
        }
        if (state.get(UP) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, UP_SHAPE);
        }
        if (state.get(DOWN) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, DOWN_SHAPE);
        }

        return shape;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockView world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        FluidState fluidState = world.getFluidState(pos);

        BlockState state = getDefaultState()
            .with(NORTH, canConnectTo(world, pos, Direction.NORTH))
            .with(SOUTH, canConnectTo(world, pos, Direction.SOUTH))
            .with(EAST, canConnectTo(world, pos, Direction.EAST))
            .with(WEST, canConnectTo(world, pos, Direction.WEST))
            .with(UP, canConnectTo(world, pos, Direction.UP))
            .with(DOWN, canConnectTo(world, pos, Direction.DOWN))
            .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);

        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                  WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        EnumProperty<ConnectionType> property = getPropertyForDirection(direction);
        if (property != null) {
            state = state.with(property, canConnectTo(world, pos, direction));
        }

        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PipeBlockEntity pipeEntity) {
                // Drop all traveling items
                for (com.logistics.item.TravelingItem travelingItem : pipeEntity.getTravelingItems()) {
                    ItemEntity itemEntity = new ItemEntity(
                        world,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        travelingItem.getStack().copy()
                    );
                    itemEntity.setToDefaultPickupDelay();
                    world.spawnEntity(itemEntity);
                }
            }
            // This ensures the block entity is properly removed before dropping the block
            world.removeBlockEntity(pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    private ConnectionType canConnectTo(BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        // Connect to other pipes
        if (neighborBlock instanceof PipeBlock) {
            return ConnectionType.PIPE;
        }

        // Connect to blocks with item storage (chests, furnaces, hoppers, etc.)
        // ItemStorage.SIDED requires a World, so only check if we have one
        if (world instanceof World actualWorld) {
            if (ItemStorage.SIDED.find(actualWorld, neighborPos, direction.getOpposite()) != null) {
                return ConnectionType.INVENTORY;
            }
        }

        return ConnectionType.NONE;
    }

    public static EnumProperty<ConnectionType> getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    /**
     * Get the target speed for items traveling through this pipe.
     * Can be overridden by subclasses to provide different speeds (e.g., based on redstone power).
     * @param world The world
     * @param pos The pipe's position
     * @param state The pipe's block state
     * @return Target speed in blocks per tick
     */
    public float getPipeSpeed(World world, BlockPos pos, BlockState state) {
        return BASE_PIPE_SPEED;
    }

    /**
     * Get the acceleration rate for this pipe.
     * Can be overridden by subclasses to provide different acceleration.
     * @return Acceleration rate in speed units per tick
     */
    public float getAccelerationRate() {
        return ACCELERATION_RATE;
    }

    /**
     * Whether this pipe can accelerate items (increase their speed).
     * Base pipes only decelerate (provide drag). Only powered gold pipes can accelerate.
     * @param world The world
     * @param pos The pipe's position
     * @param state The pipe's block state
     * @return true if this pipe can accelerate items
     */
    public boolean canAccelerate(World world, BlockPos pos, BlockState state) {
        return false; // Base pipes only decelerate
    }

    /**
     * Whether this pipe can accept items from other pipes.
     * Wooden pipes reject items from pipes (they're extraction-only entry points).
     * @param world The world
     * @param pos The pipe's position
     * @param state The pipe's block state
     * @param fromDirection The direction the item is coming from
     * @return true if this pipe accepts items from other pipes
     */
    public boolean canAcceptFromPipe(World world, BlockPos pos, BlockState state, Direction fromDirection) {
        return true; // Base pipes accept from other pipes
    }

    /**
     * Whether this pipe can accept items inserted from non-pipe blocks.
     * Base pipes are closed to external insertion; ingress pipes override this.
     * @param world The world
     * @param pos The pipe's position
     * @param state The pipe's block state
     * @param fromDirection The direction the item is coming from
     * @return true if this pipe accepts items from non-pipe sources
     */
    public boolean canAcceptFromInventory(World world, BlockPos pos, BlockState state, Direction fromDirection) {
        return false;
    }

    public BlockState refreshConnections(World world, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            EnumProperty<ConnectionType> connectionProp = getPropertyForDirection(direction);
            if (connectionProp != null) {
                state = state.with(connectionProp, canConnectTo(world, pos, direction));
            }
        }
        return state;
    }

    public boolean hasConnection(BlockState state, Direction direction) {
        EnumProperty<ConnectionType> property = getPropertyForDirection(direction);
        return property != null && state.get(property) != ConnectionType.NONE;
    }

    public boolean isInventoryConnection(BlockState state, Direction direction) {
        EnumProperty<ConnectionType> property = getPropertyForDirection(direction);
        return property != null && state.get(property) == ConnectionType.INVENTORY;
    }

    public enum ConnectionType implements net.minecraft.util.StringIdentifiable {
        NONE("none"),
        PIPE("pipe"),
        INVENTORY("inventory");

        private final String name;

        ConnectionType(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
