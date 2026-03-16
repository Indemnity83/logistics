package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;

/**
 * Role interface for modules that perform server-side tick logic.
 *
 * <p>Modules implementing this interface will have {@link #onTick(PipeContext)} called
 * every server tick by the pipe dispatcher. The dispatcher guarantees this is only
 * called server-side, so implementations do not need their own {@code isClientSide()} guard.
 */
public interface TickingModule extends Module {
    /**
     * Called every server tick. The pipe dispatcher guarantees this is server-side only.
     *
     * @param ctx the pipe context
     */
    void onTick(PipeContext ctx);
}
