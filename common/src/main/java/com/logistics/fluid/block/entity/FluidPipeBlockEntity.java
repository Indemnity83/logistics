package com.logistics.fluid.block.entity;

import com.logistics.LogisticsFluid;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.fluid.FluidUnits;
import com.logistics.fluid.block.FluidConnection;
import com.logistics.fluid.block.FluidPipeBlock;
import com.logistics.fluid.block.FluidPipeKind;
import com.logistics.fluid.pipe.FluidBodySolver;
import com.logistics.fluid.pipe.FluidExtraction;
import com.logistics.fluid.pipe.FluidPipe;
import com.logistics.fluid.pipe.FluidProvider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity backing both fluid pipe kinds.
 *
 * <p>Holds a single-variant {@link FluidTankComponent} buffer (single-fluid-per-segment + backpressure for
 * free) and, for the extractor kind, an {@link EnergyComponent} fed by engines. Each server tick the pipe
 * refreshes its connection cache and syncs visual changes to clients. Fluid movement is not yet wired up —
 * the previous physics-engine approach was removed and a new transport mechanism will hook into {@code
 * serverTick}.
 */
public class FluidPipeBlockEntity extends BaseBlockEntity
        implements HasFluidStorage, HasEnergyStorage, AcceptsLowTierEnergy {

    private static final long ENERGY_CAPACITY = 1000;
    private static final long ENERGY_MAX_INSERT = 100;
    private static final int SYNC_STEPS = 16;

    private final FluidPipeKind kind;
    private final FluidTankComponent tank;

    /** Bitmask of wrench-disabled sides (bit per {@link Direction#get3DDataValue()}). */
    private int disabledMask;

    @Nullable
    private final EnergyComponent energy;

    // Connection cache (server-computed, synced to client for shape + arm rendering).
    private final FluidConnection[] connections = new FluidConnection[6];
    private boolean connectionCacheDirty = true;

    // Game tick this pipe's component was last solved. Leader-election: the first member of a connected
    // component to tick each game-tick solves the whole component once; the rest skip. Transient.
    private long lastSolvedTick = -1;

    // Client-sync coalescing: resync on fluid-type change, a coarse level step, or the empty transition.
    private Fluid lastSyncedFluid = Fluids.EMPTY;
    private long lastSyncedStep = -1;
    private boolean lastSyncedEmpty = true;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsFluid.ENTITY.FLUID_PIPE_BLOCK_ENTITY, pos, state);
        this.kind = kindOf(state);
        for (int i = 0; i < 6; i++) {
            connections[i] = FluidConnection.NONE;
        }

        LogisticsConfig.FluidPipeConfig cfg = LogisticsConfig.get().fluidPipe;
        long capacityMb = kind.isExtractor() ? cfg.woodenCapacity : cfg.copperCapacity;
        this.tank = new FluidTankComponent(FluidUnits.mb((int) capacityMb), this::setChanged);
        this.energy = kind.isExtractor()
                ? new EnergyComponent(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0, this::setChanged)
                : null;
    }

    private static FluidPipeKind kindOf(BlockState state) {
        return state.getBlock() instanceof FluidPipeBlock block ? block.kind() : FluidPipeKind.COPPER;
    }

    public FluidPipeKind kind() {
        return kind;
    }

    public FluidTankComponent tank() {
        return tank;
    }

    public FluidConnection connection(Direction direction) {
        return connections[direction.get3DDataValue()];
    }

    public boolean isExtractor() {
        return kind.isExtractor();
    }

    // ==================== Capabilities ====================

    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        if (side != null && isDisabled(side)) {
            return null;
        }
        return tank;
    }

    @Override
    @Nullable
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return energy;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(Direction from) {
        return energy != null;
    }

    // ==================== Wrench side toggling ====================

    public boolean isDisabled(Direction direction) {
        return (disabledMask & (1 << direction.get3DDataValue())) != 0;
    }

    public void toggleSide(Direction direction) {
        disabledMask ^= (1 << direction.get3DDataValue());
        invalidateConnectionCache();
        invalidateNeighbour(direction);
        markDirtyAndSync();
    }

    public void resetSides() {
        if (disabledMask != 0) {
            disabledMask = 0;
            invalidateConnectionCache();
            for (Direction direction : Direction.values()) {
                invalidateNeighbour(direction);
            }
            markDirtyAndSync();
        }
    }

    public void invalidateConnectionCache() {
        connectionCacheDirty = true;
    }

    /** Invalidate the adjacent pipe's cache too — its connection toward this pipe just changed. */
    private void invalidateNeighbour(Direction direction) {
        Level level = getLevel();
        if (level != null
                && level.getBlockEntity(getBlockPos().relative(direction)) instanceof FluidPipeBlockEntity neighbour) {
            neighbour.invalidateConnectionCache();
        }
    }

    // ==================== Server tick ====================

    public static void tick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.serverTick(level);
    }

    private void serverTick(Level level) {
        refreshConnections(level, getBlockPos());
        solveComponentIfLeader(level);
        syncIfChanged();
    }

    /**
     * Settles this pipe's connected component each tick via the pure {@link FluidBodySolver} ("water finds its
     * level") and drives extraction for the component's powered extractors. Leader-election: the first member to
     * tick each game-tick floods the whole {@code PIPE}-connected component, lets every powered extractor inject
     * newly-pulled fluid (energy-gated), settles the component to {@link FluidBodySolver#equilibrium}, and writes
     * the settled amounts back — then marks every member solved so the rest of the component skip this tick.
     *
     * <p><b>Flow.</b> Fluid may only enter cells already wet or adjacent to one (a one-cell-per-tick advancing
     * front), so it visibly flows rather than appearing instantly at the far end; within the already-wet region
     * the body still settles instantly (hydraulic pressure), which is what lets fluid climb a riser — the excess
     * rises to the network's surface, funded by extraction energy ("infinite head").
     *
     * <p>Pipes also push fluid into adjacent handlers, paced at the transfer rate — sideways/down from any wet
     * pipe, and upward (filling a tank from below) once the pipe is full. A component carrying more than one
     * fluid is solved once per fluid ({@link #solveFluid}); each fluid only flows through its own and empty
     * cells, so the fluids never mix.
     */
    private void solveComponentIfLeader(Level level) {
        long tick = level.getGameTime();
        if (lastSolvedTick == tick || (tank.isEmpty() && !isExtractor())) {
            return;
        }

        // Flood the PIPE-connected component (loaded block entities only — getBlockEntity returns null in
        // unloaded chunks, so the solve stays bounded to what is actually ticking).
        List<FluidPipeBlockEntity> members = new ArrayList<>();
        Map<BlockPos, Integer> index = new HashMap<>();
        Deque<FluidPipeBlockEntity> queue = new ArrayDeque<>();
        members.add(this);
        index.put(getBlockPos(), 0);
        queue.add(this);
        while (!queue.isEmpty()) {
            FluidPipeBlockEntity member = queue.poll();
            BlockPos pos = member.getBlockPos();
            for (Direction direction : Direction.values()) {
                if (member.connection(direction) != FluidConnection.PIPE) {
                    continue;
                }
                BlockPos neighbourPos = pos.relative(direction);
                if (index.containsKey(neighbourPos)) {
                    continue;
                }
                if (level.getBlockEntity(neighbourPos) instanceof FluidPipeBlockEntity neighbour) {
                    index.put(neighbourPos, members.size());
                    members.add(neighbour);
                    queue.add(neighbour);
                }
            }
        }

        for (FluidPipeBlockEntity member : members) {
            member.lastSolvedTick = tick;
        }

        LogisticsConfig.FluidPipeConfig cfg = LogisticsConfig.get().fluidPipe;
        int n = members.size();

        // Snapshot every cell: amount, capacity, and the fluid it holds (null when empty).
        long[] amount = new long[n];
        long[] capacity = new long[n];
        IFluidKey[] cellFluid = new IFluidKey[n];
        for (int i = 0; i < n; i++) {
            FluidTankComponent memberTank = members.get(i).tank;
            amount[i] = FluidUnits.toMillibuckets(memberTank.getAmount());
            capacity[i] = FluidUnits.toMillibuckets(memberTank.getCapacity());
            IFluidKey key = memberTank.getFluidKey();
            cellFluid[i] = key.isBlank() ? null : key;
        }

        // Adjacency among members (PIPE connections).
        List<List<Integer>> neighbours = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            neighbours.add(new ArrayList<>());
        }
        for (int i = 0; i < n; i++) {
            FluidPipeBlockEntity member = members.get(i);
            for (Direction direction : Direction.values()) {
                if (member.connection(direction) != FluidConnection.PIPE) {
                    continue;
                }
                Integer j = index.get(member.getBlockPos().relative(direction));
                if (j != null && j.intValue() != i) {
                    neighbours.get(i).add(j);
                }
            }
        }

        // The fluid each powered extractor would pull, so an empty network can discover what to solve for.
        IFluidKey[] extractorSource = new IFluidKey[n];
        if (cfg.activeExtraction) {
            for (int i = 0; i < n; i++) {
                if (members.get(i).energy != null) {
                    extractorSource[i] = sourceFluid(members.get(i), level);
                }
            }
        }

        // Solve each fluid present (or extractable) in its own single-fluid region. A cell holding a different
        // fluid is cut from a region's graph ("that direction doesn't exist"), so fluids never mix; empty cells
        // are claimed by whichever fluid's pass reaches them first. A single-fluid network runs this just once.
        Set<IFluidKey> fluids = new LinkedHashSet<>();
        for (int i = 0; i < n; i++) {
            if (cellFluid[i] != null) {
                fluids.add(cellFluid[i]);
            }
        }
        for (int i = 0; i < n; i++) {
            if (extractorSource[i] != null) {
                fluids.add(extractorSource[i]);
            }
        }
        for (IFluidKey f : fluids) {
            solveFluid(level, members, neighbours, amount, capacity, cellFluid, extractorSource, f, tick, cfg);
        }
    }

    /** The first non-empty fluid an extractor's adjacent handlers offer, or {@code null}. */
    private IFluidKey sourceFluid(FluidPipeBlockEntity extractor, Level level) {
        for (FluidProvider<IFluidKey> provider : extractor.gatherProviders(level)) {
            IFluidKey offered = provider.fluid();
            if (offered != null) {
                return offered;
            }
        }
        return null;
    }

    /**
     * Settles one fluid's single-fluid region within the component. Cells holding a different fluid are cut out
     * (their edges dropped, capacity 0) so nothing mixes; empty cells join this region and become this fluid once
     * filled, claiming them from later passes. Mutates {@code amount}/{@code cellFluid} so subsequent fluids see
     * what this pass claimed, and writes the affected tanks and handlers back.
     */
    private void solveFluid(Level level, List<FluidPipeBlockEntity> members, List<List<Integer>> neighbours,
            long[] amount, long[] capacity, IFluidKey[] cellFluid, IFluidKey[] extractorSource, IFluidKey fluid,
            long tick, LogisticsConfig.FluidPipeConfig cfg) {
        int n = members.size();

        // This fluid's amount per cell, and which cells it may occupy (its own cells + empty cells).
        long[] fluidAmount = new long[n];
        boolean[] participating = new boolean[n];
        for (int i = 0; i < n; i++) {
            participating[i] = cellFluid[i] == null || fluid.equals(cellFluid[i]);
            fluidAmount[i] = fluid.equals(cellFluid[i]) ? amount[i] : 0;
        }
        long[] stored = fluidAmount.clone();

        // Flow speed = this fluid's own vanilla spread delay; advance the front one cell on an advance tick.
        int delay = Math.max(1, fluid.getFluid().getTickDelay(level));
        boolean advance = tick % delay == 0;

        boolean[] frontier = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (stored[i] > 0 || (members.get(i).isExtractor() && fluid.equals(extractorSource[i]))) {
                frontier[i] = true;
            }
        }
        if (advance) {
            for (int i = 0; i < n; i++) {
                if (stored[i] > 0) {
                    for (int j : neighbours.get(i)) {
                        if (participating[j]) {
                            frontier[j] = true;
                        }
                    }
                }
            }
        }

        // Extraction: extractors whose source is this fluid pull it into their own cell.
        if (cfg.activeExtraction) {
            long frontierFreeSpace = 0;
            for (int i = 0; i < n; i++) {
                if (frontier[i] && participating[i]) {
                    frontierFreeSpace += capacity[i] - fluidAmount[i];
                }
            }
            for (int i = 0; i < n; i++) {
                FluidPipeBlockEntity extractor = members.get(i);
                if (extractor.energy == null || !fluid.equals(extractorSource[i]) || !participating[i]) {
                    continue; // wrong source fluid, or this cell is already claimed by another fluid this tick
                }
                long room = cfg.passiveSettling ? frontierFreeSpace : capacity[i] - fluidAmount[i];
                if (room <= 0) {
                    continue;
                }
                IFluidKey target = fluid;
                List<FluidProvider<IFluidKey>> sources =
                        extractor.gatherProviders(level).stream().filter(s -> target.equals(s.fluid())).toList();
                FluidPipe<IFluidKey> pulled = new FluidPipe<>();
                long budget = Math.min(cfg.copperTransferRate, room);
                FluidExtraction.Result result = FluidExtraction.tick(
                        pulled, sources, extractor.energy.getAmount(), budget, cfg.woodenRequiresEngine);
                if (pulled.amount() > 0) {
                    extractor.energy.consume(result.energyToConsume());
                    fluidAmount[i] += pulled.amount();
                    amount[i] = fluidAmount[i];
                    cellFluid[i] = fluid;
                    participating[i] = true;
                    frontierFreeSpace -= pulled.amount();
                }
            }
        }

        boolean anyFluid = false;
        for (long a : fluidAmount) {
            if (a > 0) {
                anyFluid = true;
                break;
            }
        }
        if (!anyFluid) {
            return;
        }

        if (!cfg.passiveSettling) {
            // Settling disabled (debug): write the injected amounts straight back, no redistribution.
            for (int i = 0; i < n; i++) {
                if (fluidAmount[i] != stored[i]) {
                    members.get(i).tank.setContents(fluidAmount[i] > 0 ? fluid : null, FluidUnits.mb((int) fluidAmount[i]));
                    amount[i] = fluidAmount[i];
                    cellFluid[i] = fluidAmount[i] > 0 ? fluid : null;
                }
            }
            return;
        }

        // Solver cells: this fluid's amount per cell; capacity 0 for cells off-frontier or holding another fluid.
        // Edges only between participating cells — a different-fluid cell cuts the connection, as if that
        // direction didn't exist.
        List<FluidBodySolver.Cell> cells = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            long cap = participating[i] && frontier[i] ? capacity[i] : fluidAmount[i];
            cells.add(new FluidBodySolver.Cell(members.get(i).getBlockPos().getY(), cap, fluidAmount[i], false));
        }
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!participating[i]) {
                continue;
            }
            for (int j : neighbours.get(i)) {
                if (j > i && participating[j]) {
                    edges.add(new int[] {i, j});
                }
            }
        }

        // Tank output: pipes holding this fluid push it into adjacent handlers (sideways/down always, up only
        // from a full pipe), paced at the transfer rate. Extractors only pull, never push.
        long tankRate = cfg.copperTransferRate;
        List<IFluidStorage> tankHandlers = new ArrayList<>();
        Map<BlockPos, Integer> tankCellIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (members.get(i).isExtractor() || !participating[i]) {
                continue;
            }
            BlockPos pos = members.get(i).getBlockPos();
            boolean pipeFull = fluidAmount[i] == capacity[i];
            for (Direction direction : Direction.values()) {
                if (members.get(i).connection(direction) != FluidConnection.HANDLER) {
                    continue;
                }
                if (direction == Direction.UP && !pipeFull) {
                    continue; // push up into a tank only from a full pipe (fluid has risen to the top)
                }
                BlockPos handlerPos = pos.relative(direction);
                Integer existing = tankCellIndex.get(handlerPos);
                if (existing != null) {
                    edges.add(new int[] {i, existing});
                    continue;
                }
                IFluidStorage handler = FluidStorageLookup.find(level, handlerPos, direction.getOpposite());
                if (handler == null) {
                    continue;
                }
                long roomMb = FluidUnits.toMillibuckets(handler.insert(fluid, FluidUnits.mb((int) tankRate), true));
                int cellIndex = cells.size();
                tankCellIndex.put(handlerPos, cellIndex);
                tankHandlers.add(handler);
                cells.add(new FluidBodySolver.Cell(handlerPos.getY(), roomMb, 0, true));
                edges.add(new int[] {i, cellIndex});
            }
        }

        long[] settled = FluidBodySolver.equilibrium(cells, edges.toArray(new int[0][]));
        for (int i = 0; i < n; i++) {
            if (settled[i] != stored[i]) {
                members.get(i).tank.setContents(settled[i] > 0 ? fluid : null, FluidUnits.mb((int) settled[i]));
                amount[i] = settled[i];
                cellFluid[i] = settled[i] > 0 ? fluid : null;
            }
        }
        for (int k = 0; k < tankHandlers.size(); k++) {
            long intake = settled[n + k];
            if (intake > 0) {
                tankHandlers.get(k).insert(fluid, FluidUnits.mb((int) intake), false);
            }
        }
    }

    /** Collects the adjacent fluid handlers this pipe is connected to, each wrapped as a millibucket provider. */
    private List<FluidProvider<IFluidKey>> gatherProviders(Level level) {
        List<FluidProvider<IFluidKey>> providers = new ArrayList<>();
        BlockPos pos = getBlockPos();
        for (Direction direction : Direction.values()) {
            if (connection(direction) != FluidConnection.HANDLER) {
                continue;
            }
            IFluidStorage handler = FluidStorageLookup.find(level, pos.relative(direction), direction.getOpposite());
            if (handler != null) {
                providers.add(asProvider(handler));
            }
        }
        return providers;
    }

    /** Wraps an adjacent {@link IFluidStorage} (platform-native units) as a millibucket {@link FluidProvider}. */
    private static FluidProvider<IFluidKey> asProvider(IFluidStorage handler) {
        return new FluidProvider<>() {
            @Override
            public IFluidKey fluid() {
                for (IFluidView view : handler.contents()) {
                    return view.resource();
                }
                return null;
            }

            @Override
            public long drain(long millibuckets) {
                IFluidKey fluid = fluid();
                if (fluid == null || millibuckets <= 0) {
                    return 0;
                }
                long drainedNative = handler.extract(fluid, FluidUnits.mb((int) millibuckets), false);
                return FluidUnits.toMillibuckets(drainedNative);
            }
        };
    }

    private void refreshConnections(Level level, BlockPos pos) {
        if (!connectionCacheDirty) {
            return;
        }
        FluidPipeBlock block = (FluidPipeBlock) getBlockState().getBlock();
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            FluidConnection now = block.computeConnection(level, pos, direction, disabledMask);
            if (now != connections[direction.get3DDataValue()]) {
                connections[direction.get3DDataValue()] = now;
                changed = true;
            }
        }
        connectionCacheDirty = false;
        if (changed) {
            markDirtyAndSync();
        }
    }

    /**
     * Sync to clients on a fluid-type change, a coarse rendered-level step, or — critically — the
     * empty↔non-empty transition (so an emptied pipe never leaves a stale "sliver" on screen). The coarse
     * step keeps packet volume bounded on long flowing lines.
     */
    private void syncIfChanged() {
        Fluid fluid = tank.getFluidKey().getFluid();
        boolean empty = tank.isEmpty();
        long capacity = Math.max(1, tank.getCapacity());
        long step = (tank.getAmount() * SYNC_STEPS) / capacity;
        if (fluid != lastSyncedFluid || empty != lastSyncedEmpty || step != lastSyncedStep) {
            lastSyncedFluid = fluid;
            lastSyncedEmpty = empty;
            lastSyncedStep = step;
            markDirtyAndSync();
        }
    }

    // ==================== Persistence ====================

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLogisticsData(tag, registries);
        tank.writeNbt(tag, "Tank");
        if (disabledMask != 0) {
            tag.putInt("Disabled", disabledMask);
        }
        tag.putInt("Conn", encodeConnections());
        if (energy != null) {
            energy.writeNbt(tag, "Energy");
        }
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        tank.readNbt(tag, "Tank");
        disabledMask = NbtCompat.getInt(tag, "Disabled", 0);
        decodeConnections(NbtCompat.getInt(tag, "Conn", 0));
        if (energy != null) {
            energy.readNbt(tag, "Energy");
        }
    }

    private int encodeConnections() {
        int code = 0;
        for (Direction direction : Direction.values()) {
            int i = direction.get3DDataValue();
            code |= (connections[i].ordinal() & 0x3) << (i * 2);
        }
        return code;
    }

    private void decodeConnections(int code) {
        for (Direction direction : Direction.values()) {
            int i = direction.get3DDataValue();
            connections[i] = FluidConnection.byOrdinal((code >> (i * 2)) & 0x3);
        }
    }
}
