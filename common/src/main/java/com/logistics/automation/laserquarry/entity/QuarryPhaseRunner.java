package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.compat.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the laser quarry's per-phase execution state — {@code Phase} dispatch,
 * the frame-build cursor, the mining grid cursor, and the in-flight break
 * progress — plus the {@code tick} routines that drive {@link ArmController}
 * and call out through {@link QuarryContext} for energy/world side effects.
 *
 * <p>All NBT keys remain identical to the pre-refactor block entity layout so
 * existing worlds load unchanged.
 */
public final class QuarryPhaseRunner {

    private QuarryPhase phase = QuarryPhase.CLEARING;
    private int frameBuildIndex = 0;

    private int miningX = 0;
    private int miningY = 0;
    private int miningZ = 0;
    private float breakProgress = 0f;
    private boolean finished = false;

    private @Nullable BlockPos currentTarget = null;
    private float currentBreakTime = -1f;

    // ==================== Getters used by BE / save ====================

    public QuarryPhase getPhase() {
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
        phase = QuarryPhase.CLEARING;
        frameBuildIndex = 0;
        miningX = 0;
        miningY = 0;
        miningZ = 0;
        finished = false;
        resetBreakProgress();
    }

    /** True if the quarry has been placed but hasn't started any clearing yet. */
    public boolean isFreshlyPlaced() {
        return phase == QuarryPhase.CLEARING && miningX == 0 && miningY == 0 && miningZ == 0 && breakProgress == 0;
    }

    // ==================== Tick dispatch ====================

    public void tick(QuarryContext q) {
        if (LogisticsConfig.get().quarry.loadChunks) {
            loadChunks(q);
        }

        switch (phase) {
            case CLEARING -> tickClearing(q);
            case BUILDING_FRAME -> tickBuildingFrame(q);
            case MINING -> tickMining(q);
            default -> {}
        }
    }

    private void loadChunks(QuarryContext q) {
        ServerChunkCache chunkCache = q.level().getChunkSource();
        QuarryFrameRect frame = QuarryFrameRect.resolve(
                LaserQuarryBlock.getMiningDirection(q.quarryState()),
                q.pos(),
                q.bounds(),
                LogisticsConfig.get().quarry.area);
        if (frame == null) {
            throw new IllegalStateException("Quarry has null frame");
        }

        int radius = ChunkLevel.byStatus(FullChunkStatus.FULL) - ChunkLevel.byStatus(FullChunkStatus.BLOCK_TICKING);
        ChunkPos start = new ChunkPos(new BlockPos(frame.startX(), 0, frame.startZ()));
        ChunkPos end = new ChunkPos(new BlockPos(frame.endX(), 0, frame.endZ()));

        for (int x = start.x; x <= end.x; x++) {
            for (int z = start.z; z <= end.z; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);
                chunkCache.addRegionTicket(LogisticsAutomation.TICKET_TYPE.QUARRY_BOUNDARY, chunkPos, radius, chunkPos);
            }
        }

