package com.logistics.core.machine.component;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * The input/output surface a {@link RecipeProcessorComponent} works against, decoupled from the
 * concrete machine. The default implementation bridges to sibling {@link ItemStoreComponent} and
 * {@link EnergyStorageComponent}; tests supply a fake to exercise the processor's tick flow.
 *
 * <p>Supports a primary result plus ordered byproducts: byproduct {@code i} targets the
 * {@code (i+1)}-th output slot.
 */
public interface ProcessIO {

    ItemStack input();

    void consumeInput(int count);

    long energyStored();

    void consumeEnergy(long rf);

    /** Whether the primary result and every byproduct's worst-case roll fit their output slots. */
    boolean canAcceptOutputs(ItemStack result, List<ChanceOutput> byproducts);

    /** Places the primary result and the already-rolled byproduct stacks into their output slots. */
    void produceOutputs(ItemStack result, List<ItemStack> rolledByproducts);
}
