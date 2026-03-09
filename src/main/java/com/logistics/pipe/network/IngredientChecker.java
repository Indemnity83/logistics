package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Callback used during ingredient-chain validation.
 * Shares a "claimed" reservation context across sibling branches so that
 * two branches that both need the same raw material see the combined deduction.
 */
@FunctionalInterface
public interface IngredientChecker {
    /**
     * Ask whether the network can supply {@code amount} of {@code ingredient},
     * accounting for stock already claimed by sibling branches in the current
     * validation pass.
     *
     * @return empty list if the ingredient can be satisfied; otherwise the missing items
     */
    List<ItemVariant> check(ItemStack ingredient, long amount);
}
