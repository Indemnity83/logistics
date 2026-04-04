package com.logistics.pipe.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class TravelingItemRenderState {
    // Not final — set to a cached instance shared across items of the same ItemVariant (Fix 3)
    public ItemStackRenderState itemRenderState = new ItemStackRenderState();
    public Direction direction;
    public float progress;
    public float currentSpeed;
    public float yOffset; // must be set before use; 0.0f is invalid (see PipeBlockEntityRenderer.BLOCK_OFFSET / ITEM_OFFSET)
}
