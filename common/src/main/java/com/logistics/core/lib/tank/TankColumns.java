package com.logistics.core.lib.tank;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a vertical {@link TankColumn} from the world by walking neighbouring {@link TankCell}s through
 * {@link TankCellLookup}. Because membership is resolved by lookup rather than a concrete block-entity
 * cast, a column can span tanks from different mods.
 *
 * <p>A cell joins the column only if it is present, loaded, and {@link TankCell#joinsColumn()}; an
 * isolated cell (creative/void-style) is always its own single-cell column.
 *
 * <p><b>Tick ownership.</b> Only the bottom-most cell of a column should drive the once-per-tick
 * rebalance ({@link #isColumnBottom}). Since "bottom" is a single physical position regardless of which
 * mod owns the block there, exactly one cell per column satisfies it each tick — so both mods' tickers
 * can call the same helper without coordinating, and a mixed-mod column settles exactly once.
 */
public final class TankColumns {

    private static final int MAX_HEIGHT = 256;

    private TankColumns() {}

    /**
     * Resolves the {@link TankCell} at a world position, or {@code null} if none is present (or the
     * position is unloaded). This is the single seam between the pure column-walking logic below and the
     * world: the world-backed methods delegate to {@link TankCellLookup}, while tests supply an in-memory
     * source so the walking can be exercised without a live {@link Level}.
     */
    @FunctionalInterface
    interface CellSource {
        @Nullable TankCell at(BlockPos pos);
    }

    /** A {@link CellSource} reading the live world: a position is empty when unloaded or unmatched. */
    private static CellSource world(Level level) {
        return pos -> level.isLoaded(pos) ? TankCellLookup.find(level, pos) : null;
    }

    /** Whether a joinable cell sits at {@code pos} (present and not isolated). */
    private static boolean joinable(CellSource src, BlockPos pos) {
        TankCell cell = src.at(pos);
        return cell != null && cell.joinsColumn();
    }

    // ==================== Pure walking core (world-independent, unit-tested) ====================

    /**
     * Walks down from {@code pos} to the bottom of the connected, joinable column. Returns {@code pos}
     * itself if the cell there is isolated or there is no joinable cell below.
     */
    static BlockPos bottomOf(CellSource src, BlockPos pos) {
        BlockPos bottom = pos;
        TankCell here = src.at(pos);
        if (here == null || !here.joinsColumn()) {
            return bottom; // isolated or not a tank: it is its own bottom
        }
        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos below = bottom.below();
            if (!joinable(src, below)) {
                break;
            }
            bottom = below;
        }
        return bottom;
    }

    /** The connected, joinable cells at {@code pos} ordered bottom-to-top (always at least one cell). */
    static List<TankCell> column(CellSource src, BlockPos pos) {
        TankCell here = src.at(pos);
        if (here == null) {
            return List.of();
        }
        if (!here.joinsColumn()) {
            return List.of(here); // isolated: single-cell column
        }
        BlockPos bottom = bottomOf(src, pos);
        List<TankCell> cells = new ArrayList<>();
        BlockPos cur = bottom;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            TankCell cell = src.at(cur);
            if (cell == null || !cell.joinsColumn()) {
                break;
            }
            cells.add(cell);
            cur = cur.above();
        }
        return cells;
    }

    /**
     * Whether {@code pos} is the bottom of its column — the single cell allowed to rebalance each tick.
     * An isolated cell is always its own bottom; otherwise the cell is the bottom iff there is no joinable
     * cell directly below it.
     */
    static boolean isColumnBottom(CellSource src, BlockPos pos) {
        TankCell here = src.at(pos);
        if (here == null) {
            return false;
        }
        if (!here.joinsColumn()) {
            return true;
        }
        return !joinable(src, pos.below());
    }

    // ==================== World-backed public API ====================

    /** @see #bottomOf(CellSource, BlockPos) */
    public static BlockPos bottomOf(Level level, BlockPos pos) {
        return bottomOf(world(level), pos);
    }

    /** @see #column(CellSource, BlockPos) */
    public static List<TankCell> column(Level level, BlockPos pos) {
        return column(world(level), pos);
    }

    /** @see #isColumnBottom(CellSource, BlockPos) */
    public static boolean isColumnBottom(Level level, BlockPos pos) {
        return isColumnBottom(world(level), pos);
    }

    /** Builds the engine over the cross-mod column at {@code pos}, using the lookup's gas predicate. */
    public static TankColumn columnAt(Level level, BlockPos pos) {
        return new TankColumn(column(level, pos), TankCellLookup::isGas);
    }
}
