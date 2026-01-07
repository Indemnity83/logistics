package com.logistics.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class IronPipeBlock extends PipeBlock {
    // The output direction for directed flow
    public static final EnumProperty<Direction> OUTPUT_DIRECTION = Properties.FACING;

    public IronPipeBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(OUTPUT_DIRECTION, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OUTPUT_DIRECTION);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state == null) return null;

        // Set initial output direction based on player facing
        Direction playerFacing = ctx.getHorizontalPlayerFacing();

        // Find the first connected direction, starting with player facing
        Direction outputDir = playerFacing;
        boolean foundConnection = false;

        // Check if player facing direction has a connection
        if (hasConnection(state, playerFacing)) {
            outputDir = playerFacing;
            foundConnection = true;
        } else {
            // Otherwise find first connected direction
            for (Direction dir : Direction.values()) {
                if (hasConnection(state, dir)) {
                    outputDir = dir;
                    foundConnection = true;
                    break;
                }
            }
        }

        return state.with(OUTPUT_DIRECTION, outputDir);
    }

    /**
     * Called by the wrench to cycle through output directions
     */
    public void cycleOutputDirection(World world, BlockPos pos) {
        if (world.isClient) return;

        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof IronPipeBlock)) return;

        Direction currentOutput = state.get(OUTPUT_DIRECTION);

        // Find all connected directions
        Direction[] validDirections = Direction.values();
        int currentIndex = -1;

        // Find current output in the array
        for (int i = 0; i < validDirections.length; i++) {
            if (validDirections[i] == currentOutput) {
                currentIndex = i;
                break;
            }
        }

        // Cycle to next connected direction
        int nextIndex = (currentIndex + 1) % validDirections.length;
        int attempts = 0;

        while (attempts < validDirections.length) {
            Direction nextDir = validDirections[nextIndex];

            // Check if this direction has a connection
            if (hasConnection(state, nextDir)) {
                world.setBlockState(pos, state.with(OUTPUT_DIRECTION, nextDir));
                // Sync to clients
                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.updateListeners(pos, state, state.with(OUTPUT_DIRECTION, nextDir), Block.NOTIFY_ALL);
                }
                return;
            }

            nextIndex = (nextIndex + 1) % validDirections.length;
            attempts++;
        }
    }

    /**
     * Get the output direction for this iron pipe
     */
    public Direction getOutputDirection(BlockState state) {
        return state.get(OUTPUT_DIRECTION);
    }

    /**
     * Check if items can enter from a given direction (backflow prevention)
     */
    public boolean canAcceptFromDirection(BlockState state, Direction fromDirection) {
        Direction output = getOutputDirection(state);
        // Cannot accept items from the output direction (backflow prevention)
        return fromDirection != output;
    }
}
