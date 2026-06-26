package com.logistics.core.lib.block.capability;

import net.minecraft.util.RandomSource;

/**
 * Marker interface for block entities that bank smelting/processing experience internally and
 * release it when the block is broken, the way a vanilla furnace does.
 */
public interface HasExperienceStorage {
    /**
     * Removes all banked experience and returns the whole XP points to drop, rounding the
     * fractional remainder with {@code random}.
     */
    int drainExperience(RandomSource random);
}
