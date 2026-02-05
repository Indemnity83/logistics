package com.logistics.power.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.registry.PowerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
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
    public static final MapCodec<CreativeEngineBlock> CODEC = simpleCodec(CreativeEngineBlock::new);

    public CreativeEngineBlock(Properties settings) {
        super(settings, SoundType.STONE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected CreativeEngineBlockEntity getEngineBlockEntity(BlockEntity be) {
        return be instanceof CreativeEngineBlockEntity ? (CreativeEngineBlockEntity) be : null;
    }

    @Override
    protected boolean handleSpecialWrench(Level world, BlockPos pos, Player player, BlockState state) {
        // Sneak + wrench: cycle output level
        if (player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof CreativeEngineBlockEntity engine) {
            if (!world.isClientSide()) {
                long newRate = engine.cycleOutputLevel();
                player.displayClientMessage(Component.translatable("message.logistics.power.creative_engine.output", newRate), true);
            }
            return true;
        }
        return false;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PowerBlockEntities.CREATIVE_ENGINE_BLOCK_ENTITY, CreativeEngineBlockEntity::tick);
    }
}
