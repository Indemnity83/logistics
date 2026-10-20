package com.logistics.neoforge;

import com.logistics.core.lib.farmland.FarmlandHydration;
import com.logistics.core.lib.farmland.HydrationSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ticket.SimpleTicket;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Answers NeoForge farmland's water check from {@link FarmlandHydration}: each source gets a
 * {@link FarmlandWaterManager} ticket over every chunk its bounds overlap, mastered by its anchor's
 * chunk. NeoForge's {@code FarmlandBlock.isNearWater} asks the tickets only after its water scan
 * came up empty.
 *
 * <p>Asking a ticket is what charges the source, so another mod calling
 * {@link FarmlandWaterManager#hasBlockWaterTicket} on a source's farmland is charged too.
 *
 * <p>A ticket is invalidated when its registration ends, and NeoForge invalidates it as its master
 * chunk unloads; the source's owner unloads with that chunk and registers a new ticket once loaded
 * again. Tickets are held strongly by their registration, as NeoForge holds them weakly.
 */
public final class NeoForgeFarmlandHydration {

    private NeoForgeFarmlandHydration() {}

    public static void register(IEventBus gameBus) {
        FarmlandHydration.installHook(NeoForgeFarmlandHydration::attach);
        gameBus.addListener(NeoForgeFarmlandHydration::onLevelUnload);
    }

    private static FarmlandHydration.Registration attach(ServerLevel level, HydrationSource source) {
        ChunkPos master = new ChunkPos(source.anchor());
        BoundingBox bounds = source.bounds();
        List<ChunkPos> others = new ArrayList<>();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                if (!chunk.equals(master)) {
                    others.add(chunk);
                }
            }
        }
        Ticket ticket = FarmlandWaterManager.addCustomTicket(
                level, new Ticket(level, source), master, others.toArray(new ChunkPos[0]));
        return ticket::invalidate;
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FarmlandHydration.clearLevel(level);
        }
    }

    private static final class Ticket extends SimpleTicket<Vec3> {
        private final ServerLevel level;
        private final HydrationSource source;

        Ticket(ServerLevel level, HydrationSource source) {
            this.level = level;
            this.source = source;
        }

        @Override
        public boolean matches(Vec3 center) {
            return FarmlandHydration.hydrate(source, level, BlockPos.containing(center));
        }
    }
}
