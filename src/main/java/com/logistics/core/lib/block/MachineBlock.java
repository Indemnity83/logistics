package com.logistics.core.lib.block;

import com.logistics.core.lib.block.behavior.MenuBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional base class for machines that commonly have inventory and GUIs.
 * Provides default implementations for:
 * - Dropping inventory when broken (via getDrops)
 * - Opening GUIs on use (via tryOpenMenu helper)
 *
 * <p><strong>Only use this for actual machines.</strong>
 * Pipes, decorative blocks, and other non-machine blocks should NOT extend this.
 * Engines should use AbstractEngineBlock instead.
 *
 * <p>This is entirely optional - blocks can use the behavior modules directly if needed.
 */
public abstract class MachineBlock extends BaseEntityBlock {

    protected MachineBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState blockState, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(blockState, builder));

        // Drop inventory contents if the machine has storage
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity != null) {
            drops.addAll(BlockEntityUtil.getInventoryDrops(blockEntity));
        }

        return drops;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        // Try to open GUI if the machine has one
        return MenuBehavior.tryOpenMenu(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        // Try to open GUI if the machine has one
        return MenuBehavior.tryOpenMenu(level, pos, player);
    }
}