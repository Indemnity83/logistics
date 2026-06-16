package com.logistics.core.lib.tank;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

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

    /** The {@link TankCell} at {@code pos}, or {@code null} if none / unloaded. */
    private static TankCell cellAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return TankCellLookup.find(level, pos);
    }

    /** Whether a joinable cell sits at {@code pos} (present, loaded, and not isolated). */
    private static boolean joinable(Level level, BlockPos pos) {
        TankCell cell = cellAt(level, pos);
        return cell != null && cell.joinsColumn();
    }

    /**
     * Walks down from {@code pos} to the bottom of the connected, joinable column. Returns {@code pos}
     * itself if the cell there is isolated or there is no joinable cell below.
     */
    public static BlockPos bottomOf(Level level, BlockPos pos) {
        BlockPos bottom = pos;
        TankCell here = cellAt(level, pos);
        if (here == null || !here.joinsColumn()) {
            return bottom; // isolated or not a tank: it is its own bottom
        }
        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos below = bottom.below();
            if (!joinable(level, below)) {
                break;
            }
            bottom = below;
        }
        return bottom;
    }

    /** The connected, joinable cells at {@code pos} ordered bottom-to-top (always at least one cell). */
    public static List<TankCell> column(Level level, BlockPos pos) {
        TankCell here = cellAt(level, pos);
        if (here == null) {
            return List.of();
        }
        if (!here.joinsColumn()) {
            return List.of(here); // isolated: single-cell column
        }
        BlockPos bottom = bottomOf(level, pos);
        List<TankCell> cells = new ArrayList<>();
        BlockPos cur = bottom;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            TankCell cell = cellAt(level, cur);
            if (cell == null || !cell.joinsColumn()) {
                break;
            }
            cells.add(cell);
            cur = cur.above();
        }
        return cells;
    }

    /** Builds the engine over the cross-mod column at {@code pos}, using the lookup's gas predicate. */
    public static TankColumn columnAt(Level level, BlockPos pos) {
        return new TankColumn(column(level, pos), TankCellLookup::isGas);
    }

    /**
     * Whether {@code pos} is the bottom of its column — the single cell allowed to rebalance each tick.
     * An isolated cell is always its own bottom; otherwise the cell is the bottom iff there is no joinable
     * cell directly below it.
     */
    public static boolean isColumnBottom(Level level, BlockPos pos) {
        TankCell here = cellAt(level, pos);
        if (here == null) {
            return false;
        }
        if (!here.joinsColumn()) {
            return true;
        }
        return !joinable(level, pos.below());
    }
}
