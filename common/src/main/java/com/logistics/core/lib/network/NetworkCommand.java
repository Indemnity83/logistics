package com.logistics.core.lib.network;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;

/**
 * Typed command describing a single network action to be executed against the Minecraft world.
 *
 * <p>Sealed hierarchy — exactly three concrete forms:
 * <ul>
 *   <li>{@link ExtractCommand}             — pull items from a provider pipe</li>
 *   <li>{@link InsertCrafterInputsCommand} — push ingredients into a crafter's input buffer</li>
 *   <li>{@link DeliverCommand}             — route items directly to a destination (future use)</li>
 * </ul>
 *
 * <p>Commands are value objects produced by the network domain layer and consumed by
 * {@code NetworkCommandExecutor}, which bridges them to the Minecraft world.
 * The domain layer remains Minecraft-free; all world interaction is in the executor.
 */
public sealed interface NetworkCommand
        permits NetworkCommand.ExtractCommand,
                NetworkCommand.InsertCrafterInputsCommand,
                NetworkCommand.DeliverCommand {

    /**
     * Ask the provider pipe at {@code provider} to extract {@code amount} of {@code item}
     * and begin routing it toward {@code requester}.
     */
    record ExtractCommand(
            BlockPos provider,
            BlockPos requester,
            IItemKey item,
            long amount,
            UUID deliveryId) implements NetworkCommand {}

    /**
     * Insert a set of ingredients directly into the input buffer of the crafter at {@code crafter}.
     */
    record InsertCrafterInputsCommand(
            BlockPos crafter,
            Map<IItemKey, Long> ingredients) implements NetworkCommand {}

    /**
     * Deliver {@code amount} of {@code item} directly to {@code destination}, bypassing the
     * normal {@link com.logistics.pipe.runtime.TravelingItem} routing pipeline.
     */
    record DeliverCommand(
            BlockPos destination,
            IItemKey item,
            long amount,
            UUID deliveryId) implements NetworkCommand {}
}
