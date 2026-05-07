package com.logistics.core.lib.network;

import com.logistics.core.lib.storage.IItemKey;

import java.util.List;

/**
 * Callback registered by dynamic providers (crafters, machines, etc.) to declare
 * whether they can produce a requested amount given current ingredient availability.
 * Recipe knowledge stays inside each provider; the network supplies the shared
 * ingredient-check context.
 */
@FunctionalInterface
public interface ProviderCanFulfill {
    /**
     * Ask this provider whether it can produce {@code amount} units.
     *
     * @param amount  number of output units requested
     * @param checker use this (not the network directly) to validate ingredient
     *                availability — it shares claimed-stock context with other
     *                branches of the validation tree
     * @return empty list if the provider can fulfill; otherwise the missing ingredients
     */
    List<IItemKey> getMissing(long amount, IngredientChecker checker);
}
