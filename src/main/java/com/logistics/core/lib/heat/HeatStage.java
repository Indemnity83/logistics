package com.logistics.core.lib.heat;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * Represents the heat stages used by engines and heat-generating machines.
 * Ranges from COLD (minimal heat) to OVERHEAT (critical heat level).
 */
public enum HeatStage implements StringRepresentable {
    COLD,
    COOL,
    WARM,
    HOT,
    OVERHEAT;

    private static final HeatStage[] VALUES = values();

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Safely converts an ordinal to a HeatStage.
     *
     * @param ordinal the ordinal value
     * @return the corresponding HeatStage, or COLD if out of bounds
     */
    public static HeatStage fromOrdinal(int ordinal) {
        return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : COLD;
    }
}
