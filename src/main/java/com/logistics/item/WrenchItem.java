package com.logistics.item;

import com.logistics.block.IronPipeBlock;
import com.logistics.block.WoodenPipeBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

public class WrenchItem extends Item {
    public WrenchItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().isClient) {
            if (context.getWorld().getBlockState(context.getBlockPos()).getBlock() instanceof WoodenPipeBlock woodenPipe) {
                woodenPipe.cycleActiveFace(context.getWorld(), context.getBlockPos());
                return ActionResult.SUCCESS;
            }

            if (context.getWorld().getBlockState(context.getBlockPos()).getBlock() instanceof IronPipeBlock ironPipe) {
                // Cycle the output direction
                ironPipe.cycleOutputDirection(context.getWorld(), context.getBlockPos());
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }
}
