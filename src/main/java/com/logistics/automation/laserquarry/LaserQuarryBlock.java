package com.logistics.automation.laserquarry;

import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.automation.registry.AutomationBlockEntities;
import com.logistics.core.lib.block.Probeable;
import com.logistics.core.lib.support.ProbeResult;
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
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class LaserQuarryBlock extends BlockWithEntity implements Probeable {
    public static final MapCodec<LaserQuarryBlock> CODEC = createCodec(LaserQuarryBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public LaserQuarryBlock(Settings settings) {
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

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LaserQuarryBlockEntity(pos, state);
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
        }
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, AutomationBlockEntities.LASER_QUARRY_BLOCK_ENTITY, LaserQuarryBlockEntity::tick);
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

    @Override
    public ProbeResult onProbe(World world, BlockPos pos, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof LaserQuarryBlockEntity quarry) {
            return quarry.getProbeResult();
        }
        return null;
    }
}
