package com.logistics.client.render;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.util.math.Direction;

public class TravelingItemRenderState {
    public final ItemRenderState itemRenderState = new ItemRenderState();
    public Direction direction;
    public float progress;
    public float currentSpeed;
}
