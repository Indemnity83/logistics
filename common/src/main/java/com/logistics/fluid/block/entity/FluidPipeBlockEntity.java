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
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.fluid.FluidUnits;
import com.logistics.fluid.block.FluidConnection;
import com.logistics.fluid.block.FluidPipeBlock;
import com.logistics.fluid.block.FluidPipeKind;
import com.logistics.fluid.pipe.FluidExtraction;
import com.logistics.fluid.pipe.FluidPipe;
import com.logistics.fluid.pipe.FluidProvider;
import com.logistics.fluid.pipe.FluidSplit;
import com.logistics.fluid.pipe.TravelingFluid;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity backing every fluid pipe kind.
 *
 * <p>Fluid moves cellularly as discrete {@link TravelingFluid} parcels (the fluid analogue of the item
 * pipes' {@code TravelingItem}). Each pipe ticks independently — no network solve: parcels dwell for a
 * per-fluid spread-rate countdown, then hop one pipe along (splitting equally at junctions), capped at the
 * pipe's transfer rate, with leftover buffered for next tick. One fluid per pipe; a per-kind capacity gives
 * backpressure. Gravity is intentionally ignored (predictable transport over physical accuracy).
 */
public class FluidPipeBlockEntity extends BaseBlockEntity
        implements HasFluidStorage, HasEnergyStorage, AcceptsLowTierEnergy {

    private static final long ENERGY_CAPACITY = 20;
    private static final int SYNC_STEPS = 16;
    private static final float DWELL_MULTIPLIER = 2.0F;

    private final FluidPipeKind kind;
    /** Pipe buffer capacity in mB (per kind); the sum of all parcels never exceeds it. */
    private final long capacityMb;

    /** Fluid parcels currently inside this pipe. One fluid type per pipe (no mixing). */
    private final List<TravelingFluid> parcels = new ArrayList<>();

    /** Bitmask of wrench-disabled sides (bit per {@link Direction#get3DDataValue()}). */
    private int disabledMask;

    /** Merger (directional) pipe only: the single face fluid may exit, or {@code null} when unset. */
    @Nullable
    private Direction outputDirection;

    @Nullable
    private final EnergyComponent energy;

    /** Extractor only: sub-RF fluid remainder carried between ticks so small per-tick pulls still drain energy. */
    private long extractionCarryMb;

    // Connection cache (server-computed, synced to client for shape + arm rendering).
    private final FluidConnection[] connections = new FluidConnection[6];
    private boolean connectionCacheDirty = true;

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
        this.capacityMb = kind.capacity(LogisticsConfig.get().fluidPipe);
        this.energy = kind.isExtractor()
                ? new EnergyComponent(ENERGY_CAPACITY, ENERGY_CAPACITY, 0, this::setChanged)
                : null;
    }

    private static FluidPipeKind kindOf(BlockState state) {
        return state.getBlock() instanceof FluidPipeBlock block ? block.kind() : FluidPipeKind.COPPER;
    }

    public FluidPipeKind kind() {
        return kind;
    }

    public FluidConnection connection(Direction direction) {
        return connections[direction.get3DDataValue()];
    }

    public boolean isExtractor() {
        return kind.isExtractor();
    }

    // ==================== Contents (for rendering + capability) ====================

    /** Total fluid held, summed across parcels, in millibuckets. */
    public long totalMillibuckets() {
        long total = 0;
        for (TravelingFluid parcel : parcels) {
            total += parcel.amount();
        }
        return total;
    }

    /** The single fluid this pipe currently holds, or a blank key when empty. */
    public IFluidKey containedFluid() {
        return parcels.isEmpty() ? SimpleFluidKey.BLANK : parcels.get(0).fluid();
    }

    /** This pipe's buffer capacity, in millibuckets. */
    public long capacityMillibuckets() {
        return capacityMb;
    }

    // Client-side eased display fill (0..1), smoothing the coarsely-synced level so it moves fluidly between
    // sync steps instead of jittering with every packet. -1 = uninitialised. Render-only; never saved/server-used.
    private float displayFill = -1.0F;

    /** Eases the client display fill toward {@code target} (0..1) and returns it; snaps on the first frame. */
    public float advanceDisplayFill(float target) {
        if (displayFill < 0.0F) {
            displayFill = target;
        } else {
            displayFill += (target - displayFill) * 0.2F;
        }
        return displayFill;
    }

    // ==================== Merger output face ====================

    @Nullable
    public Direction outputDirection() {
        return outputDirection;
    }

    /** Sets the merger pipe's output face (or {@code null}); resyncs so the arrow renders. */
    public void setOutputDirection(@Nullable Direction direction) {
        if (outputDirection != direction) {
            outputDirection = direction;
            markDirtyAndSync();
        }
    }

    /** The directions this pipe currently connects on (pipe or handler), in {@link Direction} order. */
    private List<Direction> connectedDirections() {
        List<Direction> result = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            if (connection(direction) != FluidConnection.NONE) {
                result.add(direction);
            }
        }
        return result;
    }

    /** Wrench action for the merger pipe: advance the output to the next connected face, like the item Merger. */
    public void cycleOutputDirection() {
        List<Direction> connected = connectedDirections();
        if (connected.isEmpty()) {
            setOutputDirection(null);
            return;
        }
        int index = outputDirection == null ? -1 : connected.indexOf(outputDirection);
        setOutputDirection(connected.get((index + 1) % connected.size()));
    }

    /** Keep the merger's output on a valid connected face; default to the first connection, clear when none. */
    private void ensureValidOutput() {
        if (!kind.isDirectional()) {
            return;
        }
        List<Direction> connected = connectedDirections();
        if (connected.isEmpty()) {
            setOutputDirection(null);
        } else if (outputDirection == null || !connected.contains(outputDirection)) {
            setOutputDirection(connected.getFirst());
        }
    }

    // ==================== Capabilities ====================

    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        if (side != null && isDisabled(side)) {
            return null;
        }
        return new PipeFluidStorage(this, side == null ? Direction.UP : side);
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

    // ==================== Accept fluid (neighbour hop + capability insert) ====================

    /**
     * How much of {@code fluid} this pipe could take if it entered from {@code enteringSide}, in mB — capacity
     * minus what's held, but 0 if it holds a different fluid, or if this is a merger and the fluid would be
     * entering through its output face (check valve).
     */
    public long roomFor(IFluidKey fluid, Direction enteringSide) {
        if (kind.isDirectional() && enteringSide == outputDirection) {
            return 0;
        }
        IFluidKey contained = containedFluid();
        if (!contained.isBlank() && !contained.equals(fluid)) {
            return 0;
        }
        return Math.max(0, capacityMb - totalMillibuckets());
    }

    /**
     * Adds up to {@code amountMb} of {@code fluid} entering from {@code enteringSide} into this pipe, as a parcel
     * heading {@code enteringSide.getOpposite()} with a fresh spread-rate countdown. Bounded by {@link #roomFor}.
     * Same-tick arrivals on the same heading merge. Returns the amount accepted.
     */
    public long acceptFluid(IFluidKey fluid, long amountMb, Direction enteringSide) {
        long accepted = Math.min(amountMb, roomFor(fluid, enteringSide));
        if (accepted <= 0) {
            return 0;
        }
        Direction heading = enteringSide.getOpposite();
        int countdown = spreadRate(fluid);
        for (TravelingFluid parcel : parcels) {
            if (parcel.heading() == heading && parcel.countdown() == countdown && fluid.equals(parcel.fluid())) {
                parcel.add(accepted);
                setChanged();
                return accepted;
            }
        }
        parcels.add(new TravelingFluid(fluid, accepted, heading, countdown));
        setChanged();
        return accepted;
    }

    private int spreadRate(IFluidKey fluid) {
        Level level = getLevel();
        int base = level == null ? 5 : Math.max(1, fluid.getFluid().getTickDelay(level));
        return Math.max(1, Math.round(base * DWELL_MULTIPLIER));
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

        LogisticsConfig.FluidPipeConfig cfg = LogisticsConfig.get().fluidPipe;
        if (isExtractor() && energy != null && cfg.activeExtraction) {
            extract(level, cfg);
        }

        for (TravelingFluid parcel : parcels) {
            parcel.tickCountdown();
        }

        if (kind.isVoid()) {
            destroyReady(kind.transferRate(cfg));
        } else {
            moveReadyFluid(level, kind.transferRate(cfg));
        }

        parcels.removeIf(parcel -> parcel.amount() <= 0);
        syncIfChanged();
    }

    /** Extractor: pull this pipe's fluid (or any, if empty) from an adjacent source handler into itself. */
    private void extract(Level level, LogisticsConfig.FluidPipeConfig cfg) {
        long room = capacityMb - totalMillibuckets();
        if (room <= 0) {
            return;
        }
        IFluidKey contained = containedFluid();
        BlockPos pos = getBlockPos();
        for (Direction side : Direction.values()) {
            if (connection(side) != FluidConnection.HANDLER) {
                continue;
            }
            IFluidStorage handler = FluidStorageLookup.find(level, pos.relative(side), side.getOpposite());
            if (handler == null) {
                continue;
            }
            FluidProvider<IFluidKey> provider = asProvider(handler);
            IFluidKey fluid = provider.fluid();
            if (fluid == null || (!contained.isBlank() && !contained.equals(fluid))) {
                continue;
            }
            long budget = Math.min(kind.transferRate(cfg), room);
            FluidPipe<IFluidKey> pulled = new FluidPipe<>();
            FluidExtraction.Result result = FluidExtraction.tick(
                    pulled, List.of(provider), energy.getAmount(), budget, cfg.woodenRequiresEngine, extractionCarryMb);
            if (pulled.amount() > 0) {
                energy.consume(result.energyToConsume());
                extractionCarryMb = result.carryMb();
                acceptFluid(pulled.fluid(), pulled.amount(), side); // enter from the source side → head away from it
                return;
            }
        }
    }

    /** Void pipe: destroy ready fluid, up to the void rate per tick. */
    private void destroyReady(long rate) {
        long budget = rate;
        for (TravelingFluid parcel : parcels) {
            if (budget <= 0) {
                break;
            }
            if (!parcel.ready()) {
                continue;
            }
            long destroyed = Math.min(parcel.amount(), budget);
            parcel.add(-destroyed);
            budget -= destroyed;
        }
    }

    /**
     * Move ready parcels one pipe along, total capped at {@code rate} mB this tick. Each parcel splits equally
     * among its valid outputs (connected faces, excluding the incoming side unless the parcel is blocked); the
     * leftover stays ready and is marked blocked so it may reflect next tick.
     */
    private void moveReadyFluid(Level level, long rate) {
        long budget = rate;
        if (budget <= 0) {
            return;
        }
        for (TravelingFluid parcel : new ArrayList<>(parcels)) {
            if (budget <= 0) {
                break;
            }
            if (!parcel.ready() || parcel.amount() <= 0) {
                continue;
            }
            IFluidKey fluid = parcel.fluid();
            List<Direction> outs = candidateOutputs(parcel);
            if (outs.isEmpty()) {
                parcel.setBlocked(true);
                continue;
            }
            long[] room = new long[outs.size()];
            long totalRoom = 0;
            for (int i = 0; i < outs.size(); i++) {
                room[i] = roomToward(level, outs.get(i), fluid, rate);
                totalRoom += room[i];
            }
            long moveTotal = Math.min(Math.min(parcel.amount(), budget), totalRoom);
            if (moveTotal <= 0) {
                parcel.setBlocked(true);
                continue;
            }
            long[] alloc = FluidSplit.split(moveTotal, room);
            for (int i = 0; i < outs.size(); i++) {
                if (alloc[i] <= 0) {
                    continue;
                }
                long moved = depositToward(level, outs.get(i), fluid, alloc[i]);
                parcel.add(-moved);
                budget -= moved;
            }
            parcel.setBlocked(parcel.amount() > 0);
        }
    }

    /** The faces a parcel may flow out: a merger only its output face; others all connected faces, excluding the
     * incoming side unless the parcel is blocked (then it may reflect back the way it came). */
    private List<Direction> candidateOutputs(TravelingFluid parcel) {
        List<Direction> result = new ArrayList<>(6);
        if (kind.isDirectional()) {
            if (outputDirection != null && connection(outputDirection) != FluidConnection.NONE) {
                result.add(outputDirection);
            }
            return result;
        }
        Direction incoming = parcel.heading().getOpposite();
        for (Direction direction : Direction.values()) {
            if (connection(direction) == FluidConnection.NONE) {
                continue;
            }
            // An extractor only ever pulls fluid out of an adjacent handler; it must never deposit back into a
            // provider (which would drain the source then immediately refill it).
            if (isExtractor() && connection(direction) == FluidConnection.HANDLER) {
                continue;
            }
            if (!parcel.blocked() && direction == incoming) {
                continue;
            }
            result.add(direction);
        }
        return result;
    }

    /** How much (mB, capped at {@code cap}) the neighbour or handler in {@code direction} can take of {@code fluid}. */
    private long roomToward(Level level, Direction direction, IFluidKey fluid, long cap) {
        BlockPos neighbourPos = getBlockPos().relative(direction);
        if (connection(direction) == FluidConnection.HANDLER) {
            IFluidStorage handler = FluidStorageLookup.find(level, neighbourPos, direction.getOpposite());
            if (handler == null) {
                return 0;
            }
            return FluidUnits.toMillibuckets(handler.insert(fluid, FluidUnits.mb(cap), true));
        }
        if (level.getBlockEntity(neighbourPos) instanceof FluidPipeBlockEntity neighbour) {
            return neighbour.roomFor(fluid, direction.getOpposite());
        }
        return 0;
    }

    /** Move {@code amountMb} of {@code fluid} into the neighbour/handler in {@code direction}; returns the amount taken. */
    private long depositToward(Level level, Direction direction, IFluidKey fluid, long amountMb) {
        BlockPos neighbourPos = getBlockPos().relative(direction);
        if (connection(direction) == FluidConnection.HANDLER) {
            IFluidStorage handler = FluidStorageLookup.find(level, neighbourPos, direction.getOpposite());
            if (handler == null) {
                return 0;
            }
            return FluidUnits.toMillibuckets(handler.insert(fluid, FluidUnits.mb(amountMb), false));
        }
        if (level.getBlockEntity(neighbourPos) instanceof FluidPipeBlockEntity neighbour) {
            return neighbour.acceptFluid(fluid, amountMb, direction.getOpposite());
        }
        return 0;
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
                long drainedNative = handler.extract(fluid, FluidUnits.mb(millibuckets), false);
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
            ensureValidOutput(); // merger: keep its output face on a valid connection as neighbours change
            markDirtyAndSync();
        }
    }

    /**
     * Sync to clients on a fluid-type change, a coarse rendered-level step, or the empty↔non-empty transition
     * (so an emptied pipe never leaves a stale sliver on screen). The coarse step bounds packet volume.
     */
    private void syncIfChanged() {
        Fluid fluid = containedFluid().getFluid();
        long total = totalMillibuckets();
        boolean empty = total <= 0;
        long step = (total * SYNC_STEPS) / Math.max(1, capacityMb);
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
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        for (TravelingFluid parcel : parcels) {
            TravelingFluid.codec().encodeStart(ops, parcel).result().ifPresent(list::add);
        }
        tag.put("Parcels", list);
        if (disabledMask != 0) {
            tag.putInt("Disabled", disabledMask);
        }
        tag.putInt("Conn", encodeConnections());
        if (outputDirection != null) {
            tag.putInt("OutDir", outputDirection.get3DDataValue());
        }
        if (energy != null) {
            energy.writeNbt(tag, "Energy");
            if (extractionCarryMb != 0) {
                tag.putLong("ExtractCarry", extractionCarryMb);
            }
        }
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        parcels.clear();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        NbtCompat.ifHasList(tag, "Parcels", list -> {
            for (int i = 0; i < list.size(); i++) {
                NbtCompat.ifHasCompoundAt(list, i, parcelTag -> {
                    DataResult<TravelingFluid> parsed = TravelingFluid.codec().parse(ops, parcelTag);
                    parsed.result().ifPresent(parcels::add);
                });
            }
        });
        disabledMask = NbtCompat.getInt(tag, "Disabled", 0);
        decodeConnections(NbtCompat.getInt(tag, "Conn", 0));
        outputDirection = tag.contains("OutDir") ? Direction.from3DDataValue(NbtCompat.getInt(tag, "OutDir", 0)) : null;
        if (energy != null) {
            energy.readNbt(tag, "Energy");
            extractionCarryMb = NbtCompat.getLong(tag, "ExtractCarry", 0);
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
