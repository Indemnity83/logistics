package com.logistics.pipe.modules;

import com.logistics.item.LogisticsItems;
import com.logistics.pipe.PipeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

public class DykemModule implements Module {
    @Override
    public ActionResult onUseWithItem(PipeContext ctx, ItemUsageContext usage) {
        ItemStack stack = usage.getStack();
        if (!LogisticsItems.isDykemItem(stack)) {
            return ActionResult.PASS;
        }

        if (ctx.world().isClient()) {
            return ActionResult.SUCCESS;
        }

        PlayerEntity player = usage.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }

        stack.damage(1, player, usage.getHand());
        return ActionResult.SUCCESS;
    }
}
