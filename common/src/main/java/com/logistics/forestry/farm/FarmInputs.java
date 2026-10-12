package com.logistics.forestry.farm;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/** What a farm has on hand, as far as a {@link FarmLogic} needs to know when deciding. */
public interface FarmInputs {

    /**
     * One stack per distinct germling kind available to plant, in slot order, each counting the
     * total available of that kind.
     */
    List<ItemStack> germlings();

    /** How many germlings of {@code kind} (same item and components) are available to plant. */
    int germlingCount(ItemStack kind);

    /** Whether a soil item is available to lay. */
    boolean hasSoil();

    /** Whether a fertilizer charge is available, stored or in the fertilizer slot. */
    boolean hasFertilizer();
}
