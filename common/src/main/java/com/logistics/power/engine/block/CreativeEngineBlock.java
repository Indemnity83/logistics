package com.logistics.power.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.LogisticsPower;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Creative Engine - a special engine for Creative Mode that generates configurable energy.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>FACING property determines output direction (where energy is pushed)</li>
 *   <li>Requires redstone signal to function</li>
 *   <li>Default output: 20 RF/t</li>
 *   <li>Sneak + right-click with wrench doubles output rate (up to 1280 RF/t)</li>
 *   <li>Cannot overheat - always safe to use</li>
 * </ul>
 */
public class CreativeEngineBlock extends AbstractEngineBlock<CreativeEngineBlockEntity> {
    public CreativeEngineBlock(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult onWrench(Level world, BlockPos pos, Player player) {
        // Sneak + wrench: cycle output level
        if (player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof CreativeEngineBlockEntity engine) {
            if (!world.isClientSide()) {
                long newRate = engine.cycleOutputLevel();
                player.sendSystemMessage(Component.translatable("message.logistics.power.creative_engine.output", newRate));
            }
            return InteractionResult.SUCCESS;
        }

        return super.onWrench(world, pos, player);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, CreativeEngineBlockEntity::tick);
    }
}
