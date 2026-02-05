package com.logistics.power.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.power.registry.PowerBlockEntities;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
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
    public static final MapCodec<StirlingEngineBlock> CODEC = simpleCodec(StirlingEngineBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT; // True when burning fuel

    public StirlingEngineBlock(Properties settings) {
        super(settings, SoundType.COPPER);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected List<Property<?>> getAdditionalProperties() {
        return List.of(LIT);
    }

    @Override
    protected BlockState applyAdditionalPlacementState(BlockState base, BlockPlaceContext ctx) {
        return base.setValue(LIT, false);
    }

    @Override
    protected StirlingEngineBlockEntity getEngineBlockEntity(BlockEntity be) {
        return be instanceof StirlingEngineBlockEntity ? (StirlingEngineBlockEntity) be : null;
    }

    @Override
    protected boolean handleSpecialWrench(Level world, BlockPos pos, Player player, BlockState state) {
        // Reset overheat if engine is overheated
        if (world.getBlockEntity(pos) instanceof StirlingEngineBlockEntity engine && engine.isOverheated()) {
            if (!world.isClientSide()) {
                engine.resetOverheat();
            }
            return true;
        }
        return false;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StirlingEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PowerBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY, StirlingEngineBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return openGui(world, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        // Empty hand: open GUI
        return openGui(world, pos, player);
    }

    private InteractionResult openGui(Level world, BlockPos pos, Player player) {
        if (!world.isClientSide()) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof StirlingEngineBlockEntity stirlingEngine) {
                player.openMenu(stirlingEngine);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
