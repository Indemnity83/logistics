package com.logistics.power.cable;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.ConnectedShapeCache;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.power.DirectEnergyReceiver;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.LowTierEnergySource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import org.jetbrains.annotations.Nullable;

/**
 * Cable block that connects to engines, machines, and other cables
 * to transfer RF energy across distances.
 *
 * <p>Uses the same visual shape system as pipes (8px core + directional arms)
 * and connects to any block exposing energy storage via the loader's capability system.
 */
public class CableBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final double CABLE_SIZE = 4.0;

    private static final VoxelShape CORE_SHAPE = Block.box(
            8 - CABLE_SIZE / 2, 8 - CABLE_SIZE / 2, 8 - CABLE_SIZE / 2,
            8 + CABLE_SIZE / 2, 8 + CABLE_SIZE / 2, 8 + CABLE_SIZE / 2);

    private static final VoxelShape NORTH_SHAPE = Block.box(
            8 - CABLE_SIZE / 2, 8 - CABLE_SIZE / 2, 0,
            8 + CABLE_SIZE / 2, 8 + CABLE_SIZE / 2, 8 - CABLE_SIZE / 2);
    private static final VoxelShape SOUTH_SHAPE = Block.box(
            8 - CABLE_SIZE / 2, 8 - CABLE_SIZE / 2, 8 + CABLE_SIZE / 2,
            8 + CABLE_SIZE / 2, 8 + CABLE_SIZE / 2, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(
            8 + CABLE_SIZE / 2, 8 - CABLE_SIZE / 2, 8 - CABLE_SIZE / 2,
            16, 8 + CABLE_SIZE / 2, 8 + CABLE_SIZE / 2);
    private static final VoxelShape WEST_SHAPE = Block.box(
            0, 8 - CABLE_SIZE / 2, 8 - CABLE_SIZE / 2,
            8 - CABLE_SIZE / 2, 8 + CABLE_SIZE / 2, 8 + CABLE_SIZE / 2);
    private static final VoxelShape UP_SHAPE = Block.box(
            8 - CABLE_SIZE / 2, 8 + CABLE_SIZE / 2, 8 - CABLE_SIZE / 2,
            8 + CABLE_SIZE / 2, 16, 8 + CABLE_SIZE / 2);
    private static final VoxelShape DOWN_SHAPE = Block.box(
            8 - CABLE_SIZE / 2, 0, 8 - CABLE_SIZE / 2,
            8 + CABLE_SIZE / 2, 8 - CABLE_SIZE / 2, 8 + CABLE_SIZE / 2);

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

    private final CableTier tier;

    public CableBlock(Properties settings) {
        this(settings, CableTier.COPPER);
    }

    public CableBlock(Properties settings, CableTier tier) {
        super(settings);
        this.tier = tier;
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    public CableTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, LogisticsPower.ENTITY.CABLE_BLOCK_ENTITY, CableBlockEntity::tick);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (getConnectionType(world, pos, dir) != ConnectionType.NONE) {
                mask |= ConnectedShapeCache.bit(dir);
            }
        }
        return SHAPE_CACHE.get(mask);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(
            BlockState state, LevelReader world, ScheduledTickAccess tickView,
            BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        invalidateConnections(world, pos);
        return state;
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level world, BlockPos pos, Block block,
        @Nullable Orientation orientation, boolean notify) {
        invalidateConnections(world, pos);
        super.neighborChanged(state, world, pos, block, orientation, notify);
    }

    private void invalidateConnections(LevelReader world, BlockPos pos) {
        if (!(world instanceof Level level)) return;
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.invalidateConnectionCache();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
            if (!level.isClientSide()) {
                CableNetworkManager.get(level).markDirty();
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        removeCableFromNetwork(world, pos);
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel world, BlockPos pos, boolean movedByPiston) {
        removeCableFromNetwork(world, pos);
        super.affectNeighborsAfterRemoval(state, world, pos, movedByPiston);
    }

    private void removeCableFromNetwork(Level world, BlockPos pos) {
        if (!world.isClientSide()) {
            CableNetworkManager.get(world).removeCable(pos);
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /**
     * Determines the connection type for a cable in the given direction.
     * Cables connect to other cables and any block exposing EnergyStorage.
     */
    public ConnectionType getConnectionType(BlockGetter world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            return cable.getCachedConnectionType(direction);
        }
        return getDynamicConnectionType(world, pos, direction);
    }

    public ConnectionType getDynamicConnectionType(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);

        if (!(world instanceof Level level)) {
            return ConnectionType.NONE;
        }
        if (!level.isLoaded(neighborPos)) {
            return ConnectionType.NONE;
        }

        // Connect to other cables
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof CableBlockEntity) {
            return ConnectionType.CABLE;
        }

        // Extraction pipes / pump are engine-powered only; cables never connect to them.
        if (neighbor instanceof DirectEnergyReceiver) {
            return ConnectionType.NONE;
        }

        // Bufferless engines still need a visual connection, but only on their output face.
        // Low-tier engines are intentionally isolated from the cable network.
        if (neighbor instanceof EngineEntity engine) {
            if (engine instanceof LowTierEnergySource
                    || AbstractEngineBlock.getOutputDirection(engine.getBlockState()) != direction.getOpposite()) {
                return ConnectionType.NONE;
            }
            return ConnectionType.DEVICE;
        }

        if (neighbor instanceof HasEnergyStorage energyBlock
                && energyBlock.energyStorage(direction.getOpposite()) != null) {
            return ConnectionType.DEVICE;
        }

        // Connect to anything with energy storage (engines, machines, pipes with energy)
        var storage = EnergyCapabilityLookup.INSTANCE.find(level, neighborPos, direction.getOpposite());
        if (storage != null && (storage.canInsert() || storage.canExtract())) {
            return ConnectionType.DEVICE;
        }

        return ConnectionType.NONE;
    }

    /**
     * Types of cable connections, used for rendering different arm models.
     */
    public enum ConnectionType {
        NONE,
        CABLE,
        DEVICE
    }
}
