package com.logistics.forestry.logic;

import net.minecraft.core.BlockPos;

/** The only world access {@link TreeFeller} has: what each position is, as far as felling goes. */
@FunctionalInterface
public interface TreeView {

    Kind kind(BlockPos pos);

    /**
     * Face steps from the canopy at {@code pos} to the nearest wood as the world records it, e.g. a
     * leaf's distance, or {@link Integer#MAX_VALUE} when it records none. Only asked right after
     * {@link #kind} answered {@link Kind#CANOPY} for {@code pos}.
     */
    default int canopyDistance(BlockPos pos) {
        return Integer.MAX_VALUE;
    }

    enum Kind {
        /** Trunk or root; joined to other wood through all 26 neighbours. */
        WOOD,
        /** Natural leaves and the like; reached from wood through face neighbours only. */
        CANOPY,
        /** Anything else, including air and player-placed leaves. */
        OTHER,
        /** Must never be broken and never walked through, e.g. a beehive or any block entity. */
        NEVER_TOUCH,
        /** Not loaded; must not be read. */
        UNLOADED
    }
}
