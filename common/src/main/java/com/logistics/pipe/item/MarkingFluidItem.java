package com.logistics.pipe.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

/** A marking fluid bottle that knows its own color. */
public class MarkingFluidItem extends Item {
    private final DyeColor color;

    public MarkingFluidItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }
}
