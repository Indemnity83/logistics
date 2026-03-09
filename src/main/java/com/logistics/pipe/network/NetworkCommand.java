package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;

/**
 * Typed command describing a single network action to be executed against the Minecraft world.
 *
 * <p>Sealed hierarchy — exactly three concrete forms:
 * <ul>
 *   <li>{@link ExtractCommand}           — pull items from a provider pipe</li>
 *   <li>{@link InsertCrafterInputsCommand} — push ingredients into a crafter's input buffer</li>
 *   <li>{@link DeliverCommand}           — route items directly to a destination (future use)</li>
 * </ul>
 *
 * <p>Commands are value objects produced by the network domain layer and consumed by
 * {@link NetworkCommandExecutor}, which bridges them to the Minecraft world.
 * The domain layer remains Minecraft-free; all world interaction is in the executor.
 */
public sealed interface NetworkCommand
        permits NetworkCommand.ExtractCommand,
                NetworkCommand.InsertCrafterInputsCommand,
                NetworkCommand.DeliverCommand {

    // ───────────────────────────────────────────────────────────────────────

    /**
     * Ask the provider pipe at {@code provider} to extract {@code amount} of {@code item}
     * and begin routing it toward {@code requester}.
     *
     * @param deliveryId UUID attached to the resulting {@link com.logistics.pipe.runtime.TravelingItem}
     *                   for delivery accounting via
     *                   {@link ILogisticsNetwork#notifyDelivery}
     */
    record ExtractCommand(
            BlockPos provider,
            BlockPos requester,
            ItemVariant item,
            long amount,
            UUID deliveryId) implements NetworkCommand {}

    // ───────────────────────────────────────────────────────────────────────

    /**
     * Insert a set of ingredients directly into the input buffer of the crafter at
     * {@code crafter}.
     *
     * <p>Reserved for explicit ingredient routing; not yet used in the main dispatch path
     * (ingredients currently arrive via normal {@link ExtractCommand} routing).
     */
    record InsertCrafterInputsCommand(
            BlockPos crafter,
            Map<ItemVariant, Long> ingredients) implements NetworkCommand {}

    // ───────────────────────────────────────────────────────────────────────

    /**
     * Deliver {@code amount} of {@code item} directly to {@code destination}, bypassing the
     * normal {@link com.logistics.pipe.runtime.TravelingItem} routing pipeline.
     *
     * <p>Reserved for future use (e.g., instant delivery in creative networks).
     */
    record DeliverCommand(
            BlockPos destination,
            ItemVariant item,
            long amount,
            UUID deliveryId) implements NetworkCommand {}
}
