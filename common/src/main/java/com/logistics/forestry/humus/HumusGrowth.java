package com.logistics.forestry.humus;

import net.minecraft.util.RandomSource;

/**
 * Decisions behind Humus's sapling boost, kept free of world access so they can be tested with a
 * controlled {@link RandomSource}.
 */
public final class HumusGrowth {

    /** Light needed above a sapling for it to grow; the same threshold vanilla saplings use. */
    public static final int MIN_BRIGHTNESS = 9;

    private HumusGrowth() {}

    /** Whether the space above the sapling is bright enough for it to grow. */
    public static boolean hasLightToGrow(int brightnessAboveSapling) {
        return brightnessAboveSapling >= MIN_BRIGHTNESS;
    }

    /** Whether this random tick gives the sapling above an extra growth step. */
    public static boolean shouldBoost(RandomSource random, double growthChance) {
        return random.nextDouble() < growthChance;
    }
}
