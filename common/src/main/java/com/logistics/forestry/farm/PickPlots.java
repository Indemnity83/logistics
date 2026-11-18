package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.NbtCompat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * A picking farm's Picking Plots ({@link TendLogic#picks}): where each plant it picks from stands.
 * They need no germling, are never empty and are never replanted. Saved with the block entity,
 * never copied to the dropped item.
 */
final class PickPlots {

    private final Set<BlockPos> plots = new LinkedHashSet<>();

    /** Remembers {@code pos}; false if it already was. */
    boolean add(BlockPos pos) {
        return plots.add(pos.immutable());
    }

    /** Forgets {@code pos}; false if it was no Picking Plot. */
    boolean remove(BlockPos pos) {
        return plots.remove(pos);
    }

    boolean contains(BlockPos pos) {
        return plots.contains(pos);
    }

    /** The Picking Plots in the order they were found, as a copy that survives changes to this set. */
    List<BlockPos> snapshot() {
        return List.copyOf(plots);
    }

    void clear() {
        plots.clear();
    }

    int size() {
        return plots.size();
    }

    void save(CompoundTag tag, String key) {
        if (plots.isEmpty()) {
            return;
        }
        long[] packed = new long[plots.size()];
        int i = 0;
        for (BlockPos pos : plots) {
            packed[i++] = pos.asLong();
        }
        tag.putLongArray(key, packed);
    }

    void load(CompoundTag tag, String key) {
        plots.clear();
        for (long packed : NbtCompat.getLongArray(tag, key, new long[0])) {
            plots.add(BlockPos.of(packed));
        }
    }
}
