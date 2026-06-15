package com.logistics.fluid.pipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.fluid.pipe.FluidBodySolver.Cell;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FluidBodySolver")
class FluidBodySolverTest {

    private static final long CAP = 250;

    private static Cell pipe(int elevation, long amount) {
        return new Cell(elevation, CAP, amount, false);
    }

    private static Cell tank(int elevation, long amount, long capacity) {
        return new Cell(elevation, capacity, amount, true);
    }

    private static long sum(long[] xs) {
        long total = 0;
        for (long x : xs) {
            total += x;
        }
        return total;
    }

    private static long sumAmounts(List<Cell> cells) {
        long total = 0;
        for (Cell c : cells) {
            total += c.amount();
        }
        return total;
    }

    private static List<Cell> withAmounts(List<Cell> cells, long[] amounts) {
        List<Cell> next = new ArrayList<>(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);
            next.add(new Cell(c.elevation(), c.capacity(), amounts[i], c.tank()));
        }
        return next;
    }

    // ==================== equilibrium: the four reality checks ====================

    @Test
    @DisplayName("normal U: 20 mB over a full bottom rises equally into both arms (the 10/10 scenario)")
    void normalU_extraVolume_risesBothArmsEqually() {
        // indices: 0..3 bottom (elev 0); 4,5,6 left arm (elev 1,2,3); 7,8,9 right arm (elev 1,2,3).
        List<Cell> cells = List.of(
                pipe(0, 250), pipe(0, 250), pipe(0, 250), pipe(0, 250), // full bottom = 1000
                pipe(1, 20), pipe(2, 0), pipe(3, 0), // left arm, 20 injected at the base
                pipe(1, 0), pipe(2, 0), pipe(3, 0)); // right arm, dry
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {0, 4}, {4, 5}, {5, 6}, {3, 7}, {7, 8}, {8, 9}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        assertThat(out[0]).isEqualTo(250);
        assertThat(out[1]).isEqualTo(250);
        assertThat(out[2]).isEqualTo(250);
        assertThat(out[3]).isEqualTo(250);
        assertThat(out[4]).isEqualTo(10); // left base
        assertThat(out[7]).isEqualTo(10); // right base — the dry arm fills too
        assertThat(out[5]).isZero();
        assertThat(out[8]).isZero();
        assertThat(sum(out)).isEqualTo(sumAmounts(cells)); // conservation
    }

    @Test
    @DisplayName("normal U: pouring all fluid into one arm settles to equal heights in both arms")
    void normalU_oneArm_equalizes() {
        // Small U: 0 bottom (elev 0); 1,2 left arm (elev 1,2); 3,4 right arm (elev 1,2). 300 mB in left.
        List<Cell> cells = List.of(
                pipe(0, 250), // bottom
                pipe(1, 50), pipe(2, 0), // left arm holds 300 total with the bottom
                pipe(1, 0), pipe(2, 0)); // right arm dry
        int[][] edges = {{0, 1}, {1, 2}, {0, 3}, {3, 4}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        // 300 mB over a basin: bottom (cap 250) + two elev-1 cells. Bottom fills, 50 splits 25/25.
        assertThat(out[0]).isEqualTo(250);
        assertThat(out[1]).isEqualTo(25);
        assertThat(out[3]).isEqualTo(25);
        assertThat(out[2]).isZero();
        assertThat(out[4]).isZero();
        assertThat(sum(out)).isEqualTo(sumAmounts(cells));
    }

    @Test
    @DisplayName("inverted U: fluid in each leg stays put — a dry hump keeps the legs independent")
    void invertedU_dryHump_legsStayIndependent() {
        // ∩: 0 left-bottom (elev 0), 1 (elev 1), 2 left-top (elev 2), 3 right-top (elev 2), 4 (elev 1),
        // 5 right-bottom (elev 0). Fluid only in the two bottoms; tops dry.
        List<Cell> cells = List.of(pipe(0, 100), pipe(1, 0), pipe(2, 0), pipe(2, 0), pipe(1, 0), pipe(0, 80));
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        assertThat(out[0]).isEqualTo(100); // left leg keeps its fluid
        assertThat(out[5]).isEqualTo(80); // right leg keeps its (different) fluid — no equalization
        assertThat(out[1]).isZero();
        assertThat(out[2]).isZero();
        assertThat(out[3]).isZero();
        assertThat(out[4]).isZero();
        assertThat(sum(out)).isEqualTo(sumAmounts(cells));
    }

    @Test
    @DisplayName("siphon: once the hump is wet, the two legs are one body and equalize over the top")
    void siphon_wetHump_equalizesOverTop() {
        // Same ∩ shape, but fully primed (every cell wet) with the left leg fuller.
        List<Cell> cells = List.of(pipe(0, 250), pipe(1, 250), pipe(2, 250), pipe(2, 250), pipe(1, 250), pipe(0, 50));
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        // 1300 mB finds its level: both bottoms full (250), both elev-1 full (250), tops split 150/150.
        assertThat(out[0]).isEqualTo(250);
        assertThat(out[5]).isEqualTo(250);
        assertThat(out[1]).isEqualTo(250);
        assertThat(out[4]).isEqualTo(250);
        assertThat(out[2]).isEqualTo(150);
        assertThat(out[3]).isEqualTo(150);
        assertThat(sum(out)).isEqualTo(sumAmounts(cells));
    }

    @Test
    @DisplayName("riser: volume beyond a full horizontal run spills up into the dry riser above it")
    void riser_excessVolumeRisesIntoDryColumn() {
        // 3 horizontal cells (elev 0) holding 790 mB — past their 750 capacity, as if an extractor injected
        // into the network — with a dry vertical riser (elev 1, 2) above the last one.
        List<Cell> cells = List.of(pipe(0, 250), pipe(0, 250), pipe(0, 290), pipe(1, 0), pipe(2, 0));
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 4}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        assertThat(out[0]).isEqualTo(250);
        assertThat(out[1]).isEqualTo(250);
        assertThat(out[2]).isEqualTo(250); // horizontal run full
        assertThat(out[3]).isEqualTo(40); // the 40 mB excess rises into the riser
        assertThat(out[4]).isZero();
        assertThat(sum(out)).isEqualTo(sumAmounts(cells));
    }

    @Test
    @DisplayName("tower: fluid at the bottom of a vertical run stays at the bottom — no free climb")
    void tower_fluidStaysAtBottom() {
        List<Cell> cells = List.of(pipe(0, 100), pipe(1, 0), pipe(2, 0), pipe(3, 0));
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        assertThat(out[0]).isEqualTo(100);
        assertThat(out[1]).isZero();
        assertThat(out[2]).isZero();
        assertThat(out[3]).isZero();
        assertThat(sum(out)).isEqualTo(sumAmounts(cells));
    }

    // ==================== equilibrium: tanks ====================

    @Test
    @DisplayName("perched fluid falls to the bottom of a column and conserves")
    void perched_fluidFallsAndConserves() {
        // A wet cell sitting above two empty cells in a vertical column — must drain to the bottom, not vanish
        // or multiply. (This is the state a loop produces when fluid crests the top and descends.)
        List<Cell> cells = List.of(pipe(0, 0), pipe(1, 0), pipe(2, 100));
        int[][] edges = {{0, 1}, {1, 2}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        assertThat(sum(out)).isEqualTo(100); // conservation — the real test
        assertThat(out[0]).isEqualTo(100); // settled at the bottom
        assertThat(out[2]).isZero();
    }

    @Test
    @DisplayName("tank absorbs adjacent body fluid greedily")
    void tank_absorbsGreedily() {
        List<Cell> cells = List.of(pipe(0, 100), tank(0, 0, 1000));
        int[][] edges = {{0, 1}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        assertThat(out[0]).isZero(); // pipe drained into the tank
        assertThat(out[1]).isEqualTo(100);
        assertThat(sum(out)).isEqualTo(sumAmounts(cells));
    }

    @Test
    @DisplayName("a high tank is never drained to feed an empty body below it")
    void tank_neverDrainedBySolver() {
        List<Cell> cells = List.of(pipe(0, 0), tank(1, 200, 1000));
        int[][] edges = {{0, 1}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        assertThat(out[0]).isZero(); // pipe stays empty
        assertThat(out[1]).isEqualTo(200); // tank keeps its fluid (emptying a tank is the extractor's job)
        assertThat(sum(out)).isEqualTo(sumAmounts(cells));
    }

    @Test
    @DisplayName("empty component is left untouched")
    void empty_noChange() {
        List<Cell> cells = List.of(pipe(0, 0), pipe(1, 0), pipe(0, 0));
        int[][] edges = {{0, 1}, {1, 2}};

        long[] out = FluidBodySolver.equilibrium(cells, edges);

        assertThat(out).containsExactly(0, 0, 0);
    }

    // ==================== step: rate-limited convergence ====================

    @Test
    @DisplayName("step relocates at most the rate per call and conserves fluid")
    void step_boundedAndConserves() {
        List<Cell> cells = List.of(pipe(0, 250), pipe(0, 0), pipe(0, 0), pipe(0, 0));
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}};
        long rate = 20;

        long[] next = FluidBodySolver.step(cells, edges, rate);

        long moved = 0;
        for (int i = 0; i < cells.size(); i++) {
            moved += Math.max(0, next[i] - cells.get(i).amount());
        }
        assertThat(moved).isLessThanOrEqualTo(rate);
        assertThat(sum(next)).isEqualTo(sumAmounts(cells)); // conservation
    }

    @Test
    @DisplayName("an extractor injecting each tick climbs a riser without losing fluid (BE loop)")
    void injectionLoop_climbsRiserAndConserves() {
        // Mirror FluidPipeBlockEntity: 3 horizontal cells (elev 0) + a riser (elev 1..5), extractor at index 0.
        List<Cell> shape = List.of(
                pipe(0, 0), pipe(0, 0), pipe(0, 0), // horizontal: extractor, copper, copper
                pipe(1, 0), pipe(2, 0), pipe(3, 0), pipe(4, 0), pipe(5, 0)); // riser
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}};
        int n = shape.size();
        long perCellCap = CAP;
        long rate = 20;

        long[] amounts = new long[n];
        long injectedTotal = 0;
        for (int tick = 0; tick < 500; tick++) {
            long free = 0;
            for (int i = 0; i < n; i++) {
                free += perCellCap - amounts[i];
            }
            long inject = Math.min(rate, free);
            amounts[0] += inject; // extractor injects into its own cell (may exceed capacity, as the BE does)
            injectedTotal += inject;

            long[] settled = FluidBodySolver.equilibrium(withAmounts(shape, amounts), edges);

            // The solver itself must conserve every tick.
            long before = 0;
            for (long a : amounts) {
                before += a;
            }
            assertThat(sum(settled)).isEqualTo(before);

            // The BE writes back via setContents, which CLAMPS to capacity — that must not drop fluid.
            for (int i = 0; i < n; i++) {
                assertThat(settled[i]).isLessThanOrEqualTo(perCellCap);
                amounts[i] = settled[i];
            }
        }

        assertThat(sum(amounts)).isEqualTo(injectedTotal); // nothing lost over 500 ticks
        assertThat(amounts[6]).isGreaterThan(0); // fluid climbed well up the riser
    }

    @Test
    @DisplayName("inverted-U loop with injection crests, descends the far leg, and never loses fluid")
    void invertedULoop_injection_conserves() {
        // ∩ shape (the user's setup): extractor at bottom-left, up the left leg, over the top, down the right
        // leg to a dead end at bottom-right. Fluid must climb, crest, and descend the far (perched) leg.
        List<Cell> shape = List.of(
                pipe(0, 0), // 0 extractor bottom-left
                pipe(1, 0), pipe(2, 0), pipe(3, 0), // 1-3 left leg
                pipe(4, 0), pipe(4, 0), // 4-5 top bar
                pipe(3, 0), pipe(2, 0), pipe(1, 0), // 6-8 right leg (descending)
                pipe(0, 0)); // 9 bottom-right dead end
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 8}, {8, 9}};
        int n = shape.size();
        long rate = 20;

        long[] amounts = new long[n];
        long injectedTotal = 0;
        for (int tick = 0; tick < 2000; tick++) {
            long free = 0;
            for (int i = 0; i < n; i++) {
                free += CAP - amounts[i];
            }
            long inject = Math.min(rate, free);
            amounts[0] += inject;
            injectedTotal += inject;

            long[] settled = FluidBodySolver.equilibrium(withAmounts(shape, amounts), edges);

            long before = 0;
            for (long a : amounts) {
                before += a;
            }
            assertThat(sum(settled)).as("tick %d conserves", tick).isEqualTo(before);
            for (int i = 0; i < n; i++) {
                assertThat(settled[i]).as("tick %d cell %d within capacity", tick, i).isLessThanOrEqualTo(CAP);
                amounts[i] = settled[i];
            }
        }

        assertThat(sum(amounts)).isEqualTo(injectedTotal); // no fluid lost over 2000 ticks
        assertThat(amounts[9]).isGreaterThan(0); // fluid crested the top and reached the far bottom
    }

    @Test
    @DisplayName("repeated steps converge to the equilibrium state")
    void step_convergesToEquilibrium() {
        List<Cell> base = List.of(
                pipe(0, 250), pipe(0, 250), pipe(0, 250), pipe(0, 250),
                pipe(1, 20), pipe(2, 0), pipe(3, 0),
                pipe(1, 0), pipe(2, 0), pipe(3, 0));
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {0, 4}, {4, 5}, {5, 6}, {3, 7}, {7, 8}, {8, 9}};
        long[] target = FluidBodySolver.equilibrium(base, edges);

        long[] amounts = new long[base.size()];
        for (int i = 0; i < base.size(); i++) {
            amounts[i] = base.get(i).amount();
        }
        long startTotal = sum(amounts);
        for (int tick = 0; tick < 200; tick++) {
            long[] next = FluidBodySolver.step(withAmounts(base, amounts), edges, 5);
            assertThat(sum(next)).isEqualTo(startTotal); // conserves every tick
            amounts = next;
        }

        assertThat(amounts).containsExactly(target);
    }
}
