package com.logistics.core.lib.network;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The single rule the network uses to choose between candidate positions, shared by every
 * selection point: sink resolution, item provider dispatch, and fluid provider dispatch.
 *
 * <p>Candidates are ranked by, in order:
 * <ol>
 *   <li><b>Priority</b> — applied by the call site, since the sense differs (sinks prefer the
 *       highest priority, the provider tables prefer the lowest). This class covers only what
 *       happens once priorities tie.</li>
 *   <li><b>Routed distance</b> — the candidate fewer pipe hops from the source wins, so an
 *       adjacent destination beats a distant one.</li>
 *   <li><b>Most positive direction</b> — greater Y wins; if Y is equal, greater X; if X is also
 *       equal, greater Z. This is a total order over distinct positions, so a tie always has
 *       exactly one winner and that winner is a pure function of the candidates' positions.</li>
 * </ol>
 *
 * <p>The third tier is not cosmetic. Without it the winner follows hash-iteration order (sinks)
 * or registration order (providers), both of which shift whenever a network merges or splits —
 * so a working sorting setup would silently start feeding a different destination after an
 * unrelated pipe edit.
 */
public final class RoutingPreference {

    private RoutingPreference() {}

    /**
     * Ranks equal-priority candidates, most preferred first: fewer hops from {@code source},
     * then the most positive position.
     *
     * <p>The returned comparator only measures distance when it is actually invoked, and
     * {@link Comparator#thenComparing} only reaches the positional tier on an exact distance
     * tie. Chaining this behind a priority comparator therefore costs nothing at all until two
     * candidates genuinely tie on priority.
     *
     * @param source   the position the item or fluid travels from; {@code null} skips the
     *                 distance tier and ranks by position alone
     * @param distance routed-distance oracle; {@link HopDistance#UNKNOWN} skips the distance tier
     */
    public static Comparator<BlockPos> among(@Nullable BlockPos source, HopDistance distance) {
        if (source == null) return RoutingPreference::mostPositiveFirst;
        return Comparator.comparingInt((BlockPos pos) -> distance.hops(source, pos))
                .thenComparing(RoutingPreference::mostPositiveFirst);
    }

    /**
     * The "most positive direction" rule: greater Y, then greater X, then greater Z.
     *
     * @return a negative number when {@code a} is preferred over {@code b}
     */
    public static int mostPositiveFirst(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) return Integer.compare(b.getY(), a.getY());
        if (a.getX() != b.getX()) return Integer.compare(b.getX(), a.getX());
        return Integer.compare(b.getZ(), a.getZ());
    }
}
