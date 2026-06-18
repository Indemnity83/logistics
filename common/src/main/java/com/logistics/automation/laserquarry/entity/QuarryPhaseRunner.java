package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity.ArmState;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity.Phase;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.compat.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Owns the laser quarry's per-phase execution state — {@code Phase} dispatch,
 * the frame-build cursor, the mining grid cursor, and the in-flight break
 * progress — plus the {@code tick} routines that drive {@link ArmController}
 * and call back into the block entity for energy/world side effects.
 *
 * <p>All NBT keys remain identical to the pre-refactor block entity layout so
 * existing worlds load unchanged.
 */
public final class QuarryPhaseRunner {

    private Phase phase = Phase.CLEARING;
    private int frameBuildIndex = 0;

    private int miningX = 0;
    private int miningY = 0;
    private int miningZ = 0;
    private float breakProgress = 0f;
    private boolean finished = false;

    private @Nullable BlockPos currentTarget = null;
    private float currentBreakTime = -1f;

    // ==================== Getters used by BE / save ====================

    public Phase getPhase() {
        return phase;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getMiningX() {
        return miningX;
    }

    public int getMiningY() {
        return miningY;
    }

    public int getMiningZ() {
        return miningZ;
    }

    public float getBreakProgress() {
        return breakProgress;
    }

    public float getCurrentBreakTime() {
        return currentBreakTime;
    }

    public @Nullable BlockPos getCurrentTarget() {
        return currentTarget;
    }

    public void clearCurrentTarget() {
        this.currentTarget = null;
    }

    /** Marker callback: bounds changed, restart the full clear -> frame -> mine sequence. */
    public void onCustomBoundsSet() {
        phase = Phase.CLEARING;
        frameBuildIndex = 0;
        miningX = 0;
        miningY = 0;
        miningZ = 0;
        finished = false;
        resetBreakProgress();
    }

    /** True if the quarry has been placed but hasn't started any clearing yet. */
    public boolean isFreshlyPlaced() {
        return phase == Phase.CLEARING && miningX == 0 && miningY == 0 && miningZ == 0 && breakProgress == 0;
    }

    // ==================== Tick dispatch ====================

    public void tick(LaserQuarryBlockEntity be, ServerLevel world, BlockPos pos, BlockState state) {
        if (LogisticsConfig.get().quarry.loadChunks) {
            loadChunks(be, world, pos, state);
        }

        switch (phase) {
            case CLEARING -> tickClearing(be, world, pos, state);
            case BUILDING_FRAME -> tickBuildingFrame(be, world, state);
            case MINING -> tickMining(be, world, state);
            default -> {
            }
        }
    }

    private void loadChunks(LaserQuarryBlockEntity be, ServerLevel world, BlockPos pos, BlockState state) {
        ServerChunkCache chunkCache = world.getChunkSource();
        QuarryFrameRect frame = QuarryFrameRect.resolve(
                LaserQuarryBlock.getMiningDirection(state),
                pos,
                be.getBounds(),
                LogisticsConfig.get().quarry.area
        );
        if (frame == null) {
            throw new IllegalStateException("Query has null frame");
        }

        Set<ChunkPos> chunks = new HashSet<>();
        ChunkPos start = ChunkPos.containing(new BlockPos(frame.startX(), 0, frame.startZ()));
        ChunkPos end = ChunkPos.containing(new BlockPos(frame.endX(), 0, frame.endZ()));

        for (int x = start.x(); x <= end.x(); x++) {
            for (int z = start.z(); z <= end.z(); z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }

        for (ChunkPos chunk : chunks) {
            Ticket ticket = new Ticket(
                    LogisticsAutomation.TICKET_TYPE.QUARRY_BOUNDARY,
                    ChunkLevel.byStatus(FullChunkStatus.BLOCK_TICKING)
            );

            chunkCache.addTicket(ticket, chunk);
        }

        Ticket ticket = new Ticket(
                LogisticsAutomation.TICKET_TYPE.QUARRY,
                ChunkLevel.byStatus(FullChunkStatus.BLOCK_TICKING)
        );
        ChunkPos chunk = ChunkPos.containing(pos);
        chunkCache.addTicket(ticket, chunk);
    }

    private void tickClearing(LaserQuarryBlockEntity be, ServerLevel world, BlockPos pos, BlockState state) {
        if (be.getEnergyAmount() == 0) {
            resetBreakProgress();
            return;
        }

        BlockPos target = null;
        BlockState targetState = null;
        for (int skipped = 0; skipped < LogisticsConfig.get().quarry.scanRate; skipped++) {
            target = clearingTargetPos(be, state);
            if (target == null) {
                transitionFromClearingToBuildingFrame(be);
                return;
            }

            targetState = world.getBlockState(target);
            if (!GridScanner.shouldSkip(world, target, targetState)) {
                break;
            }

            advanceToNextBlock(be);
            resetBreakProgress();
        }

        if (target == null) {
            transitionFromClearingToBuildingFrame(be);
            return;
        }

        if (GridScanner.shouldSkip(world, target, targetState)) {
            return;
        }

        if (!target.equals(currentTarget) || currentBreakTime < 0) {
            currentTarget = target;
            float hardness = targetState.getDestroySpeed(world, target);
            currentBreakTime = (float) (LogisticsConfig.get().quarry.energyPerBlockMultiplier() * (hardness + 1));
            breakProgress = 0;
        }

        long energyNeeded = (long) Math.ceil(currentBreakTime - breakProgress);
        long energyToUse = Math.min(be.getEnergyAmount(), energyNeeded);
        if (energyToUse > 0) {
            be.consumeEnergy(energyToUse);
            breakProgress += energyToUse;
        }

        if (breakProgress >= currentBreakTime) {
            QuarryBlockBreaker.mineBlock(world, be.getBlockPos(), target, targetState);
            advanceToNextBlock(be);
            resetBreakProgress();
        }
    }

    private void tickBuildingFrame(LaserQuarryBlockEntity be, ServerLevel world, BlockState state) {
        if (!be.hasEnergy(LaserQuarryBlockEntity.FRAME_BUILD_COST)) {
            return;
        }

        BlockPos framePos = FrameLayout.nextFramePosition(
                LaserQuarryBlock.getMiningDirection(state),
                be.getBlockPos(),
                be.getBounds(),
                LogisticsConfig.get().quarry.area,
                frameBuildIndex);
        if (framePos == null) {
            // Frame built — transition to mining.
            phase = Phase.MINING;
            miningX = 0;
            miningY = 0;
            miningZ = 0;
            be.getArmController().resetExpectedTravelTicks();
            // armInitialized flips false so first MINING tick re-anchors the arm.
            be.getArmController().enterMoving();
            be.getArmController().markUninitialized();
            be.syncToClients();
            be.setChanged();
            return;
        }

        be.consumeEnergy(LaserQuarryBlockEntity.FRAME_BUILD_COST);

        BlockState existingState = world.getBlockState(framePos);
        if (existingState.isAir() || existingState.canBeReplaced()) {
            BlockState frameState = FrameLayout.frameBlockState(
                    LaserQuarryBlock.getMiningDirection(state),
                    be.getBlockPos(),
                    framePos,
                    be.getBounds(),
                    LogisticsConfig.get().quarry.area);
            world.setBlockAndUpdate(framePos, frameState);
        }

        frameBuildIndex++;
        be.setChanged();
    }

    private void tickMining(LaserQuarryBlockEntity be, ServerLevel world, BlockState state) {
        ArmController arm = be.getArmController();

        BlockPos target = null;
        boolean skippedAny = false;
        for (int skipped = 0; skipped < LogisticsConfig.get().quarry.scanRate; skipped++) {
            target = miningTargetPos(be, state);
            if (target == null) {
                if (currentTarget != null) {
                    world.destroyBlockProgress(be.getBreakingEntityId(), currentTarget, -1);
                }
                finished = true;
                be.setChanged();
                be.syncToClients();
                return;
            }

            BlockState targetState = world.getBlockState(target);
            if (!GridScanner.shouldSkip(world, target, targetState)) {
                break;
            }

            advanceMiningPosition(be);
            target = null;
            skippedAny = true;
        }

        if (target == null) {
            return;
        }

        if (skippedAny) {
            be.syncToClients();
        }

        float targetX = target.getX() + 0.5f;
        float targetY = target.getY() + 1.0f;
        float targetZ = target.getZ() + 0.5f;

        if (!arm.isInitialized()) {
            arm.initializeAt(targetX, targetY, targetZ);
            be.syncToClients();
        }

        if (arm.getState() == ArmState.MOVING) {
            long moveCost = be.getMoveCost();
            if (!be.hasEnergy(moveCost)) {
                return;
            }
            be.consumeEnergy(moveCost);

            float speed = be.getEffectiveArmSpeed();
            if (arm.getExpectedTravelTicks() == 0 && !arm.isAt(targetX, targetY, targetZ, speed)) {
                arm.setExpectedTravelTicks(arm.travelTicksFor(targetX, targetY, targetZ, speed));
            }

            boolean reachedTarget = arm.moveTowards(targetX, targetY, targetZ, speed);

            if (reachedTarget) {
                arm.enterSettling(arm.getExpectedTravelTicks());
                arm.resetExpectedTravelTicks();
                be.syncToClients();
                currentTarget = target;
            }
        } else if (arm.getState() == ArmState.SETTLING) {
            if (arm.tickSettling()) {
                arm.enterBreaking();
            }
        } else if (arm.getState() == ArmState.BREAKING) {
            BlockState targetState = world.getBlockState(target);

            if (!target.equals(currentTarget) || currentBreakTime < 0) {
                currentTarget = target;
                float hardness = targetState.getDestroySpeed(world, target);
                currentBreakTime = (float) (LogisticsConfig.get().quarry.energyPerBlockMultiplier() * (hardness + 1));
                breakProgress = 0;
            }

            long energyNeeded = (long) Math.ceil(currentBreakTime - breakProgress);
            long energyToUse = Math.min(be.getEnergyAmount(), energyNeeded);
            if (energyToUse > 0) {
                be.consumeEnergy(energyToUse);
                breakProgress += energyToUse;
            }

            int breakStage = (int) ((breakProgress / currentBreakTime) * 10f);
            breakStage = Math.min(breakStage, 9);
            world.destroyBlockProgress(be.getBreakingEntityId(), target, breakStage);

            if (breakProgress >= currentBreakTime) {
                world.destroyBlockProgress(be.getBreakingEntityId(), target, -1);

                QuarryBlockBreaker.mineBlock(world, be.getBlockPos(), target, targetState);
                advanceMiningPosition(be);
                resetBreakProgress();

                skipToNextSolidBlock(be, world, state);

                BlockPos nextTarget = miningTargetPos(be, state);
                if (nextTarget != null) {
                    float newX = nextTarget.getX() + 0.5f;
                    float newY = nextTarget.getY() + 1.0f;
                    float newZ = nextTarget.getZ() + 0.5f;

                    arm.warpTo(newX, newY, newZ, be.getEffectiveArmSpeed());
                    arm.enterSettling(arm.getExpectedTravelTicks());
                    currentTarget = nextTarget;
                } else {
                    arm.enterMoving();
                }
                be.syncToClients();
            }
        }
    }

    // ==================== Internal grid math ====================

    private @Nullable BlockPos clearingTargetPos(LaserQuarryBlockEntity be, BlockState quarryState) {
        return GridScanner.clearingTarget(
                LaserQuarryBlock.getMiningDirection(quarryState),
                be.getBlockPos(),
                be.getBounds(),
                LogisticsConfig.get().quarry.area,
                miningX,
                miningY,
                miningZ);
    }

    private @Nullable BlockPos miningTargetPos(LaserQuarryBlockEntity be, BlockState quarryState) {
        return GridScanner.miningTarget(
                LaserQuarryBlock.getMiningDirection(quarryState),
                be.getBlockPos(),
                be.getBounds(),
                LogisticsConfig.get().quarry.area,
                be.getLevelMinY(),
                miningX,
                miningY,
                miningZ);
    }

    private void advanceToNextBlock(LaserQuarryBlockEntity be) {
        miningX++;
        int maxX = be.getBounds().isCustom()
                ? (be.getBounds().getMaxX() - be.getBounds().getMinX() + 1)
                : LogisticsConfig.get().quarry.area;
        int maxZ = be.getBounds().isCustom()
                ? (be.getBounds().getMaxZ() - be.getBounds().getMinZ() + 1)
                : LogisticsConfig.get().quarry.area;

        if (miningX >= maxX) {
            miningX = 0;
            miningZ++;
            if (miningZ >= maxZ) {
                miningZ = 0;
                miningY++;
            }
        }
        be.setChanged();
    }

    private void transitionFromClearingToBuildingFrame(LaserQuarryBlockEntity be) {
        phase = Phase.BUILDING_FRAME;
        frameBuildIndex = 0;
        resetBreakProgress();
        be.setChanged();
        be.syncToClients();
    }

    private void advanceMiningPosition(LaserQuarryBlockEntity be) {
        int innerSizeX;
        int innerSizeZ;
        QuarryBounds bounds = be.getBounds();
        if (bounds.isCustom()) {
            innerSizeX = bounds.getMaxX() - bounds.getMinX() - 1;
            innerSizeZ = bounds.getMaxZ() - bounds.getMinZ() - 1;
        } else {
            innerSizeX = LogisticsConfig.get().quarry.area - 2;
            innerSizeZ = LogisticsConfig.get().quarry.area - 2;
        }
        if (innerSizeX <= 0 || innerSizeZ <= 0) {
            return;
        }

        miningX++;
        if (miningX >= innerSizeX) {
            miningX = 0;
            miningZ++;
            if (miningZ >= innerSizeZ) {
                miningZ = 0;
                miningY++;
            }
        }
        be.setChanged();
    }

    private void skipToNextSolidBlock(LaserQuarryBlockEntity be, ServerLevel world, BlockState quarryState) {
        for (int skipped = 0; skipped < LogisticsConfig.get().quarry.scanRate; skipped++) {
            BlockPos target = miningTargetPos(be, quarryState);
            if (target == null) {
                finished = true;
                return;
            }

            BlockState targetState = world.getBlockState(target);
            if (!GridScanner.shouldSkip(world, target, targetState)) {
                return;
            }

            advanceMiningPosition(be);
        }
    }

    private void resetBreakProgress() {
        breakProgress = 0f;
        currentTarget = null;
        currentBreakTime = -1f;
    }

    // ==================== NBT ====================

    public void save(CompoundTag tag) {
        tag.putInt("MiningX", miningX);
        tag.putInt("MiningY", miningY);
        tag.putInt("MiningZ", miningZ);
        tag.putFloat("BreakProgress", breakProgress);
        tag.putBoolean("MiningFinished", finished);
        tag.putString("CurrentPhase", phase.name());
        tag.putInt("FrameBuildIndex", frameBuildIndex);
    }

    public void load(CompoundTag tag) {
        miningX = NbtCompat.getInt(tag, "MiningX", 0);
        miningY = NbtCompat.getInt(tag, "MiningY", 0);
        miningZ = NbtCompat.getInt(tag, "MiningZ", 0);
        breakProgress = NbtCompat.getFloat(tag, "BreakProgress", 0f);
        finished = NbtCompat.getBoolean(tag, "MiningFinished", false);
        frameBuildIndex = NbtCompat.getInt(tag, "FrameBuildIndex", 0);

        String phaseName = NbtCompat.getString(tag, "CurrentPhase", "CLEARING");
        try {
            phase = Phase.valueOf(phaseName);
        } catch (IllegalArgumentException e) {
            phase = Phase.CLEARING;
        }
    }

    /** Load from the legacy {@code MiningState} compound. */
    public void loadLegacy(CompoundTag miningStateTag) {
        miningX = NbtCompat.getInt(miningStateTag, "X", 0);
        miningY = NbtCompat.getInt(miningStateTag, "Y", 0);
        miningZ = NbtCompat.getInt(miningStateTag, "Z", 0);
        breakProgress = NbtCompat.getFloat(miningStateTag, "Progress", 0f);
        finished = NbtCompat.getBoolean(miningStateTag, "Finished", false);
        frameBuildIndex = NbtCompat.getInt(miningStateTag, "FrameBuildIndex", 0);

        String phaseName = NbtCompat.getString(miningStateTag, "Phase", "CLEARING");
        try {
            phase = Phase.valueOf(phaseName);
        } catch (IllegalArgumentException e) {
            phase = Phase.CLEARING;
        }
    }
}
