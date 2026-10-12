package com.logistics.automation.laserquarry.entity;

import com.logistics.core.lib.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;

/**
 * Whether a quarry is running for chunk-loading purposes: it has power now, or had it within the
 * last {@link #GRACE_TICKS} ticks. The grace window rides out brown-outs — an engine between piston
 * strokes, a network sharing too little power — so the tickets are not dropped and re-taken every
 * few ticks, while an unpowered quarry still lets its chunks go within half a minute.
 *
 * <p>The remaining window is saved: the quarry's own chunk ticket persists across a restart but its
 * work-area tickets do not, so a quarry saved mid brown-out must re-take its area on its first tick
 * or a power source inside that area never loads again.
 */
final class QuarryChunkHold {

    /** Ticks a quarry keeps holding its chunks after it last had power. */
    static final int GRACE_TICKS = 600;

    private static final String KEY = "ChunkHoldTicks";

    private int remaining;

    /** Records one server tick; {@code powered} when the quarry had or spent energy this tick. */
    void tick(boolean powered) {
        if (powered) {
            remaining = GRACE_TICKS;
        } else if (remaining > 0) {
            remaining--;
        }
    }

    /** True until {@link #GRACE_TICKS} ticks have passed since the last powered tick. */
    boolean holds() {
        return remaining > 0;
    }

    void save(CompoundTag tag) {
        tag.putInt(KEY, remaining);
    }

    void load(CompoundTag tag) {
        remaining = Math.max(0, Math.min(GRACE_TICKS, NbtCompat.getInt(tag, KEY, 0)));
    }
}
