package com.logistics.fluid.block;

import com.logistics.LogisticsFluid;
import com.logistics.core.lib.block.behavior.WrenchBehavior;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.fluid.block.entity.FluidPipeBlockEntity;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Block for both fluid pipe kinds. Renders as an invisible block; geometry is produced in code by
 * {@code FluidPipeBlockEntityRenderer}. Connections are computed by this block and cached on the
 * block entity for shape and arm rendering.
 */
public class FluidPipeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock, WrenchBehavior.Wrenchable {

    public static final MapCodec<FluidPipeBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            FluidPipeKind.CODEC.fieldOf("kind").forGetter(FluidPipeBlock::kind))
            .apply(instance, FluidPipeBlock::new));

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final double PIPE_SIZE = 8.0;
    private static final VoxelShape CORE_SHAPE = Block.box(
            8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2,
            8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
    private static final VoxelShape[] ARM_SHAPES = new VoxelShape[6];

    static {
        ARM_SHAPES[Direction.NORTH.get3DDataValue()] = Block.box(
                8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 0, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2);
        ARM_SHAPES[Direction.SOUTH.get3DDataValue()] = Block.box(
                8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 16);
        ARM_SHAPES[Direction.EAST.get3DDataValue()] = Block.box(
                8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 16, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
        ARM_SHAPES[Direction.WEST.get3DDataValue()] = Block.box(
                0, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
        ARM_SHAPES[Direction.UP.get3DDataValue()] = Block.box(
                8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 16, 8 + PIPE_SIZE / 2);
        ARM_SHAPES[Direction.DOWN.get3DDataValue()] = Block.box(
                8 - PIPE_SIZE / 2, 0, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
    }

    private final FluidPipeKind kind;

    public FluidPipeBlock(BlockBehaviour.Properties settings, FluidPipeKind kind) {
        super(settings);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public FluidPipeKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
                type,
                LogisticsFluid.ENTITY.FLUID_PIPE_BLOCK_ENTITY,
                FluidPipeBlockEntity::tick);
    }

    // ==================== Connections ====================

    /**
     * Compute the fluid connection on a side. A side connects iff it is not wrench-disabled and the
     * neighbour exposes fluid storage on the shared face. The neighbour's own wrench state is honoured
     * because a disabled neighbour face returns no storage, making the connection symmetric.
     */
    public FluidConnection computeConnection(Level level, BlockPos pos, Direction direction, int disabledMask) {
        if ((disabledMask & (1 << direction.get3DDataValue())) != 0) {
            return FluidConnection.NONE;
        }
        BlockPos neighbour = pos.relative(direction);
        if (!level.isLoaded(neighbour)) {
            return FluidConnection.NONE;
        }
        if (FluidStorageLookup.find(level, neighbour, direction.getOpposite()) == null) {
            return FluidConnection.NONE;
        }
        return level.getBlockState(neighbour).getBlock() instanceof FluidPipeBlock
                ? FluidConnection.PIPE
                : FluidConnection.HANDLER;
    }

    private boolean isConnected(BlockGetter world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            return pipe.connection(direction) != FluidConnection.NONE;
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        for (Direction direction : Direction.values()) {
            if (isConnected(world, pos, direction)) {
                shape = Shapes.or(shape, ARM_SHAPES[direction.get3DDataValue()]);
            }
        }
        return shape;
    }

    // ==================== Placement / waterlogging / neighbour updates ====================

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, world, pos, oldState, movedByPiston);
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.invalidateConnectionCache();
        }
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
            BlockState state, Level world, BlockPos pos, Block block, @Nullable Orientation orientation, boolean notify) {
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.invalidateConnectionCache();
        }
        super.neighborChanged(state, world, pos, block, orientation, notify);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    // ==================== Wrench ====================

    @Override
    public InteractionResult onWrench(Level level, BlockPos pos, Player player) {
        return onWrench(level, pos, player, Direction.NORTH);
    }

    @Override
    public InteractionResult onWrench(Level level, BlockPos pos, Player player, Direction face) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            pipe.resetSides();
        } else {
            pipe.toggleSide(face);
        }
        return InteractionResult.SUCCESS;
    }
}