        ChunkPos center = new ChunkPos(q.pos());
        chunkCache.addRegionTicket(LogisticsAutomation.TICKET_TYPE.QUARRY, center, radius, center);
    }

    private void tickClearing(QuarryContext q) {
        if (q.energyStored() == 0) {
            resetBreakProgress();
            return;
        }

        BlockPos target = null;
        BlockState targetState = null;
        for (int skipped = 0; skipped < LogisticsConfig.get().quarry.scanRate; skipped++) {
            target = clearingTargetPos(q);
            if (target == null) {
                transitionFromClearingToBuildingFrame(q);
                return;
            }

            targetState = q.level().getBlockState(target);
            if (!GridScanner.shouldSkip(q.level(), target, targetState)) {
                break;
            }

            advanceToNextBlock(q);
            resetBreakProgress();
        }

        if (target == null) {
            transitionFromClearingToBuildingFrame(q);
            return;
        }

        if (GridScanner.shouldSkip(q.level(), target, targetState)) {
            return;
        }

        if (!target.equals(currentTarget) || currentBreakTime < 0) {
            currentTarget = target;
            float hardness = targetState.getDestroySpeed(q.level(), target);
            currentBreakTime = q.breakCost(hardness);
            breakProgress = 0;
        }

        long energyNeeded = (long) Math.ceil(currentBreakTime - breakProgress);
        long energyToUse = Math.min(q.energyStored(), energyNeeded);
        if (energyToUse > 0) {
            q.consumeEnergy(energyToUse);
            breakProgress += energyToUse;
        }

        if (breakProgress >= currentBreakTime) {
            QuarryBlockBreaker.mineBlock(q.level(), q.pos(), target, targetState);
            advanceToNextBlock(q);
            resetBreakProgress();
        }
    }

    private void tickBuildingFrame(QuarryContext q) {
        if (!q.hasEnergy(q.frameBuildCost())) {
            return;
        }

        BlockPos framePos = FrameLayout.nextFramePosition(
                LaserQuarryBlock.getMiningDirection(q.quarryState()),
                q.pos(),
                q.bounds(),
                LogisticsConfig.get().quarry.area,
                frameBuildIndex);
        if (framePos == null) {
            // Frame built — transition to mining.
            phase = QuarryPhase.MINING;
            miningX = 0;
            miningY = 0;
            miningZ = 0;
            q.arm().resetExpectedTravelTicks();
            // armInitialized flips false so first MINING tick re-anchors the arm.
            q.arm().enterMoving();
            q.arm().markUninitialized();
            q.sync();
            q.markChanged();
            return;
        }

        q.consumeEnergy(q.frameBuildCost());

        BlockState existingState = q.level().getBlockState(framePos);
        if (existingState.isAir() || existingState.canBeReplaced()) {
            BlockState frameState = FrameLayout.frameBlockState(
                    LaserQuarryBlock.getMiningDirection(q.quarryState()),
                    q.pos(),
                    framePos,
                    q.bounds(),
                    LogisticsConfig.get().quarry.area);
            q.level().setBlockAndUpdate(framePos, frameState);
        }

        frameBuildIndex++;
        q.markChanged();
    }

    private void tickMining(QuarryContext q) {
        ArmController arm = q.arm();

        BlockPos target = null;
        boolean skippedAny = false;
        for (int skipped = 0; skipped < LogisticsConfig.get().quarry.scanRate; skipped++) {
            target = miningTargetPos(q);
            if (target == null) {
                if (currentTarget != null) {
                    q.level().destroyBlockProgress(q.breakingEntityId(), currentTarget, -1);
                }
                finished = true;
                q.markChanged();
                q.sync();
                return;
            }

            BlockState targetState = q.level().getBlockState(target);
            if (!GridScanner.shouldSkip(q.level(), target, targetState)) {
                break;
            }

            advanceMiningPosition(q);
            target = null;
            skippedAny = true;
        }

        if (target == null) {
            return;
        }

        if (skippedAny) {
            q.sync();
        }

        float targetX = target.getX() + 0.5f;
        float targetY = target.getY() + 1.0f;
        float targetZ = target.getZ() + 0.5f;

        if (!arm.isInitialized()) {
            arm.initializeAt(targetX, targetY, targetZ);
            q.sync();
        }

        if (arm.getState() == QuarryArmState.MOVING) {
            long moveCost = q.moveCost();
            if (!q.hasEnergy(moveCost)) {
                return;
            }
            q.consumeEnergy(moveCost);

            float speed = q.effectiveArmSpeed();
            if (arm.getExpectedTravelTicks() == 0 && !arm.isAt(targetX, targetY, targetZ, speed)) {
                arm.setExpectedTravelTicks(arm.travelTicksFor(targetX, targetY, targetZ, speed));
            }

            boolean reachedTarget = arm.moveTowards(targetX, targetY, targetZ, speed);

            if (reachedTarget) {
                arm.enterSettling(arm.getExpectedTravelTicks());
                arm.resetExpectedTravelTicks();
                q.sync();
                currentTarget = target;
            }
        } else if (arm.getState() == QuarryArmState.SETTLING) {
            if (arm.tickSettling()) {
                arm.enterBreaking();
            }
        } else if (arm.getState() == QuarryArmState.BREAKING) {
            BlockState targetState = q.level().getBlockState(target);

            if (!target.equals(currentTarget) || currentBreakTime < 0) {
                currentTarget = target;
                float hardness = targetState.getDestroySpeed(q.level(), target);
                currentBreakTime = (float) (LogisticsConfig.get().quarry.energyPerBlockMultiplier() * (hardness + 1));
                breakProgress = 0;
            }

            long energyNeeded = (long) Math.ceil(currentBreakTime - breakProgress);
            long energyToUse = Math.min(q.energyStored(), energyNeeded);
            if (energyToUse > 0) {
                q.consumeEnergy(energyToUse);
                breakProgress += energyToUse;
            }

            int breakStage = (int) ((breakProgress / currentBreakTime) * 10f);
            breakStage = Math.min(breakStage, 9);
            q.level().destroyBlockProgress(q.breakingEntityId(), target, breakStage);

            if (breakProgress >= currentBreakTime) {
                q.level().destroyBlockProgress(q.breakingEntityId(), target, -1);

                QuarryBlockBreaker.mineBlock(q.level(), q.pos(), target, targetState);
                advanceMiningPosition(q);
                resetBreakProgress();

                skipToNextSolidBlock(q);

                BlockPos nextTarget = miningTargetPos(q);
                if (nextTarget != null) {
                    float newX = nextTarget.getX() + 0.5f;
                    float newY = nextTarget.getY() + 1.0f;
                    float newZ = nextTarget.getZ() + 0.5f;

                    arm.warpTo(newX, newY, newZ, q.effectiveArmSpeed());
                    arm.enterSettling(arm.getExpectedTravelTicks());
                    currentTarget = nextTarget;
                } else {
                    arm.enterMoving();
                }
                q.sync();
            }
        }
    }

    // ==================== Internal grid math ====================

    private @Nullable BlockPos clearingTargetPos(QuarryContext q) {
        return GridScanner.clearingTarget(
                LaserQuarryBlock.getMiningDirection(q.quarryState()),
                q.pos(),
                q.bounds(),
                LogisticsConfig.get().quarry.area,
                miningX,
                miningY,
                miningZ);
    }

    private @Nullable BlockPos miningTargetPos(QuarryContext q) {
        return GridScanner.miningTarget(
                LaserQuarryBlock.getMiningDirection(q.quarryState()),
                q.pos(),
                q.bounds(),
                LogisticsConfig.get().quarry.area,
                q.levelMinY(),
                miningX,
                miningY,
                miningZ);
    }

    private void advanceToNextBlock(QuarryContext q) {
        miningX++;
        int maxX = q.bounds().isCustom()
                ? (q.bounds().getMaxX() - q.bounds().getMinX() + 1)
                : LogisticsConfig.get().quarry.area;
        int maxZ = q.bounds().isCustom()
                ? (q.bounds().getMaxZ() - q.bounds().getMinZ() + 1)
                : LogisticsConfig.get().quarry.area;

        if (miningX >= maxX) {
            miningX = 0;
            miningZ++;
            if (miningZ >= maxZ) {
                miningZ = 0;
                miningY++;
            }
        }
        q.markChanged();
    }

    private void transitionFromClearingToBuildingFrame(QuarryContext q) {
        phase = QuarryPhase.BUILDING_FRAME;
        frameBuildIndex = 0;
        resetBreakProgress();
        q.markChanged();
        q.sync();
    }

    private void advanceMiningPosition(QuarryContext q) {
        int innerSizeX;
        int innerSizeZ;
        QuarryBounds bounds = q.bounds();
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
        q.markChanged();
    }

    private void skipToNextSolidBlock(QuarryContext q) {
        for (int skipped = 0; skipped < LogisticsConfig.get().quarry.scanRate; skipped++) {
            BlockPos target = miningTargetPos(q);
            if (target == null) {
                finished = true;
                return;
            }

            BlockState targetState = q.level().getBlockState(target);
            if (!GridScanner.shouldSkip(q.level(), target, targetState)) {
                return;
            }

            advanceMiningPosition(q);
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
            phase = QuarryPhase.valueOf(phaseName);
        } catch (IllegalArgumentException e) {
            phase = QuarryPhase.CLEARING;
        }
    }
}
