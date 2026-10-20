package com.logistics.forestry.farm;

import com.logistics.core.machine.area.RingArea;
import net.minecraft.core.BlockPos;

/**
 * A farm's footprint: a fixed {@link #COURTYARD} square centred on the farm, which it never works
 * and leaves free for pipes, engines and chests, and a ring of plots around it out to its tier's
 * {@link FarmTierProfile#outerSize()}.
 */
public final class FarmGeometry {

    /** Side of the square around the farm, the farm included, that is never worked. */
    public static final int COURTYARD = 7;

    private FarmGeometry() {}

    /** The ring of plots centred on {@code origin} out to {@code outerSize}. */
    public static RingArea ring(BlockPos origin, int outerSize) {
        return new RingArea(origin, COURTYARD, outerSize);
    }
}
