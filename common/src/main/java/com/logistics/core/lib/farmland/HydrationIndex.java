package com.logistics.core.lib.farmland;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * One level's {@link HydrationSource}s, filed under every chunk their bounds overlap, so a lookup
 * reads only the sources of the farmland's own chunk.
 */
final class HydrationIndex {

    private final Long2ObjectOpenHashMap<List<HydrationSource>> byChunk = new Long2ObjectOpenHashMap<>();
    private int size;

    void add(HydrationSource source) {
        BoundingBox bounds = source.bounds();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                byChunk.computeIfAbsent(ChunkPos.asLong(cx, cz), key -> new ArrayList<>(1)).add(source);
            }
        }
        size++;
    }

    /** Removes {@code source}, which still reports the bounds it was added with. */
    void remove(HydrationSource source) {
        BoundingBox bounds = source.bounds();
        boolean removed = false;
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                long key = ChunkPos.asLong(cx, cz);
                List<HydrationSource> sources = byChunk.get(key);
                if (sources != null && sources.remove(source)) {
                    removed = true;
                    if (sources.isEmpty()) {
                        byChunk.remove(key);
                    }
                }
            }
        }
        if (removed) {
            size--;
        }
    }

    /** Whether a source holding {@code pos} wets it; the first one that does is the one that pays. */
    boolean hydrate(ServerLevel level, BlockPos pos) {
        List<HydrationSource> sources = byChunk.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (sources == null) {
            return false;
        }
        for (int i = 0; i < sources.size(); i++) {
            if (FarmlandHydration.hydrate(sources.get(i), level, pos)) {
                return true;
            }
        }
        return false;
    }

    boolean isEmpty() {
        return size == 0;
    }

    int size() {
        return size;
    }

    /** Chunks holding at least one source. */
    int chunkCount() {
        return byChunk.size();
    }
}
