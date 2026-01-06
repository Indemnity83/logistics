package com.logistics.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Gold pipes accelerate items when powered by redstone.
 * Max speed is VERY high - items can reach incredible speeds with a long chain of powered gold pipes.
 * Unpowered gold pipes don't accelerate but maintain the high max speed (items decelerate slowly).
 * When items enter slower pipes, they decelerate quickly back to that pipe's speed.
 */
public class GoldPipeBlock extends PipeBlock {
    public static final BooleanProperty POWERED = Properties.POWERED;

    // Gold pipes have a very high max speed - allows items to reach incredible speeds
    // At 1.0 blocks/tick, items traverse a segment in just 1 tick (0.05 seconds)
    private static final float GOLD_PIPE_SPEED = 1.0f; // Blocks per tick (VERY fast)

    public GoldPipeBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWERED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state != null) {
            boolean powered = ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos());

            // If not directly powered, check if any connected adjacent gold pipe is directly powered
            if (!powered) {
                powered = hasAdjacentPoweredGoldPipe(ctx.getWorld(), ctx.getBlockPos(), state);
            }

            state = state.with(POWERED, powered);
        }
        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                  net.minecraft.world.WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        state = super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);

        // Update powered state when neighbors change
        if (world instanceof World actualWorld) {
            boolean powered = actualWorld.isReceivingRedstonePower(pos);

            // If not directly powered, check if any connected adjacent gold pipe is directly powered
            // This allows powering 3 pipes in a row by powering the middle one
            if (!powered) {
                powered = hasAdjacentPoweredGoldPipe(actualWorld, pos, state);
            }

            state = state.with(POWERED, powered);
        }

        return state;
    }

    /**
     * Check if any connected adjacent gold pipe is receiving direct redstone power
     */
    private boolean hasAdjacentPoweredGoldPipe(World world, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            // Check if this direction is connected
            BooleanProperty property = getPropertyForDirection(dir);
            if (property != null && state.get(property)) {
                BlockPos adjacentPos = pos.offset(dir);
                BlockState adjacentState = world.getBlockState(adjacentPos);

                // If adjacent block is a gold pipe that's directly powered
                if (adjacentState.getBlock() instanceof GoldPipeBlock) {
                    if (world.isReceivingRedstonePower(adjacentPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static BooleanProperty getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> PipeBlock.NORTH;
            case SOUTH -> PipeBlock.SOUTH;
            case EAST -> PipeBlock.EAST;
            case WEST -> PipeBlock.WEST;
            case UP -> PipeBlock.UP;
            case DOWN -> PipeBlock.DOWN;
        };
    }

    @Override
    public float getPipeSpeed(World world, BlockPos pos, BlockState state) {
        // Always return the high max speed regardless of powered state
        // The powered state only controls whether items can accelerate toward this speed
        return GOLD_PIPE_SPEED;
    }

    @Override
    public boolean canAccelerate(World world, BlockPos pos, BlockState state) {
        // Only accelerate when powered
        return state.get(POWERED);
    }
}
