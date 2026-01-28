package com.logistics.core.marker;

import com.logistics.core.registry.CoreItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A marker block used to define custom quarry mining areas.
 * Place 3 markers in an L-shape on the ground and activate with a wrench
 * to create a bounding box that a quarry can use.
 */
public class MarkerBlock extends BaseEntityBlock {
    public static final MapCodec<MarkerBlock> CODEC = simpleCodec(MarkerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    // Blue particle color for active markers
    private static final int ACTIVE_PARTICLE_COLOR = 0x0132FD;

    // Torch-like shape matching vanilla torch dimensions
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 10.0, 10.0);

    public MarkerBlock(Properties settings) {
        super(settings.strength(0.0f)
                .sound(SoundType.GLASS)
                .noCollision()
                .lightLevel(state -> state.getValue(ACTIVE) ? 7 : 0));
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
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
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(ACTIVE, false);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        // Must be placed on a solid block
        BlockPos below = pos.below();
        return world.getBlockState(below).isFaceSturdy(world, below, Direction.UP);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        // Only activate with wrench
        if (!CoreItems.isWrench(player.getMainHandItem()) && !CoreItems.isWrench(player.getOffhandItem())) {
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
            world.addParticle(new DustParticleOptions(ACTIVE_PARTICLE_COLOR, 1.0f), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
