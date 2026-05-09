package com.logistics.power.cable;

import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.core.lib.power.EnergyDemandProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Represents a connected group of cables that transfers energy without storage.
 *
 * <p>Each tick, the network:
 * <ol>
 *   <li>Finds source devices that expose extractable energy</li>
 *   <li>Finds target devices that can accept energy</li>
 *   <li>Transfers only energy that can be accepted by a target</li>
 * </ol>
 *
 * <p>Energy transfer uses simulate-boolean semantics: capacity is checked first
 * ({@code simulate=true}), then energy is committed ({@code simulate=false}).
 * This is compatible with both Fabric (Team Reborn Energy) and NeoForge Energy.
 *
 * <p>Push-based sources, such as engines, insert into a cable endpoint directly.
 * That insertion is forwarded to connected targets; any energy that cannot be
 * delivered is rejected rather than stored.
 */
public class CableNetwork {
    private final Set<BlockPos> cablePositions = new HashSet<>();
    private final Map<DeviceConnectionKey, Double> allocationDebt = new HashMap<>();
    private final Map<BlockPos, Long> cableTransferredThisTick = new HashMap<>();

    private long accountingGameTime = Long.MIN_VALUE;
    private long transferredThisTick = 0;

    public Set<BlockPos> getCablePositions() {
        return Set.copyOf(cablePositions);
    }

    public boolean contains(BlockPos pos) {
        return cablePositions.contains(pos);
    }

    public boolean isEmpty() {
        return cablePositions.isEmpty();
    }

    /**
     * Builds a cable network by flood-filling from the given starting position.
     */
    public static CableNetwork buildFrom(Level level, BlockPos start) {
        CableNetwork network = new CableNetwork();
        if (!isPositionLoaded(level, start)) {
            return network;
        }

        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        network.cablePositions.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!network.cablePositions.contains(neighbor)
                        && isPositionLoaded(level, neighbor)
                        && level.getBlockEntity(neighbor) instanceof CableBlockEntity) {
                    network.cablePositions.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return network;
    }

    /**
     * Moves energy across the network without buffering it in cables.
     *
     * <p>Managed push sources, such as engines, are skipped here because they
     * actively push into cable endpoints on their own output cadence.
     */
    public void tick(Level level) {
        resetAccountingIfNeeded(level);
        DeviceConnections connections = collectDeviceConnections(level, null);
        transferBetween(level, connections.sources(), connections.targets(), remainingNetworkTransfer(level));
    }

    public long insert(
            Level level, BlockPos entryCablePos, @Nullable Direction sourceSide,
            long maxAmount, boolean simulate) {
        if (maxAmount <= 0 || !contains(entryCablePos)) {
            return 0;
        }

        resetAccountingIfNeeded(level);
        DeviceConnectionKey excludedConnection = sourceSide == null
                ? null
                : new DeviceConnectionKey(entryCablePos.relative(sourceSide), sourceSide.getOpposite());
        DeviceConnections connections = collectDeviceConnections(level, excludedConnection);
        long transferLimit = Math.min(
            maxAmount,
            Math.min(remainingCableTransfer(level, entryCablePos), remainingNetworkTransfer(level)));
        return insertIntoTargets(level, connections.targets(), transferLimit, simulate, entryCablePos);
    }

    private DeviceConnections collectDeviceConnections(Level level, @Nullable DeviceConnectionKey excludedConnection) {
        List<DeviceConnection> sources = new ArrayList<>();
        List<DeviceConnection> targets = new ArrayList<>();
        Set<DeviceConnectionKey> seenConnections = new HashSet<>();

        for (BlockPos cablePos : cablePositions) {
            if (!isPositionLoaded(level, cablePos)) continue;

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(dir);
                if (cablePositions.contains(neighborPos)) continue;
                if (!isPositionLoaded(level, neighborPos)) continue;

                Direction side = dir.getOpposite();
                DeviceConnectionKey key = new DeviceConnectionKey(neighborPos, side);
                if (key.equals(excludedConnection)) continue;

                IEnergyStorage storage = EnergyCapabilityLookup.INSTANCE.find(level, neighborPos, side);
                if (storage != null) {
                    if (!seenConnections.add(key)) continue;

                    BlockEntity blockEntity = level.getBlockEntity(neighborPos);
                    DeviceConnection connection = new DeviceConnection(cablePos, neighborPos, side, storage, blockEntity);
                    if (storage.canInsert()) {
                        targets.add(connection);
                    }
                    if (storage.canExtract() && !isManagedPushSource(blockEntity)) {
                        sources.add(connection);
                    }
                }
            }
        }

