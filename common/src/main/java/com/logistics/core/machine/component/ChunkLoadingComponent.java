package com.logistics.core.machine.component;

import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps a machine's work area chunk-loaded. Each server tick it (re-)adds a center ticket and a
 * boundary ticket per chunk in the area the machine supplies; the tickets lapse on their own when the
 * provider stops returning an area (machine disabled, no area, or removed), so no explicit release is
 * needed. The component knows nothing about what the machine does inside the area.
 *
 * <p>The provider returns {@code null} when nothing should be loaded this tick — a self-contained
 * gate the machine owns (e.g. a "load chunks" config toggle or a finished machine).
 */
public final class ChunkLoadingComponent implements MachineComponent {

    /** Supplies the area to keep loaded for the given host context, or {@code null} for none. */
    @FunctionalInterface
    public interface AreaProvider {
        @Nullable
        ChunkArea areaFor(MachineContext ctx);
    }

    private final String id;
    private final TicketType centerType;
    private final TicketType boundaryType;
    private final AreaProvider area;

    public ChunkLoadingComponent(
            String id, TicketType centerType, TicketType boundaryType, AreaProvider area) {
        this.id = id;
        this.centerType = centerType;
        this.boundaryType = boundaryType;
        this.area = area;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        ChunkArea current = area.areaFor(ctx);
        if (current == null) {
            return;
        }

        ServerChunkCache chunkCache = ((ServerLevel) ctx.level()).getChunkSource();
        int ticketLevel = ChunkLevel.byStatus(FullChunkStatus.BLOCK_TICKING);
        for (ChunkPos pos : current.boundary()) {
            chunkCache.addTicket(new Ticket(boundaryType, ticketLevel), pos);
        }
        chunkCache.addTicket(new Ticket(centerType, ticketLevel), current.center());
    }
}
