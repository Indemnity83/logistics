package com.logistics.quarry;

import com.logistics.quarry.entity.QuarryBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

/**
 * Quarry frame block - forms the outline of the quarry mining area.
 * Uses BlockState properties to show arms extending in each direction.
 * Does not connect to pipes or have a block entity.
 */
public class QuarryFrameBlock extends Block {
    public static final MapCodec<QuarryFrameBlock> CODEC = createCodec(QuarryFrameBlock::new);

    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty WEST = BooleanProperty.of("west");
    public static final BooleanProperty UP = BooleanProperty.of("up");
    public static final BooleanProperty DOWN = BooleanProperty.of("down");

    private static final VoxelShape CORE = Block.createCuboidShape(5, 5, 5, 11, 11, 11);
    private static final VoxelShape ARM_NORTH = Block.createCuboidShape(5, 5, 0, 11, 11, 5);
    private static final VoxelShape ARM_SOUTH = Block.createCuboidShape(5, 5, 11, 11, 11, 16);
    private static final VoxelShape ARM_EAST = Block.createCuboidShape(11, 5, 5, 16, 11, 11);
    private static final VoxelShape ARM_WEST = Block.createCuboidShape(0, 5, 5, 5, 11, 11);
    private static final VoxelShape ARM_UP = Block.createCuboidShape(5, 11, 5, 11, 16, 11);
    private static final VoxelShape ARM_DOWN = Block.createCuboidShape(5, 0, 5, 11, 5, 11);

    public QuarryFrameBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
                .with(NORTH, false)
                .with(SOUTH, false)
                .with(EAST, false)
                .with(WEST, false)
                .with(UP, false)
                .with(DOWN, false));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE;

        if (state.get(NORTH)) shape = VoxelShapes.union(shape, ARM_NORTH);
        if (state.get(SOUTH)) shape = VoxelShapes.union(shape, ARM_SOUTH);
        if (state.get(EAST)) shape = VoxelShapes.union(shape, ARM_EAST);
        if (state.get(WEST)) shape = VoxelShapes.union(shape, ARM_WEST);
        if (state.get(UP)) shape = VoxelShapes.union(shape, ARM_UP);
        if (state.get(DOWN)) shape = VoxelShapes.union(shape, ARM_DOWN);

        return shape;
    }

    @Nullable @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // When placed manually, don't connect to anything
        // The quarry will place these with the correct connections
        return getDefaultState();
    }

    /**
     * Create a block state with the specified arms enabled.
     */
    public BlockState withArms(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
        return getDefaultState()
                .with(NORTH, north)
                .with(SOUTH, south)
                .with(EAST, east)
                .with(WEST, west)
                .with(UP, up)
                .with(DOWN, down);
    }

    /**
     * Get the property for a given direction.
     */
    public static BooleanProperty getArmProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return true;
    }

    /**
     * Random tick - check if there's an owning quarry, decay if not.
     * Similar to how leaves decay when not connected to logs.
     */
    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!hasOwningQuarry(world, pos)) {
            // No quarry found - decay this frame block (no drops)
            world.removeBlock(pos, false);
        }
    }

    /**
     * Check if there's a quarry that owns this frame block.
     * Searches for quarries within range and checks if this position is part of their frame.
     */
    private boolean hasOwningQuarry(ServerWorld world, BlockPos framePos) {
        int searchRadius = 64; // Support large custom bounds

        for (BlockPos quarryPos : QuarryBlockEntity.getActiveQuarries(world)) {
            if (Math.abs(quarryPos.getX() - framePos.getX()) > searchRadius) continue;
            if (Math.abs(quarryPos.getZ() - framePos.getZ()) > searchRadius) continue;

            int dy = framePos.getY() - quarryPos.getY();
            if (dy < 0 || dy > QuarryConfig.Y_OFFSET_ABOVE) continue;

            BlockState checkState = world.getBlockState(quarryPos);
            if (!(checkState.getBlock() instanceof QuarryBlock)) continue;

            if (isFramePositionForQuarry(world, quarryPos, checkState, framePos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a frame position belongs to a specific quarry.
     */
    private boolean isFramePositionForQuarry(
            ServerWorld world, BlockPos quarryPos, BlockState quarryState, BlockPos framePos) {
        // Get frame bounds - check if quarry has custom bounds
        int startX;
        int startZ;
        int endX;
        int endZ;

        BlockEntity entity = world.getBlockEntity(quarryPos);
        if (entity instanceof QuarryBlockEntity quarry && quarry.hasCustomBounds()) {
            // Use custom bounds from the quarry
            startX = quarry.getCustomMinX();
            startZ = quarry.getCustomMinZ();
            endX = quarry.getCustomMaxX();
            endZ = quarry.getCustomMaxZ();
        } else {
            // Calculate default bounds from facing direction
            Direction facing = QuarryBlock.getMiningDirection(quarryState);
            switch (facing) {
                case NORTH:
                    startX = quarryPos.getX() - 8;
                    startZ = quarryPos.getZ() - QuarryConfig.CHUNK_SIZE;
                    break;
                case SOUTH:
                    startX = quarryPos.getX() - 8;
                    startZ = quarryPos.getZ() + 1;
                    break;
                case EAST:
                    startX = quarryPos.getX() + 1;
                    startZ = quarryPos.getZ() - 8;
                    break;
                case WEST:
                    startX = quarryPos.getX() - QuarryConfig.CHUNK_SIZE;
                    startZ = quarryPos.getZ() - 8;
                    break;
                default:
                    return false;
            }
            endX = startX + QuarryConfig.CHUNK_SIZE - 1;
            endZ = startZ + QuarryConfig.CHUNK_SIZE - 1;
        }

        int bottomY = quarryPos.getY();
        int topY = quarryPos.getY() + QuarryConfig.Y_OFFSET_ABOVE;

        int fx = framePos.getX();
        int fy = framePos.getY();
        int fz = framePos.getZ();

        // Check if framePos is on the frame perimeter
        // Frame consists of: bottom ring, corner pillars, top ring

        // Check Y level
        if (fy < bottomY || fy > topY) {
            return false;
        }

        // Bottom or top ring (Y = bottomY or Y = topY)
        if (fy == bottomY || fy == topY) {
            // Must be on the perimeter
            boolean onNorthEdge = (fz == startZ) && (fx >= startX && fx <= endX);
            boolean onSouthEdge = (fz == endZ) && (fx >= startX && fx <= endX);
            boolean onWestEdge = (fx == startX) && (fz >= startZ && fz <= endZ);
            boolean onEastEdge = (fx == endX) && (fz >= startZ && fz <= endZ);

            return onNorthEdge || onSouthEdge || onWestEdge || onEastEdge;
        }

        // Middle pillars (Y between bottom and top) - only at corners
        boolean isCorner = (fx == startX || fx == endX) && (fz == startZ || fz == endZ);
        return isCorner;
    }
}
