package com.logistics.block;

import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
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
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BlockWithEntity implements Waterloggable {
    public static final MapCodec<PipeBlock> CODEC = createCodec(PipeBlock::new);

    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.of("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.of("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.of("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.of("west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> UP = EnumProperty.of("up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> DOWN = EnumProperty.of("down", ConnectionType.class);
    public static final EnumProperty<FeatureFace> FEATURE_FACE = EnumProperty.of("feature_face", FeatureFace.class);
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

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

    private final Pipe pipe;

    public PipeBlock(Settings settings) {
        this(settings, null);
    }

    public PipeBlock(Settings settings, Pipe pipe) {
        super(settings);
        this.pipe = pipe;
        setDefaultState(getDefaultState()
            .with(NORTH, ConnectionType.NONE)
            .with(SOUTH, ConnectionType.NONE)
            .with(EAST, ConnectionType.NONE)
            .with(WEST, ConnectionType.NONE)
            .with(UP, ConnectionType.NONE)
            .with(DOWN, ConnectionType.NONE)
            .with(FEATURE_FACE, FeatureFace.NONE)
            .with(POWERED, false)
            .with(WATERLOGGED, false)
        );
    }

    public Pipe getPipe() {
        return pipe;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, FEATURE_FACE, POWERED, WATERLOGGED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public ActionResult onWrenchUse(ItemUsageContext context) {
        if (pipe == null) {
            return ActionResult.PASS;
        }

        if (context.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        BlockEntity blockEntity = context.getWorld().getBlockEntity(context.getBlockPos());
        if (!(blockEntity instanceof PipeBlockEntity pipeEntity)) {
            return ActionResult.PASS;
        }

        PipeContext pipeContext = new PipeContext(
            context.getWorld(),
            context.getBlockPos(),
            context.getWorld().getBlockState(context.getBlockPos()),
            pipeEntity
        );
        pipe.onWrenchUse(pipeContext, context);
        return ActionResult.SUCCESS;
    }

    public static FeatureFace toFeatureFace(@Nullable Direction direction) {
        if (direction == null) {
            return FeatureFace.NONE;
        }

        return switch (direction) {
            case NORTH -> FeatureFace.NORTH;
            case SOUTH -> FeatureFace.SOUTH;
            case EAST -> FeatureFace.EAST;
            case WEST -> FeatureFace.WEST;
            case UP -> FeatureFace.UP;
            case DOWN -> FeatureFace.DOWN;
        };
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return pipe != null && pipe.hasComparatorOutput();
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (pipe == null) {
            return 0;
        }

        if (world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity) {
            return pipe.getComparatorOutput(new PipeContext(world, pos, state, blockEntity));
        }

        return 0;
    }

    public enum FeatureFace implements net.minecraft.util.StringIdentifiable {
        NONE("none"),
        NORTH("north"),
        SOUTH("south"),
        EAST("east"),
        WEST("west"),
        UP("up"),
        DOWN("down");

        private final String name;

        FeatureFace(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
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
            .with(POWERED, ctx.getWorld().isReceivingRedstonePower(pos))
            .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);

        return state;
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos,
                                                   Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        EnumProperty<ConnectionType> property = getPropertyForDirection(direction);
        if (property != null) {
            state = state.with(property, canConnectTo(world, pos, direction));
        }

        return state;
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, @Nullable WireOrientation wireOrientation,
                               boolean notify) {
        if (!world.isClient) {
            boolean powered = world.isReceivingRedstonePower(pos);
            if (powered != state.get(POWERED)) {
                world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_LISTENERS);
            }
        }
        super.neighborUpdate(state, world, pos, block, wireOrientation, notify);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);

        if (pipe != null && world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity) {
            PipeContext context = new PipeContext(world, pos, state, blockEntity);
            pipe.randomDisplayTick(context, random);
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PipeBlockEntity pipeEntity) {
                // Drop all traveling items
                for (com.logistics.pipe.runtime.TravelingItem travelingItem : pipeEntity.getTravelingItems()) {
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
            if (pipe != null && !pipe.allowsInventoryConnections()) {
                return ConnectionType.NONE;
            }
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
