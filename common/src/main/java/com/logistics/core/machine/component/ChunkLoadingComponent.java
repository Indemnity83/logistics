package com.logistics.core.machine.component;

import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.function.Supplier;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * Keeps a machine's work area chunk-loaded. Each server tick it (re-)adds a center ticket and a
 * boundary ticket per chunk in the area supplied by the machine; the tickets lapse on their own when
 * the supplier stops returning an area (machine disabled, no area, or removed), so no explicit
 * release is needed. The component knows nothing about what the machine does inside the area.
 *
 * <p>The supplier returns {@code null} when nothing should be loaded this tick — a self-contained
 * gate the machine owns (e.g. a "load chunks" config toggle).
 */
public final class ChunkLoadingComponent implements MachineComponent {

    private final String id;
    private final TicketType<ChunkPos> centerType;
    private final TicketType<ChunkPos> boundaryType;
    private final Supplier<ChunkArea> area;

    public ChunkLoadingComponent(
            String id, TicketType<ChunkPos> centerType, TicketType<ChunkPos> boundaryType, Supplier<ChunkArea> area) {
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
        keepLoaded((ServerLevel) ctx.level());
    }

    /** Re-add the tickets for the current area, if any. Safe to call every tick. */
    public void keepLoaded(ServerLevel level) {
        ChunkArea current = area.get();
        if (current == null) {
            return;
        }

        ServerChunkCache chunkCache = level.getChunkSource();
        int radius = ChunkLevel.byStatus(FullChunkStatus.FULL) - ChunkLevel.byStatus(FullChunkStatus.BLOCK_TICKING);
        for (ChunkPos pos : current.boundary()) {
            chunkCache.addRegionTicket(boundaryType, pos, radius, pos);
        }
        ChunkPos center = current.center();
        chunkCache.addRegionTicket(centerType, center, radius, center);
    }
}
