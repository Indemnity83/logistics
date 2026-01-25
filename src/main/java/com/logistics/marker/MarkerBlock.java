package com.logistics.marker;

import com.logistics.item.LogisticsItems;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

/**
 * A marker block used to define custom quarry mining areas.
 * Place 3 markers in an L-shape on the ground and activate with a wrench
 * to create a bounding box that a quarry can use.
 */
public class MarkerBlock extends BlockWithEntity {
    public static final MapCodec<MarkerBlock> CODEC = createCodec(MarkerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    // Torch-like shape matching vanilla torch dimensions
    private static final VoxelShape SHAPE = Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, 10.0, 10.0);

    public MarkerBlock(Settings settings) {
        super(settings
                .strength(0.0f)
                .sounds(BlockSoundGroup.GLASS)
                .noCollision()
                .luminance(state -> state.get(ACTIVE) ? 7 : 0));
        setDefaultState(getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Nullable @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(ACTIVE, false);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // Must be placed on a solid block
        BlockPos below = pos.down();
        return world.getBlockState(below).isSideSolidFullSquare(world, below, net.minecraft.util.math.Direction.UP);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Only activate with wrench
        if (!LogisticsItems.isWrench(player.getMainHandStack()) && !LogisticsItems.isWrench(player.getOffHandStack())) {
            return ActionResult.PASS;
        }

        if (!world.isClient()) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof MarkerBlockEntity marker) {
                marker.toggleActivation(player);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MarkerBlockEntity(pos, state);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(ACTIVE)) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double y = pos.getY() + 0.7;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            world.addParticleClient(new DustParticleEffect(0x0132FD, 1.0f), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
