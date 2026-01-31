package com.logistics.power.engine.block;

import static com.logistics.power.engine.block.entity.AbstractEngineBlockEntity.STAGE;

import com.logistics.core.lib.block.Probeable;
import com.logistics.core.lib.block.Wrenchable;
import com.logistics.core.lib.support.ProbeResult;
import com.logistics.power.engine.block.entity.AbstractEngineBlockEntity.HeatStage;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
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
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

/**
 * Stirling Engine - converts fuel to energy.
 * A more powerful engine that requires fuel (coal, charcoal, etc.) to operate.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>FACING property determines output direction (where energy is pushed)</li>
 *   <li>Requires redstone signal to operate</li>
 *   <li>Burns fuel items to generate 3-10 RF/t (PID-controlled)</li>
 *   <li>Has a GUI for adding fuel</li>
 *   <li>Thermal shutdown at 250°C if output is blocked (no explosion)</li>
 * </ul>
 */
public class StirlingEngineBlock extends BlockWithEntity implements Probeable, Wrenchable {
    public static final MapCodec<StirlingEngineBlock> CODEC = createCodec(StirlingEngineBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final BooleanProperty LIT = Properties.LIT; // True when burning fuel

    public StirlingEngineBlock(Settings settings) {
        super(settings.strength(3.5f)
                .sounds(BlockSoundGroup.COPPER)
                .nonOpaque()
                .solidBlock((state, world, pos) -> false)
                .suffocates((state, world, pos) -> false)
                .blockVision((state, world, pos) -> false));
        setDefaultState(getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false)
                .with(LIT, false)
                .with(STAGE, HeatStage.COLD));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, LIT, STAGE);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return BlockSoundGroup.COPPER;
    }

    @Nullable @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getPlayerLookDirection();
        boolean powered = hasDirectRedstonePower(ctx.getWorld(), ctx.getBlockPos());
        return getDefaultState()
                .with(FACING, facing)
                .with(POWERED, powered)
                .with(LIT, false)
                .with(STAGE, HeatStage.COLD);
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StirlingEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, PowerBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY, StirlingEngineBlockEntity::tick);
    }

    @Override
    public ProbeResult onProbe(World world, BlockPos pos, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof StirlingEngineBlockEntity engine) {
            return engine.getProbeResult();
        }
        return null;
    }

    @Override
    public ActionResult onWrench(World world, BlockPos pos, PlayerEntity player) {
        // Reset overheat if engine is overheated
        if (world.getBlockEntity(pos) instanceof StirlingEngineBlockEntity engine && engine.isOverheated()) {
            if (!world.isClient()) {
                engine.resetOverheat();
            }
            return ActionResult.SUCCESS;
        }

        // Normal wrench: rotate facing
        if (!world.isClient()) {
            BlockState state = world.getBlockState(pos);
            Direction currentFacing = state.get(FACING);
            Direction newFacing = getNextDirection(currentFacing);
            world.setBlockState(pos, state.with(FACING, newFacing), Block.NOTIFY_ALL);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUseWithItem(
            ItemStack stack,
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            BlockHitResult hit) {
        return openGui(world, pos, player);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Empty hand: open GUI
        return openGui(world, pos, player);
    }

    private ActionResult openGui(World world, BlockPos pos, PlayerEntity player) {
        if (!world.isClient()) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof StirlingEngineBlockEntity stirlingEngine) {
                player.openHandledScreen(stirlingEngine);
            }
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
