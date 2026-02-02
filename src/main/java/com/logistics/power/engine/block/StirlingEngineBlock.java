package com.logistics.power.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.power.registry.PowerBlockEntities;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
public class StirlingEngineBlock extends AbstractEngineBlock<StirlingEngineBlockEntity> {
    public static final MapCodec<StirlingEngineBlock> CODEC = createCodec(StirlingEngineBlock::new);
    public static final BooleanProperty LIT = Properties.LIT; // True when burning fuel

    public StirlingEngineBlock(Settings settings) {
        super(settings, BlockSoundGroup.COPPER);
        setDefaultState(getDefaultState().with(LIT, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected List<Property<?>> getAdditionalProperties() {
        return List.of(LIT);
    }

    @Override
    protected BlockState applyAdditionalPlacementState(BlockState base, ItemPlacementContext ctx) {
        return base.with(LIT, false);
    }

    @Override
    protected StirlingEngineBlockEntity getEngineBlockEntity(BlockEntity be) {
        return be instanceof StirlingEngineBlockEntity ? (StirlingEngineBlockEntity) be : null;
    }

    @Override
    protected boolean handleSpecialWrench(World world, BlockPos pos, PlayerEntity player, BlockState state) {
        // Reset overheat if engine is overheated
        if (world.getBlockEntity(pos) instanceof StirlingEngineBlockEntity engine && engine.isOverheated()) {
            if (!world.isClient()) {
                engine.resetOverheat();
            }
            return true;
        }
        return false;
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
}
