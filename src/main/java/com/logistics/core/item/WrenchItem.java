package com.logistics.core.item;

import com.logistics.core.lib.block.Wrenchable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;

/**
 * Wrench tool for rotating blocks and special interactions.
 * Delegates wrench actions to blocks implementing {@link Wrenchable}.
 */
public class WrenchItem extends Item {

    public WrenchItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }

        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var block = world.getBlockState(pos).getBlock();

        if (block instanceof Wrenchable wrenchable) {
            return wrenchable.onWrench(world, pos, player);
        }

        return InteractionResult.PASS;
    }
}
