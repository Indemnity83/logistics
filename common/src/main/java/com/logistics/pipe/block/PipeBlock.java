package com.logistics.pipe.block;

import com.logistics.core.lib.block.ConnectedShapeCache;
import com.logistics.core.lib.platform.CreativeVariantProvider;
import java.util.List;
import com.logistics.core.lib.block.behavior.WrenchBehavior;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.core.lib.pipe.PipeConnectionLookup;
import com.logistics.pipe.ItemPipe;
import com.logistics.core.lib.pipe.ModularPipe;
import com.logistics.core.lib.pipe.ModularPipeBlock;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.PipeFamily;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.LogisticsPipe;
import com.mojang.serialization.MapCodec;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.storage.ItemStorageLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BaseEntityBlock
        implements SimpleWaterloggedBlock, WrenchBehavior.Wrenchable, ModularPipeBlock, CreativeVariantProvider {
    public static final MapCodec<PipeBlock> CODEC = simpleCodec(PipeBlock::new);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty CRAFTING = BooleanProperty.create("crafting");

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

    private static final ConnectedShapeCache SHAPE_CACHE;

    static {
        VoxelShape[] arms = new VoxelShape[6];
        arms[Direction.NORTH.get3DDataValue()] = NORTH_SHAPE;
        arms[Direction.SOUTH.get3DDataValue()] = SOUTH_SHAPE;
        arms[Direction.EAST.get3DDataValue()] = EAST_SHAPE;
        arms[Direction.WEST.get3DDataValue()] = WEST_SHAPE;
        arms[Direction.UP.get3DDataValue()] = UP_SHAPE;
        arms[Direction.DOWN.get3DDataValue()] = DOWN_SHAPE;
        SHAPE_CACHE = new ConnectedShapeCache(CORE_SHAPE, arms);
    }

    private final ItemPipe pipe;

    public PipeBlock(BlockBehaviour.Properties settings) {
        this(settings, null);
    }

    public PipeBlock(Properties settings, ItemPipe pipe) {
        super(settings);
        this.pipe = pipe;
        if (pipe != null) {
            pipe.setPipeBlock(this);
        }
        registerDefaultState(defaultBlockState().setValue(POWERED, false).setValue(WATERLOGGED, false).setValue(CRAFTING, false));
    }

    public ItemPipe getPipe() {
        return pipe;
    }

    @Override
    public void appendCreativeMenuVariants(List<ItemStack> variants, ItemStack baseStack) {
        ItemPipe pipe = getPipe();
        if (pipe != null) {
            pipe.appendCreativeMenuVariants(variants, baseStack);
        }
    }

    @Override
    public ModularPipe modularPipe() {
        return pipe;
    }

    @Override
    public PipeFamily family() {
        return PipeFamily.ITEM;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Drop in-transit items + chassis modules and detach from the network for any removal
        // (player break, explosion, /setblock), while the block entity is still present.
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity) {
                pipeEntity.onPipeRemoved(pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, WATERLOGGED, CRAFTING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * Route item use interactions to pipe modules before default block handling.
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (pipe == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof PipeBlockEntity pipeEntity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        PipeContext pipeContext = new PipeContext(world, pos, state, pipeEntity);
        InteractionResult result = pipe.onUseWithItem(pipeContext, new UseOnContext(player, hand, hit));
        if (result != InteractionResult.PASS) {
            // Convert InteractionResult to ItemInteractionResult
            return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
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
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (getConnectionType(world, pos, dir) != PipeConnection.Type.NONE) {
                mask |= ConnectedShapeCache.bit(dir);
            }
        }
        return SHAPE_CACHE.get(mask);
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
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, world, pos, oldState, movedByPiston);

        // Join or create network when pipe is placed
        if (!world.isClientSide() && !oldState.is(state.getBlock())) {
            NetworkRegistry.getOrCreateNetwork(world, pos);
        }
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            net.minecraft.world.level.LevelAccessor world,
            BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        return state;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level world,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean notify) {
        if (!world.isClientSide()) {
            boolean powered = world.hasNeighborSignal(pos);
            if (powered != state.getValue(POWERED)) {
                world.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            }

            // Invalidate connection cache when neighbors change
            if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity) {
                pipeEntity.invalidateConnectionCache();
            }

            // Update network membership when neighbors change
            NetworkRegistry.getOrCreateNetwork(world, pos);
        }
        super.neighborChanged(state, world, pos, block, fromPos, notify);
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
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(world, pos, state);

        // Copy components from block entity to preserve weathering state on pick-block.
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
        // A settled cache answers on either side — a plain array read, maintained every tick and
        // persisted, so it is as good on the server as on the client. The dynamic path below costs
        // up to three capability lookups and a PipeContext per direction, and getShape asks for all
        // six on every entity collision and pathfinding node near a pipe.
        //
        // Only when the cache is clean: a pipe placed this tick still holds the all-NONE array it
        // was constructed with, and the network graph is built before its first tick, so trusting a
        // dirty cache would leave every fresh pipe in a network of its own.
        //
        // It also answers where the dynamic path cannot: a BlockGetter that is not a Level — the
        // PathNavigationRegion mob pathfinding runs on — makes getDynamicConnectionType return NONE
        // for everything, which is why pathfinding used to see pipes as a bare core with no arms.
        if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity
                && !pipeEntity.isConnectionCacheDirty()) {
            return pipeEntity.getCachedConnectionType(direction);
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

            // Fluid provider/supplier pipes also connect to an adjacent fluid tank (renders an arm to it).
            result = checkFluidStorage(actualWorld, pos, neighborPos, direction, neighborBlock);
            if (result != null) return result;
        }

        return PipeConnection.Type.NONE;
    }

    /**
     * Connect to a neighbor exposing fluid storage, but only when this pipe opts in
     * ({@link Pipe#connectsToFluidStorage}). Returns INVENTORY (so an arm renders) or null.
     */
    @Nullable private PipeConnection.Type checkFluidStorage(
            Level world, BlockPos pos, BlockPos neighborPos, Direction direction, Block neighborBlock) {
        if (pipe == null) {
            return null;
        }
        PipeBlockEntity pipeEntity =
                world.getBlockEntity(pos) instanceof PipeBlockEntity blockEntity ? blockEntity : null;
        PipeContext context =
                pipeEntity != null ? new PipeContext(world, pos, world.getBlockState(pos), pipeEntity) : null;
        if (!pipe.connectsToFluidStorage(context)) {
            return null;
        }
        if (FluidStorageLookup.find(world, neighborPos, direction.getOpposite()) == null) {
            return null;
        }
        return pipe.filterConnection(context, direction, neighborBlock, PipeConnection.Type.INVENTORY);
    }

    /**
     * Check for a PipeConnectionRegistry.SIDED registration on the neighboring block.
     * Returns the PipeConnection.Type declared by the neighbor, or null if not registered.
     */
    @Nullable private PipeConnection.Type checkPipeConnection(
            Level world, BlockPos pos, BlockPos neighborPos, Direction direction, Block neighborBlock) {
        var connectable = PipeConnectionLookup.find(world, neighborPos, direction.getOpposite());
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
     * Check for item storage on the neighboring block (legacy inventory support).
     * Returns INVENTORY if found, null otherwise.
     */
    @Nullable private PipeConnection.Type checkItemStorage(
            Level world, BlockPos pos, BlockPos neighborPos, Direction direction, Block neighborBlock) {
        var storage = ItemStorageLookup.find(world, neighborPos, direction.getOpposite());

        if (storage != null) {
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
