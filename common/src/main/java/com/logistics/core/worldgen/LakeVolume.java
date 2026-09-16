package com.logistics.core.worldgen;

/**
 * The block volume vanilla's lake feature writes, as offsets from the feature origin. The lake re-anchors
 * to {@code origin - (8,4,8)} and fills a 16x16x8 grid from there, so its carve, barrier and ice passes all
 * land inside this box — a block outside it was never touched by the lake.
 *
 * <p>The anchor is Minecraft-version specific: mc/1.21.x re-anchors to {@code origin - (0,4,0)} and so
 * spans {@code 0..15} on x/z. Re-derive {@link #MIN_X}/{@link #MIN_Z} against the target's
 * {@code LakeFeature} when porting.
 */
final class LakeVolume {

    static final int MIN_X = -8;
    static final int MIN_Y = -4;
    static final int MIN_Z = -8;
    static final int SIZE_X = 16;
    static final int SIZE_Y = 8;
    static final int SIZE_Z = 16;

    /** Number of block positions in the volume, and the length of an array {@link #index} addresses. */
    static final int SIZE = SIZE_X * SIZE_Y * SIZE_Z;

    private LakeVolume() {}

    /** Receives each offset in the volume together with its {@link #index}. */
    interface OffsetVisitor {
        void visit(int dx, int dy, int dz, int index);
    }

    /** Walks every offset in the volume exactly once. */
    static void forEach(OffsetVisitor visitor) {
        for (int dx = MIN_X; dx < MIN_X + SIZE_X; dx++) {
            for (int dy = MIN_Y; dy < MIN_Y + SIZE_Y; dy++) {
                for (int dz = MIN_Z; dz < MIN_Z + SIZE_Z; dz++) {
                    visitor.visit(dx, dy, dz, index(dx, dy, dz));
                }
            }
        }
    }

    /** Slot in a {@link #SIZE}-length array for an offset from the feature origin. */
    static int index(int dx, int dy, int dz) {
        return ((dx - MIN_X) * SIZE_Z + (dz - MIN_Z)) * SIZE_Y + (dy - MIN_Y);
    }
}