        sources.sort(DeviceConnection.ORDER);
        targets.sort(DeviceConnection.ORDER);
        return new DeviceConnections(sources, targets);
    }

    private long getNetworkTransferLimit(Level level) {
        long limit = 0;
        for (BlockPos cablePos : cablePositions) {
            limit = saturatedAdd(limit, cableTransferRate(level, cablePos));
        }
        return limit;
    }

    private long cableTransferRate(Level level, BlockPos cablePos) {
        if (!isPositionLoaded(level, cablePos)) return 0;
        return level.getBlockEntity(cablePos) instanceof CableBlockEntity cable ? cable.getTransferRate() : 0;
    }

    private static boolean isPositionLoaded(Level level, BlockPos pos) {
        return level.isLoaded(pos);
    }

    private void resetAccountingIfNeeded(Level level) {
        long gameTime = level.getGameTime();
        if (gameTime == accountingGameTime) return;

        accountingGameTime = gameTime;
        transferredThisTick = 0;
        cableTransferredThisTick.clear();
    }

    private long remainingNetworkTransfer(Level level) {
        return Math.max(0, getNetworkTransferLimit(level) - transferredThisTick);
    }

    private long remainingCableTransfer(Level level, BlockPos cablePos) {
        return Math.max(0, cableTransferRate(level, cablePos) - cableTransferredThisTick.getOrDefault(cablePos, 0L));
    }

    private long transferBetween(
            Level level, List<DeviceConnection> sources, List<DeviceConnection> targets, long maxAmount) {
        if (sources.isEmpty() || targets.isEmpty() || maxAmount <= 0) return 0;

        List<DeviceConnection> validSources = filterSources(sources);
        List<DeviceConnection> validTargets = filterTargets(targets);
        if (validSources.isEmpty() || validTargets.isEmpty()) return 0;

        long input = Math.min(maxAmount, availableSourceEnergy(validSources, maxAmount));
        if (input <= 0) return 0;

        long totalTransferred = 0;
        long remaining = input;
        Set<DeviceConnectionKey> blockedTargets = new HashSet<>();
        Map<DeviceConnectionKey, Long> deliveredThisCall = new HashMap<>();

        while (remaining > 0) {
            long transferredThisPass = 0;
            List<EnergyAllocation> allocations = allocateToDemand(
                    validTargets, remaining, blockedTargets, deliveredThisCall);
            if (allocations.isEmpty()) break;

            for (EnergyAllocation allocation : allocations) {
                long requested = Math.min(allocation.amount(), remaining - transferredThisPass);
                if (requested <= 0) continue;

                long moved = moveFromSources(level, validSources, allocation.target(), requested);
                if (moved > 0) {
                    recordDelivered(deliveredThisCall, allocation.target().key(), moved);
                    totalTransferred += moved;
                    transferredThisPass += moved;
                }
                if (moved < requested) {
                    blockedTargets.add(allocation.target().key());
                }
            }

            if (transferredThisPass <= 0) break;
            remaining -= transferredThisPass;
        }

        return totalTransferred;
    }

    /**
     * Inserts energy into connected targets, distributing by demand.
     * Uses simulate-boolean semantics: if {@code simulate=true}, checks capacity without committing.
     *
     * <p>When simulating, provisional cable transfers are tracked locally so the while-loop's
     * repeated passes don't re-use the same cable capacity (which is never recorded in the
     * real {@code cableTransferredThisTick} during a dry run).
     */
    private long insertIntoTargets(
            Level level, List<DeviceConnection> targets, long maxAmount, boolean simulate,
            @Nullable BlockPos entryCablePos) {
        if (targets.isEmpty() || maxAmount <= 0) return 0;

        List<DeviceConnection> validTargets = filterTargets(targets);
        if (validTargets.isEmpty()) return 0;

        long totalInserted = 0;
        long remaining = maxAmount;
        Set<DeviceConnectionKey> blockedTargets = new HashSet<>();
        Map<DeviceConnectionKey, Long> deliveredThisCall = new HashMap<>();
        // Tracks simulated cable usage within this call so repeated passes don't
        // re-use the same cable capacity (recordTransfer is skipped during simulate).
        Map<BlockPos, Long> provisionalTransfers = simulate ? new HashMap<>() : null;

        while (remaining > 0) {
            long insertedThisPass = 0;
            List<EnergyAllocation> allocations = allocateToDemand(
                    validTargets, remaining, blockedTargets, deliveredThisCall);
            if (allocations.isEmpty()) break;

            for (EnergyAllocation allocation : allocations) {
                CableRoute route = entryCablePos == null
                        ? null
                        : findBestRoute(level, entryCablePos, allocation.target().cablePos(),
                                provisionalTransfers);
                long routeLimit = route == null ? 0 : route.remainingTransfer();
                if (routeLimit <= 0) {
                    blockedTargets.add(allocation.target().key());
                    continue;
                }

                long requested = Math.min(allocation.amount(), Math.min(remaining - insertedThisPass, routeLimit));
                if (requested <= 0) continue;

                long inserted = allocation.target().storage().insert(requested, simulate);
                if (inserted > 0) {
                    if (simulate) {
                        recordProvisional(provisionalTransfers, inserted, route);
                    } else {
                        recordTransfer(inserted, route);
                    }
                    recordDelivered(deliveredThisCall, allocation.target().key(), inserted);
                    totalInserted += inserted;
                    insertedThisPass += inserted;
                }
                if (inserted < requested) {
                    blockedTargets.add(allocation.target().key());
                }
            }

            if (insertedThisPass <= 0) break;
            remaining -= insertedThisPass;
        }

        return totalInserted;
    }

    private void recordProvisional(@Nullable Map<BlockPos, Long> provisionalTransfers,
            long amount, @Nullable CableRoute route) {
        if (provisionalTransfers == null || amount <= 0 || route == null) return;
        for (BlockPos cablePos : route.positions()) {
            provisionalTransfers.merge(cablePos, amount, this::saturatedAdd);
        }
    }

    private List<DeviceConnection> filterSources(List<DeviceConnection> sources) {
        List<DeviceConnection> validSources = new ArrayList<>();
        for (DeviceConnection source : sources) {
            if (source.storage().canExtract()) {
                validSources.add(source);
            }
        }
        return validSources;
    }

    private List<DeviceConnection> filterTargets(List<DeviceConnection> targets) {
        List<DeviceConnection> validTargets = new ArrayList<>();
        for (DeviceConnection target : targets) {
            if (target.storage().canInsert()) {
                validTargets.add(target);
            }
        }
        return validTargets;
    }

    private long availableSourceEnergy(List<DeviceConnection> sources, long maxAmount) {
        long total = 0;
        for (DeviceConnection source : sources) {
            if (total >= maxAmount) break;
            total = saturatedAdd(total, source.storage().extract(maxAmount - total, true));
        }
        return total;
    }

    private long moveFromSources(
            Level level, List<DeviceConnection> sources, DeviceConnection target, long maxAmount) {
        long movedTotal = 0;
        for (DeviceConnection source : sources) {
            if (movedTotal >= maxAmount) break;
            if (source.storage() == target.storage()) continue;

            CableRoute route = findBestRoute(level, source.cablePos(), target.cablePos());
            if (route == null || route.remainingTransfer() <= 0) continue;

            long toMove = Math.min(maxAmount - movedTotal, route.remainingTransfer());
            long canExtract = source.storage().extract(toMove, true);
            if (canExtract <= 0) continue;
            long canInsert = target.storage().insert(canExtract, true);
            if (canInsert <= 0) continue;
            long actualToMove = Math.min(canExtract, canInsert);
            long extracted = source.storage().extract(actualToMove, false);
            target.storage().insert(extracted, false);
            if (extracted > 0) {
                recordTransfer(extracted, route);
                movedTotal += extracted;
            }
        }
        return movedTotal;
    }

    private List<EnergyAllocation> allocateToDemand(
            List<DeviceConnection> targets, long input, Set<DeviceConnectionKey> blockedTargets,
            Map<DeviceConnectionKey, Long> deliveredThisCall) {
        if (input <= 0) return List.of();

        List<TargetDemand> finiteDemands = new ArrayList<>();
        List<DeviceConnection> infiniteTargets = new ArrayList<>();
        long totalFiniteDemand = 0;
        for (DeviceConnection target : targets) {
            if (blockedTargets.contains(target.key())) continue;

            long demand = targetDemand(target, deliveredThisCall);
            if (demand <= 0) continue;

            if (demand == Long.MAX_VALUE) {
                infiniteTargets.add(target);
            } else {
                finiteDemands.add(new TargetDemand(target, demand));
                totalFiniteDemand = saturatedAdd(totalFiniteDemand, demand);
            }
        }

        if (finiteDemands.isEmpty() && infiniteTargets.isEmpty()) return List.of();

        List<EnergyAllocation> allocations = new ArrayList<>();
        long finiteBudget = Math.min(input, totalFiniteDemand);
        if (finiteBudget > 0) {
            allocations.addAll(allocateWeighted(finiteDemands, finiteBudget));
        }

        long infiniteBudget = input - finiteBudget;
        if (infiniteBudget > 0 && !infiniteTargets.isEmpty()) {
            allocations.addAll(allocateEvenly(infiniteTargets, infiniteBudget));
        }

        return allocations;
    }

    private List<EnergyAllocation> allocateWeighted(List<TargetDemand> demands, long budget) {
        long totalDemand = allocationDemandTotal(demands);
        if (budget <= 0 || totalDemand <= 0) return List.of();

        List<AllocationCandidate> candidates = new ArrayList<>();
        long allocated = 0;
        for (TargetDemand demand : demands) {
            double ideal = (double) budget * (double) demand.amount() / (double) totalDemand;
            long amount = Math.min(demand.amount(), Math.max(0, (long) Math.floor(ideal)));
            allocated += amount;
            candidates.add(new AllocationCandidate(demand.target(), demand.amount(), amount, ideal));
        }

        distributeRemainder(candidates, budget - allocated);
        return finishAllocations(candidates);
    }

    private List<EnergyAllocation> allocateEvenly(List<DeviceConnection> targets, long budget) {
        if (budget <= 0 || targets.isEmpty()) return List.of();

        double ideal = (double) budget / (double) targets.size();
        List<AllocationCandidate> candidates = new ArrayList<>();
        long allocated = 0;
        for (DeviceConnection target : targets) {
            long amount = Math.max(0, (long) Math.floor(ideal));
            allocated += amount;
            candidates.add(new AllocationCandidate(target, Long.MAX_VALUE, amount, ideal));
        }

        distributeRemainder(candidates, budget - allocated);
        return finishAllocations(candidates);
    }

    private void distributeRemainder(List<AllocationCandidate> candidates, long remainder) {
        if (remainder <= 0 || candidates.isEmpty()) return;

        candidates.sort(Comparator
                .comparingDouble(AllocationCandidate::priority).reversed()
                .thenComparing(candidate -> candidate.target(), DeviceConnection.ORDER));

        while (remainder > 0) {
            boolean distributed = false;
            for (AllocationCandidate candidate : candidates) {
                if (remainder <= 0) break;
                if (!candidate.canReceiveMore()) continue;

                candidate.addOne();
                remainder--;
                distributed = true;
            }
            if (!distributed) break;
        }
    }

    private List<EnergyAllocation> finishAllocations(List<AllocationCandidate> candidates) {
        List<EnergyAllocation> allocations = new ArrayList<>();
        for (AllocationCandidate candidate : candidates) {
            updateAllocationDebt(candidate);
            if (candidate.amount() > 0) {
                allocations.add(new EnergyAllocation(candidate.target(), candidate.amount()));
            }
        }
        allocations.sort(Comparator.comparing(EnergyAllocation::target, DeviceConnection.ORDER));
        return allocations;
    }

    private void updateAllocationDebt(AllocationCandidate candidate) {
        DeviceConnectionKey key = candidate.target().key();
        double nextDebt = allocationDebt.getOrDefault(key, 0.0) + candidate.ideal() - candidate.amount();
        if (Math.abs(nextDebt) < 0.000001) {
            allocationDebt.remove(key);
        } else {
            allocationDebt.put(key, Math.max(-1.0, Math.min(1.0, nextDebt)));
        }
    }

    private long allocationDemandTotal(List<TargetDemand> demands) {
        long total = 0;
        for (TargetDemand demand : demands) {
            total = saturatedAdd(total, demand.amount());
        }
        return total;
    }

    private long targetDemand(DeviceConnection target, Map<DeviceConnectionKey, Long> deliveredThisCall) {
        long demand;
        if (target.blockEntity() instanceof EnergyDemandProvider provider) {
            demand = provider.networkDemandPerTick();
            if (demand != Long.MAX_VALUE) {
                demand = Math.max(0, demand - deliveredThisCall.getOrDefault(target.key(), 0L));
            }
        } else {
            demand = storageRoom(target.storage());
        }
        return Math.max(0, demand);
    }

    private static long storageRoom(IEnergyStorage storage) {
        long capacity = storage.getCapacity();
        if (capacity == Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0, capacity - storage.getAmount());
    }

    private long saturatedAdd(long a, long b) {
        if (a == Long.MAX_VALUE || b == Long.MAX_VALUE) return Long.MAX_VALUE;
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    private void recordDelivered(Map<DeviceConnectionKey, Long> deliveredThisCall, DeviceConnectionKey key, long amount) {
        if (amount <= 0) return;
        deliveredThisCall.merge(key, amount, this::saturatedAdd);
    }

    @Nullable
    private CableRoute findBestRoute(Level level, BlockPos start, BlockPos end) {
        return findBestRoute(level, start, end, null);
    }

    @Nullable
    private CableRoute findBestRoute(Level level, BlockPos start, BlockPos end,
            @Nullable Map<BlockPos, Long> provisionalTransfers) {
        if (!contains(start) || !contains(end)) return null;

        long startCapacity = effectiveRemainingTransfer(level, start, provisionalTransfers);
        if (startCapacity <= 0) return null;

        Map<BlockPos, Long> bestCapacity = new HashMap<>();
        Map<BlockPos, BlockPos> previous = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        PriorityQueue<BlockPos> queue = new PriorityQueue<>((a, b) -> Long.compare(
                bestCapacity.getOrDefault(b, 0L), bestCapacity.getOrDefault(a, 0L)));

        bestCapacity.put(start, startCapacity);
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!visited.add(current)) continue;
            if (current.equals(end)) break;

            long currentCapacity = bestCapacity.getOrDefault(current, 0L);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!cablePositions.contains(neighbor) || !isPositionLoaded(level, neighbor)) continue;

                long neighborCapacity = effectiveRemainingTransfer(level, neighbor, provisionalTransfers);
                if (neighborCapacity <= 0) continue;

                long pathCapacity = Math.min(currentCapacity, neighborCapacity);
                if (pathCapacity > bestCapacity.getOrDefault(neighbor, 0L)) {
                    bestCapacity.put(neighbor, pathCapacity);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        Long remainingTransfer = bestCapacity.get(end);
        if (remainingTransfer == null || remainingTransfer <= 0) return null;

        List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = end;
        path.add(cursor);
        while (!cursor.equals(start)) {
            cursor = previous.get(cursor);
            if (cursor == null) return null;
            path.add(cursor);
        }
        Collections.reverse(path);
        return new CableRoute(List.copyOf(path), remainingTransfer);
    }

    private long effectiveRemainingTransfer(Level level, BlockPos cablePos,
            @Nullable Map<BlockPos, Long> provisionalTransfers) {
        long remaining = remainingCableTransfer(level, cablePos);
        if (provisionalTransfers != null) {
            remaining = Math.max(0, remaining - provisionalTransfers.getOrDefault(cablePos, 0L));
        }
        return remaining;
    }

    private void recordTransfer(long amount, @Nullable CableRoute route) {
        if (amount <= 0) return;

        transferredThisTick = saturatedAdd(transferredThisTick, amount);
        if (route != null) {
            for (BlockPos cablePos : route.positions()) {
                cableTransferredThisTick.merge(cablePos, amount, this::saturatedAdd);
            }
        }
    }

    private boolean isManagedPushSource(BlockEntity blockEntity) {
        return blockEntity instanceof AbstractEngineBlockEntity;
    }

    private record DeviceConnections(List<DeviceConnection> sources, List<DeviceConnection> targets) {}

    private record DeviceConnection(
            BlockPos cablePos, BlockPos pos, Direction side, IEnergyStorage storage, @Nullable BlockEntity blockEntity) {
        private static final Comparator<DeviceConnection> ORDER = Comparator
                .comparingInt((DeviceConnection connection) -> connection.pos().getX())
                .thenComparingInt(connection -> connection.pos().getY())
                .thenComparingInt(connection -> connection.pos().getZ())
                .thenComparingInt(connection -> connection.side().ordinal());

        private DeviceConnectionKey key() {
            return new DeviceConnectionKey(pos, side);
        }
    }

    private record TargetDemand(DeviceConnection target, long amount) {}

    private record EnergyAllocation(DeviceConnection target, long amount) {}

    private record CableRoute(List<BlockPos> positions, long remainingTransfer) {}

    private final class AllocationCandidate {
        private final DeviceConnection target;
        private final long demand;
        private long amount;
        private final double ideal;

        private AllocationCandidate(DeviceConnection target, long demand, long amount, double ideal) {
            this.target = target;
            this.demand = demand;
            this.amount = amount;
            this.ideal = ideal;
        }

        private DeviceConnection target() {
            return target;
        }

        private long amount() {
            return amount;
        }

        private double ideal() {
            return ideal;
        }

        private double priority() {
            return ideal - Math.floor(ideal) + allocationDebt.getOrDefault(target.key(), 0.0);
        }

        private boolean canReceiveMore() {
            return amount < demand;
        }

        private void addOne() {
            amount++;
        }
    }

    private record DeviceConnectionKey(BlockPos pos, Direction side) {}
}
