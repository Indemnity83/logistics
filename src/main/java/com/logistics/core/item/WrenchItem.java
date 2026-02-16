package com.logistics.core.item;

import com.logistics.core.lib.block.behavior.WrenchBehavior;
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

        return WrenchBehavior.tryWrench(
            context.getLevel(),
            context.getClickedPos(),
            context.getPlayer()
        );
    }
}
