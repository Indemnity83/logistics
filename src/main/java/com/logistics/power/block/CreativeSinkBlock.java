package com.logistics.power.block;

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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Creative mode block that accepts and discards energy at a configurable rate.
 * Useful for testing engine output without needing actual energy consumers.
 *
 * <p>Interactions:
 * <ul>
 *   <li>Right-click: Increase drain rate</li>
 *   <li>Sneak + right-click: Decrease drain rate</li>
 * </ul>
 */
public class CreativeSinkBlock extends BlockWithEntity {
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
        return validateTicker(type, PowerBlockEntities.CREATIVE_SINK_BLOCK_ENTITY, CreativeSinkBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof CreativeSinkBlockEntity sink) {
                if (player.isSneaking()) {
                    sink.decreaseDrainRate();
                } else {
                    sink.increaseDrainRate();
                }
                player.sendMessage(Text.literal("Creative Sink: " + sink.getDrainRate() + " E/t"), true);
            }
        }
        return ActionResult.SUCCESS;
    }
}
