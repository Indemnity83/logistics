package com.logistics.core.lib.pipe;

import com.logistics.LogisticsMod;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;

/**
 * Fabric-specific registry for pipe connections.
 *
 * <p>This class provides the Fabric BlockApiLookup for {@link PipeConnection}.
 * In a multi-loader environment, this would be replaced with platform-specific implementations.
 *
 * <p>Example registration for a quarry that accepts pipes from above but rejects items:
 * <pre>{@code
 * PipeConnectionRegistry.SIDED.registerForBlockEntity(
 *     (quarry, direction) -> direction == Direction.UP ? quarry : null,
 *     QUARRY_BLOCK_ENTITY);
 * }</pre>
 */
public final class PipeConnectionRegistry {

    /**
     * Sided lookup for pipe connections.
     * Returns a {@link PipeConnection} if a pipe can connect from the given direction, null otherwise.
     */
    public static final BlockApiLookup<PipeConnection, Direction> SIDED = BlockApiLookup.get(
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "pipe_connection"), PipeConnection.class, Direction.class);

    private PipeConnectionRegistry() {}
}
