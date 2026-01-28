package com.logistics.automation.quarry;

import com.logistics.automation.quarry.entity.QuarryBlockEntity;
import com.logistics.automation.registry.AutomationBlockEntities;
import com.logistics.core.marker.MarkerManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class QuarryBlock extends BaseEntityBlock {
    public static final MapCodec<QuarryBlock> CODEC = simpleCodec(QuarryBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public QuarryBlock(Properties settings) {
        super(settings.strength(3.5f).sound(SoundType.STONE));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public SoundType getSoundType(BlockState state) {
        return SoundType.STONE;
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // FACING is the direction the quarry mines (the direction the player is looking)
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof QuarryBlockEntity quarry) {
                player.openMenu(quarry);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuarryBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(
            Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);

        if (!world.isClientSide()) {
            // Check for adjacent marker-defined area
            MarkerManager.MarkerBounds bounds = MarkerManager.findAdjacentMarkerBounds(world, pos);
            if (bounds != null) {
                BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof QuarryBlockEntity quarry) {
                    // Set custom bounds (2D only - X and Z from markers, Y derived from quarry position)
                    quarry.setCustomBounds(
                            bounds.min().getX(),
                            bounds.min().getZ(),
                            bounds.max().getX(),
                            bounds.max().getZ());

                    // Break all markers in the configuration
                    MarkerManager.breakMarkers(world, bounds.allMarkers());
                }
            }
        }
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, AutomationBlockEntities.QUARRY_BLOCK_ENTITY, QuarryBlockEntity::tick);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * Gets the direction the quarry will mine in (behind the block).
     */
    public static Direction getMiningDirection(BlockState state) {
        return state.getValue(FACING);
    }
}
