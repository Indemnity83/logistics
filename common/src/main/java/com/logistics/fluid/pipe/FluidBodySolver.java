package com.logistics.fluid.pipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Pure "water finds its level" solver for a connected component of fluid cells. Holds no Minecraft types and
 * works entirely in millibuckets, so it is unit-testable without a running game.
 *
 * <p>Unlike the local cellular rules, this is a non-local solver: it identifies contiguous fluid
 * <em>bodies</em> and settles each to a single surface level, which is the only way to get passive
 * communicating-vessels behaviour (a normal U-tube equalizes its two arms for free). See
 * {@code solver-design.md} in this package for the full design.
 *
 * <p>The unit is the <em>body</em>, not the graph component: a body spreads through wet cells and into dry
 * cells <em>below</em> the rising surface, but cannot climb over a dry cell <em>above</em> the surface. That
 * single rule makes a normal U equalize, an inverted U stay split, and a tower not climb.
 *
 * <p>Tanks are absorbing boundaries: the solver fills them greedily but never drains them (emptying a tank is
 * the active extractor's job).
 */
public final class FluidBodySolver {

    private FluidBodySolver() {}

    /** Bisection steps for the level search — far more than double precision needs over any block range. */
    private static final int LEVEL_SEARCH_ITERATIONS = 60;

    /**
     * One pipe/tank segment in a connected component.
     *
     * @param elevation integer block Y; a cell occupies the vertical span {@code [elevation, elevation+1)}
     * @param capacity  segment capacity in mB
     * @param amount    fluid currently held in mB
     * @param tank      whether this cell is an absorbing boundary (filled greedily, never drained)
     */
    public record Cell(int elevation, long capacity, long amount, boolean tank) {}

    /**
     * Settles the component to equilibrium and returns the resulting amount per cell (same indexing),
     * conserving total fluid. This is the instantaneous target; {@link #step} approaches it rate-limited.
     *
     * <p>Bodies are identified by flooding through currently-wet, non-tank cells, then each body's volume is
     * distributed by a level search that extends into dry cells below the surface. Two wet regions joined only
     * through a dry-but-below-surface gap are treated as separate this call and merge over subsequent
     * {@link #step} calls once the gap wets — fine for transport, where fluid flows over many ticks.
     *
     * @param cells     the component's cells
     * @param adjacency undirected edges as index pairs, e.g. {@code {{0,1},{1,2}}}
     */
    public static long[] equilibrium(List<Cell> cells, int[][] adjacency) {
        int n = cells.size();
        List<List<Integer>> neighbours = buildNeighbours(n, adjacency);
        long[] result = new long[n];
        for (int i = 0; i < n; i++) {
            result[i] = cells.get(i).amount();
        }

        boolean[] pooled = new boolean[n];
        boolean[] claimed = new boolean[n]; // cells already filled by a settled pool (prevents double-claim)
        for (int seed = 0; seed < n; seed++) {
            Cell cell = cells.get(seed);
            if (pooled[seed] || cell.tank() || cell.amount() <= 0) {
                continue;
            }
            List<Integer> pool = floodWetPool(seed, cells, neighbours, pooled);
            boolean[] inPool = new boolean[n];
            for (int i : pool) {
                inPool[i] = true;
            }
            settlePool(pool, inPool, claimed, cells, neighbours, result);
        }
        return result;
    }

    /**
     * Moves the component one tick toward {@link #equilibrium}, relocating at most {@code rate} mB of fluid
     * (so fluid flows visibly rather than teleporting). Conserves total fluid exactly. Applied repeatedly,
     * converges to the equilibrium state.
     */
    public static long[] step(List<Cell> cells, int[][] adjacency, long rate) {
        int n = cells.size();
        long[] current = new long[n];
        for (int i = 0; i < n; i++) {
            current[i] = cells.get(i).amount();
        }
        long[] target = equilibrium(cells, adjacency);

        long[] delta = new long[n];
        long totalGain = 0;
        for (int i = 0; i < n; i++) {
            delta[i] = target[i] - current[i];
            if (delta[i] > 0) {
                totalGain += delta[i];
            }
        }
        if (totalGain == 0 || rate <= 0) {
            return current; // already settled (or no budget)
        }
        if (totalGain <= rate) {
            return target; // the whole move fits in this tick's budget
        }

        // Relocate exactly `rate` mB: split it across gainers and (separately) losers in proportion to their
        // deltas, using largest-remainder rounding so both sides sum to `rate` and the result conserves.
        long[] gains = allocate(rate, delta, n, true, totalGain);
        long[] losses = allocate(rate, delta, n, false, totalGain);
        long[] next = new long[n];
        for (int i = 0; i < n; i++) {
            next[i] = current[i] + gains[i] - losses[i];
        }
        return next;
    }

    // ==================== pool settling ====================

    /** Settles one wet pool: greedy tank intake first, then a level fill of the remainder over the basin. */
    private static void settlePool(List<Integer> pool, boolean[] inPool, boolean[] claimed,
            List<Cell> cells, List<List<Integer>> neighbours, long[] result) {
        long volume = 0;
        for (int i : pool) {
            volume += cells.get(i).amount();
        }

        // Tanks adjacent to any wet pool cell absorb greedily (deterministic by index), never drained.
        List<Integer> tanks = adjacentTanks(pool, cells, neighbours);
        for (int t : tanks) {
            if (volume <= 0) {
                break;
            }
            long free = cells.get(t).capacity() - result[t];
            long intake = Math.min(volume, Math.max(0, free));
            result[t] += intake;
            volume -= intake;
        }

        // Drain every pool cell, then refill the reachable basin at the solved surface level.
        for (int i : pool) {
            result[i] = 0;
        }
        levelFill(inPool, claimed, cells, neighbours, volume, result);
    }

    /**
     * Distributes {@code volume} mB across the pool's basin by raising a single surface level: cells fully
     * below it are full, the straddling layer is partial, cells at or above it stay empty. The basin floods
     * from the pool's wet cells into adjacent dry cells below the surface (a normal U's far arm) and falls
     * through cells downhill (perched fluid draining to the basin floor), but never rises over a dry cell at or
     * above the surface (an inverted-U hump).
     */
    private static void levelFill(boolean[] inPool, boolean[] claimed, List<Cell> cells,
            List<List<Integer>> neighbours, long volume, long[] result) {
        if (volume <= 0) {
            return;
        }
        // The cavity: every cell this pool's water could occupy (graph-reachable through non-tank, unclaimed,
        // non-other-pool cells), captured before we claim anything — used as the conservation safety net below.
        List<Integer> cavity = cavityCells(inPool, claimed, cells, neighbours);

        // Search the surface across the whole component's elevation span: fluid can settle BELOW the lowest wet
        // cell (perched fluid draining to the basin floor) and rise ABOVE it (a fed riser), so bound the search
        // by the shortest and tallest non-tank cells. Over-wide bounds are safe — volume(S) is monotonic.
        int minElevation = Integer.MAX_VALUE;
        int maxElevation = Integer.MIN_VALUE;
        for (Cell cell : cells) {
            if (!cell.tank()) {
                minElevation = Math.min(minElevation, cell.elevation());
                maxElevation = Math.max(maxElevation, cell.elevation());
            }
        }

        // Binary search the surface level S in [minElevation, maxElevation + 1]: volume(S) is monotonic.
        double lo = minElevation;
        double hi = maxElevation + 1.0;
        for (int iter = 0; iter < LEVEL_SEARCH_ITERATIONS; iter++) {
            double mid = (lo + hi) / 2.0;
            if (volumeAtLevel(inPool, claimed, cells, neighbours, mid) < volume) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double surface = (lo + hi) / 2.0;

        long assigned = 0;
        for (int i : reachableAtLevel(inPool, claimed, cells, neighbours, surface)) {
            Cell cell = cells.get(i);
            long fill = clampFill((surface - cell.elevation()) * cell.capacity(), cell.capacity());
            result[i] = fill;
            assigned += fill;
            claimed[i] = true;
        }

        // Conservation safety net. Place any shortfall — or remove any excess — greedily across the cavity,
        // lowest cells first, so the pool conserves *exactly* even where the single-level model is discontinuous
        // (the surface landing on a rim as fluid crests an inverted U). Spill-over then lands on the floor of
        // the next basin, which is the physical behaviour, and rounding error is absorbed the same way.
        long error = volume - assigned;
        if (error > 0) {
            for (int i : cavity) {
                if (error <= 0) {
                    break;
                }
                long add = Math.min(error, cells.get(i).capacity() - result[i]);
                if (add > 0) {
                    result[i] += add;
                    error -= add;
                    claimed[i] = true;
                }
            }
        } else if (error < 0) {
            for (int k = cavity.size() - 1; k >= 0 && error < 0; k--) {
                int i = cavity.get(k);
                long remove = Math.min(-error, result[i]);
                result[i] -= remove;
                error += remove;
            }
        }
    }

    /** Cells this pool's water can occupy (graph-reachable, ignoring elevation), sorted lowest-elevation first. */
    private static List<Integer> cavityCells(boolean[] inPool, boolean[] claimed, List<Cell> cells, List<List<Integer>> neighbours) {
        boolean[] seen = new boolean[cells.size()];
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < inPool.length; i++) {
            if (inPool[i]) {
                seen[i] = true;
                queue.add(i);
            }
        }
        List<Integer> cavity = new ArrayList<>();
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            cavity.add(cur);
            for (int next : neighbours.get(cur)) {
                if (seen[next]) {
                    continue;
                }
                Cell cell = cells.get(next);
                if (cell.tank() || claimed[next] || (cell.amount() > 0 && !inPool[next])) {
                    continue; // tank, another settled pool, or another pool's wet cell
                }
                seen[next] = true;
                queue.add(next);
            }
        }
        cavity.sort((a, b) -> Integer.compare(cells.get(a).elevation(), cells.get(b).elevation()));
        return cavity;
    }

    private static double volumeAtLevel(boolean[] inPool, boolean[] claimed, List<Cell> cells,
            List<List<Integer>> neighbours, double surface) {
        double total = 0;
        for (int i : reachableAtLevel(inPool, claimed, cells, neighbours, surface)) {
            Cell cell = cells.get(i);
            double fill = (surface - cell.elevation()) * cell.capacity();
            total += Math.max(0, Math.min(cell.capacity(), fill));
        }
        return total;
    }

    /**
     * The basin cells the pool can fill at {@code surface}: flood from the pool's wet cells through any
     * non-tank cell strictly below the surface — including dry cells (water fills them) and the pool's own
     * cells, but not another pool's wet cell, nor a cell already claimed by a settled pool. Stopping at
     * elevation ≥ surface is what blocks an inverted-U hump while still letting a normal U's far arm fill.
     */
    private static List<Integer> reachableAtLevel(boolean[] inPool, boolean[] claimed, List<Cell> cells,
            List<List<Integer>> neighbours, double surface) {
        int n = cells.size();
        boolean[] seen = new boolean[n];
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (inPool[i]) {
                // A wet cell always seeds the flood, even if it sits above the surface — its water falls
                // downhill to the basin and the perched cell itself drains (fills to 0 below the surface).
                seen[i] = true;
                queue.add(i);
            }
        }
        List<Integer> reachable = new ArrayList<>();
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            reachable.add(cur);
            for (int next : neighbours.get(cur)) {
                if (seen[next]) {
                    continue;
                }
                Cell cell = cells.get(next);
                if (cell.tank() || claimed[next]) {
                    continue;
                }
                if (cell.amount() > 0 && !inPool[next]) {
                    continue; // another pool's wet cell — don't merge across it this call
                }
                // Reach a neighbour below the surface (it fills) or strictly downhill (water falls through it,
                // even if it ends up above the surface and stays empty). Never rise over a dry rim.
                if (cell.elevation() >= surface && cell.elevation() >= cells.get(cur).elevation()) {
                    continue;
                }
                seen[next] = true;
                queue.add(next);
            }
        }
        return reachable;
    }

    // ==================== flood / adjacency helpers ====================

    /** Flood a maximal pool of connected wet, non-tank cells; marks them in {@code pooled}. */
    private static List<Integer> floodWetPool(int seed, List<Cell> cells, List<List<Integer>> neighbours, boolean[] pooled) {
        List<Integer> pool = new ArrayList<>();
        Deque<Integer> queue = new ArrayDeque<>();
        pooled[seed] = true;
        queue.add(seed);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            pool.add(cur);
            for (int next : neighbours.get(cur)) {
                Cell cell = cells.get(next);
                if (pooled[next] || cell.tank() || cell.amount() <= 0) {
                    continue;
                }
                pooled[next] = true;
                queue.add(next);
            }
        }
        return pool;
    }

    /** Distinct tank cells adjacent to any cell of the pool, sorted by index for determinism. */
    private static List<Integer> adjacentTanks(List<Integer> pool, List<Cell> cells, List<List<Integer>> neighbours) {
        boolean[] found = new boolean[cells.size()];
        for (int i : pool) {
            for (int next : neighbours.get(i)) {
                if (cells.get(next).tank()) {
                    found[next] = true;
                }
            }
        }
        List<Integer> tanks = new ArrayList<>();
        for (int i = 0; i < found.length; i++) {
            if (found[i]) {
                tanks.add(i);
            }
        }
        return tanks;
    }

    private static List<List<Integer>> buildNeighbours(int n, int[][] adjacency) {
        List<List<Integer>> neighbours = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            neighbours.add(new ArrayList<>());
        }
        for (int[] edge : adjacency) {
            int a = edge[0];
            int b = edge[1];
            neighbours.get(a).add(b);
            neighbours.get(b).add(a);
        }
        return neighbours;
    }

    // ==================== integer arithmetic helpers ====================

    private static long clampFill(double raw, long capacity) {
        return Math.max(0, Math.min(capacity, Math.round(raw)));
    }

    /**
     * Largest-remainder allocation of {@code total} mB across cells in proportion to their delta magnitude.
     * When {@code gainSide} is true, allocates over positive deltas; otherwise over negative deltas. The
     * denominator {@code totalGain} equals the magnitude on both sides (deltas sum to zero), so both calls sum
     * to {@code total} and the step conserves.
     */
    private static long[] allocate(long total, long[] delta, int n, boolean gainSide, long totalGain) {
        long[] alloc = new long[n];
        long[] remainder = new long[n];
        long assigned = 0;
        for (int i = 0; i < n; i++) {
            long magnitude = gainSide ? Math.max(0, delta[i]) : Math.max(0, -delta[i]);
            if (magnitude == 0) {
                continue;
            }
            long numerator = total * magnitude;
            alloc[i] = numerator / totalGain;
            remainder[i] = numerator % totalGain;
            assigned += alloc[i];
        }
        // Hand out the leftover one mB at a time to the largest remainders.
        long leftover = total - assigned;
        while (leftover > 0) {
            int best = -1;
            long bestRem = -1;
            for (int i = 0; i < n; i++) {
                if (remainder[i] > bestRem) {
                    bestRem = remainder[i];
                    best = i;
                }
            }
            if (best < 0 || bestRem <= 0) {
                break;
            }
            alloc[best]++;
            remainder[best] = 0;
            leftover--;
        }
        return alloc;
    }
}
