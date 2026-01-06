package com.logistics.block;

import com.logistics.block.entity.PipeBlockEntity;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
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

    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    // Properties to track which connections are to inventories (for visual extension)
    public static final BooleanProperty NORTH_INVENTORY = BooleanProperty.of("north_inventory");
    public static final BooleanProperty SOUTH_INVENTORY = BooleanProperty.of("south_inventory");
    public static final BooleanProperty EAST_INVENTORY = BooleanProperty.of("east_inventory");
    public static final BooleanProperty WEST_INVENTORY = BooleanProperty.of("west_inventory");
    public static final BooleanProperty UP_INVENTORY = BooleanProperty.of("up_inventory");
    public static final BooleanProperty DOWN_INVENTORY = BooleanProperty.of("down_inventory");

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
            .with(NORTH, false)
            .with(SOUTH, false)
            .with(EAST, false)
            .with(WEST, false)
            .with(UP, false)
            .with(DOWN, false)
            .with(WATERLOGGED, false)
            .with(NORTH_INVENTORY, false)
            .with(SOUTH_INVENTORY, false)
            .with(EAST_INVENTORY, false)
            .with(WEST_INVENTORY, false)
            .with(UP_INVENTORY, false)
            .with(DOWN_INVENTORY, false)
        );
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED);
        builder.add(NORTH_INVENTORY, SOUTH_INVENTORY, EAST_INVENTORY, WEST_INVENTORY, UP_INVENTORY, DOWN_INVENTORY);
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

        if (state.get(NORTH)) {
            shape = VoxelShapes.union(shape, NORTH_SHAPE);
        }
        if (state.get(SOUTH)) {
            shape = VoxelShapes.union(shape, SOUTH_SHAPE);
        }
        if (state.get(EAST)) {
            shape = VoxelShapes.union(shape, EAST_SHAPE);
        }
        if (state.get(WEST)) {
            shape = VoxelShapes.union(shape, WEST_SHAPE);
        }
        if (state.get(UP)) {
            shape = VoxelShapes.union(shape, UP_SHAPE);
        }
        if (state.get(DOWN)) {
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

        // Update inventory connections if world is available
        if (ctx.getWorld() instanceof World actualWorld) {
            state = updateInventoryConnections(actualWorld, pos, state);
        }

        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                  WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        BooleanProperty property = getPropertyForDirection(direction);
        if (property != null) {
            state = state.with(property, canConnectTo(world, pos, direction));
        }

        // Update inventory connections
        if (world instanceof World actualWorld) {
            state = updateInventoryConnections(actualWorld, pos, state);
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

    private boolean canConnectTo(BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        // Connect to other pipes
        if (neighborBlock instanceof PipeBlock) {
            return true;
        }

        // Connect to blocks with item storage (chests, furnaces, hoppers, etc.)
        // ItemStorage.SIDED requires a World, so only check if we have one
        if (world instanceof World actualWorld) {
            if (ItemStorage.SIDED.find(actualWorld, neighborPos, direction.getOpposite()) != null) {
                return true;
            }
        }

        return false;
    }

    private static BooleanProperty getPropertyForDirection(Direction direction) {
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
     * Update inventory connection properties based on adjacent blocks
     */
    protected BlockState updateInventoryConnections(World world, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            BooleanProperty connectionProp = getPropertyForDirection(direction);
            BooleanProperty inventoryProp = getInventoryPropertyForDirection(direction);

            if (connectionProp != null && inventoryProp != null) {
                // Only check if this direction is connected
                if (state.get(connectionProp)) {
                    BlockPos neighborPos = pos.offset(direction);
                    boolean isInventory = isInventoryConnection(world, neighborPos, direction);
                    state = state.with(inventoryProp, isInventory);
                } else {
                    state = state.with(inventoryProp, false);
                }
            }
        }
        return state;
    }

    /**
     * Check if a position has an inventory (but not a pipe)
     */
    protected boolean isInventoryConnection(World world, BlockPos pos, Direction fromDirection) {
        BlockState neighborState = world.getBlockState(pos);

        // Don't mark pipes as inventories
        if (neighborState.getBlock() instanceof PipeBlock) {
            return false;
        }

        // Check if there's an item storage
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, pos, fromDirection.getOpposite());
        return storage != null;
    }

    protected static BooleanProperty getInventoryPropertyForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH_INVENTORY;
            case SOUTH -> SOUTH_INVENTORY;
            case EAST -> EAST_INVENTORY;
            case WEST -> WEST_INVENTORY;
            case UP -> UP_INVENTORY;
            case DOWN -> DOWN_INVENTORY;
        };
    }
}
