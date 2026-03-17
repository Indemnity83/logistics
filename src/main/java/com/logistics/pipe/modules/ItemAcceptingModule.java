package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import net.minecraft.world.item.ItemStack;

/**
 * Role interface for sink-style modules that can accept specific items from the network.
 *
 * <p>Used by {@link com.logistics.pipe.network.MinecraftWorldView#matchesSinkFilter} to
 * check whether a pipe at a given position would accept an item — without hard-coding a
 * list of concrete sink types. Adding a new sink module type is zero-touch on
 * {@code MinecraftWorldView}: just implement this interface.
 */
public interface ItemAcceptingModule extends Module {
    /**
     * Return true if this module would accept {@code stack} from the network at this pipe.
     *
     * @param ctx   the pipe context
     * @param stack the item to check
     * @return true if the module accepts this item
     */
    boolean acceptsItem(PipeContext ctx, ItemStack stack);
}
