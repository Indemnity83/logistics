package com.logistics.core.lib.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RoutingPreference}, the one tie-breaking rule shared by every
 * selection point in the network.
 */
class RoutingPreferenceTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 0, 0);

    // ===== "Most positive direction": Y, then X, then Z, greater wins =====

    @Test
    void mostPositiveFirst_prefersGreaterY() {
        assertTrue(RoutingPreference.mostPositiveFirst(new BlockPos(0, 70, 0), new BlockPos(9, 64, 9)) < 0);
    }

    @Test
    void mostPositiveFirst_prefersGreaterXWhenYIsEqual() {
        assertTrue(RoutingPreference.mostPositiveFirst(new BlockPos(8, 64, 0), new BlockPos(6, 64, 9)) < 0);
    }

    @Test
    void mostPositiveFirst_prefersGreaterZWhenYAndXAreEqual() {
        assertTrue(RoutingPreference.mostPositiveFirst(new BlockPos(6, 64, 8), new BlockPos(6, 64, 7)) < 0);
    }

    @Test
    void mostPositiveFirst_isATotalOrderOverDistinctPositions() {
        // Every distinct pair must order strictly one way, so a tie always has exactly one winner.
        List<BlockPos> positions = new ArrayList<>();
        for (int y = 0; y < 3; y++) {
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) positions.add(new BlockPos(x, y, z));
            }
        }
        for (BlockPos a : positions) {
            for (BlockPos b : positions) {
                int order = RoutingPreference.mostPositiveFirst(a, b);
                if (a.equals(b)) {
                    assertEquals(0, order, a + " vs itself");
                } else {
                    assertNotEquals(0, order, a + " vs " + b + " must be strictly ordered");
                    assertEquals(order > 0, RoutingPreference.mostPositiveFirst(b, a) < 0, "antisymmetric");
                }
            }
        }
    }

    // ===== Distance outranks position =====

    @Test
    void among_prefersTheNearerCandidateEvenWhenPositionDisagrees() {
        BlockPos near = new BlockPos(-2, 0, 0);
        BlockPos far = new BlockPos(6, 0, 0);
        HopDistance distance = (from, to) -> Math.abs(to.getX() - from.getX());

        assertTrue(RoutingPreference.mostPositiveFirst(far, near) < 0, "far is the more positive position");
        assertTrue(RoutingPreference.among(ORIGIN, distance).compare(near, far) < 0);
    }

    @Test
    void among_fallsBackToPositionWhenDistancesAreEqual() {
        BlockPos lessPositive = new BlockPos(6, 64, 7);
        BlockPos morePositive = new BlockPos(8, 64, 8);
        HopDistance sameDistance = (from, to) -> 4;

        assertTrue(RoutingPreference.among(ORIGIN, sameDistance).compare(morePositive, lessPositive) < 0);
    }

    @Test
    void among_ranksUnreachableCandidatesLastButStillDeterministically() {
        BlockPos reachable = new BlockPos(-2, 0, 0);
        BlockPos unreachable = new BlockPos(6, 0, 0);
        HopDistance distance = (from, to) -> to.equals(unreachable) ? HopDistance.UNREACHABLE : 5;

        Comparator<BlockPos> order = RoutingPreference.among(ORIGIN, distance);
        assertTrue(order.compare(reachable, unreachable) < 0);
        assertTrue(order.compare(unreachable, unreachable) == 0);
    }

    @Test
    void among_withoutASourceRanksByPositionAlone() {
        BlockPos lessPositive = new BlockPos(6, 64, 7);
        BlockPos morePositive = new BlockPos(8, 64, 8);
        // A distance oracle that would otherwise flip the order must not be consulted at all.
        HopDistance neverCalled = (from, to) -> {
            throw new AssertionError("distance must not be measured without a source");
        };

        assertTrue(RoutingPreference.among(null, neverCalled).compare(morePositive, lessPositive) < 0);
    }

    @Test
    void among_withUnknownDistanceRanksByPositionAlone() {
        BlockPos lessPositive = new BlockPos(6, 64, 7);
        BlockPos morePositive = new BlockPos(8, 64, 8);

        assertTrue(RoutingPreference.among(ORIGIN, HopDistance.UNKNOWN)
                .compare(morePositive, lessPositive) < 0);
    }
}
