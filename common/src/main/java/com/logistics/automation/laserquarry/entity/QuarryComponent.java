package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.ChunkArea;
import com.logistics.core.machine.component.ChunkLoadingComponent;
import com.logistics.core.machine.upgrade.MachineModifiers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Laser Quarry's behavior as a single machine component. Owns the mining region, the arm, the
 * phase runner, the energy policy, the output sink, and chunk loading, and runs the full mining
 * tick. Implements {@link QuarryContext} so {@link QuarryPhaseRunner} depends only on it; world,
 * sync, and dirty handles come from the per-tick {@link MachineContext}.
 */
public final class QuarryComponent implements MachineComponent, QuarryContext {

    private final String id;
    private final int breakingEntityId;
    private final QuarryEnergyPolicy energyPolicy;
    private final QuarryOutput output;
    private final ChunkLoadingComponent chunkLoader;

    private final QuarryBounds bounds = new QuarryBounds();
    private final ArmController armController = new ArmController();
    private final QuarryPhaseRunner phaseRunner = new QuarryPhaseRunner();

    private long lastSyncedEnergy = 0;

    // Set for the duration of serverTick so the QuarryContext methods can reach world/sync/dirty.
    @Nullable
    private MachineContext ctx;

    public QuarryComponent(String id, BlockPos pos, EnergyComponent energy, MachineModifiers modifiers) {
        this.id = id;
        this.breakingEntityId = pos.hashCode();
        this.energyPolicy = new QuarryEnergyPolicy(energy, modifiers, this::markChanged);
        this.output = new QuarryOutput(pos);
        this.chunkLoader = new ChunkLoadingComponent(
                "chunks",
                LogisticsAutomation.TICKET_TYPE.QUARRY,
                LogisticsAutomation.TICKET_TYPE.QUARRY_BOUNDARY,
                this::chunkArea);
    }

    @Override
    public String id() {
        return id;
    }

    // ==================== Tick ====================

    @Override
    public void serverTick(MachineContext context) {
        this.ctx = context;
        try {
            ActiveQuarryRegistry.register((ServerLevel) context.level(), context.pos());

            boolean wasConsumedEnergy = energyPolicy.consumedThisTick();
            energyPolicy.resetConsumedThisTick();

            if (phaseRunner.isFinished()) {
                setActive(false);
                if (wasConsumedEnergy) {
                    context.sync();
                }
                return;
            }

            chunkLoader.keepLoaded((ServerLevel) context.level());
            phaseRunner.tick(this);

            // Idle power consumption: 1 RF every 4 ticks (5 RF/second) to slowly drain buffer.
            if (context.level().getGameTime() % 4 == 0 && energyPolicy.stored() > 0) {
                energyPolicy.drainIdle(1);
            }

            setActive(energyPolicy.stored() > 0);

            boolean needsSync = energyPolicy.stored() != lastSyncedEnergy
                    || energyPolicy.consumedThisTick() != wasConsumedEnergy;
            if (needsSync) {
                lastSyncedEnergy = energyPolicy.stored();
                armController.setSyncedSpeed(energyPolicy.effectiveArmSpeed(context.level(), context.pos()));
                context.sync();
            }
        } finally {
            this.ctx = null;
        }
    }

