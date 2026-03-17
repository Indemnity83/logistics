package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.runtime.TravelingItem;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Role interface for modules that intercept item transfer into adjacent non-pipe storage.
 *
 * <p>Modules implementing this interface will have
 * {@link #onTransferToStorage(PipeContext, TravelingItem, Direction)} called when a
 * {@link TravelingItem} is about to exit this pipe into an adjacent non-pipe storage
 * (at {@code SERVER_EXIT_THRESHOLD}), before the default generic {@code storage.insert()} runs.
 *
 * <p>Return {@code null} if the module fully handled the transfer (item consumed). PipeRuntime
 * will not perform any further insertion. The module is responsible for calling
 * {@code network.notifyDelivery()} if needed.
 *
 * <p>Return the item (or a modified copy with a reduced count) if the module did not handle
 * it — PipeRuntime will perform the default generic insertion on whatever is returned.
 */
public interface TransferHandlerModule extends Module {
    /**
     * Called before the default generic storage insertion.
     *
     * @param ctx       the pipe context
     * @param item      the traveling item being transferred
     * @param direction the direction the item is exiting (toward the storage)
     * @return null if fully handled, or the item (possibly with reduced count) to fall through
     */
    @Nullable TravelingItem onTransferToStorage(PipeContext ctx, TravelingItem item, Direction direction);
}
