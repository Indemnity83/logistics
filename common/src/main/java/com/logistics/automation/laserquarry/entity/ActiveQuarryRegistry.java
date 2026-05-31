package com.logistics.automation.laserquarry.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Per-dimension registry of active laser quarry positions, with TTL eviction so
 * stale entries from unloaded quarries are cleaned up automatically on read.
 */
public final class ActiveQuarryRegistry {
    public static final long REGISTRY_TTL_TICKS = 200L;

    private static final Map<ResourceKey<Level>, Map<Long, Long>> ENTRIES = new HashMap<>();

    private ActiveQuarryRegistry() {}

    public static void register(ServerLevel world, BlockPos pos) {
        register(world.dimension(), world.getGameTime(), pos);
    }

    public static void register(ResourceKey<Level> dimension, long gameTime, BlockPos pos) {
        Map<Long, Long> entries = ENTRIES.computeIfAbsent(dimension, unused -> new HashMap<>());
        entries.put(pos.asLong(), gameTime);
    }

    public static void unregister(ServerLevel world, BlockPos pos) {
        unregister(world.dimension(), pos);
    }

    public static void unregister(ResourceKey<Level> dimension, BlockPos pos) {
        Map<Long, Long> entries = ENTRIES.get(dimension);
        if (entries == null) {
            return;
        }
        entries.remove(pos.asLong());
        if (entries.isEmpty()) {
            ENTRIES.remove(dimension);
        }
    }

    public static List<BlockPos> getAll(ServerLevel world) {
        return getAll(world.dimension(), world.getGameTime());
    }

    public static List<BlockPos> getAll(ResourceKey<Level> dimension, long gameTime) {
        Map<Long, Long> entries = ENTRIES.get(dimension);
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        entries.entrySet().removeIf(entry -> gameTime - entry.getValue() > REGISTRY_TTL_TICKS);

        if (entries.isEmpty()) {
            ENTRIES.remove(dimension);
            return List.of();
        }

        List<BlockPos> positions = new ArrayList<>(entries.size());
        for (Long posLong : entries.keySet()) {
            positions.add(BlockPos.of(posLong));
        }
        return positions;
    }

    public static void clear(ServerLevel world) {
        clear(world.dimension());
    }

    public static void clear(ResourceKey<Level> dimension) {
        ENTRIES.remove(dimension);
    }
}
