package com.logistics.power.block;

import com.logistics.core.lib.block.Probeable;
import com.logistics.core.lib.block.Wrenchable;
import com.logistics.core.lib.support.ProbeResult;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.LogisticsPower;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Creative mode block that accepts and discards energy at a configurable rate.
 * Useful for testing engine output without needing actual energy consumers.
 *
 * <p>Interactions:
 * <ul>
 *   <li>Sneak + right-click with wrench: Cycle through drain rates</li>
 * </ul>
 */
public class CreativeSinkBlock extends BaseEntityBlock implements Probeable, Wrenchable {
    public static final MapCodec<CreativeSinkBlock> CODEC = simpleCodec(CreativeSinkBlock::new);

    public CreativeSinkBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeSinkBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        // Only tick on server - energy tracking is server-side only
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, LogisticsPower.ENTITY.CREATIVE_SINK_BLOCK_ENTITY, CreativeSinkBlockEntity::tick);
    }

    @Nullable @Override
    public ProbeResult onProbe(Level world, BlockPos pos, Player player) {
        if (world.getBlockEntity(pos) instanceof CreativeSinkBlockEntity sink) {
            return sink.getProbeResult();
        }
        return null;
    }

    @Override
    public InteractionResult onWrench(Level world, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (world.getBlockEntity(pos) instanceof CreativeSinkBlockEntity sink) {
            if (!world.isClientSide()) {
                long newRate = sink.cycleDrainRate();
                player.displayClientMessage(
                        Component.translatable("message.logistics.power.creative_sink.drain_rate", newRate), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
