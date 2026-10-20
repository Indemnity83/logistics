package com.logistics.forestry.farm;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sorts a block a farm would clear for its survey. A block entity, an unbreakable block or a
 * {@code logistics:forestry/never_clear} block is {@link Kind#PROTECTED}: never cleared, its plot
 * blocked. Of the rest, blocks in {@code logistics:forestry/natural_clearable} are
 * {@link Kind#NATURAL}, except player-placed (persistent) leaves; anything else is
 * {@link Kind#FLAGGED}, as it looks player-built. Only drives the survey and its warnings.
 */
public final class ClearClassifier {

    public enum Kind {
        /** Natural ground or growth; cleared without a warning. */
        NATURAL,
        /** Cleared, but looks player-built. */
        FLAGGED,
        /** Never cleared. */
        PROTECTED
    }

    private ClearClassifier() {}

    /**
     * Classifies {@code state}. {@code destroySpeed} is its hardness where it stands, below 0 when
     * unbreakable; {@code natural} and {@code neverClear} are its tag memberships.
     */
    public static Kind classify(BlockState state, float destroySpeed, boolean natural, boolean neverClear) {
        if (state.hasBlockEntity() || destroySpeed < 0 || neverClear) {
            return Kind.PROTECTED;
        }
        return natural && !isPersistentLeaves(state) ? Kind.NATURAL : Kind.FLAGGED;
    }

    private static boolean isPersistentLeaves(BlockState state) {
        return state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT);
    }
}
