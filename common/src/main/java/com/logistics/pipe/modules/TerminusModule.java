package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;

/**
 * Terminus module — a filter-only sink with no default-route capability.
 * Routes items whose type matches any configured filter slot.
 * Unlike {@link SinkModule}, this module cannot be toggled into a catch-all
 * default route and sits at a lower priority.
 *
 * <p>Priority: {@link SinkPriority#TERMINUS} — below the Item Sink module
 * ({@link SinkPriority#ITEM_SINK}) and the Basic Logistics Pipe's own sink
 * ({@link SinkPriority#BASIC_LOGISTICS_PIPE_SINK}), above the Enchantment Sink module
 * ({@link SinkPriority#ENCHANTMENT_SINK}).
 */
public class TerminusModule extends SinkModule {

    public TerminusModule(int priority) {
        super(priority);
    }

    /** Terminus never acts as a default route. */
    @Override
    public boolean isDefaultRoute(PipeContext ctx) {
        return false;
    }

    /** Default route cannot be enabled on a Terminus module. */
    @Override
    public void setDefaultRoute(PipeContext ctx, boolean enabled) {}
}
