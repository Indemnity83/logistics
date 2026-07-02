package com.logistics.core.machine.component;

import com.logistics.core.lib.recipe.FluidResult;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * The input/output surface a {@link RecipeProcessorComponent} works against, decoupled from the
 * concrete machine. The default implementation bridges to sibling {@link ItemStoreComponent} and
 * {@link EnergyStorageComponent}; tests supply a fake to exercise the processor's tick flow.
 *
 * <p>Supports a primary result plus ordered byproducts: byproduct {@code i} targets the
 * {@code (i+1)}-th output slot. Multi-input machines expose more than one input slot via the
 * indexed overloads; single-input implementations get them for free from the defaults.
 */
public interface ProcessIO {

    ItemStack input();

    void consumeInput(int count);

    /** Number of input slots this IO exposes. */
    default int inputSlotCount() {
        return 1;
    }

    /** The stack in input slot {@code index}; defaults to the single input for index 0, empty otherwise. */
    default ItemStack input(int index) {
        return index == 0 ? input() : ItemStack.EMPTY;
    }

    /** Consumes {@code count} from input slot {@code index}; defaults to the single input for index 0. */
    default void consumeInput(int index, int count) {
        if (index == 0) {
            consumeInput(count);
        }
    }

    long energyStored();

    void consumeEnergy(long rf);

    /** Whether the primary result and every byproduct's worst-case roll fit their output slots. */
    boolean canAcceptOutputs(ItemStack result, List<ChanceOutput> byproducts);

    /** Places the primary result and the already-rolled byproduct stacks into their output slots. */
    void produceOutputs(ItemStack result, List<ItemStack> rolledByproducts);

    /**
     * Whether the machine's fluid output can fully accept {@code fluid}. Only called for recipes that
     * declare a fluid result; machines without a tank never see one, so the default returns {@code true}.
     */
    default boolean canAcceptFluid(FluidResult fluid) {
        return true;
    }

    /** Deposits {@code fluid} into the machine's tank; the default no-ops for machines without one. */
    default void produceFluid(FluidResult fluid) {}
}
