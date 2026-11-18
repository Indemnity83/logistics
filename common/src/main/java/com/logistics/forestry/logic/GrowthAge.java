package com.logistics.forestry.logic;

import java.util.Collections;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

/** A plant's growth stage, read from its block state's {@code age} property. */
public final class GrowthAge {

    private static final String AGE = "age";

    private GrowthAge() {}

    /** {@code state}'s {@code age} property, or {@code null} when it has none. */
    @Nullable
    public static IntegerProperty of(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals(AGE)) {
                return age;
            }
        }
        return null;
    }

    /** The highest value {@code age} takes: fully grown. */
    public static int max(IntegerProperty age) {
        return Collections.max(age.getPossibleValues());
    }
}
