package com.logistics.core.machine.component;

import net.minecraft.world.item.ItemStack;

/**
 * The input/output surface a {@link RecipeProcessorComponent} works against, decoupled from the
 * concrete machine. The default implementation bridges to sibling {@link ItemStoreComponent} and
 * {@link EnergyStorageComponent}; tests supply a fake to exercise the processor's tick flow.
 */
public interface ProcessIO {

    ItemStack input();

    boolean canAcceptOutput(ItemStack result);

    void consumeInput();

    void produceOutput(ItemStack result);

    long energyStored();

    void consumeEnergy(long rf);
}
