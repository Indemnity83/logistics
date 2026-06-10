package com.logistics.core.lib.pipe;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The sink a {@link Module} writes its look-at HUD (Jade) status to. Modules express intent — a text line
 * or a row of item icons — and the loader-side renderer turns that into Jade UI elements (icons can't be
 * built in loader-agnostic common code). Convention: terse always; fuller detail when {@link #showDetails()}.
 */
public interface PipeHud {

    /** Whether the player is holding Jade's details key (show extra info). */
    boolean showDetails();

    /** Adds a text line. */
    void line(Component text);

    /** Adds a row of item icons (rendered inventory-style with their stack counts). Empty stacks skipped. */
    void items(List<ItemStack> stacks);
}
