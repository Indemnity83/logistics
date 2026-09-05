package com.logistics.pipe.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.LogisticsPipe;
import com.logistics.core.LogisticsProfiler;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidLight;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.DestinationPriority;
import com.logistics.core.lib.pipe.IModuleHost;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.core.lib.power.DirectEnergyReceiver;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.pipe.block.FluidConnection;
import com.logistics.LogisticsCore;
import com.logistics.pipe.block.FluidPipeBlock;
import com.logistics.pipe.data.PipeDataComponents.WeatheringState;
import com.logistics.pipe.modules.WeatheringModule;
import com.logistics.pipe.FluidPipe;
import com.logistics.core.lib.pipe.FluidExtraction;
import com.logistics.core.lib.pipe.FluidBuffer;
import com.logistics.core.lib.pipe.FluidProvider;
import com.logistics.core.lib.pipe.FluidSplit;
import com.logistics.core.lib.pipe.TravelingFluid;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
        implements HasFluidStorage, HasEnergyStorage, AcceptsLowTierEnergy, DirectEnergyReceiver, IModuleHost {

    private static final long ENERGY_CAPACITY = 20;
    private static final int SYNC_STEPS = 16;
    private static final float DWELL_MULTIPLIER = 2.0F;
    /**
     * Reference fluid tick delay (vanilla water's spread rate) that extraction rate is scaled against. Fluids
     * slower than this extract proportionally less per tick; water and anything faster pull at the full rate.
     */
    private static final int EXTRACTION_REFERENCE_TICK_DELAY = 5;

    @Nullable
    private final FluidPipe def;
    /** Pipe buffer capacity in mB (shared base); the sum of all parcels never exceeds it. */
    private final long capacityMb;

    /** Fluid parcels currently inside this pipe. One fluid type per pipe (no mixing). */
    private final List<TravelingFluid> parcels = new ArrayList<>();

    /** Persisted, client-synced NBT state for hosted modules (weathering, marking, …). */
    private final CompoundTag moduleState = new CompoundTag();

    /** Bitmask of wrench-disabled sides (bit per {@link Direction#get3DDataValue()}). */
    private int disabledMask;

    /**
     * The single wrench-selected "feature" face, or {@code null} when unset. Its meaning depends on the kind:
     * for the merger it's the one face fluid may exit; for the extractor it's the one handler face fluid is
     * pulled from. Other kinds don't use it.
     */
    @Nullable
    private Direction featureDirection;

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
        super(LogisticsPipe.ENTITY.FLUID_PIPE_BLOCK_ENTITY, pos, state);
        this.def = defOf(state);
        for (int i = 0; i < 6; i++) {
            connections[i] = FluidConnection.NONE;
        }
        this.capacityMb = def != null
                ? def.capacity()
                : LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PIPE_BASE_CAPACITY);
        this.energy = (def != null && def.isExtractor())
                ? new EnergyComponent(ENERGY_CAPACITY, ENERGY_CAPACITY, 0, this::setChanged)
                : null;
    }

    @Nullable
    private static FluidPipe defOf(BlockState state) {
        return state.getBlock() instanceof FluidPipeBlock block ? block.fluidPipe() : null;
    }

    /** The fluid pipe definition backing this block entity (transport policy + cosmetic/connection modules). */
    @Nullable
    public FluidPipe fluidPipe() {
        return def;
    }

    public FluidConnection connection(Direction direction) {
        return connections[direction.get3DDataValue()];
    }

    public boolean isExtractor() {
        return def != null && def.isExtractor();
    }

    private boolean isMerger() {
        return def != null && def.isMerger();
    }

    private boolean isVoid() {
        return def != null && def.isVoid();
    }

    /** This pipe's transfer rate in mB/tick, from its definition (falls back to the base rate if unbound). */
    private long transferRate() {
        return def != null ? def.transferRate() : LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PIPE_BASE_TRANSFER_RATE);
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

    // Tick clock (game time + partial tick) at the last ease, so the rate below is per tick rather
    // than per frame. -1 = not yet eased.
    private float lastDisplayFillTicks = -1.0F;

    // The last non-blank fluid drawn here, kept so the fade-out has something to draw once the pipe
    // is empty. Render-only, like displayFill; never saved, never read by the server.
    private IFluidKey displayFluid = SimpleFluidKey.BLANK;

    /** Fraction of the gap closed per tick. Convergence is ~10 ticks, whatever the frame rate. */
    private static final float DISPLAY_FILL_RATE_PER_TICK = 0.2F;

    /**
     * Eases the client display fill toward {@code target} (0..1) and returns it; snaps on the first
     * frame. {@code nowTicks} is the client tick clock — game time plus partial tick — so the easing
     * takes the same wall-clock time at any frame rate, and a pipe that was off screen catches up in
     * one step instead of resuming from a stale value.
     */
    public float advanceDisplayFill(float target, float nowTicks) {
        if (displayFill < 0.0F || lastDisplayFillTicks < 0.0F) {
            displayFill = target;
            lastDisplayFillTicks = nowTicks;
            return displayFill;
        }
        float elapsed = Math.max(0.0F, nowTicks - lastDisplayFillTicks);
        lastDisplayFillTicks = nowTicks;
        float eased = 1.0F - (float) Math.pow(1.0F - DISPLAY_FILL_RATE_PER_TICK, elapsed);
        displayFill += (target - displayFill) * eased;
        return displayFill;
    }

    /**
     * The fluid the client should draw: whatever is contained, or the last non-blank one while the
     * fill eases back down. Without the fallback the pipe reports blank the moment the final parcel
     * leaves, and the fade-out never renders.
     */
    public IFluidKey advanceDisplayFluid() {
        IFluidKey contained = containedFluid();
        if (!contained.isBlank()) {
            displayFluid = contained;
        }
        return displayFluid;
    }

    // ==================== Feature face (merger output / extractor pull) ====================

    @Nullable
    public Direction featureDirection() {
        return featureDirection;
    }

    /** Sets the feature face (or {@code null}); resyncs so the arrow renders. */
    public void setFeatureDirection(@Nullable Direction direction) {
        if (featureDirection != direction) {
            featureDirection = direction;
            markDirtyAndSync();
        }
    }

    /** Whether this kind uses a single wrench-selected feature face (merger output or extractor pull). */
    private boolean usesFeatureFace() {
        return isMerger() || isExtractor();
    }

    /**
     * The faces the feature direction may sit on, in {@link Direction} order: every connection for the merger,
     * but only handler connections for the extractor (it pulls from adjacent storage, never from a pipe).
     */
    private List<Direction> featureCandidates() {
        List<Direction> result = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            FluidConnection conn = connection(direction);
            if (conn == FluidConnection.NONE) {
                continue;
            }
            if (isExtractor() && conn != FluidConnection.HANDLER) {
                continue;
            }
            result.add(direction);
        }
        return result;
    }

    /** Wrench action: advance the feature face to the next candidate, like the item Merger/Extractor pipes. */
    public void cycleFeatureDirection() {
        List<Direction> candidates = featureCandidates();
        if (candidates.isEmpty()) {
            setFeatureDirection(null);
            return;
        }
        int index = featureDirection == null ? -1 : candidates.indexOf(featureDirection);
        setFeatureDirection(candidates.get((index + 1) % candidates.size()));
    }

    /** Keep the feature face on a valid candidate; default to the first one, clear when none. */
    private void ensureValidFeature() {
        if (!usesFeatureFace()) {
            return;
        }
        List<Direction> candidates = featureCandidates();
        if (candidates.isEmpty()) {
            setFeatureDirection(null);
        } else if (featureDirection == null || !candidates.contains(featureDirection)) {
            setFeatureDirection(candidates.getFirst());
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
        if (isMerger() && enteringSide == featureDirection) {
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

    /** A fluid's natural spread rate (vanilla {@code getTickDelay}), in ticks; the dwell/extraction baseline. */
    private int tickDelay(IFluidKey fluid) {
        Level level = getLevel();
        return level == null
                ? EXTRACTION_REFERENCE_TICK_DELAY
                : Math.max(1, fluid.getFluid().getTickDelay(level));
    }

    private int spreadRate(IFluidKey fluid) {
        return Math.max(1, Math.round(tickDelay(fluid) * DWELL_MULTIPLIER));
    }

    /**
     * Per-tick extraction rate for {@code fluid}, scaled down for slow-spreading fluids. A slow fluid (lava)
     * dwells far longer per pipe, so pulling at the full rate just fills the first pipe to capacity before its
     * parcel hops. Scaling by {@code referenceDelay / tickDelay} keeps the amount moved per dwell period roughly
     * constant, so lava flows block-to-block like water — just in smaller per-tick amounts. Water (the fast
     * reference) is unaffected, and no fluid extracts faster than the base rate.
     */
    private long scaledExtractionRate(long baseRate, IFluidKey fluid) {
        long scaled = baseRate * EXTRACTION_REFERENCE_TICK_DELAY / tickDelay(fluid);
        return Math.max(1, Math.min(baseRate, scaled));
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

    /** Invalidate this pipe's connection cache and all six neighbours' (used after a marking change). */
    public void invalidateConnectionsAndNeighbours() {
        invalidateConnectionCache();
        for (Direction direction : Direction.values()) {
            invalidateNeighbour(direction);
        }
    }

    // ==================== Module host (IModuleHost) ====================

    public PipeContext createContext() {
        return new PipeContext(getLevel(), getBlockPos(), getBlockState(), this);
    }

    private static PipeConnection.Type mapConnection(FluidConnection connection) {
        return switch (connection) {
            case PIPE -> PipeConnection.Type.PIPE;
            case HANDLER -> PipeConnection.Type.INVENTORY;
            case NONE -> PipeConnection.Type.NONE;
        };
    }

    @Override
    public CompoundTag moduleState(String key) {
        if (!moduleState.contains(key)) {
            moduleState.put(key, new CompoundTag());
        }
        return NbtCompat.getCompoundOrEmpty(moduleState, key);
    }

    @Override
    @Nullable
    public CompoundTag existingModuleState(String key) {
        return moduleState.contains(key) ? NbtCompat.getCompoundOrEmpty(moduleState, key) : null;
    }

    @Override
    public void clearModuleState(String key) {
        moduleState.remove(key);
    }

    @Override
    @Nullable
    public EnergyComponent getEnergy() {
        return energy;
    }

    @Override
    public void markDirty() {
        setChanged();
    }

    @Override
    public PipeConnection.Type getCachedConnectionType(Direction direction) {
        return mapConnection(connection(direction));
    }

    @Override
    public PipeConnection.Type getConnectionType(Level world, BlockPos pos, Direction direction) {
        return mapConnection(connection(direction));
    }

    @Override
    public boolean isNeighborPipe(Level world, BlockPos pos, Direction direction) {
        return world.getBlockState(pos.relative(direction)).getBlock() instanceof FluidPipeBlock;
    }

    @Override
    public boolean isPowered() {
        Level level = getLevel();
        return level != null && level.hasNeighborSignal(getBlockPos());
    }

    @Override
    @Nullable
    public ILogisticsNetwork getNetwork() {
        return null; // fluid pipes are cellular; no logistics network
    }

    // ==================== Server tick ====================

    public static void tick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        LogisticsProfiler.push("fluid_pipes");
        try {
            be.serverTick(level);
        } finally {
            LogisticsProfiler.pop();
        }
    }

    private void serverTick(Level level) {
        refreshConnections(level, getBlockPos());

        if (isExtractor() && energy != null && LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PIPE_ACTIVE_EXTRACTION)) {
            extract(level);
        }

        for (TravelingFluid parcel : parcels) {
            parcel.tickCountdown();
        }

        if (isVoid()) {
            destroyReady(transferRate());
        } else {
            moveReadyFluid(level, transferRate());
        }

        parcels.removeIf(parcel -> parcel.amount() <= 0);
        updateLightLevel(level);
        syncIfChanged();
    }

    /** Emit the contained fluid's light through the block state so a glowing fluid lights the pipe. */
    private void updateLightLevel(Level level) {
        FluidLight.update(
                level, getBlockPos(), getBlockState(), FluidPipeBlock.LIGHT_LEVEL,
                containedFluid().getFluid(), totalMillibuckets(), LogisticsCore::fluidLuminance);
    }

    /** Extractor: pull fluid from the handler on the wrench-selected pull face into this pipe. */
    private void extract(Level level) {
        long room = capacityMb - totalMillibuckets();
        Direction side = featureDirection;
        if (room <= 0 || side == null || connection(side) != FluidConnection.HANDLER) {
            return;
        }
        IFluidStorage handler = FluidStorageLookup.find(level, getBlockPos().relative(side), side.getOpposite());
        if (handler == null) {
            return;
        }
        FluidProvider<IFluidKey> provider = asProvider(handler);
        IFluidKey fluid = provider.fluid();
        IFluidKey contained = containedFluid();
        if (fluid == null || (!contained.isBlank() && !contained.equals(fluid))) {
            return;
        }
        long budget = Math.min(scaledExtractionRate(transferRate(), fluid), room);
        // Sized to the real room, not the buffer default, so FluidExtraction can re-offer the whole space to
        // an all-or-nothing source (a cauldron yields a whole level or nothing).
        FluidBuffer<IFluidKey> pulled = FluidBuffer.extractor(room);
        FluidExtraction.Result result = FluidExtraction.tick(
                pulled,
                provider,
                energy.getAmount(),
                budget,
                LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PIPE_WOODEN_REQUIRES_ENGINE),
                extractionCarryMb);
        if (pulled.amount() > 0) {
            energy.consume(result.energyToConsume());
            extractionCarryMb = result.carryMb();
            acceptFluid(pulled.fluid(), pulled.amount(), side); // enter from the source side → head away from it
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
            for (int i = 0; i < outs.size(); i++) {
                room[i] = roomToward(level, outs.get(i), fluid, rate);
            }
            if (def != null && def.destinationPriority() == DestinationPriority.HANDLERS_FIRST) {
                preferHandlers(outs, room);
            }
            long totalRoom = 0;
            for (long r : room) {
                totalRoom += r;
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
        if (isMerger()) {
            if (featureDirection != null && connection(featureDirection) != FluidConnection.NONE) {
                result.add(featureDirection);
            }
            return result;
        }
        Direction incoming = parcel.heading().getOpposite();
        for (Direction direction : Direction.values()) {
            if (connection(direction) == FluidConnection.NONE) {
                continue;
            }
            // An extractor must never deposit back into its own pull face (that would drain the source then
            // immediately refill it); it may still push into any other adjacent handler.
            if (isExtractor() && direction == featureDirection) {
                continue;
            }
            if (!parcel.blocked() && direction == incoming) {
                continue;
            }
            result.add(direction);
        }
        return result;
    }

    /**
     * Insertion pipe routing: when any adjacent handler can take fluid this tick, zero the room of every
     * non-handler output so the split fills tanks first; pipes get the overflow only once the tanks are full.
     */
    private void preferHandlers(List<Direction> outs, long[] room) {
        boolean handlerHasRoom = false;
        for (int i = 0; i < outs.size(); i++) {
            if (room[i] > 0 && connection(outs.get(i)) == FluidConnection.HANDLER) {
                handlerHasRoom = true;
                break;
            }
        }
        if (!handlerHasRoom) {
            return;
        }
        for (int i = 0; i < outs.size(); i++) {
            if (connection(outs.get(i)) != FluidConnection.HANDLER) {
                room[i] = 0;
            }
        }
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
            return FluidUnits.toMillibucketsCeil(handler.insert(fluid, FluidUnits.mb(amountMb), false));
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
            ensureValidFeature(); // keep the merger output / extractor pull face valid as neighbours change
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
        if (featureDirection != null) {
            tag.putInt("FeatureDir", featureDirection.get3DDataValue());
        }
        if (energy != null) {
            energy.writeNbt(tag, "Energy");
            if (extractionCarryMb != 0) {
                tag.putLong("ExtractCarry", extractionCarryMb);
            }
        }
        if (!moduleState.isEmpty()) {
            tag.put("ModuleState", moduleState);
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
        featureDirection =
                tag.contains("FeatureDir") ? Direction.from3DDataValue(NbtCompat.getInt(tag, "FeatureDir", 0)) : null;
        if (energy != null) {
            energy.readNbt(tag, "Energy");
            extractionCarryMb = NbtCompat.getLong(tag, "ExtractCarry", 0);
        }
        new ArrayList<>(moduleState.getAllKeys()).forEach(moduleState::remove);
        NbtCompat.ifHasCompound(tag, "ModuleState", stored -> {
            for (String key : stored.getAllKeys()) {
                Tag value = stored.get(key);
                if (value != null) {
                    moduleState.put(key, value.copy());
                }
            }
        });
    }

    // ==================== Component round-trip (item drops / placement) ====================

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        FluidPipe pipe = fluidPipe();
        if (pipe != null) {
            pipe.addItemComponents(builder, createContext());
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        super.applyImplicitComponents(components);
        FluidPipe pipe = fluidPipe();
        if (pipe == null) return;
        WeatheringState ws = components.get(LogisticsPipe.DATA.WEATHERING_STATE);
        if (ws != null && !ws.isDefault()) {
            WeatheringModule wm = pipe.getModule(WeatheringModule.class, this);
            if (wm != null) {
                wm.applyWeatheringState(ws, createContext());
            }
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
