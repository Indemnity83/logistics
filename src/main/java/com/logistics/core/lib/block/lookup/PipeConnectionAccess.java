package com.logistics.core.lib.block.lookup;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.PipeConnectionRegistry;

/**
 * Bridges internal {@link PipeConnection} interface to the PipeConnectionRegistry lookup API.
 * <p>
 * Any block entity implementing PipeConnection will automatically work with:
 * <ul>
 *   <li>Pipe networks for connectivity queries</li>
 *   <li>Other mods that interact with the pipe system</li>
 * </ul>
 * <p>
 * Call {@link #register()} once during mod initialization.
 */
public final class PipeConnectionAccess {
    private PipeConnectionAccess() {}

    /**
     * Register the fallback adapter that exposes PipeConnection block entities
     * to the PipeConnectionRegistry lookup system.
     */
    public static void register() {
        PipeConnectionRegistry.SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (blockEntity instanceof PipeConnection connection) {
                return connection;
            }
            return null;
        });
    }
}