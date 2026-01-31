package com.logistics.power.engine.block;

import static com.logistics.power.engine.block.entity.AbstractEngineBlockEntity.STAGE;

import com.logistics.core.lib.block.Wrenchable;
import com.logistics.power.engine.block.entity.AbstractEngineBlockEntity.HeatStage;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.registry.PowerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

/**
 * Redstone Engine - converts redstone signals to MJ energy.
 * The simplest engine type that outputs 0.05 MJ/t (1 MJ/s) when powered.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>FACING property determines output direction (where energy is pushed)</li>
 *   <li>Only responds to direct redstone signals (levers, buttons) not dust</li>
 *   <li>Has a small internal buffer (10 MJ) and stalls when full</li>
 * </ul>
 */
public class RedstoneEngineBlock extends BlockWithEntity implements Wrenchable {
    public static final MapCodec<RedstoneEngineBlock> CODEC = createCodec(RedstoneEngineBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    public RedstoneEngineBlock(Settings settings) {
        super(settings.strength(3.5f)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque()
                .solidBlock((state, world, pos) -> false)
                .suffocates((state, world, pos) -> false)
                .blockVision((state, world, pos) -> false));
        setDefaultState(getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false)
                .with(STAGE, HeatStage.COLD));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, STAGE);
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
        // Output faces the direction the player is looking
        Direction facing = ctx.getPlayerLookDirection();
        boolean powered = hasDirectRedstonePower(ctx.getWorld(), ctx.getBlockPos());
        return getDefaultState().with(FACING, facing).with(POWERED, powered).with(STAGE, HeatStage.COLD);
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, PowerBlockEntities.REDSTONE_ENGINE_BLOCK_ENTITY, RedstoneEngineBlockEntity::tick);
    }

    @Override
    public ActionResult onWrench(World world, BlockPos pos, PlayerEntity player) {
        if (player.isSneaking()) {
            return ActionResult.PASS;
        }
        if (!world.isClient()) {
            BlockState state = world.getBlockState(pos);
            Direction currentFacing = state.get(FACING);
            Direction newFacing = getNextDirection(currentFacing);
            world.setBlockState(pos, state.with(FACING, newFacing), Block.NOTIFY_ALL);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * Gets the next direction in the rotation cycle (cycles through all 6 directions).
     */
    private static Direction getNextDirection(Direction current) {
        Direction[] directions = Direction.values();
        return directions[(current.ordinal() + 1) % directions.length];
    }

    @Override
    protected void neighborUpdate(
            BlockState state,
            World world,
            BlockPos pos,
            Block block,
            @Nullable WireOrientation wireOrientation,
            boolean notify) {
        if (!world.isClient()) {
            boolean powered = hasDirectRedstonePower(world, pos);
            if (powered != state.get(POWERED)) {
                world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_LISTENERS);
            }
        }
        super.neighborUpdate(state, world, pos, block, wireOrientation, notify);
    }

    /**
     * Check if the block has direct redstone power (from levers, buttons, etc.)
     * but not from redstone dust passing by.
     */
    private boolean hasDirectRedstonePower(World world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            // Get emitted redstone power directly from neighbors
            // This excludes weak power from dust and only counts strong power sources
            int power = world.getEmittedRedstonePower(pos.offset(direction), direction);
            if (power > 0) {
                return true;
            }
        }
        return false;
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
     * Gets the direction this engine outputs energy to.
     */
    public static Direction getOutputDirection(BlockState state) {
        return state.get(FACING);
    }
}
