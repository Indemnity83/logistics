package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsAutomation;

import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
import com.logistics.core.lib.compat.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.minecraft.world.level.BlockGetter;
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

    /** Frame positions checked per mining tick; the whole ring is swept over many ticks. */
    private static final int FRAME_SCAN_PER_TICK = 8;

    /** Clearance positions checked per tick; the band is far larger than the frame ring. */
    private static final int CLEARANCE_SCAN_PER_TICK = 64;

    private QuarryPhase phase = QuarryPhase.CLEARING;
    private int frameBuildIndex = 0;

    /** Rolling cursor for the background frame-integrity sweep run during MINING. */
    private int frameScanIndex = 0;

    /** Cursor into the frame sequence while REPAIRING_FRAME walks it looking for gaps. */
    private int frameRepairIndex = 0;

    /** Rolling cursor over the clearance volume, shared by the mining sweep and the clear-out. */
    private int clearanceIndex = 0;

    // Break progress for the clearance sweep, kept separate from the mining fields above so a
    // maintenance detour never disturbs the block mining was part-way through.
    private @Nullable BlockPos clearanceTarget = null;
    private float clearanceBreakProgress = 0f;
    private float clearanceBreakTime = -1f;

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
        frameScanIndex = 0;
        frameRepairIndex = 0;
        clearanceIndex = 0;
        resetClearanceProgress();
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
        switch (phase) {
            case CLEARING -> tickClearing(q);
            case BUILDING_FRAME -> tickBuildingFrame(q);
            case MINING -> tickMining(q);
            case MAINTAINING_CLEARANCE -> tickMaintainingClearance(q);
            case REPAIRING_FRAME -> tickRepairingFrame(q);
            default -> {}
        }
    }

    private void tickClearing(QuarryContext q) {
        if (q.energyStored() == 0) {
            resetBreakProgress();
            return;
        }

        int topY = clearingTopY(q);
        BlockPos target = null;
        BlockState targetState = null;
        for (int skipped = 0; skipped < LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_SCAN_RATE); skipped++) {
            BlockPos sequential = clearingTargetPos(q);
            if (sequential == null) {
                transitionFromClearingToBuildingFrame(q);
                return;
            }

            target = resolveTarget(q, sequential, topY);
            if (target == null) {
                advanceToNextBlock(q);
                resetBreakProgress();
                continue;
            }
            targetState = q.level().getBlockState(target);
            break;
        }

        if (target == null) {
            // Scan budget exhausted this tick without resolving a target — retry next tick; only
            // clearingTargetPos itself returning null (handled above, inside the loop) means
            // clearing has actually reached the end of the area.
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
            QuarryBlockBreaker.mineBlock(q.level(), target, targetState, q.output());
            // Only advance the cursor once the sequential position itself is what got mined —
            // a shallower reappeared block above it (resolveTarget's detour) doesn't count as
            // that grid cell being done, so the cursor must stay put and re-resolve next tick.
            if (target.equals(clearingTargetPos(q))) {
                advanceToNextBlock(q);
            }
            resetBreakProgress();
        }
    }

    private void tickBuildingFrame(QuarryContext q) {
        BlockPos framePos = framePositionAt(q, frameBuildIndex);
        if (framePos == null) {
            // Frame built — transition to mining.
            phase = QuarryPhase.MINING;
            miningX = 0;
            miningY = 0;
            miningZ = 0;
            frameScanIndex = 0;
            q.arm().resetExpectedTravelTicks();
            // armInitialized flips false so first MINING tick re-anchors the arm.
            q.arm().enterMoving();
            q.arm().markUninitialized();
            q.sync();
            q.markChanged();
            return;
        }

        if (!placeFrameBlock(q, framePos)) {
            // Can't afford it yet — hold this index and retry next tick.
            return;
        }

        frameBuildIndex++;
        q.markChanged();
    }

    /**
     * Paused mining while the frame is put back together. Walks the frame sequence for gaps and
     * rebuilds one block per tick, charging only for blocks actually placed. The mining cursor and
     * in-flight break progress are left untouched, so mining resumes exactly where it stopped.
     */
    private void tickRepairingFrame(QuarryContext q) {
        int gap = nextGapIndex(index -> framePositionAt(q, index), pos -> q.level().getBlockState(pos), frameRepairIndex);
        if (gap >= 0) {
            frameRepairIndex = gap;
        }
        BlockPos framePos = gap < 0 ? null : framePositionAt(q, gap);

        if (framePos == null) {
            phase = QuarryPhase.MINING;
            frameRepairIndex = 0;
            frameScanIndex = 0;
            q.markChanged();
            q.sync();
            return;
        }

        if (!placeFrameBlock(q, framePos)) {
            return;
        }

        frameRepairIndex++;
        q.markChanged();
    }


    /**
     * Clears blocks that have found their way into the band the quarry keeps open around its frame
     * — the same volume {@link #tickClearing} empties before the frame goes up. Breaks one intruder
     * at a time using its own break progress, then hands over to {@link QuarryPhase#REPAIRING_FRAME}
     * so any frame slot the intruder was squatting in gets its block back.
     */
    private void tickMaintainingClearance(QuarryContext q) {
        BlockPos target = clearancePositionAt(q, clearanceIndex);
        BlockState state = target == null ? null : q.level().getBlockState(target);

        // Walk past anything that no longer needs clearing (already mined, or never did).
        int scanned = 0;
        while (target != null
                && !isClearanceIntrusion(q.level(), target, state)
                && scanned < CLEARANCE_SCAN_PER_TICK) {
            clearanceIndex++;
            scanned++;
            target = clearancePositionAt(q, clearanceIndex);
            state = target == null ? null : q.level().getBlockState(target);
        }

        if (target == null) {
            // Band is clear — top the frame back up before mining resumes.
            clearanceIndex = 0;
            resetClearanceProgress();
            phase = QuarryPhase.REPAIRING_FRAME;
            frameRepairIndex = 0;
            q.markChanged();
            q.sync();
            return;
        }

        if (!isClearanceIntrusion(q.level(), target, state)) {
            return; // scan budget spent without reaching a decision; resume next tick
        }

        if (!target.equals(clearanceTarget) || clearanceBreakTime < 0) {
            clearanceTarget = target;
            clearanceBreakTime = q.breakCost(state.getDestroySpeed(q.level(), target));
            clearanceBreakProgress = 0f;
        }

        long needed = (long) Math.ceil(clearanceBreakTime - clearanceBreakProgress);
        long toUse = Math.min(q.energyStored(), needed);
        if (toUse > 0) {
            q.consumeEnergy(toUse);
            clearanceBreakProgress += toUse;
        }

        if (clearanceBreakProgress >= clearanceBreakTime) {
            QuarryBlockBreaker.mineBlock(q.level(), target, state, q.output());
            resetClearanceProgress();
            q.markChanged();
        }
    }

    /**
     * Sweeps a slice of the clearance band each mining tick. True as soon as an intruding block
     * turns up, leaving {@link #clearanceIndex} parked on it.
     */
    boolean clearanceHasIntruder(
            IntFunction<@Nullable BlockPos> positionAt, Function<BlockPos, BlockState> stateAt, BlockGetter level) {
        for (int checked = 0; checked < CLEARANCE_SCAN_PER_TICK; checked++) {
            BlockPos pos = positionAt.apply(clearanceIndex);
            if (pos == null) {
                if (clearanceIndex == 0) {
                    return false; // no resolvable band — nothing to police
                }
                clearanceIndex = 0;
                continue;
            }
            if (isClearanceIntrusion(level, pos, stateAt.apply(pos))) {
                return true;
            }
            clearanceIndex++;
        }
        return false;
    }

    /**
     * True when a block has no business being in the clearance band. The quarry's own frame is
     * always welcome, and anything the quarry cannot mine anyway — air, fluids, bedrock — is left
     * alone so an unbreakable intruder can never wedge the phase.
     */
    static boolean isClearanceIntrusion(BlockGetter level, BlockPos pos, @Nullable BlockState state) {
        if (state == null || state.getBlock() instanceof LaserQuarryFrameBlock) {
            return false;
        }
        return !GridScanner.shouldSkip(level, pos, state);
    }

    /** Maps a flat index onto the frame band: whole rectangle, {@code bottomY} up to {@code topY}. */
    static @Nullable BlockPos clearancePositionAt(QuarryFrameRect rect, int index) {
        if (rect == null || index < 0) {
            return null;
        }
        int width = rect.width();
        int depth = rect.depth();
        int height = rect.topY() - rect.bottomY() + 1;
        if (width <= 0 || depth <= 0 || height <= 0 || index >= width * depth * height) {
            return null;
        }
        int layer = index / (width * depth);
        int withinLayer = index % (width * depth);
        return new BlockPos(
                rect.startX() + withinLayer / depth, rect.bottomY() + layer, rect.startZ() + withinLayer % depth);
    }

    private static @Nullable BlockPos clearancePositionAt(QuarryContext q, int index) {
        return clearancePositionAt(frameRect(q), index);
    }

    private static @Nullable QuarryFrameRect frameRect(QuarryContext q) {
        return QuarryFrameRect.resolve(
                LaserQuarryBlock.getMiningDirection(q.quarryState()),
                q.pos(),
                q.bounds(),
                LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA));
    }

    private void resetClearanceProgress() {
        clearanceTarget = null;
        clearanceBreakProgress = 0f;
        clearanceBreakTime = -1f;
    }

    /**
     * Sweeps a slice of the frame each mining tick. True as soon as a gap turns up, which sends the
     * quarry into {@link QuarryPhase#REPAIRING_FRAME}. The cursor rolls across ticks so the whole
     * frame is covered without ever scanning it all at once.
     */
    boolean frameHasGap(IntFunction<@Nullable BlockPos> positionAt, Function<BlockPos, BlockState> stateAt) {
        for (int checked = 0; checked < FRAME_SCAN_PER_TICK; checked++) {
            BlockPos framePos = positionAt.apply(frameScanIndex);
            if (framePos == null) {
                if (frameScanIndex == 0) {
                    return false; // no resolvable frame at all — nothing to police
                }
                frameScanIndex = 0;
                continue;
            }
            frameScanIndex++;
            if (isGap(stateAt.apply(framePos))) {
                return true;
            }
        }
        return false;
    }

    /** Index of the first gap at or after {@code from}, or -1 when the frame is whole. */
    static int nextGapIndex(
            IntFunction<@Nullable BlockPos> positionAt, Function<BlockPos, BlockState> stateAt, int from) {
        for (int index = from; ; index++) {
            BlockPos framePos = positionAt.apply(index);
            if (framePos == null) {
                return -1;
            }
            if (isGap(stateAt.apply(framePos))) {
                return index;
            }
        }
    }

    private void enterClearanceMaintenance(QuarryContext q) {
        phase = QuarryPhase.MAINTAINING_CLEARANCE;
        resetClearanceProgress();
        if (currentTarget != null) {
            q.level().destroyBlockProgress(q.breakingEntityId(), currentTarget, -1);
        }
        q.markChanged();
        q.sync();
    }

    private void enterFrameRepair(QuarryContext q) {
        phase = QuarryPhase.REPAIRING_FRAME;
        frameRepairIndex = 0;
        // Drop the crack overlay; break progress itself is kept so no mining work is wasted.
        if (currentTarget != null) {
            q.level().destroyBlockProgress(q.breakingEntityId(), currentTarget, -1);
        }
        q.markChanged();
        q.sync();
    }

    /**
     * True when the position is a hole the quarry should fill. A player-placed solid block is not a
     * hole — it can't be replaced, so treating it as one would wedge the repair phase forever.
     */
    static boolean isGap(BlockState state) {
        if (state.getBlock() instanceof LaserQuarryFrameBlock) {
            return false;
        }
        return state.isAir() || state.canBeReplaced();
    }

    /**
     * Places the frame block at {@code framePos}, charging the build cost only when a block is
     * actually placed. False means the buffer couldn't afford the placement and the caller should
     * retry the same index next tick.
     */
    private boolean placeFrameBlock(QuarryContext q, BlockPos framePos) {
        if (!isGap(q.level().getBlockState(framePos))) {
            return true;
        }
        if (!q.hasEnergy(q.frameBuildCost())) {
            return false;
        }
        q.consumeEnergy(q.frameBuildCost());
        q.level()
                .setBlockAndUpdate(
                        framePos,
                        FrameLayout.frameBlockState(
                                LaserQuarryBlock.getMiningDirection(q.quarryState()),
                                q.pos(),
                                framePos,
                                q.bounds(),
                                LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA)));
        return true;
    }

    private static @Nullable BlockPos framePositionAt(QuarryContext q, int index) {
        return FrameLayout.nextFramePosition(
                LaserQuarryBlock.getMiningDirection(q.quarryState()),
                q.pos(),
                q.bounds(),
                LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA),
                index);
    }

    private void tickMining(QuarryContext q) {
        if (!finished
                && clearanceHasIntruder(
                        index -> clearancePositionAt(q, index), pos -> q.level().getBlockState(pos), q.level())) {
            enterClearanceMaintenance(q);
            return;
        }

        if (!finished && frameHasGap(index -> framePositionAt(q, index), pos -> q.level().getBlockState(pos))) {
            enterFrameRepair(q);
            return;
        }

        ArmController arm = q.arm();
        int topY = q.pos().getY() - 1;

        BlockPos target = null;
        boolean skippedAny = false;
        for (int skipped = 0; skipped < LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_SCAN_RATE); skipped++) {
            BlockPos sequential = miningTargetPos(q);
            if (sequential == null) {
                if (currentTarget != null) {
                    q.level().destroyBlockProgress(q.breakingEntityId(), currentTarget, -1);
                }
                finished = true;
                q.markChanged();
                q.sync();
                return;
            }

            target = resolveTarget(q, sequential, topY);
            if (target == null) {
                advanceMiningPosition(q);
                skippedAny = true;
                continue;
            }
            break;
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
                currentBreakTime = (float) (QuarryEnergy.energyPerBlockMultiplier() * (hardness + 1));
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

                QuarryBlockBreaker.mineBlock(q.level(), target, targetState, q.output());
                // Only advance the cursor once the sequential position itself is what got mined —
                // a shallower reappeared block above it (resolveTarget's detour) doesn't count as
                // that grid cell being done, so the cursor must stay put and get re-resolved by
                // findNextTarget below.
                if (target.equals(miningTargetPos(q))) {
                    advanceMiningPosition(q);
                }
                resetBreakProgress();

                BlockPos nextTarget = findNextTarget(q, topY);
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
                LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA),
                miningX,
                miningY,
                miningZ);
    }

    private @Nullable BlockPos miningTargetPos(QuarryContext q) {
        return GridScanner.miningTarget(
                LaserQuarryBlock.getMiningDirection(q.quarryState()),
                q.pos(),
                q.bounds(),
                LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA),
                q.levelMinY(),
                miningX,
                miningY,
                miningZ);
    }

    /** The clearing phase's own top boundary — {@code QuarryFrameRect.topY()}, resolved fresh (facing/bounds can change). */
    private int clearingTopY(QuarryContext q) {
        QuarryFrameRect rect = QuarryFrameRect.resolve(
                LaserQuarryBlock.getMiningDirection(q.quarryState()),
                q.pos(),
                q.bounds(),
                LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA));
        return rect != null ? rect.topY() : q.pos().getY();
    }

    /**
     * Resolves the real block to work on for {@code sequential}'s column: scans live, every call
     * (never cached), from the region's top down to {@code sequential} itself, and returns the
     * shallowest position that still needs handling. A hazardous fluid stops the scan outright —
     * nothing below it is safe to reach, so this returns {@code null} without ever considering
     * {@code sequential}. Otherwise the shallowest still-minable block is returned, which is
     * {@code sequential} itself once everything shallower has already been cleared; {@code null}
     * means the whole span is already resolved.
     *
     * <p>This is also how reappeared blocks get handled — settled sand/gravel, one placed back in
     * by a player — with no separate cache or one-time recheck: any column gets its full processed
     * height re-examined every time mining reaches it, so a block that reappears anywhere above,
     * at any point before the cursor gets there next, is always picked up. A cache tied to a
     * single moment (a specific layer transition, a scan-order index, a session's memory) can only
     * ever cover part of that — it was the same failure mode that let lava get mined under, and
     * players placing blocks the quarry then ignored, before this stopped being cached at all.
     */
    private @Nullable BlockPos resolveTarget(QuarryContext q, BlockPos sequential, int topY) {
        for (int y = topY; y >= sequential.getY(); y--) {
            BlockPos candidate = (y == sequential.getY())
                    ? sequential
                    : new BlockPos(sequential.getX(), y, sequential.getZ());
            BlockState state = q.level().getBlockState(candidate);
            if (GridScanner.isHazardousFluid(state)) {
                return null;
            }
            if (!GridScanner.shouldSkip(q.level(), candidate, state)) {
                return candidate;
            }
        }
        return null;
    }

    private void advanceToNextBlock(QuarryContext q) {
        miningX++;
        int maxX = q.bounds().isCustom()
                ? (q.bounds().getMaxX() - q.bounds().getMinX() + 1)
                : LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA);
        int maxZ = q.bounds().isCustom()
                ? (q.bounds().getMaxZ() - q.bounds().getMinZ() + 1)
                : LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA);

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
            innerSizeX = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) - 2;
            innerSizeZ = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA) - 2;
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

    /** Finds the next block to work on after one just finished, advancing the cursor past whatever's already resolved. */
    private @Nullable BlockPos findNextTarget(QuarryContext q, int topY) {
        for (int skipped = 0; skipped < LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_SCAN_RATE); skipped++) {
            BlockPos sequential = miningTargetPos(q);
            if (sequential == null) {
                finished = true;
                return null;
            }

            BlockPos resolved = resolveTarget(q, sequential, topY);
            if (resolved != null) {
                return resolved;
            }

            advanceMiningPosition(q);
        }
        return null;
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
        tag.putInt("FrameRepairIndex", frameRepairIndex);
        tag.putInt("FrameScanIndex", frameScanIndex);
        tag.putInt("ClearanceIndex", clearanceIndex);
    }

    public void load(CompoundTag tag) {
        miningX = NbtCompat.getInt(tag, "MiningX", 0);
        miningY = NbtCompat.getInt(tag, "MiningY", 0);
        miningZ = NbtCompat.getInt(tag, "MiningZ", 0);
        breakProgress = NbtCompat.getFloat(tag, "BreakProgress", 0f);
        finished = NbtCompat.getBoolean(tag, "MiningFinished", false);
        frameBuildIndex = NbtCompat.getInt(tag, "FrameBuildIndex", 0);
        frameRepairIndex = Math.max(0, NbtCompat.getInt(tag, "FrameRepairIndex", 0));
        frameScanIndex = Math.max(0, NbtCompat.getInt(tag, "FrameScanIndex", 0));
        clearanceIndex = Math.max(0, NbtCompat.getInt(tag, "ClearanceIndex", 0));

        String phaseName = NbtCompat.getString(tag, "CurrentPhase", "CLEARING");
        try {
            phase = QuarryPhase.valueOf(phaseName);
        } catch (IllegalArgumentException e) {
            phase = QuarryPhase.CLEARING;
        }
    }
}
