package com.logistics.power.cable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.ToLongFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

final class CableNetworkPlanner {
    private CableNetworkPlanner() {}

    static <T> List<Allocation<T>> allocateToDemand(
            List<Target<T>> targets,
            long input,
            Set<ConnectionKey> blockedTargets,
            Map<ConnectionKey, Long> deliveredThisCall,
            Map<ConnectionKey, Double> allocationDebt,
            Comparator<T> targetOrder) {
        if (input <= 0) return List.of();

        List<TargetDemand<T>> finiteDemands = new ArrayList<>();
        List<Target<T>> infiniteTargets = new ArrayList<>();
        long totalFiniteDemand = 0;
        for (Target<T> target : targets) {
            if (blockedTargets.contains(target.key())) continue;

            long demand = remainingDemand(target, deliveredThisCall);
            if (demand <= 0) continue;

            if (demand == Long.MAX_VALUE) {
                infiniteTargets.add(target);
            } else {
                finiteDemands.add(new TargetDemand<>(target, demand));
                totalFiniteDemand = saturatedAdd(totalFiniteDemand, demand);
            }
        }

        if (finiteDemands.isEmpty() && infiniteTargets.isEmpty()) return List.of();

        List<Allocation<T>> allocations = new ArrayList<>();
        long finiteBudget = Math.min(input, totalFiniteDemand);
        if (finiteBudget > 0) {
            allocations.addAll(allocateWeighted(finiteDemands, finiteBudget, allocationDebt, targetOrder));
        }

        long infiniteBudget = input - finiteBudget;
        if (infiniteBudget > 0 && !infiniteTargets.isEmpty()) {
            allocations.addAll(allocateEvenly(infiniteTargets, infiniteBudget, allocationDebt, targetOrder));
        }

        return allocations;
    }

    private static <T> long remainingDemand(Target<T> target, Map<ConnectionKey, Long> deliveredThisCall) {
        long demand = target.demand();
        if (demand != Long.MAX_VALUE) {
            demand = Math.max(0, demand - deliveredThisCall.getOrDefault(target.key(), 0L));
        }
        return Math.max(0, demand);
    }

    private static <T> List<Allocation<T>> allocateWeighted(
            List<TargetDemand<T>> demands,
            long budget,
            Map<ConnectionKey, Double> allocationDebt,
            Comparator<T> targetOrder) {
        long totalDemand = allocationDemandTotal(demands);
        if (budget <= 0 || totalDemand <= 0) return List.of();

        List<AllocationCandidate<T>> candidates = new ArrayList<>();
        long allocated = 0;
        for (TargetDemand<T> demand : demands) {
            double ideal = (double) budget * (double) demand.amount() / (double) totalDemand;
            long amount = Math.min(demand.amount(), Math.max(0, (long) Math.floor(ideal)));
            allocated += amount;
            candidates.add(new AllocationCandidate<>(demand.target(), demand.amount(), amount, ideal));
        }

        distributeRemainder(candidates, budget - allocated, allocationDebt, targetOrder);
        return finishAllocations(candidates, allocationDebt, targetOrder);
    }

    private static <T> List<Allocation<T>> allocateEvenly(
            List<Target<T>> targets,
            long budget,
            Map<ConnectionKey, Double> allocationDebt,
            Comparator<T> targetOrder) {
        if (budget <= 0 || targets.isEmpty()) return List.of();

        double ideal = (double) budget / (double) targets.size();
        List<AllocationCandidate<T>> candidates = new ArrayList<>();
        long allocated = 0;
        for (Target<T> target : targets) {
            long amount = Math.max(0, (long) Math.floor(ideal));
            allocated += amount;
            candidates.add(new AllocationCandidate<>(target, Long.MAX_VALUE, amount, ideal));
        }

        distributeRemainder(candidates, budget - allocated, allocationDebt, targetOrder);
        return finishAllocations(candidates, allocationDebt, targetOrder);
    }

    private static <T> void distributeRemainder(
            List<AllocationCandidate<T>> candidates,
            long remainder,
            Map<ConnectionKey, Double> allocationDebt,
            Comparator<T> targetOrder) {
        if (remainder <= 0 || candidates.isEmpty()) return;

        candidates.sort(Comparator
                .comparingDouble((AllocationCandidate<T> candidate) -> candidate.priority(allocationDebt)).reversed()
                .thenComparing(AllocationCandidate::targetValue, targetOrder));

        while (remainder > 0) {
            boolean distributed = false;
            for (AllocationCandidate<T> candidate : candidates) {
                if (remainder <= 0) break;
                if (!candidate.canReceiveMore()) continue;

                candidate.addOne();
                remainder--;
                distributed = true;
            }
            if (!distributed) break;
        }
    }

