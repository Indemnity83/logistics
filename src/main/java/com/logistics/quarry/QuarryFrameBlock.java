package com.logistics.quarry;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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

    // Shape constants (similar to pipes)
    private static final double MIN = 5.0 / 16.0;
    private static final double MAX = 11.0 / 16.0;

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

    @Nullable
    @Override
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
}
