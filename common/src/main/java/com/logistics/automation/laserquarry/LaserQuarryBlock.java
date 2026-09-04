package com.logistics.automation.laserquarry;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsAutomation;

import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.LogisticsAutomation;
import com.logistics.core.marker.MarkerManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class LaserQuarryBlock extends BaseEntityBlock {
    public static final MapCodec<LaserQuarryBlock> CODEC = simpleCodec(LaserQuarryBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public LaserQuarryBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Tear down the frame for any real removal (player break, explosion, /setblock), while the
        // block entity is still present. BlockEntity has no preRemoveSideEffects on this version.
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof LaserQuarryBlockEntity quarryEntity) {
                quarryEntity.preRemoveSideEffects(pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // FACING is the direction the quarry mines (the direction the player is looking)
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserQuarryBlockEntity(pos, state);
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
                if (entity instanceof LaserQuarryBlockEntity quarry) {
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

            // Show action bar message with mining area dimensions
            if (placer instanceof ServerPlayer serverPlayer) {
                int width, depth;
                if (bounds != null) {
                    width = Math.max(0, bounds.max().getX() - bounds.min().getX() - 1);
                    depth = Math.max(0, bounds.max().getZ() - bounds.min().getZ() - 1);
                } else {
                    width = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) - 2;
                    depth = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) - 2;
                }
                serverPlayer.sendSystemMessage(
                        Component.translatable("laser_quarry.area_preview", width, depth), true);
            }
        }
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntity::tick);
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
