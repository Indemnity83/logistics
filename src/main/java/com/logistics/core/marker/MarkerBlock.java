package com.logistics.core.marker;

import com.logistics.core.lib.block.Wrenchable;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.particles.DustParticleOptions;
import org.jetbrains.annotations.Nullable;

/**
 * A marker block used to define custom quarry mining areas.
 * Place 3 markers in an L-shape on the ground and activate with a wrench
 * to create a bounding box that a quarry can use.
 */
public class MarkerBlock extends Block implements EntityBlock, Wrenchable {
    public static final MapCodec<MarkerBlock> CODEC = simpleCodec(MarkerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    // Blue particle color for active markers
    private static final int ACTIVE_PARTICLE_COLOR = 0x0132FD;

    // Torch-like shape matching vanilla torch dimensions
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 10.0, 10.0);

    public MarkerBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(ACTIVE) ? 7 : 0));
        this.registerDefaultState(this.getStateDefinition().any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(ACTIVE, false);
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader world, BlockPos pos) {
        // Must be placed on a solid block
        BlockPos below = pos.below();
        return world.getBlockState(below).isFaceSturdy(world, below, net.minecraft.core.Direction.UP);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(ACTIVE)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MarkerBlockEntity marker) {
                marker.deactivateConnectedMarkers();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult onWrench(Level world, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide()) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof MarkerBlockEntity marker) {
                marker.toggleActivation(player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarkerBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double y = pos.getY() + 0.7;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            // Blue particle color for active markers
            world.addParticle(new DustParticleOptions(ACTIVE_PARTICLE_COLOR, 1.0f), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
