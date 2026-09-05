package com.logistics.pipe.block;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.platform.CreativeVariantProvider;
import java.util.List;
import com.logistics.core.lib.block.ConnectedShapeCache;
import com.logistics.core.lib.block.behavior.WrenchBehavior;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.pipe.ModularPipe;
import com.logistics.core.lib.pipe.ModularPipeBlock;
import com.logistics.core.lib.pipe.PipeFamily;
import com.logistics.pipe.FluidPipe;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
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
import net.minecraft.world.phys.BlockHitResult;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Block for every fluid pipe. Renders as an invisible block; geometry is produced in code by
 * {@code FluidPipeBlockEntityRenderer}. The pipe's behavior comes from its bound {@link FluidPipe}
 * definition (bound at registration, mirroring how item {@code PipeBlock} binds its {@code ItemPipe}).
 * Connections are computed here and cached on the block entity for shape and arm rendering.
 */
public class FluidPipeBlock extends BaseEntityBlock
        implements SimpleWaterloggedBlock, WrenchBehavior.Wrenchable, ModularPipeBlock, CreativeVariantProvider {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Block light (0-15) emitted while the pipe holds a light-emitting fluid; driven by the block entity. */
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);

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

    private static final ConnectedShapeCache SHAPE_CACHE = new ConnectedShapeCache(CORE_SHAPE, ARM_SHAPES);

    @Nullable
    private final FluidPipe fluidPipe;

    /** Codec/registry constructor: produces a definition-less block (never placed in-world). */
    public FluidPipeBlock(BlockBehaviour.Properties settings) {
        this(settings, null);
    }

    public FluidPipeBlock(BlockBehaviour.Properties settings, @Nullable FluidPipe fluidPipe) {
        super(settings.lightLevel(state -> state.getValue(LIGHT_LEVEL)));
        this.fluidPipe = fluidPipe;
        if (fluidPipe != null) {
            fluidPipe.setPipeBlock(this);
        }
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(LIGHT_LEVEL, 0));
    }

    /** The fluid pipe definition backing this block (its transport policy + cosmetic/connection modules). */
    @Nullable
    public FluidPipe fluidPipe() {
        return fluidPipe;
    }

    @Override
    public void appendCreativeMenuVariants(List<ItemStack> variants, ItemStack baseStack) {
        FluidPipe pipe = fluidPipe();
        if (pipe != null) {
            pipe.appendCreativeMenuVariants(variants, baseStack);
        }
    }

    @Override
    public ModularPipe modularPipe() {
        return fluidPipe;
    }

    @Override
    public PipeFamily family() {
        return PipeFamily.FLUID;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, LIGHT_LEVEL);
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
                LogisticsPipe.ENTITY.FLUID_PIPE_BLOCK_ENTITY,
                FluidPipeBlockEntity::tick);
    }

    // ==================== Module dispatch (use / random tick) ====================

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (fluidPipe != null && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity be) {
            InteractionResult result = fluidPipe.onUseWithItem(be.createContext(), new UseOnContext(player, hand, hit));
            if (result != InteractionResult.PASS) {
                // A connection-affecting change (e.g. marking) must re-evaluate this pipe and its
                // neighbours so a marked boundary actually splits the fluid body.
                be.invalidateConnectionsAndNeighbours();
                return result;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (fluidPipe != null && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity be) {
            InteractionResult result =
                    fluidPipe.onUseWithoutItem(be.createContext(), new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            if (result != InteractionResult.PASS) {
                be.invalidateConnectionsAndNeighbours();
                return result;
            }
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return fluidPipe != null && fluidPipe.hasRandomTicks();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (fluidPipe != null && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity be) {
            fluidPipe.randomTick(be.createContext(), random);
        }
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
        FluidConnection candidate;
        if (level.getBlockState(neighbour).getBlock() instanceof FluidPipeBlock) {
            candidate = FluidConnection.PIPE;
        } else {
            // Void and bypass pipes connect only to other fluid pipes, never to external handlers.
            candidate = (fluidPipe == null || fluidPipe.canConnectToFluidHandler())
                    ? FluidConnection.HANDLER
                    : FluidConnection.NONE;
        }
        if (candidate == FluidConnection.NONE) {
            return FluidConnection.NONE;
        }
        // Let hosted modules veto the connection (mirror of Pipe.filterConnection) — e.g. differently
        // marked pipes refuse to connect.
        if (fluidPipe != null && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity be) {
            Block neighbourBlock = level.getBlockState(neighbour).getBlock();
            if (!fluidPipe.allowsConnection(be.createContext(), direction, neighbourBlock)) {
                return FluidConnection.NONE;
            }
        }
        return candidate;
    }

    private boolean isConnected(BlockGetter world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            return pipe.connection(direction) != FluidConnection.NONE;
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (isConnected(world, pos, direction)) {
                mask |= ConnectedShapeCache.bit(direction);
            }
        }
        return SHAPE_CACHE.get(mask);
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(world, pos, state, includeData);

        // Copy components from the block entity so pick-block preserves state (e.g. copper weathering stage).
        if (world.getBlockEntity(pos) instanceof FluidPipeBlockEntity be) {
            stack.applyComponents(be.collectComponents());
        }

        return stack;
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
        if (fluidPipe != null && fluidPipe.usesFeatureFace()) {
            // Merger output / extractor pull face: each wrench advances it to the next candidate face,
            // like the item Merger and Extractor pipes.
            pipe.cycleFeatureDirection();
        } else if (player.isShiftKeyDown()) {
            pipe.resetSides();
        } else {
            pipe.toggleSide(face);
        }
        return InteractionResult.SUCCESS;
    }
}
