package com.logistics.core.lib.pipe;

import java.util.List;
import net.minecraft.core.Direction;

/**
 * Role interface for modules that participate in routing decisions.
 *
 * <p>Modules implementing this interface will have {@link #route} called for each
 * {@link TravelingItem} passing through the pipe. Return a non-PASS plan to claim the item.
 * The first module returning non-PASS wins; remaining modules are skipped.
 */
public interface RoutingModule extends Module {
    /**
     * Decide how to route the traveling item.
     *
     * @param ctx     the pipe context
     * @param item    the item in transit
     * @param options available exit directions (excludes the direction the item came from)
     * @return a routing decision; return {@link RoutePlan#pass()} to defer to the next module
     */
    RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options);
}
