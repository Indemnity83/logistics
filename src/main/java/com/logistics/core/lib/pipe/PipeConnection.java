package com.logistics.core.lib.pipe;

import com.logistics.LogisticsMod;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.Direction;

/**
 * API lookup for blocks that can connect to pipes.
 *
 * <p>This follows the same pattern as {@code ItemStorage.SIDED} from Fabric API.
 * Blocks register with {@code PipeConnection.SIDED} to indicate they accept
 * pipe connections, optionally filtering by direction.
 *
 * <p>Example registration for a block that only accepts pipes from above:
 * <pre>{@code
 * PipeConnection.SIDED.registerForBlocks(
 *     (world, pos, state, blockEntity, direction) ->
 *         direction == Direction.UP ? Unit.INSTANCE : null,
 *     MY_BLOCK);
 * }</pre>
 */
public final class PipeConnection {

    /**
     * Sided lookup for pipe connections.
     * Returns {@link Unit#INSTANCE} if a pipe can connect from the given direction, null otherwise.
     */
    public static final BlockApiLookup<Unit, Direction> SIDED =
            BlockApiLookup.get(Identifier.of(LogisticsMod.MOD_ID, "pipe_connection"), Unit.class, Direction.class);

    private PipeConnection() {}
}
