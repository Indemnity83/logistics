package com.logistics.pipe.network;

import com.logistics.core.lib.network.IWorldView;
import com.logistics.core.lib.network.NetworkCommand;

/**
 * Executes {@link NetworkCommand} objects against the Minecraft world via {@link IWorldView}.
 *
 * <p>This is the single boundary point where typed network commands cross from the pure-Java
 * domain layer into world-coupled code. All Minecraft API interaction is delegated to
 * {@link IWorldView}; the domain layer remains free of world coupling.
 *
 * <p>One executor instance is created per network tick and used for all commands in that tick.
 * Executors are stateless beyond their {@link IWorldView} reference.
 */
public class NetworkCommandExecutor {

    private final IWorldView worldView;

    public NetworkCommandExecutor(IWorldView worldView) {
        this.worldView = worldView;
    }

    /**
     * Execute a network command and return the result.
     *
     * @param command the command to execute
     * @return result describing what was accomplished ({@link CommandResult#failed()} if the
     *         command could not be executed or the provider returned nothing)
     */
    public CommandResult execute(NetworkCommand command) {
        return switch (command) {
            case NetworkCommand.ExtractCommand cmd -> executeExtract(cmd);
            case NetworkCommand.InsertCrafterInputsCommand cmd -> executeInsertCrafterInputs(cmd);
            case NetworkCommand.DeliverCommand cmd -> executeDeliver(cmd);
        };
    }

    // ───────────────────────────────────────────────────────────────────────

    private CommandResult executeExtract(NetworkCommand.ExtractCommand cmd) {
        long shipped = worldView.dispatch(
                cmd.provider(), cmd.requester(), cmd.item(), cmd.amount(), cmd.deliveryId());
        if (shipped > 0) return CommandResult.ok(shipped);
        if (shipped < 0) return CommandResult.defer(); // provider temporarily busy (e.g. buffer full)
        return CommandResult.failed();
    }

    private CommandResult executeInsertCrafterInputs(NetworkCommand.InsertCrafterInputsCommand cmd) {
        // Phase 9 placeholder: crafter input insertion is currently handled by
        // CraftingModule.onDispatch() via the normal ExtractCommand path.
        // This command type is reserved for explicit ingredient routing in a future phase.
        return CommandResult.failed();
    }

    private CommandResult executeDeliver(NetworkCommand.DeliverCommand cmd) {
        // Phase 9 placeholder: item delivery is driven by TravelingItem movement in the
        // pipe runtime. This command type is reserved for direct delivery bypass (e.g.
        // creative networks) in a future phase.
        return CommandResult.failed();
    }
}
