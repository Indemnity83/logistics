package com.logistics.core.lib.pipe;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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

    /**
     * The face this module hands accepted items to, or {@code null} if it does not unload through a
     * capability on one particular face.
     *
     * <p>Routing probes this face for room. Checking any adjacent inventory instead would accept a
     * sink whose chosen face is full merely because some other face has space, and the item would
     * still be handed to the full one on arrival.
     */
    @Nullable
    default Direction unloadFace(PipeContext ctx) {
        return null;
    }
}
