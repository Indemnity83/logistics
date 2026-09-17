package com.logistics.power.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.LogisticsPower;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
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
    public static final BooleanProperty LIT = BlockStateProperties.LIT; // True when burning fuel

    public StirlingEngineBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
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
    public InteractionResult onWrench(Level world, BlockPos pos, Player player) {
        // Reset overheat if engine is overheated
        if (world.getBlockEntity(pos) instanceof StirlingEngineBlockEntity engine && engine.isOverheated()) {
            if (!world.isClientSide()) {
                engine.resetOverheat();
            }
            return InteractionResult.SUCCESS;
        }

        return super.onWrench(world, pos, player);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StirlingEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, StirlingEngineBlockEntity::tick);
    }
}