    private static <T> List<Allocation<T>> finishAllocations(
            List<AllocationCandidate<T>> candidates,
            Map<ConnectionKey, Double> allocationDebt,
            Comparator<T> targetOrder) {
        List<Allocation<T>> allocations = new ArrayList<>();
        for (AllocationCandidate<T> candidate : candidates) {
            updateAllocationDebt(candidate, allocationDebt);
            if (candidate.amount() > 0) {
                allocations.add(new Allocation<>(candidate.targetValue(), candidate.amount()));
            }
        }
        allocations.sort(Comparator.comparing(Allocation::target, targetOrder));
        return allocations;
    }

    private static <T> void updateAllocationDebt(
            AllocationCandidate<T> candidate, Map<ConnectionKey, Double> allocationDebt) {
        ConnectionKey key = candidate.target().key();
        double nextDebt = allocationDebt.getOrDefault(key, 0.0) + candidate.ideal() - candidate.amount();
        if (Math.abs(nextDebt) < 0.000001) {
            allocationDebt.remove(key);
        } else {
            allocationDebt.put(key, Math.max(-1.0, Math.min(1.0, nextDebt)));
        }
    }

    private static <T> long allocationDemandTotal(List<TargetDemand<T>> demands) {
        long total = 0;
        for (TargetDemand<T> demand : demands) {
            total = saturatedAdd(total, demand.amount());
        }
        return total;
    }

    @Nullable
    static CableRoute findBestRoute(
            Set<BlockPos> cablePositions,
            BlockPos start,
            BlockPos end,
            ToLongFunction<BlockPos> remainingTransfer) {
        if (!cablePositions.contains(start) || !cablePositions.contains(end)) return null;

        long startCapacity = remainingTransfer.applyAsLong(start);
        if (startCapacity <= 0) return null;

        Map<BlockPos, Long> bestCapacity = new HashMap<>();
        Map<BlockPos, BlockPos> previous = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        PriorityQueue<RouteCandidate> queue = new PriorityQueue<>(RouteCandidate.ORDER);

        bestCapacity.put(start, startCapacity);
        queue.add(new RouteCandidate(start, startCapacity));

        while (!queue.isEmpty()) {
            RouteCandidate candidate = queue.poll();
            BlockPos current = candidate.pos();
            if (candidate.capacity() < bestCapacity.getOrDefault(current, 0L)) continue;
            if (!visited.add(current)) continue;
            if (current.equals(end)) break;

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!cablePositions.contains(neighbor)) continue;

                long neighborCapacity = remainingTransfer.applyAsLong(neighbor);
                if (neighborCapacity <= 0) continue;

                long pathCapacity = Math.min(candidate.capacity(), neighborCapacity);
                if (pathCapacity > bestCapacity.getOrDefault(neighbor, 0L)) {
                    bestCapacity.put(neighbor, pathCapacity);
                    previous.put(neighbor, current);
                    queue.add(new RouteCandidate(neighbor, pathCapacity));
                }
            }
        }

        Long remaining = bestCapacity.get(end);
        if (remaining == null || remaining <= 0) return null;

        List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = end;
        path.add(cursor);
        while (!cursor.equals(start)) {
            cursor = previous.get(cursor);
            if (cursor == null) return null;
            path.add(cursor);
        }
        Collections.reverse(path);
        return new CableRoute(List.copyOf(path), remaining);
    }

    static long saturatedAdd(long a, long b) {
        if (a == Long.MAX_VALUE || b == Long.MAX_VALUE) return Long.MAX_VALUE;
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    record ConnectionKey(BlockPos pos, Direction side) {}

    record Target<T>(ConnectionKey key, T value, long demand) {}

    record Allocation<T>(T target, long amount) {}

    record CableRoute(List<BlockPos> positions, long remainingTransfer) {}

    private record RouteCandidate(BlockPos pos, long capacity) {
        private static final Comparator<RouteCandidate> ORDER = Comparator
                .comparingLong(RouteCandidate::capacity).reversed()
                .thenComparingInt(candidate -> candidate.pos().getX())
                .thenComparingInt(candidate -> candidate.pos().getY())
                .thenComparingInt(candidate -> candidate.pos().getZ());
    }

    private record TargetDemand<T>(Target<T> target, long amount) {}

    private static final class AllocationCandidate<T> {
        private final Target<T> target;
        private final long demand;
        private long amount;
        private final double ideal;

        private AllocationCandidate(Target<T> target, long demand, long amount, double ideal) {
            this.target = target;
            this.demand = demand;
            this.amount = amount;
            this.ideal = ideal;
        }

        private Target<T> target() {
            return target;
        }

        private T targetValue() {
            return target.value();
        }

        private long amount() {
            return amount;
        }

        private double ideal() {
            return ideal;
        }

        private double priority(Map<ConnectionKey, Double> allocationDebt) {
            return ideal - Math.floor(ideal) + allocationDebt.getOrDefault(target.key(), 0.0);
        }

        private boolean canReceiveMore() {
            return amount < demand;
        }

        private void addOne() {
            amount++;
        }
    }
}
