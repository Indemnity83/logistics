package com.logistics.power.cable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class CableNetworkPlannerTest {
    private static final Comparator<String> STRING_ORDER = Comparator.naturalOrder();

    @Test
    void allocateToDemand_distributesFiniteBudgetByDemandWeight() {
        List<CableNetworkPlanner.Allocation<String>> allocations = CableNetworkPlanner.allocateToDemand(
                List.of(
                        target("a", 30),
                        target("b", 10)),
                20,
                Set.of(),
                Map.of(),
                new HashMap<>(),
                STRING_ORDER);

        assertThat(amountsByTarget(allocations))
                .containsEntry("a", 15L)
                .containsEntry("b", 5L);
    }

    @Test
    void allocateToDemand_skipsBlockedTargetsAndSubtractsDeliveredFiniteDemand() {
        CableNetworkPlanner.ConnectionKey blocked = key("b");

        List<CableNetworkPlanner.Allocation<String>> allocations = CableNetworkPlanner.allocateToDemand(
                List.of(
                        target("a", 10),
                        target("b", 10),
                        target("c", Long.MAX_VALUE)),
                6,
                Set.of(blocked),
                Map.of(key("a"), 8L),
                new HashMap<>(),
                STRING_ORDER);

        assertThat(amountsByTarget(allocations))
                .containsEntry("a", 2L)
                .containsEntry("c", 4L)
                .doesNotContainKey("b");
    }

    @Test
    void allocateToDemand_usesAllocationDebtToRotateRemainderFairly() {
        Map<CableNetworkPlanner.ConnectionKey, Double> allocationDebt = new HashMap<>();
        List<CableNetworkPlanner.Target<String>> targets = List.of(
                target("a", Long.MAX_VALUE),
                target("b", Long.MAX_VALUE));

        List<CableNetworkPlanner.Allocation<String>> first = CableNetworkPlanner.allocateToDemand(
                targets, 1, Set.of(), Map.of(), allocationDebt, STRING_ORDER);
        List<CableNetworkPlanner.Allocation<String>> second = CableNetworkPlanner.allocateToDemand(
                targets, 1, Set.of(), Map.of(), allocationDebt, STRING_ORDER);

        assertThat(amountsByTarget(first))
                .containsEntry("a", 1L)
                .doesNotContainKey("b");
        assertThat(amountsByTarget(second))
                .containsEntry("b", 1L)
                .doesNotContainKey("a");
    }

    @Test
    void findBestRoute_prefersPathWithHigherRemainingTransfer() {
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos lowMiddle = new BlockPos(1, 0, 0);
        BlockPos end = new BlockPos(2, 0, 0);
        BlockPos bypassA = new BlockPos(0, 0, 1);
        BlockPos bypassB = new BlockPos(1, 0, 1);
        BlockPos bypassC = new BlockPos(2, 0, 1);

        Set<BlockPos> cables = Set.of(start, lowMiddle, end, bypassA, bypassB, bypassC);
        Map<BlockPos, Long> capacity = Map.of(
                start, 10L,
                lowMiddle, 3L,
                end, 9L,
                bypassA, 9L,
                bypassB, 9L,
                bypassC, 9L);

        CableNetworkPlanner.CableRoute route = CableNetworkPlanner.findBestRoute(
                cables, start, end, pos -> capacity.getOrDefault(pos, 0L));

        assertThat(route).isNotNull();
        assertThat(route.remainingTransfer()).isEqualTo(9);
        assertThat(route.positions()).containsExactly(start, bypassA, bypassB, bypassC, end);
    }

    @Test
    void findBestRoute_usesStableTieBreakForEqualCapacityPaths() {
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos end = new BlockPos(2, 0, 0);
        BlockPos lowA = new BlockPos(0, -1, 0);
        BlockPos lowB = new BlockPos(1, -1, 0);
        BlockPos lowC = new BlockPos(2, -1, 0);
        BlockPos highA = new BlockPos(0, 1, 0);
        BlockPos highB = new BlockPos(1, 1, 0);
        BlockPos highC = new BlockPos(2, 1, 0);
        Set<BlockPos> cables = Set.of(start, end, lowA, lowB, lowC, highA, highB, highC);

        for (int i = 0; i < 20; i++) {
            CableNetworkPlanner.CableRoute route = CableNetworkPlanner.findBestRoute(
                    cables, start, end, pos -> 10L);

            assertThat(route).isNotNull();
            assertThat(route.remainingTransfer()).isEqualTo(10);
            assertThat(route.positions()).containsExactly(start, lowA, lowB, lowC, end);
        }
    }

    @Test
    void findBestRoute_returnsNullWhenNoPositiveCapacityPathExists() {
        BlockPos start = new BlockPos(0, 0, 0);
        BlockPos end = new BlockPos(1, 0, 0);

        CableNetworkPlanner.CableRoute route = CableNetworkPlanner.findBestRoute(
                Set.of(start, end), start, end, pos -> pos.equals(start) ? 10L : 0L);

        assertThat(route).isNull();
    }

    @Test
    void saturatedAdd_capsAtLongMaxValue() {
        assertThat(CableNetworkPlanner.saturatedAdd(Long.MAX_VALUE - 1, 10)).isEqualTo(Long.MAX_VALUE);
        assertThat(CableNetworkPlanner.saturatedAdd(10, Long.MAX_VALUE)).isEqualTo(Long.MAX_VALUE);
        assertThat(CableNetworkPlanner.saturatedAdd(10, 20)).isEqualTo(30);
    }

    private static CableNetworkPlanner.Target<String> target(String id, long demand) {
        return new CableNetworkPlanner.Target<>(key(id), id, demand);
    }

    private static CableNetworkPlanner.ConnectionKey key(String id) {
        int x = id.charAt(0) - 'a';
        return new CableNetworkPlanner.ConnectionKey(new BlockPos(x, 0, 0), Direction.NORTH);
    }

    private static Map<String, Long> amountsByTarget(List<CableNetworkPlanner.Allocation<String>> allocations) {
        Map<String, Long> amounts = new HashMap<>();
        for (CableNetworkPlanner.Allocation<String> allocation : allocations) {
            amounts.put(allocation.target(), allocation.amount());
        }
        return amounts;
    }
}
