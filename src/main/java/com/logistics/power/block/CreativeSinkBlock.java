package com.logistics.power.block;

import com.logistics.core.lib.block.Wrenchable;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.power.registry.PowerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
public class CreativeSinkBlock extends BlockWithEntity implements Wrenchable {
    public static final MapCodec<CreativeSinkBlock> CODEC = createCodec(CreativeSinkBlock::new);

    public CreativeSinkBlock(Settings settings) {
        super(settings.strength(1.0f).sounds(BlockSoundGroup.STONE));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeSinkBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        // Only tick on server - energy tracking is server-side only
        if (world.isClient()) {
            return null;
        }
        return validateTicker(type, PowerBlockEntities.CREATIVE_SINK_BLOCK_ENTITY, CreativeSinkBlockEntity::tick);
    }

    @Override
    public ActionResult onWrench(World world, BlockPos pos, PlayerEntity player) {
        if (!player.isSneaking()) {
            return ActionResult.PASS;
        }
        if (world.getBlockEntity(pos) instanceof CreativeSinkBlockEntity sink) {
            if (!world.isClient()) {
                long newRate = sink.cycleDrainRate();
                player.sendMessage(
                        Text.translatable("message.logistics.power.creative_sink.drain_rate", newRate), true);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