    /** Drives {@link LaserQuarryBlock#ACTIVE} so the front lights up while powered and mining. */
    private void setActive(boolean active) {
        BlockState state = ctx.blockState();
        if (state.getValue(LaserQuarryBlock.ACTIVE) != active) {
            ctx.setBlockState(state.setValue(LaserQuarryBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    /**
     * The chunks to keep loaded this tick — the work-area boundary plus the quarry's own chunk — or
     * {@code null} when chunk loading is disabled. Throws if a configured quarry resolves no frame,
     * preserving the original guard.
     */
    @Nullable
    private ChunkArea chunkArea() {
        if (!LogisticsConfig.get().quarry.loadChunks) {
            return null;
        }
        QuarryFrameRect frame = QuarryFrameRect.resolve(
                LaserQuarryBlock.getMiningDirection(ctx.blockState()),
                ctx.pos(),
                bounds,
                LogisticsConfig.get().quarry.area);
        if (frame == null) {
            throw new IllegalStateException("Quarry has null frame");
        }

        ChunkPos start = ChunkPos.containing(new BlockPos(frame.startX(), 0, frame.startZ()));
        ChunkPos end = ChunkPos.containing(new BlockPos(frame.endX(), 0, frame.endZ()));
        List<ChunkPos> boundary = new ArrayList<>();
        for (int x = start.x(); x <= end.x(); x++) {
            for (int z = start.z(); z <= end.z(); z++) {
                boundary.add(new ChunkPos(x, z));
            }
        }
        ChunkPos center = ChunkPos.containing(ctx.pos());

        return new ChunkArea() {
            @Override
            public ChunkPos center() {
                return center;
            }

            @Override
            public Iterable<ChunkPos> boundary() {
                return boundary;
            }
        };
    }

    // ==================== QuarryContext ====================

    @Override
    public ServerLevel level() {
        return (ServerLevel) ctx.level();
    }

    @Override
    public BlockPos pos() {
        return ctx.pos();
    }

    @Override
    public BlockState quarryState() {
        return ctx.blockState();
    }

    @Override
    public QuarryBounds bounds() {
        return bounds;
    }

    @Override
    public ArmController arm() {
        return armController;
    }

    @Override
    public QuarryOutput output() {
        return output;
    }

    @Override
    public int breakingEntityId() {
        return breakingEntityId;
    }

    @Override
    public int levelMinY() {
        return ctx.level().getMinY();
    }

    @Override
    public long energyStored() {
        return energyPolicy.stored();
    }

    @Override
    public boolean hasEnergy(long rf) {
        return energyPolicy.has(rf);
    }

    @Override
    public void consumeEnergy(long rf) {
        energyPolicy.consume(rf);
    }

    @Override
    public long moveCost() {
        return energyPolicy.moveCost();
    }

    @Override
    public float effectiveArmSpeed() {
        return energyPolicy.effectiveArmSpeed(ctx.level(), ctx.pos());
    }

    @Override
    public long frameBuildCost() {
        return energyPolicy.frameBuildCost();
    }

    @Override
    public float breakCost(float hardness) {
        return energyPolicy.breakCost(hardness);
    }

    @Override
    public void markChanged() {
        ctx.setChanged();
    }

    @Override
    public void sync() {
        ctx.sync();
    }

    // ==================== Facade accessors (block entity delegates these) ====================

    public QuarryPhase phase() {
        return phaseRunner.getPhase();
    }

    public boolean isFinished() {
        return phaseRunner.isFinished();
    }

    public boolean isFreshlyPlaced() {
        return phaseRunner.isFreshlyPlaced();
    }

    public boolean consumedEnergyThisTick() {
        return energyPolicy.consumedThisTick();
    }

    public float armX() {
        return armController.getX();
    }

    public float armY() {
        return armController.getY();
    }

    public float armZ() {
        return armController.getZ();
    }

    public QuarryArmState armState() {
        return armController.getState();
    }

    public float syncedArmSpeed() {
        return armController.getSyncedSpeed();
    }

    public boolean armInitialized() {
        return armController.isInitialized();
    }

    public boolean hasCustomBounds() {
        return bounds.isCustom();
    }

    public int customMinX() {
        return bounds.getMinX();
    }

    public int customMinZ() {
        return bounds.getMinZ();
    }

    public int customMaxX() {
        return bounds.getMaxX();
    }

    public int customMaxZ() {
        return bounds.getMaxZ();
    }

    @Nullable
    public BlockPos currentTarget() {
        return phaseRunner.getCurrentTarget();
    }

    public int breakingEntityIdValue() {
        return breakingEntityId;
    }

    /** Marker callback: applies the new region and restarts the clear -> frame -> mine sequence. */
    public void setCustomBounds(int minX, int minZ, int maxX, int maxZ) {
        bounds.setCustom(minX, minZ, maxX, maxZ);
        phaseRunner.onCustomBoundsSet();
    }

    // ==================== NBT ====================

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        phaseRunner.save(tag);
        armController.save(tag);
        bounds.save(tag);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        phaseRunner.load(tag);
        armController.load(tag);
        bounds.load(tag);
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        // Pre-components-tag worlds stored these keys at the LogisticsData root (the current format).
        phaseRunner.load(root);
        armController.load(root);
        bounds.load(root);
    }
}
