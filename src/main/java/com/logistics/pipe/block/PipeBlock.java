package com.logistics.pipe.block;

import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.registry.PipeBlockEntities;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<PipeBlock> CODEC = simpleCodec(PipeBlock::new);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // Uniform 8px thickness for both core and arms
    private static final double PIPE_SIZE = 8.0;

    private static final VoxelShape CORE_SHAPE = Block.box(
            8 - PIPE_SIZE / 2,
            8 - PIPE_SIZE / 2,
            8 - PIPE_SIZE / 2,
            8 + PIPE_SIZE / 2,
            8 + PIPE_SIZE / 2,
            8 + PIPE_SIZE / 2);

    private static final VoxelShape NORTH_SHAPE =
            Block.box(8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 0, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2);
    private static final VoxelShape SOUTH_SHAPE = Block.box(
            8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(
            8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 16, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
    private static final VoxelShape WEST_SHAPE =
            Block.box(0, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
    private static final VoxelShape UP_SHAPE = Block.box(
            8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 16, 8 + PIPE_SIZE / 2);
    private static final VoxelShape DOWN_SHAPE =
            Block.box(8 - PIPE_SIZE / 2, 0, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);

    private final Pipe pipe;

    public PipeBlock(Properties settings) {
        this(settings, null);
    }

    public PipeBlock(Properties settings, Pipe pipe) {
        super(settings.mapColor(MapColor.NONE).noOcclusion().strength(0.0f));
        this.pipe = pipe;
        registerDefaultState(defaultBlockState().setValue(POWERED, false).setValue(WATERLOGGED, false));
    }

    public Pipe getPipe() {
        return pipe;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public SoundType getSoundType(BlockState state) {
        return SoundType.METAL;
    }

    /**
     * Route item use interactions to pipe modules before default block handling.
     */
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (pipe == null) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof PipeBlockEntity pipeEntity)) {
            return InteractionResult.PASS;
        }

        PipeContext pipeContext = new PipeContext(world, pos, state, pipeEntity);
        InteractionResult result = pipe.onUseWithItem(pipeContext, new UseOnContext(player, hand, hit));
        if (result != InteractionResult.PASS) {
            return result;
        }

        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return pipe != null && pipe.hasComparatorOutput();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (pipe == null) {
            return 0;
        }

        if (world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity) {
            return pipe.getComparatorOutput(new PipeContext(world, pos, state, blockEntity));
        }

        return 0;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                PipeBlockEntities.PIPE_BLOCK_ENTITY,
                (world1, pos, state1, blockEntity) -> PipeBlockEntity.tick(world1, pos, state1, blockEntity));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;

        if (getConnectionType(world, pos, Direction.NORTH) != ConnectionType.NONE) {
            shape = Shapes.or(shape, NORTH_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.SOUTH) != ConnectionType.NONE) {
            shape = Shapes.or(shape, SOUTH_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.EAST) != ConnectionType.NONE) {
            shape = Shapes.or(shape, EAST_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.WEST) != ConnectionType.NONE) {
            shape = Shapes.or(shape, WEST_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.UP) != ConnectionType.NONE) {
            shape = Shapes.or(shape, UP_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.DOWN) != ConnectionType.NONE) {
            shape = Shapes.or(shape, DOWN_SHAPE);
        }

        return shape;
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockGetter world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        FluidState fluidState = world.getFluidState(pos);

        return defaultBlockState()
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(pos))
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader world,
            ScheduledTickAccess tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        return state;
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
            boolean powered = world.hasNeighborSignal(pos);
            if (powered != state.getValue(POWERED)) {
                world.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            }
        }
        super.neighborChanged(state, world, pos, block, wireOrientation, notify);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        super.animateTick(state, world, pos, random);

        if (pipe != null && world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity) {
            PipeContext context = new PipeContext(world, pos, state, blockEntity);
            pipe.randomDisplayTick(context, random);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return pipe != null && pipe.hasRandomTicks();
    }

    @Override
    public ItemStack getCloneItemStack(
            net.minecraft.world.level.LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(world, pos, state, includeData);

        // Copy components from block entity to preserve state (e.g., weathering) on normal pick-block.
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof PipeBlockEntity pipeEntity) {
            stack.applyComponents(pipeEntity.collectComponents());
        }

        return stack;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        super.randomTick(state, world, pos, random);

        if (pipe != null && world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity) {
            PipeContext context = new PipeContext(world, pos, state, blockEntity);
            pipe.randomTick(context, random);
        }
    }

    public ConnectionType getConnectionType(BlockGetter world, BlockPos pos, Direction direction) {
        // On client side, use cached values from block entity for rendering performance
        if (world instanceof Level actualWorld && actualWorld.isClientSide()) {
            PipeBlockEntity pipeEntity =
                    actualWorld.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity ? blockEntity : null;
            if (pipeEntity != null) {
                return pipeEntity.getConnectionType(direction);
            }
        }

        return getDynamicConnectionType(world, pos, direction);
    }

    public ConnectionType getDynamicConnectionType(BlockGetter world, BlockPos pos, Direction direction) {
        // Server side or when cache not available: calculate dynamically
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        // Connect to other pipes
        if (neighborBlock instanceof PipeBlock) {
            ConnectionType candidate = ConnectionType.PIPE;
            if (pipe != null && world instanceof Level actualWorld) {
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
        if (world instanceof Level actualWorld) {
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

    public enum ConnectionType implements net.minecraft.util.StringRepresentable {
        NONE("none"),
        PIPE("pipe"),
        INVENTORY("inventory");

        private final String name;

        ConnectionType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
