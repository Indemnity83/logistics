package com.logistics.pipe.block.entity;

import com.logistics.core.LogisticsConfig;
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
import com.logistics.pipe.block.entity.FluidPumpBlockEntity.Phase;
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
    private final ArrayDeque<BlockPos> sourceQueue = new ArrayDeque<>();
    private boolean infiniteBody;
    @Nullable private Fluid queuedFluid;
    private int queuedY;

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

    @Override
    public void serverTick(MachineContext ctx) {
        Level level = ctx.level();
        BlockPos pos = ctx.pos();

        pushOut(level, pos);

        LogisticsConfig.FluidPumpConfig cfg = LogisticsConfig.get().fluidPump;
        if ((level.getGameTime() + tickOffset) % cfg.pumpIntervalTicks == 0) {
            work((ServerLevel) level, pos, cfg);
        }

        long currentEnergy = energy.amount();
        long currentTank = tank().getAmount();
        if (currentEnergy != lastSyncedEnergy || currentTank != lastSyncedTank || targetY != lastSyncedTargetY) {
            lastSyncedEnergy = currentEnergy;
            lastSyncedTank = currentTank;
            lastSyncedTargetY = targetY;
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
        float armSpeed = LogisticsConfig.get().fluidPump.armSpeed;
        if (armY > target) {
            armY = Math.max(target, armY - armSpeed);
            if (server) {
                ctx.setChanged();
            }
        } else if (armY < target) {
            armY = Math.min(target, armY + armSpeed);
            if (server) {
                ctx.setChanged();
            }
        }
        if (targetY > pos.getY() - 1) {
            targetY = pos.getY() - 1;
        }
    }

    private void pushOut(Level level, BlockPos pos) {
        long budget = FluidUnits.mb(LogisticsConfig.get().fluidPump.pushRateMb);
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

    private void work(ServerLevel level, BlockPos pos, LogisticsConfig.FluidPumpConfig cfg) {
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
                || isPumpableSource(fluidState);
        if (atLayer && armY > targetY + 0.55f) {
            return;
        }
        if (phase == Phase.STALLED && !sourceQueue.isEmpty() && queuedFluid != null) {
            if (energy.amount() < cfg.energyPerSource) {
                return;
            }
            phase = Phase.PUMPING;
            if (pumpFromLayer(level, pos, target, queuedFluid, cfg)) {
                return;
            }
            descendToNextLayer(level, pos);
            return;
        }
        if (phase == Phase.PUMPING && queuedFluid != null) {
            if (energy.amount() < cfg.energyPerSource) {
                phase = Phase.STALLED;
                return;
            }
            // Keep draining the layer based on the body being pumped, not the tank's momentary level,
            // so an attached pipe emptying the tank doesn't cut the layer short.
            if (pumpFromLayer(level, pos, target, queuedFluid, cfg)) {
                return;
            }
            descendToNextLayer(level, pos);
            return;
        }
        if (isPumpableSource(fluidState)) {
            if (energy.amount() < cfg.energyPerSource) {
                phase = Phase.STALLED;
                return;
            }
            phase = Phase.PUMPING;
            if (!pumpFromLayer(level, pos, target, fluidState.getType(), cfg)) {
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
        return nextY >= level.getMinY() && !isBlocked(level, new BlockPos(pos.getX(), nextY, pos.getZ()));
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
            ServerLevel level, BlockPos pumpPos, BlockPos origin, Fluid fluid, LogisticsConfig.FluidPumpConfig cfg) {
        BlockPos source = findConnectedSource(level, pumpPos, origin, fluid, cfg.searchRadius);
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
        energy.consume(cfg.energyPerSource);
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
            if (state.getType().isSame(fluid) && state.isSource()) {
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
        int threshold = LogisticsConfig.get().fluidPump.infiniteSourceThreshold;
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
            if (state.isSource()) {
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
        if (dx * dx + dz * dz > radiusSq || next.getY() < level.getMinY() || next.getY() > level.getMaxY()) {
            return;
        }
        if (visited.add(next) && level.getFluidState(next).getType().isSame(fluid)) {
            queue.add(next);
        }
    }

    private boolean isPumpableSource(FluidState state) {
        Fluid fluid = state.getType();
        if (fluid == Fluids.EMPTY || !state.isSource()) {
            return false;
        }
        FluidTankComponent tank = tank();
        IFluidKey current = tank.getFluidKey();
        return tank.isEmpty() || current.getFluid() == fluid;
    }

    private boolean isBlocked(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && level.getFluidState(pos).isEmpty() && state.blocksMotion();
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
