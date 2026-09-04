package com.logistics.automation.pump;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.automation.pump.FluidPumpBlockEntity.Phase;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid pump's work behavior as a {@link MachineComponent}: lowers an intake tube layer by
 * layer, drains connected source blocks (furthest-first) for RF, and buffers the result in the
 * machine's tank, pushing the overflow out to adjacent pipes/tanks.
 *
 * <p>Reads energy and fluid from its sibling {@link EnergyStorageComponent}/{@link FluidStoreComponent}.
 * {@link #advanceArm} runs on both sides for smooth tube animation; the rest is server-only.
 */
public final class FluidPumpComponent implements MachineComponent {

    // Output faces: top and sides (the bottom is the intake tube).
    private static final Direction[] OUTPUT_SIDES = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private final String id;
    private final EnergyStorageComponent energy;
    private final FluidStoreComponent fluidStore;

    private Phase phase = Phase.DESCENDING;
    private int targetY;
    private float armY;
    private long tickOffset;
    private long lastSyncedEnergy = -1;
    private long lastSyncedTank = -1;
    private int lastSyncedTargetY = Integer.MIN_VALUE;
    @Nullable private Phase lastSyncedPhase;
    private final ArrayDeque<BlockPos> sourceQueue = new ArrayDeque<>();
    private boolean infiniteBody;
    @Nullable private Fluid queuedFluid;
    private int queuedY;
    // Per-instance arm-speed override for tests; negative falls back to the config value.
    private float armSpeedOverride = -1f;

    public FluidPumpComponent(String id, EnergyStorageComponent energy, FluidStoreComponent fluidStore, BlockPos pos) {
        this.id = id;
        this.energy = energy;
        this.fluidStore = fluidStore;
        this.targetY = pos.getY() - 1;
        this.armY = pos.getY();
        this.tickOffset = Math.floorMod(pos.asLong(), 16L);
    }

    @Override
    public String id() {
        return id;
    }

    private FluidTankComponent tank() {
        return fluidStore.tank();
    }

    Phase phase() {
        return phase;
    }

    float armY() {
        return armY;
    }

    // Test seam: override the arm speed for this pump instance; negative restores the config value.
    void setArmSpeedOverride(float armSpeed) {
        this.armSpeedOverride = armSpeed;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        Level level = ctx.level();
        BlockPos pos = ctx.pos();

        pushOut(level, pos);

        if ((level.getGameTime() + tickOffset) % LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) == 0) {
            work((ServerLevel) level, pos);
        }

        long currentEnergy = energy.amount();
        long currentTank = tank().getAmount();
        if (currentEnergy != lastSyncedEnergy
                || currentTank != lastSyncedTank
                || targetY != lastSyncedTargetY
                || phase != lastSyncedPhase) {
            lastSyncedEnergy = currentEnergy;
            lastSyncedTank = currentTank;
            lastSyncedTargetY = targetY;
            lastSyncedPhase = phase;
            ctx.sync();
        }
    }

    /**
     * Animate the tube toward the synced {@code targetY}. Called on both sides from the block-entity
     * tick so the client descent stays smooth between the sparse state syncs (which only fire on real
     * state changes).
     */
    void advanceArm(MachineContext ctx) {
        BlockPos pos = ctx.pos();
        float target = targetY + 0.5f;
        boolean server = ctx.isServer();
        float armSpeed = armSpeedOverride >= 0f ? armSpeedOverride : LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ARM_SPEED);
        float stepped = stepArm(armY, target, armSpeed);
        if (stepped != armY) {
            armY = stepped;
            if (server) {
                ctx.markChanged();
            }
        }
        if (targetY > pos.getY() - 1) {
            targetY = pos.getY() - 1;
        }
    }

    /** Steps {@code armY} one tick toward {@code target} at {@code armSpeed} without overshooting; unchanged at rest. */
    static float stepArm(float armY, float target, float armSpeed) {
        if (armY > target) {
            return Math.max(target, armY - armSpeed);
        }
        if (armY < target) {
            return Math.min(target, armY + armSpeed);
        }
        return armY;
    }

    private void pushOut(Level level, BlockPos pos) {
        long budget = FluidUnits.mb(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_PUSH_RATE_MB));
        FluidTankComponent tank = tank();
        for (Direction side : OUTPUT_SIDES) {
            if (budget <= 0 || tank.isEmpty()) {
                return;
            }
            IFluidStorage target = FluidStorageLookup.find(level, pos.relative(side), side.getOpposite());
            if (target == null) {
                continue;
            }
            IFluidView view = tank.contents().iterator().next();
            long max = Math.min(budget, view.amount());
            long accepted = target.insert(view.resource(), max, true);
            if (accepted <= 0) {
                continue;
            }
            accepted = target.insert(view.resource(), accepted, false);
            if (accepted > 0) {
                tank.extract(view.resource(), accepted, false);
                budget -= accepted;
            }
        }
    }

    private void work(ServerLevel level, BlockPos pos) {
        FluidTankComponent tank = tank();
        if (tank.getCapacity() - tank.getAmount() < FluidUnits.mb(1_000)) {
            phase = Phase.STALLED;
            return;
        }

        BlockPos target = new BlockPos(pos.getX(), targetY, pos.getZ());
        FluidState fluidState = level.getFluidState(target);

        // Only draw fluid once the tube tip is seated at the middle of the target block (targetY + 0.5),
        // and don't step down until the layer is exhausted. The tube still descends freely through air /
        // non-source fluid so it reaches the body smoothly.
        boolean atLayer = phase == Phase.PUMPING
                || (phase == Phase.STALLED && !sourceQueue.isEmpty() && queuedFluid != null)
                || isPumpableSource(level, target, fluidState);
        if (atLayer && armY > targetY + 0.55f) {
            return;
        }
        if (phase == Phase.STALLED && !sourceQueue.isEmpty() && queuedFluid != null) {
            if (energy.amount() < LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_PER_SOURCE)) {
                return;
            }
            phase = Phase.PUMPING;
            if (pumpFromLayer(level, pos, target, queuedFluid)) {
                return;
            }
            descendToNextLayer(level, pos);
            return;
        }
        if (phase == Phase.PUMPING && queuedFluid != null) {
            if (energy.amount() < LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_PER_SOURCE)) {
                phase = Phase.STALLED;
                return;
            }
            // Keep draining the layer based on the body being pumped, not the tank's momentary level,
            // so an attached pipe emptying the tank doesn't cut the layer short.
            if (pumpFromLayer(level, pos, target, queuedFluid)) {
                return;
            }
            descendToNextLayer(level, pos);
            return;
        }
        if (isPumpableSource(level, target, fluidState)) {
            if (energy.amount() < LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_PER_SOURCE)) {
                phase = Phase.STALLED;
                return;
            }
            phase = Phase.PUMPING;
            if (!pumpFromLayer(level, pos, target, fluidState.getType())) {
                descendToNextLayer(level, pos);
            }
            return;
        }

        // Don't descend the tube into a solid block or below the world; stall instead.
        if (isBlocked(level, target) || !canDescend(level, pos)) {
            phase = Phase.STALLED;
            return;
        }

        phase = Phase.DESCENDING;
        targetY--;
    }

    // The tube can extend one more block down only into air/fluid that's still inside the world.
    private boolean canDescend(ServerLevel level, BlockPos pos) {
        int nextY = targetY - 1;
        return nextY >= level.getMinBuildHeight() && !isBlocked(level, new BlockPos(pos.getX(), nextY, pos.getZ()));
    }

    private void descendToNextLayer(ServerLevel level, BlockPos pos) {
        sourceQueue.clear();
        infiniteBody = false;
        queuedFluid = null;
        // Don't step the tube into a solid block or below the world; stall instead.
        if (!canDescend(level, pos)) {
            phase = Phase.STALLED;
            return;
        }
        targetY--;
        phase = Phase.DESCENDING;
    }

    private boolean pumpFromLayer(
            ServerLevel level, BlockPos pumpPos, BlockPos origin, Fluid fluid) {
        BlockPos source = findConnectedSource(level, pumpPos, origin, fluid, LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_SEARCH_RADIUS));
        if (source == null) {
            return false;
        }
        FluidTankComponent tank = tank();
        IFluidKey key = SimpleFluidKey.of(fluid);
        long bucket = FluidUnits.mb(1_000);
        if (tank.insert(key, bucket, true) != bucket) {
            phase = Phase.STALLED;
            return true;
        }
        energy.consume(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_PER_SOURCE));
        tank.insert(key, bucket, false);
        if (infiniteBody) {
            // Effectively infinite body: draw fluid without carving the landscape. Re-queue at the front
            // so the furthest-first ordering keeps cycling the body.
            sourceQueue.addFirst(source);
        } else {
            // Finite body: remove the source and let neighbors settle normally. Reforming fluids (water)
            // may flow back in, but draining a sub-9-source pool is the player's choice; a 3x3+ pool is
            // treated as infinite and skipped entirely.
            level.setBlock(source, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        // The tank and energy both changed; serverTick's change check pushes the sync this tick.
        return true;
    }

    @Nullable
    private BlockPos findConnectedSource(ServerLevel level, BlockPos pumpPos, BlockPos origin, Fluid fluid, int radius) {
        if (sourceQueue.isEmpty() || queuedFluid != fluid || queuedY != origin.getY()) {
            rebuildSourceQueue(level, pumpPos, origin, fluid, radius);
        }

        // Pump the furthest-out source first (BFS order is nearest-first) so the body recedes toward the
        // tube and the cell under the tube is drained last, instead of vanishing from under it.
        while (!sourceQueue.isEmpty()) {
            BlockPos source = sourceQueue.removeLast();
            FluidState state = level.getFluidState(source);
            if (state.getType().isSame(fluid) && state.isSource() && isStandaloneLiquid(level, source)) {
                return source;
            }
        }
        return null;
    }

    // Whether the fluid forms new source blocks (water), so the pump must suppress reflow; lava does not.
    private static boolean createsSourceBlocks(Fluid fluid) {
        return fluid.defaultFluidState().is(FluidTags.WATER);
    }

    // Floods only the horizontal layer at origin's Y, so the pump exhausts one layer before stepping down.
    private void rebuildSourceQueue(ServerLevel level, BlockPos pumpPos, BlockPos origin, Fluid fluid, int radius) {
        sourceQueue.clear();
        infiniteBody = false;
        queuedFluid = fluid;
        queuedY = origin.getY();

        // Only reforming fluids (water) get infinite treatment; lava is always consumed.
        int threshold = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INFINITE_SOURCE_THRESHOLD);
        boolean canBeInfinite = threshold > 0 && createsSourceBlocks(fluid);

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin);
        int radiusSq = radius * radius;

        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            FluidState state = level.getFluidState(current);
            if (!state.getType().isSame(fluid)) {
                continue;
            }
            if (state.isSource() && isStandaloneLiquid(level, current)) {
                sourceQueue.add(current);
                if (canBeInfinite && sourceQueue.size() >= threshold) {
                    infiniteBody = true;
                    return;
                }
            }

            addFluidNeighbor(level, pumpPos, current.north(), fluid, radiusSq, visited, queue);
            addFluidNeighbor(level, pumpPos, current.south(), fluid, radiusSq, visited, queue);
            addFluidNeighbor(level, pumpPos, current.east(), fluid, radiusSq, visited, queue);
            addFluidNeighbor(level, pumpPos, current.west(), fluid, radiusSq, visited, queue);
        }
    }

    private void addFluidNeighbor(
            ServerLevel level,
            BlockPos pumpPos,
            BlockPos next,
            Fluid fluid,
            int radiusSq,
            Set<BlockPos> visited,
            Queue<BlockPos> queue) {
        int dx = next.getX() - pumpPos.getX();
        int dz = next.getZ() - pumpPos.getZ();
        if (dx * dx + dz * dz > radiusSq || next.getY() < level.getMinBuildHeight() || next.getY() >= level.getMaxBuildHeight()) {
            return;
        }
        if (visited.add(next) && level.getFluidState(next).getType().isSame(fluid)) {
            queue.add(next);
        }
    }

    private boolean isPumpableSource(Level level, BlockPos pos, FluidState state) {
        Fluid fluid = state.getType();
        if (fluid == Fluids.EMPTY || !state.isSource() || !isStandaloneLiquid(level, pos)) {
            return false;
        }
        FluidTankComponent tank = tank();
        IFluidKey current = tank.getFluidKey();
        return tank.isEmpty() || current.getFluid() == fluid;
    }

    /** True only for a standalone liquid block (not a waterlogged solid), so draining won't replace blocks. */
    private static boolean isStandaloneLiquid(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof LiquidBlock;
    }

    private boolean isBlocked(Level level, BlockPos pos) {
        return blocksTube(level.getBlockState(pos));
    }

    /**
     * True when a block physically stops the intake tube. A standalone liquid has no collision
     * shape and so never blocks motion on its own; testing the fluid state as well would let the
     * tube pass through any waterlogged solid a player laid down to seal a pocket off.
     */
    static boolean blocksTube(BlockState state) {
        return state.blocksMotion();
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Phase", phase.name());
        tag.putInt("TargetY", targetY);
        tag.putFloat("ArmY", armY);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        readState(tag);
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        readState(root);
    }

    private void readState(CompoundTag tag) {
        try {
            phase = Phase.valueOf(NbtCompat.getString(tag, "Phase", Phase.DESCENDING.name()));
        } catch (IllegalArgumentException e) {
            phase = Phase.DESCENDING;
        }
        targetY = NbtCompat.getInt(tag, "TargetY", targetY);
        armY = NbtCompat.getFloat(tag, "ArmY", armY);
    }
}
