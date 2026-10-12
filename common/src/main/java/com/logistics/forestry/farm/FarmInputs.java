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

    /**
     * Whether the water tank holds at least one irrigation's worth; always false for a farm without
     * a tank. A logic asks to irrigate only when this is true.
     */
    boolean hasWater();

    /** Whether a fertilizer charge is available, stored or in the fertilizer slot. */
    boolean hasFertilizer();
}
