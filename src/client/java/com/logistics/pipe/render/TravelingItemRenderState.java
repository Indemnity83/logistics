package com.logistics.pipe.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class TravelingItemRenderState {
    public final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    public Direction direction;
    public float progress;
    public float currentSpeed;
}
