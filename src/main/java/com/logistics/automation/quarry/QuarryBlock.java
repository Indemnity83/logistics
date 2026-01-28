package com.logistics.automation.quarry;

import com.logistics.automation.quarry.entity.QuarryBlockEntity;
import com.logistics.automation.registry.AutomationBlockEntities;
import com.logistics.core.marker.MarkerManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class QuarryBlock extends BlockWithEntity {
    public static final MapCodec<QuarryBlock> CODEC = createCodec(QuarryBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public QuarryBlock(Settings settings) {
        super(settings.strength(3.5f).sounds(BlockSoundGroup.STONE));
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return BlockSoundGroup.STONE;
    }

    @Nullable @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // FACING is the direction the quarry mines (the direction the player is looking)
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof QuarryBlockEntity quarry) {
                player.openHandledScreen(quarry);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new QuarryBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(
            World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (!world.isClient()) {
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
            World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, AutomationBlockEntities.QUARRY_BLOCK_ENTITY, QuarryBlockEntity::tick);
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    /**
     * Gets the direction the quarry will mine in (behind the block).
     */
    public static Direction getMiningDirection(BlockState state) {
        return state.get(FACING);
    }
}
