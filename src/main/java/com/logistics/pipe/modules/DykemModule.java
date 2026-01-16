package com.logistics.pipe.modules;

import com.logistics.item.LogisticsItems;
import com.logistics.pipe.PipeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;

public class DykemModule implements Module {
    private static final String COLOR_KEY = "pipe_color";

    @Override
    public ActionResult onUseWithItem(PipeContext ctx, ItemUsageContext usage) {
        ItemStack stack = usage.getStack();
        PlayerEntity player = usage.getPlayer();
        if (stack.isEmpty() && player != null && player.isInSneakingPose()) {
            if (!ctx.getString(this, COLOR_KEY, "").isEmpty()) {
                if (ctx.world().isClient()) {
                    return ActionResult.SUCCESS;
                }
                ctx.remove(this, COLOR_KEY);
            }
            return ActionResult.SUCCESS;
        }
        DyeColor color = LogisticsItems.getDykemColor(stack);
        if (color == null) {
            return ActionResult.PASS;
        }

        if (ctx.world().isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player == null) {
            return ActionResult.PASS;
        }

        String colorId = color.getId();
        String current = ctx.getString(this, COLOR_KEY, "");
        if (colorId.equals(current)) {
            return ActionResult.SUCCESS;
        }

        ctx.saveString(this, COLOR_KEY, colorId);
        stack.damage(1, player, usage.getHand());
        return ActionResult.SUCCESS;
    }
}
