package com.logistics.core.lib.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import java.util.UUID;

/**
 * A standing order for items. Placed by requesters (supplier/requester pipes or crafting modules).
 * Persists in the network until explicitly cancelled or fulfilled via notifyDelivery.
 * Replaces ItemRequest + LogisticsOrder.
 */
public record Order(UUID id, ItemVariant item, long amount, BlockPos requester) {}
