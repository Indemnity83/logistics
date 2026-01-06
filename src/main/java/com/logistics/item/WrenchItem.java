package com.logistics.item;

import com.logistics.block.IronPipeBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

public class WrenchItem extends Item {
    public WrenchItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // Check if the block is an iron pipe
        if (context.getWorld().getBlockState(context.getBlockPos()).getBlock() instanceof IronPipeBlock ironPipe) {
            if (!context.getWorld().isClient) {
                // Cycle the output direction
                ironPipe.cycleOutputDirection(context.getWorld(), context.getBlockPos());
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
