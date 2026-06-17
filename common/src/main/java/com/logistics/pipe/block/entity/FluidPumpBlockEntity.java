package com.logistics.pipe.block.entity;

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
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.EnergyDemandProvider;
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

public class FluidPumpBlockEntity extends BaseBlockEntity
        implements HasEnergyStorage, HasFluidStorage, EnergyDemandProvider {

    public enum Phase {
        DESCENDING,
        PUMPING,
        STALLED
    }

    private final EnergyComponent energy = new EnergyComponent(
            () -> LogisticsConfig.get().fluidPump.energyCapacity,
            LogisticsConfig.get().fluidPump.maxEnergyInput,
            0,
            this::markDirtyAndSync);
    private final FluidTankComponent tank = new FluidTankComponent(
            FluidUnits.mb(LogisticsConfig.get().fluidPump.tankCapacityMb),
            this::markDirtyAndSync);

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

    public FluidPumpBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsFluid.ENTITY.FLUID_PUMP_BLOCK_ENTITY, pos, state);
        this.targetY = pos.getY() - 1;
        this.armY = pos.getY();
        this.tickOffset = Math.floorMod(pos.asLong(), 16L);
    }

    @Override
    @Nullable
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return energy;
    }

    @Override
    @Nullable
    public IFluidStorage fluidStorage(@Nullable Direction side) {
        return tank;
    }

    public FluidTankComponent tank() {
        return tank;
    }

    public Phase phase() {
        return phase;
    }

    public float armY() {
        return armY;
    }

    public long energyAmount() {
        return energy.getAmount();
    }

    @Override
    public long networkDemandPerTick() {
        return Math.max(0, energy.getCapacity() - energy.getAmount());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidPumpBlockEntity be) {
        // Animate the tube on both sides; the client advances the arm toward the synced targetY so the
        // descent stays smooth between the sparse state syncs (which only fire on real state changes).
        be.advanceArm(pos);
        if (level.isClientSide()) {
            return;
        }

        be.pushUp(level, pos);

        LogisticsConfig.FluidPumpConfig cfg = LogisticsConfig.get().fluidPump;
        if ((level.getGameTime() + be.tickOffset) % cfg.pumpIntervalTicks == 0) {
            be.work((ServerLevel) level, pos, cfg);
        }

        if (be.energy.getAmount() != be.lastSyncedEnergy
                || be.tank.getAmount() != be.lastSyncedTank
                || be.targetY != be.lastSyncedTargetY) {
            be.lastSyncedEnergy = be.energy.getAmount();
            be.lastSyncedTank = be.tank.getAmount();
            be.lastSyncedTargetY = be.targetY;
            be.markDirtyAndSync();
        }
    }

    private void pushUp(Level level, BlockPos pos) {
        if (tank.isEmpty()) {
            return;
        }
        IFluidStorage target = FluidStorageLookup.find(level, pos.above(), Direction.DOWN);
        if (target == null) {
            return;
        }
        IFluidView view = tank.contents().iterator().next();
        long max = Math.min(FluidUnits.mb(LogisticsConfig.get().fluidPump.pushRateMb), view.amount());
        long accepted = target.insert(view.resource(), max, true);
        if (accepted <= 0) {
            return;
        }
        accepted = target.insert(view.resource(), accepted, false);
        if (accepted > 0) {
            tank.extract(view.resource(), accepted, false);
        }
    }

    private void advanceArm(BlockPos pos) {
        float target = targetY + 0.5f;
        boolean server = level != null && !level.isClientSide();
        if (armY > target) {
            armY = Math.max(target, armY - LogisticsConfig.get().fluidPump.armSpeed);
            if (server) {
                setChanged();
            }
        } else if (armY < target) {
            armY = Math.min(target, armY + LogisticsConfig.get().fluidPump.armSpeed);
            if (server) {
                setChanged();
            }
        }
        if (targetY > pos.getY() - 1) {
            targetY = pos.getY() - 1;
        }
    }

    private void work(ServerLevel level, BlockPos pos, LogisticsConfig.FluidPumpConfig cfg) {
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
            if (energy.getAmount() < cfg.energyPerSource) {
                return;
            }
            phase = Phase.PUMPING;
            if (pumpFromLayer(level, pos, target, queuedFluid, cfg)) {
                return;
            }
            descendToNextLayer();
            return;
        }
        if (phase == Phase.PUMPING && !tank.isEmpty()) {
            if (energy.getAmount() < cfg.energyPerSource) {
                phase = Phase.STALLED;
                return;
            }
            Fluid fluid = tank.getFluidKey().getFluid();
            if (pumpFromLayer(level, pos, target, fluid, cfg)) {
                return;
            }
            descendToNextLayer();
            return;
        }
        if (isPumpableSource(fluidState)) {
            if (energy.getAmount() < cfg.energyPerSource) {
                phase = Phase.STALLED;
                return;
            }
            phase = Phase.PUMPING;
            if (!pumpFromLayer(level, pos, target, fluidState.getType(), cfg)) {
                descendToNextLayer();
            }
            return;
        }

        if (isBlocked(level, target)) {
            phase = Phase.STALLED;
            return;
        }

        phase = Phase.DESCENDING;
        targetY--;
    }

    private void descendToNextLayer() {
        sourceQueue.clear();
        infiniteBody = false;
        queuedFluid = null;
        targetY--;
        phase = Phase.DESCENDING;
    }

    private boolean pumpFromLayer(
            ServerLevel level, BlockPos pumpPos, BlockPos origin, Fluid fluid, LogisticsConfig.FluidPumpConfig cfg) {
        BlockPos source = findConnectedSource(level, pumpPos, origin, fluid, cfg.searchRadius);
        if (source == null) {
            return false;
        }
        IFluidKey key = SimpleFluidKey.of(fluid);
        long bucket = FluidUnits.mb(1_000);
        if (tank.insert(key, bucket, true) != bucket) {
            phase = Phase.STALLED;
            return true;
        }
        energy.consume(cfg.energyPerSource);
        tank.insert(key, bucket, false);
        if (infiniteBody) {
            // Effectively infinite body: draw fluid without carving the landscape.
            sourceQueue.addLast(source);
        } else {
            // UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE removes the source without notifying
            // neighbors, so adjacent sources never reflow to refill the hole.
            level.setBlock(source, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        markDirtyAndSync();
        return true;
    }

    @Nullable
    private BlockPos findConnectedSource(ServerLevel level, BlockPos pumpPos, BlockPos origin, Fluid fluid, int radius) {
        if (sourceQueue.isEmpty() || queuedFluid != fluid || queuedY != origin.getY()) {
            rebuildSourceQueue(level, pumpPos, origin, fluid, radius);
        }

        while (!sourceQueue.isEmpty()) {
            BlockPos source = sourceQueue.removeFirst();
            FluidState state = level.getFluidState(source);
            if (state.getType() == fluid && state.isSource()) {
                return source;
            }
        }
        return null;
    }

    // Floods only the horizontal layer at origin's Y, so the pump exhausts one layer before stepping down.
    private void rebuildSourceQueue(ServerLevel level, BlockPos pumpPos, BlockPos origin, Fluid fluid, int radius) {
        sourceQueue.clear();
        infiniteBody = false;
        queuedFluid = fluid;
        queuedY = origin.getY();

        // Only reforming fluids (water) get infinite treatment; lava is always consumed.
        int threshold = LogisticsConfig.get().fluidPump.infiniteSourceThreshold;
        boolean canBeInfinite = threshold > 0 && fluid.defaultFluidState().is(FluidTags.WATER);

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin);
        int radiusSq = radius * radius;

        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            FluidState state = level.getFluidState(current);
            if (state.getType() != fluid) {
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
        if (visited.add(next) && level.getFluidState(next).getType() == fluid) {
            queue.add(next);
        }
    }

    private boolean isPumpableSource(FluidState state) {
        Fluid fluid = state.getType();
        if (fluid == Fluids.EMPTY || !state.isSource()) {
            return false;
        }
        IFluidKey current = tank.getFluidKey();
        return tank.isEmpty() || current.getFluid() == fluid;
    }

    private boolean isBlocked(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && level.getFluidState(pos).isEmpty() && state.blocksMotion();
    }

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLogisticsData(tag, registries);
        energy.writeNbt(tag, "Energy");
        tank.writeNbt(tag, "Tank");
        tag.putString("Phase", phase.name());
        tag.putInt("TargetY", targetY);
        tag.putFloat("ArmY", armY);
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        energy.readNbt(tag, "Energy");
        tank.readNbt(tag, "Tank");
        try {
            phase = Phase.valueOf(NbtCompat.getString(tag, "Phase", Phase.DESCENDING.name()));
        } catch (IllegalArgumentException e) {
            phase = Phase.DESCENDING;
        }
        targetY = NbtCompat.getInt(tag, "TargetY", worldPosition.getY() - 1);
        armY = NbtCompat.getFloat(tag, "ArmY", worldPosition.getY());
    }
}
