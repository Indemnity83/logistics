package com.logistics.core.lib.network;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Read-only view of network supply state consumed by the request planner.
 *
 * <p>Implemented by {@code NetworkController}. Exposes only the data the planner needs
 * without leaking mutable internal structures.
 */
public interface PlanningView {

    /**
     * Immutable snapshot of a single provider's supply for a specific item.
     *
     * @param provider  position of the supplying pipe
     * @param available raw amount available (0 = on-demand crafter, >0 = real stock)
     * @param priority  dispatch priority (lower = preferred; 1 = real stock, 5 = crafter)
     */
    record SupplyPoint(BlockPos provider, long available, int priority) {}

    /**
     * All supply entries for an item, sorted by priority ascending.
     * Returns an empty list if the item has no registered supply.
     */
    List<SupplyPoint> getSupply(IItemKey item);

    /**
     * Provider fulfillment-check callback for a dynamic provider (crafter, machine, etc.).
     * Returns {@code null} if the provider has no registered check.
     */
    @Nullable
    ProviderCanFulfill getCrafterCheck(BlockPos provider);

    /**
     * Current buffer snapshot for a crafter at the given position.
     * Returns {@code null} if no snapshot has been registered (crafter not yet scanned).
     * When null, callers should treat buffer capacity as unlimited.
     */
    @Nullable
    CrafterSnapshot getCrafterSnapshot(BlockPos provider);
}
