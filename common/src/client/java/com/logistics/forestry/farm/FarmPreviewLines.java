package com.logistics.forestry.farm;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * The line segments of a farm's area preview, relative to the farm block's corner: the courtyard
 * square in white and the ring's outer square in green, each at the plot level (the top of the
 * soil, one block above the farm's base) with a post at every corner up to the height kept clear,
 * then a box around each flagged block in amber and each protected block in red.
 */
public final class FarmPreviewLines {

    public static final int COURTYARD_COLOR = 0xFFFFFFFF;
    public static final int OUTER_COLOR = 0xFF55FF55;
    public static final int FLAGGED_COLOR = 0xFFFFAA00;
    public static final int PROTECTED_COLOR = 0xFFFF5555;

    /** Lifts the squares off the ground and grows the boxes past their block, so neither z-fights. */
    static final float LIFT = 0.01f;
    static final float INFLATE = 0.005f;

    /** One segment, in block units from the farm block's corner. */
    public record Line(float x0, float y0, float z0, float x1, float y1, float z1, int color) {}

    private FarmPreviewLines() {}

    /** Every segment; {@code flagged} and {@code protectedBlocks} are relative to the farm. */
    public static List<Line> of(int inner, int outer, int height, List<BlockPos> flagged, List<BlockPos> protectedBlocks) {
        List<Line> lines = new ArrayList<>(24 + 12 * (flagged.size() + protectedBlocks.size()));
        square(lines, inner, height, COURTYARD_COLOR);
        square(lines, outer, height, OUTER_COLOR);
        for (BlockPos pos : flagged) {
            box(lines, pos, FLAGGED_COLOR);
        }
        for (BlockPos pos : protectedBlocks) {
            box(lines, pos, PROTECTED_COLOR);
        }
        return lines;
    }

    /** The edges of the square of {@code side} blocks centred on the farm, and its corner posts. */
    private static void square(List<Line> lines, int side, int height, int color) {
        if (side <= 0) {
            return;
        }
        float min = -(side / 2);
        float max = side / 2 + 1;
        float y = 1 + LIFT;
        lines.add(new Line(min, y, min, max, y, min, color));
        lines.add(new Line(max, y, min, max, y, max, color));
        lines.add(new Line(max, y, max, min, y, max, color));
        lines.add(new Line(min, y, max, min, y, min, color));
        if (height > 0) {
            float top = 1 + height;
            lines.add(new Line(min, y, min, min, top, min, color));
            lines.add(new Line(max, y, min, max, top, min, color));
            lines.add(new Line(max, y, max, max, top, max, color));
            lines.add(new Line(min, y, max, min, top, max, color));
        }
    }

    /** The twelve edges of the block at {@code pos}, grown a little. */
    private static void box(List<Line> lines, BlockPos pos, int color) {
        float x0 = pos.getX() - INFLATE, y0 = pos.getY() - INFLATE, z0 = pos.getZ() - INFLATE;
        float x1 = pos.getX() + 1 + INFLATE, y1 = pos.getY() + 1 + INFLATE, z1 = pos.getZ() + 1 + INFLATE;
        for (float y : new float[] {y0, y1}) {
            lines.add(new Line(x0, y, z0, x1, y, z0, color));
            lines.add(new Line(x1, y, z0, x1, y, z1, color));
            lines.add(new Line(x1, y, z1, x0, y, z1, color));
            lines.add(new Line(x0, y, z1, x0, y, z0, color));
        }
        lines.add(new Line(x0, y0, z0, x0, y1, z0, color));
        lines.add(new Line(x1, y0, z0, x1, y1, z0, color));
        lines.add(new Line(x1, y0, z1, x1, y1, z1, color));
        lines.add(new Line(x0, y0, z1, x0, y1, z1, color));
    }
}
