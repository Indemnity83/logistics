package com.logistics.power.cable;

import com.logistics.core.LogisticsProfiler;
import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.energy.IEnergyStorage;
import java.util.function.Predicate;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.lib.power.EnergyDemandProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/cable");

    /** Bounds the refund loop so a source that only takes a sliver per call cannot stall a tick. */
    private static final int MAX_REFUND_ATTEMPTS = 16;

    private static boolean strandedEnergyReported = false;

    private final Set<BlockPos> cablePositions = new HashSet<>();
    private final Map<CableNetworkPlanner.ConnectionKey, Double> allocationDebt = new HashMap<>();
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
        LogisticsProfiler.push("cable_scan");
        try {
            DeviceConnections connections = collectDeviceConnections(level, null);
            LogisticsProfiler.popPush("cable_transfer");
            transferBetween(level, connections.sources(), connections.targets(), remainingNetworkTransfer(level));
        } finally {
            LogisticsProfiler.pop();
        }
    }

    public long insert(
            Level level, BlockPos entryCablePos, @Nullable Direction sourceSide,
            long maxAmount, boolean simulate) {
        if (maxAmount <= 0 || !contains(entryCablePos)) {
            return 0;
        }

        resetAccountingIfNeeded(level);
        CableNetworkPlanner.ConnectionKey excludedConnection = sourceSide == null
                ? null
                : new CableNetworkPlanner.ConnectionKey(entryCablePos.relative(sourceSide), sourceSide.getOpposite());
        DeviceConnections connections = collectDeviceConnections(level, excludedConnection);
        long transferLimit = Math.min(
            maxAmount,
            Math.min(remainingCableTransfer(level, entryCablePos), remainingNetworkTransfer(level)));
        return insertIntoTargets(level, connections.targets(), transferLimit, simulate, entryCablePos);
    }

    private DeviceConnections collectDeviceConnections(
            Level level, @Nullable CableNetworkPlanner.ConnectionKey excludedConnection) {
        List<DeviceConnection> sources = new ArrayList<>();
        List<DeviceConnection> targets = new ArrayList<>();
        Set<CableNetworkPlanner.ConnectionKey> seenConnections = new HashSet<>();

        for (BlockPos cablePos : cablePositions) {
            if (!isPositionLoaded(level, cablePos)) continue;

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(dir);
                if (cablePositions.contains(neighborPos)) continue;
                if (!isPositionLoaded(level, neighborPos)) continue;

                Direction side = dir.getOpposite();
                CableNetworkPlanner.ConnectionKey key = new CableNetworkPlanner.ConnectionKey(neighborPos, side);
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
            limit = CableNetworkPlanner.saturatedAdd(limit, cableTransferRate(level, cablePos));
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
        Set<CableNetworkPlanner.ConnectionKey> blockedTargets = new HashSet<>();
        Map<CableNetworkPlanner.ConnectionKey, Long> deliveredThisCall = new HashMap<>();

        while (remaining > 0) {
            long transferredThisPass = 0;
            List<CableNetworkPlanner.Allocation<DeviceConnection>> allocations = allocateToDemand(
                    validTargets, remaining, blockedTargets, deliveredThisCall);
            if (allocations.isEmpty()) break;

            for (CableNetworkPlanner.Allocation<DeviceConnection> allocation : allocations) {
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
        Set<CableNetworkPlanner.ConnectionKey> blockedTargets = new HashSet<>();
        Map<CableNetworkPlanner.ConnectionKey, Long> deliveredThisCall = new HashMap<>();
        // Tracks simulated cable usage within this call so repeated passes don't
        // re-use the same cable capacity (recordTransfer is skipped during simulate).
        Map<BlockPos, Long> provisionalTransfers = simulate ? new HashMap<>() : null;

        while (remaining > 0) {
            long insertedThisPass = 0;
            List<CableNetworkPlanner.Allocation<DeviceConnection>> allocations = allocateToDemand(
                    validTargets, remaining, blockedTargets, deliveredThisCall);
            if (allocations.isEmpty()) break;

            for (CableNetworkPlanner.Allocation<DeviceConnection> allocation : allocations) {
                CableNetworkPlanner.CableRoute route = entryCablePos == null
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
            long amount, @Nullable CableNetworkPlanner.CableRoute route) {
        if (provisionalTransfers == null || amount <= 0 || route == null) return;
        for (BlockPos cablePos : route.positions()) {
            provisionalTransfers.merge(cablePos, amount, CableNetworkPlanner::saturatedAdd);
        }
    }

    private List<DeviceConnection> filterSources(List<DeviceConnection> sources) {
        return filterByStorage(sources, IEnergyStorage::canExtract);
    }

    private List<DeviceConnection> filterTargets(List<DeviceConnection> targets) {
        return filterByStorage(targets, IEnergyStorage::canInsert);
    }

    private static List<DeviceConnection> filterByStorage(
            List<DeviceConnection> connections, Predicate<IEnergyStorage> keep) {
        List<DeviceConnection> result = new ArrayList<>();
        for (DeviceConnection connection : connections) {
            if (keep.test(connection.storage())) {
                result.add(connection);
            }
        }
        return result;
    }

    private long availableSourceEnergy(List<DeviceConnection> sources, long maxAmount) {
        long total = 0;
        for (DeviceConnection source : sources) {
            if (total >= maxAmount) break;
            total = CableNetworkPlanner.saturatedAdd(total, source.storage().extract(maxAmount - total, true));
        }
        return total;
    }

    private long moveFromSources(
            Level level, List<DeviceConnection> sources, DeviceConnection target, long maxAmount) {
        long movedTotal = 0;
        for (DeviceConnection source : sources) {
            if (movedTotal >= maxAmount) break;
            if (source.storage() == target.storage()) continue;

            CableNetworkPlanner.CableRoute route = findBestRoute(level, source.cablePos(), target.cablePos());
            if (route == null || route.remainingTransfer() <= 0) continue;

            long toMove = Math.min(maxAmount - movedTotal, route.remainingTransfer());
            long moved = moveEnergy(source.storage(), target.storage(), toMove);
            if (moved > 0) {
                recordTransfer(moved, route);
                movedTotal += moved;
            }
        }
        return movedTotal;
    }

    /**
     * Pulls up to {@code maxAmount} from {@code source} into {@code target}.
     *
     * <p>A simulated insert is only a hint: a rate-limited storage may accept less on commit
     * than it reported. Anything the target refuses is put back into the source instead of
     * being destroyed, and only what actually arrived is billed to the network.
     *
     * @return the amount the target actually accepted
     */
    static long moveEnergy(IEnergyStorage source, IEnergyStorage target, long maxAmount) {
        if (maxAmount <= 0) return 0;
        long canExtract = source.extract(maxAmount, true);
        if (canExtract <= 0) return 0;
        long canInsert = target.insert(canExtract, true);
        if (canInsert <= 0) return 0;
        long actualToMove = Math.min(canExtract, canInsert);
        long extracted = source.extract(actualToMove, false);
        if (extracted <= 0) return 0;

        long accepted = Math.max(0, target.insert(extracted, false));
        long stranded = refund(source, extracted - accepted);
        if (stranded > 0) reportStrandedEnergy(source, target, stranded);
        return accepted;
    }

    /**
     * Puts {@code amount} back into {@code source}.
     *
     * <p>Sources clamp each insert to their own per-operation input rate, so one call can
     * return far less than the refund. Repeats until the source stops accepting; the source
     * has room by construction, having just given this energy up.
     *
     * @return the amount the source would not take back, which now exists nowhere
     */
    private static long refund(IEnergyStorage source, long amount) {
        long remaining = Math.max(0, amount);
        for (int attempt = 0; remaining > 0 && attempt < MAX_REFUND_ATTEMPTS; attempt++) {
            long put = source.insert(remaining, false);
            if (put <= 0) break;
            remaining -= put;
        }
        return Math.max(0, remaining);
    }

    /** Warns once per run: repeating every tick would drown the log. */
    private static void reportStrandedEnergy(IEnergyStorage source, IEnergyStorage target, long amount) {
        if (strandedEnergyReported) return;
        strandedEnergyReported = true;
        LOGGER.warn(
                "Lost {} energy moving from {} to {}: the target accepted less than its simulated insert"
                        + " promised and the source would not take the remainder back."
                        + " Later occurrences are not logged.",
                amount,
                source.getClass().getName(),
                target.getClass().getName());
    }

    private List<CableNetworkPlanner.Allocation<DeviceConnection>> allocateToDemand(
            List<DeviceConnection> targets, long input, Set<CableNetworkPlanner.ConnectionKey> blockedTargets,
            Map<CableNetworkPlanner.ConnectionKey, Long> deliveredThisCall) {
        List<CableNetworkPlanner.Target<DeviceConnection>> plannerTargets = new ArrayList<>();
        for (DeviceConnection target : targets) {
            plannerTargets.add(new CableNetworkPlanner.Target<>(target.key(), target, targetDemand(target)));
        }
        return CableNetworkPlanner.allocateToDemand(
                plannerTargets, input, blockedTargets, deliveredThisCall, allocationDebt, DeviceConnection.ORDER);
    }

    private long targetDemand(DeviceConnection target) {
        long demand;
        if (target.blockEntity() instanceof EnergyDemandProvider provider) {
            demand = provider.networkDemandPerTick();
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

    private void recordDelivered(
            Map<CableNetworkPlanner.ConnectionKey, Long> deliveredThisCall,
            CableNetworkPlanner.ConnectionKey key,
            long amount) {
        if (amount <= 0) return;
        deliveredThisCall.merge(key, amount, CableNetworkPlanner::saturatedAdd);
    }

    @Nullable
    private CableNetworkPlanner.CableRoute findBestRoute(Level level, BlockPos start, BlockPos end) {
        return findBestRoute(level, start, end, null);
    }

    @Nullable
    private CableNetworkPlanner.CableRoute findBestRoute(Level level, BlockPos start, BlockPos end,
            @Nullable Map<BlockPos, Long> provisionalTransfers) {
        return CableNetworkPlanner.findBestRoute(cablePositions, start, end, cablePos ->
                isPositionLoaded(level, cablePos) ? effectiveRemainingTransfer(level, cablePos, provisionalTransfers) : 0);
    }

    private long effectiveRemainingTransfer(Level level, BlockPos cablePos,
            @Nullable Map<BlockPos, Long> provisionalTransfers) {
        long remaining = remainingCableTransfer(level, cablePos);
        if (provisionalTransfers != null) {
            remaining = Math.max(0, remaining - provisionalTransfers.getOrDefault(cablePos, 0L));
        }
        return remaining;
    }

    private void recordTransfer(long amount, @Nullable CableNetworkPlanner.CableRoute route) {
        if (amount <= 0) return;

        transferredThisTick = CableNetworkPlanner.saturatedAdd(transferredThisTick, amount);
        if (route != null) {
            for (BlockPos cablePos : route.positions()) {
                cableTransferredThisTick.merge(cablePos, amount, CableNetworkPlanner::saturatedAdd);
            }
        }
    }

    private boolean isManagedPushSource(BlockEntity blockEntity) {
        return blockEntity instanceof EngineEntity;
    }

    private record DeviceConnections(List<DeviceConnection> sources, List<DeviceConnection> targets) {}

    private record DeviceConnection(
            BlockPos cablePos, BlockPos pos, Direction side, IEnergyStorage storage, @Nullable BlockEntity blockEntity) {
        private static final Comparator<DeviceConnection> ORDER = Comparator
                .comparingInt((DeviceConnection connection) -> connection.pos().getX())
                .thenComparingInt(connection -> connection.pos().getY())
                .thenComparingInt(connection -> connection.pos().getZ())
                .thenComparingInt(connection -> connection.side().ordinal());

        private CableNetworkPlanner.ConnectionKey key() {
            return new CableNetworkPlanner.ConnectionKey(pos, side);
        }
    }
}
