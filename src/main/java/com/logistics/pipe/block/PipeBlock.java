package com.logistics.pipe.block;

import com.logistics.core.lib.block.Probeable;
import com.logistics.core.lib.block.Wrenchable;
import com.logistics.core.lib.pipe.PipeConnection;
import com.logistics.core.lib.pipe.PipeConnectionRegistry;
import com.logistics.core.lib.support.ProbeResult;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.LogisticsPipe;
import com.logistics.pipe.runtime.TravelingItem;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BaseEntityBlock implements Probeable, SimpleWaterloggedBlock, Wrenchable {
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

    private static final VoxelShape NORTH_SHAPE = Block.box(
            8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 0, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2);
    private static final VoxelShape SOUTH_SHAPE = Block.box(
            8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(
            8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 16, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
    private static final VoxelShape WEST_SHAPE = Block.box(
            0, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);
    private static final VoxelShape UP_SHAPE = Block.box(
            8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 16, 8 + PIPE_SIZE / 2);
    private static final VoxelShape DOWN_SHAPE = Block.box(
            8 - PIPE_SIZE / 2, 0, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2, 8 - PIPE_SIZE / 2, 8 + PIPE_SIZE / 2);

    private final Pipe pipe;

    public PipeBlock(BlockBehaviour.Properties settings) {
        this(settings, null);
    }

    public PipeBlock(Properties settings, Pipe pipe) {
        super(settings);
        this.pipe = pipe;
        if (pipe != null) {
            pipe.setPipeBlock(this);
        }
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

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        // Drop traveling items when pipe is broken by player
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PipeBlockEntity pipeEntity) {
                for (com.logistics.pipe.runtime.TravelingItem item : pipeEntity.getTravelingItems()) {
                    PipeBlockEntity.dropItem(level, pos, item);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Route item use interactions to pipe modules before default block handling.
     */
    @Override
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
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (pipe == null) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof PipeBlockEntity pipeEntity)) {
            return InteractionResult.PASS;
        }

        PipeContext pipeContext = new PipeContext(world, pos, state, pipeEntity);
        InteractionResult result = pipe.onUseWithoutItem(pipeContext, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        if (result != InteractionResult.PASS) {
            return result;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return pipe != null && pipe.hasComparatorOutput();
    }

    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
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
                LogisticsPipe.ENTITY.PIPE_BLOCK_ENTITY,
                (world1, pos, state1, blockEntity) -> PipeBlockEntity.tick(world1, pos, state1, blockEntity));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;

        if (getConnectionType(world, pos, Direction.NORTH) != PipeConnection.Type.NONE) {
            shape = Shapes.or(shape, NORTH_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.SOUTH) != PipeConnection.Type.NONE) {
            shape = Shapes.or(shape, SOUTH_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.EAST) != PipeConnection.Type.NONE) {
            shape = Shapes.or(shape, EAST_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.WEST) != PipeConnection.Type.NONE) {
            shape = Shapes.or(shape, WEST_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.UP) != PipeConnection.Type.NONE) {
            shape = Shapes.or(shape, UP_SHAPE);
        }
        if (getConnectionType(world, pos, Direction.DOWN) != PipeConnection.Type.NONE) {
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
            @Nullable Orientation orientation,
            boolean notify) {
        if (!world.isClientSide()) {
            boolean powered = world.hasNeighborSignal(pos);
            if (powered != state.getValue(POWERED)) {
                world.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            }
        }
        super.neighborChanged(state, world, pos, block, orientation, notify);
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
            LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
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

    @Override
    public ProbeResult onProbe(Level world, BlockPos pos, Player player) {
        if (!(world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity)) {
            return null;
        }

        var items = pipeEntity.getTravelingItems();
        ProbeResult.Builder builder = ProbeResult.builder("Pipe Contents");

        if (items.isEmpty()) {
            builder.entry("Items", "Empty", ChatFormatting.GRAY);
        } else {
            builder.entry("Items", String.valueOf(items.size()), ChatFormatting.WHITE);
            builder.separator();
            for (TravelingItem item : items) {
                String name = item.getStack().getHoverName().getString();
                int count = item.getStack().getCount();
                String info = String.format(
                        "%dx %s -> %s (%.0f%%)",
                        count, name, item.getDirection().name(), item.getProgress() * 100);
                builder.entry("", info, ChatFormatting.AQUA);
            }
        }

        return builder.build();
    }

    @Override
    public InteractionResult onWrench(Level world, BlockPos pos, Player player) {
        if (pipe == null) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof PipeBlockEntity pipeEntity)) {
            return InteractionResult.PASS;
        }

        PipeContext ctx = new PipeContext(world, pos, world.getBlockState(pos), pipeEntity);
        return pipe.onWrench(ctx, player);
    }

    public PipeConnection.Type getConnectionType(BlockGetter world, BlockPos pos, Direction direction) {
        // On client side, use cached values from block entity for rendering performance
        if (world instanceof Level actualWorld && actualWorld.isClientSide()) {
            PipeBlockEntity pipeEntity =
                    actualWorld.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity ? blockEntity : null;
            if (pipeEntity != null) {
                return pipeEntity.getCachedConnectionType(direction);
            }
        }

        return getDynamicConnectionType(world, pos, direction);
    }

    public PipeConnection.Type getDynamicConnectionType(BlockGetter world, BlockPos pos, Direction direction) {
        // Server side or when cache not available: calculate dynamically
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        if (world instanceof Level actualWorld) {
            // Connect to blocks registered with PipeConnectionRegistry.SIDED (pipes, quarries, etc.)
            PipeConnection.Type result = checkPipeConnection(actualWorld, pos, neighborPos, direction, neighborBlock);
            if (result != null) return result;

            // Connect to blocks with item storage (chests, furnaces, hoppers, etc.)
            result = checkItemStorage(actualWorld, pos, neighborPos, direction, neighborBlock);
            if (result != null) return result;
        }

        return PipeConnection.Type.NONE;
    }

    /**
     * Check for a PipeConnectionRegistry.SIDED registration on the neighboring block.
     * Returns the PipeConnection.Type declared by the neighbor, or null if not registered.
     */
    @Nullable private PipeConnection.Type checkPipeConnection(
            Level world, BlockPos pos, BlockPos neighborPos, Direction direction, Block neighborBlock) {
        var connectable = PipeConnectionRegistry.SIDED.find(world, neighborPos, direction.getOpposite());
        if (connectable != null) {
            PipeConnection.Type candidate = connectable.getConnectionType(direction.getOpposite());
            if (candidate != PipeConnection.Type.NONE && pipe != null) {
                PipeBlockEntity pipeEntity =
                        world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity ? blockEntity : null;
                PipeContext context =
                        pipeEntity != null ? new PipeContext(world, pos, world.getBlockState(pos), pipeEntity) : null;
                return pipe.filterConnection(context, direction, neighborBlock, candidate);
            }
            return candidate;
        }
        return null;
    }

    /**
     * Check for ItemStorage.SIDED on the neighboring block (legacy inventory support).
     * Returns INVENTORY if found, null otherwise.
     */
    @Nullable private PipeConnection.Type checkItemStorage(
            Level world, BlockPos pos, BlockPos neighborPos, Direction direction, Block neighborBlock) {
        if (ItemStorage.SIDED.find(world, neighborPos, direction.getOpposite()) != null) {
            PipeConnection.Type candidate = PipeConnection.Type.INVENTORY;
            if (pipe != null) {
                PipeBlockEntity pipeEntity =
                        world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity ? blockEntity : null;
                PipeContext context =
                        pipeEntity != null ? new PipeContext(world, pos, world.getBlockState(pos), pipeEntity) : null;
                return pipe.filterConnection(context, direction, neighborBlock, candidate);
            }
            return candidate;
        }
        return null;
    }
}
