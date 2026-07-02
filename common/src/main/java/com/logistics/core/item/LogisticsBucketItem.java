package com.logistics.core.item;

import net.minecraft.world.item.Item;

/**
 * A filled bucket of one custom fluid. Unlike a vanilla {@code BucketItem} it never places the fluid in
 * the world (the fluids have no block) — it only fills/drains tanks and machines through the loader's
 * fluid-item capability, emptying back to a plain {@code minecraft:bucket}. The fluid it holds is
 * identified by {@link #fluidName()} (its {@code logistics:core/<name>} id).
 */
public class LogisticsBucketItem extends Item {

    private final String fluidName;

    public LogisticsBucketItem(String fluidName, Properties properties) {
        super(properties);
        this.fluidName = fluidName;
    }

    /** The short fluid name (e.g. {@code liquid_redstone}); the fluid is {@code logistics:core/<name>}. */
    public String fluidName() {
        return fluidName;
    }
}
