package com.logistics.block;

import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.MapColor;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BlockWithEntity implements Waterloggable {
    public static final MapCodec<PipeBlock> CODEC = createCodec(PipeBlock::new);

    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    // Uniform 8px thickness for both core and arms
    private static final double PIPE_SIZE = 8.0;

    private static final VoxelShape CORE_SHAPE = Block.createCuboidShape(
            8 - PIPE_SIZE / 2,
            8 - PIPE_SIZE / 2,
            8 - PIPE_SIZE / 2,
            8 + PIPE_SIZE / 2,
            8 + PIPE_SIZE / 2,
            8 + PIPE_SIZE / 2);

    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(
            8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 0, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(
            8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 16);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(
            8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 16, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(
            0, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
    private static final VoxelShape UP_SHAPE = Block.createCuboidShape(
            8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 16, 8 + PIPE_SIZE / 2);
    private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(
            8 - PIPE_SIZE / 2, 0, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);

    private final Pipe pipe;

    public PipeBlock(Settings settings) {
        this(settings, null);
    }

    public PipeBlock(Settings settings, Pipe pipe) {
        super(settings.mapColor(MapColor.CLEAR).nonOpaque().strength(0.0f));
        this.pipe = pipe;
        setDefaultState(getDefaultState().with(POWERED, false).with(WATERLOGGED, false));
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
        builder.add(POWERED, WATERLOGGED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return BlockSoundGroup.METAL;
    }

    /**
     * Route item use interactions to pipe modules before default block handling.
     */
    protected ActionResult onUseWithItem(
            ItemStack stack,
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            BlockHitResult hit) {
        if (pipe == null) {
            return ActionResult.PASS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof PipeBlockEntity pipeEntity)) {
            return ActionResult.PASS;
        }

        PipeContext pipeContext = new PipeContext(world, pos, state, pipeEntity);
        ActionResult result = pipe.onUseWithItem(pipeContext, new ItemUsageContext(player, hand, hit));
        if (result != ActionResult.PASS) {
            return result;
        }

        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return pipe != null && pipe.hasComparatorOutput();
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        if (pipe == null) {
            return 0;
        }

        if (world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity) {
            return pipe.getComparatorOutput(new PipeContext(world, pos, state, blockEntity));
        }

        return 0;
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(
                type,
                com.logistics.block.entity.LogisticsBlockEntities.PIPE_BLOCK_ENTITY,
                (world1, pos, state1, blockEntity) -> PipeBlockEntity.tick(world1, pos, state1, blockEntity));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE_SHAPE;

        if (getConnectionType(world, pos, Direction.NORTH) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, NORTH_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.SOUTH) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, SOUTH_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.EAST) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, EAST_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.WEST) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, WEST_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.UP) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, UP_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.DOWN) != ConnectionType.NONE) {
            shape = VoxelShapes.union(shape, DOWN_SHAPE);
        }

        return shape;
    }

    @Nullable @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockView world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        FluidState fluidState = world.getFluidState(pos);

        return getDefaultState()
                .with(POWERED, ctx.getWorld().isReceivingRedstonePower(pos))
                .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state,
            WorldView world,
            ScheduledTickView tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            Random random) {
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        return state;
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
    protected boolean hasRandomTicks(BlockState state) {
        return pipe != null && pipe.hasRandomTicks();
    }

    @Override
    public ItemStack getPickStack(
            net.minecraft.world.WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getPickStack(world, pos, state, includeData);

        // Copy components from block entity to preserve state (e.g., weathering)
        // Only when includeData is true (Ctrl+pick) to match vanilla behavior
        if (includeData) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PipeBlockEntity pipeEntity) {
                stack.applyComponentsFrom(pipeEntity.createComponentMap());
            }
        }

        return stack;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);

        if (pipe != null && world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity) {
            PipeContext context = new PipeContext(world, pos, state, blockEntity);
            pipe.randomTick(context, random);
        }
    }

    public ConnectionType getConnectionType(BlockView world, BlockPos pos, Direction direction) {
        // On client side, use cached values from block entity for rendering performance
        if (world instanceof World actualWorld && actualWorld.isClient()) {
            PipeBlockEntity pipeEntity =
                    actualWorld.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity ? blockEntity : null;
            if (pipeEntity != null) {
                return pipeEntity.getConnectionType(direction);
            }
        }

        return getDynamicConnectionType(world, pos, direction);
    }

    public ConnectionType getDynamicConnectionType(BlockView world, BlockPos pos, Direction direction) {
        // Server side or when cache not available: calculate dynamically
        BlockPos neighborPos = pos.offset(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        // Connect to other pipes
        if (neighborBlock instanceof PipeBlock) {
            ConnectionType candidate = ConnectionType.PIPE;
            if (pipe != null && world instanceof World actualWorld) {
                PipeBlockEntity pipeEntity =
                        actualWorld.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity ? blockEntity : null;
                PipeContext context = pipeEntity != null
                        ? new PipeContext(actualWorld, pos, actualWorld.getBlockState(pos), pipeEntity)
                        : null;
                return pipe.filterConnection(context, direction, neighborBlock, candidate);
            }
            return candidate;
        }

        // Connect to blocks with item storage (chests, furnaces, hoppers, etc.)
        // ItemStorage.SIDED requires a World, so only check if we have one
        if (world instanceof World actualWorld) {
            if (ItemStorage.SIDED.find(actualWorld, neighborPos, direction.getOpposite()) != null) {
                ConnectionType candidate = ConnectionType.INVENTORY;
                if (pipe != null) {
                    PipeBlockEntity pipeEntity =
                            actualWorld.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity ? blockEntity : null;
                    PipeContext context = pipeEntity != null
                            ? new PipeContext(actualWorld, pos, actualWorld.getBlockState(pos), pipeEntity)
                            : null;
                    return pipe.filterConnection(context, direction, neighborBlock, candidate);
                }
                return candidate;
            }
        }

        return ConnectionType.NONE;
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
