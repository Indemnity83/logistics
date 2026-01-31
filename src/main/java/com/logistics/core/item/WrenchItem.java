package com.logistics.core.item;

import com.logistics.core.lib.block.Wrenchable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

/**
 * Wrench tool for rotating blocks and special interactions.
 * Delegates wrench actions to blocks implementing {@link Wrenchable}.
 */
public class WrenchItem extends Item {

    public WrenchItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null) {
            return ActionResult.PASS;
        }

        var world = context.getWorld();
        var pos = context.getBlockPos();
        var player = context.getPlayer();
        var block = world.getBlockState(pos).getBlock();

        if (block instanceof Wrenchable wrenchable) {
            return wrenchable.onWrench(world, pos, player);
        }

        return ActionResult.PASS;
    }
}
