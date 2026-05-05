package com.logistics.pipe.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class TravelingItemRenderState {
    public final ItemStackRenderState itemRenderState;
    public Direction direction;
    public float progress;
    public float currentSpeed;
    public float yOffset; // must be set before use; 0.0f is invalid (see PipeBlockEntityRenderer.BLOCK_OFFSET / ITEM_OFFSET)

    public TravelingItemRenderState(ItemStackRenderState itemRenderState) {
        this.itemRenderState = itemRenderState;
    }
}
