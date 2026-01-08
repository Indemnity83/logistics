package com.logistics.item;

import com.logistics.block.PipeBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

public class WrenchItem extends Item {
    public WrenchItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().getBlockState(context.getBlockPos()).getBlock() instanceof PipeBlock pipeBlock) {
            return pipeBlock.onWrenchUse(context);
        }

        return ActionResult.PASS;
    }
}
