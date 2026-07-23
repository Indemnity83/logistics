package com.logistics.core.lib.power;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Heat stages of an engine, and the block-state property that carries the current stage. */
public enum HeatStage implements StringRepresentable {
    COLD,
    COOL,
    WARM,
    HOT,
    OVERHEAT;

    private static final HeatStage[] VALUES = values();

    /** Block state property for engine heat stage. */
    public static final EnumProperty<HeatStage> STAGE = EnumProperty.create("stage", HeatStage.class);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HeatStage fromOrdinal(int ordinal) {
        return (ordinal >= 0 && ordinal < VALUES.length) ? VALUES[ordinal] : COLD;
    }
}
