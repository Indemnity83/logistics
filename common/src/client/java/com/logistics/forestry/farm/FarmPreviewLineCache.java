package com.logistics.forestry.farm;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;

/**
 * Each farm's preview lines, rebuilt only when its preview or origin changes. Keyed weakly by
 * identity of the owner, so an unloaded farm's entry is collected.
 */
final class FarmPreviewLineCache<K> {

    private record Entry(FarmPreview preview, BlockPos origin, List<FarmPreviewLines.Line> lines) {}

    private final Map<K, Entry> entries = new WeakHashMap<>();
    private final BiFunction<FarmPreview, BlockPos, List<FarmPreviewLines.Line>> build;

    FarmPreviewLineCache(BiFunction<FarmPreview, BlockPos, List<FarmPreviewLines.Line>> build) {
        this.build = build;
    }

    /** The owner's lines for this preview and origin, built on a miss. */
    List<FarmPreviewLines.Line> lines(K owner, FarmPreview preview, BlockPos origin) {
        Entry entry = entries.get(owner);
        if (entry != null
                && (entry.preview() == preview || entry.preview().equals(preview))
                && entry.origin().equals(origin)) {
            return entry.lines();
        }
        List<FarmPreviewLines.Line> lines = build.apply(preview, origin);
        entries.put(owner, new Entry(preview, origin, lines));
        return lines;
    }

    /** Drops the owner's lines. */
    void forget(K owner) {
        entries.remove(owner);
    }
}
