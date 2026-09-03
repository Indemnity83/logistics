package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsAutomation;

import com.logistics.core.lib.compat.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Tracks the laser quarry arm's world-space position, movement state, and timing
 * for client interpolation catch-up. All math is pure; the controller does not
 * directly trigger sync — callers (typically {@link QuarryPhaseRunner}) decide
 * when to push state to clients.
 */
public final class ArmController {
    /**
     * Upper bound on any travel or settling estimate, in ticks. Caps what a degenerate
     * arm speed can stamp onto the timer, and clamps values already saved to NBT.
     */
    public static final int MAX_TRAVEL_TICKS = 6000;

    private QuarryArmState state = QuarryArmState.MOVING;
    private float x = 0f;
    private float y = 0f;
    private float z = 0f;
    private boolean initialized = false;
    private int settlingTicksRemaining = 0;
    private int expectedTravelTicks = 0;
    private float syncedSpeed = 0.0f;

    // ==================== Public getters ====================

    public QuarryArmState getState() {
        return state;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public float getSyncedSpeed() {
        return syncedSpeed;
    }

    public void setSyncedSpeed(float speed) {
        this.syncedSpeed = speed;
    }

    // ==================== State transitions ====================

    public void initializeAt(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.initialized = true;
        this.state = QuarryArmState.MOVING;
        this.expectedTravelTicks = 0;
    }

    public void enterMoving() {
        this.state = QuarryArmState.MOVING;
    }

    /** Drop the initialized flag so the next mining tick re-anchors at the first target. */
    public void markUninitialized() {
        this.initialized = false;
    }

    public void enterSettling(int ticks) {
        this.state = QuarryArmState.SETTLING;
        this.settlingTicksRemaining = Math.max(1, Math.min(ticks, MAX_TRAVEL_TICKS));
    }

    public void enterBreaking() {
        this.state = QuarryArmState.BREAKING;
    }

    public int getSettlingTicksRemaining() {
        return settlingTicksRemaining;
    }

    /** Decrement the settling timer; returns {@code true} when it reaches zero. */
    public boolean tickSettling() {
        settlingTicksRemaining--;
        return settlingTicksRemaining <= 0;
    }

    public int getExpectedTravelTicks() {
        return expectedTravelTicks;
    }

    public void setExpectedTravelTicks(int ticks) {
        this.expectedTravelTicks = ticks;
    }

    public void resetExpectedTravelTicks() {
        this.expectedTravelTicks = 0;
    }

    /**
     * Jumps the arm to the supplied position without interpolation and computes
     * the matching settling-tick count for the given speed.
     */
    public void warpTo(float tx, float ty, float tz, float speed) {
        float dx = tx - this.x;
        float dy = ty - this.y;
        float dz = tz - this.z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        this.x = tx;
        this.y = ty;
        this.z = tz;
        this.expectedTravelTicks = ticksForDistance(distance, speed);
    }

    // ==================== Kinematics ====================

    /** Move the arm one tick toward the target at the given speed; returns {@code true} on snap-to-target. */
    public boolean moveTowards(float tx, float ty, float tz, float speed) {
        float dx = tx - x;
        float dy = ty - y;
        float dz = tz - z;

        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= speed) {
            x = tx;
            y = ty;
            z = tz;
            return true;
        }

        float factor = speed / distance;
        x += dx * factor;
        y += dy * factor;
        z += dz * factor;

        return false;
    }

    public boolean isAt(float tx, float ty, float tz, float speed) {
        float dx = tx - x;
        float dy = ty - y;
        float dz = tz - z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance <= speed;
    }

    public int travelTicksFor(float tx, float ty, float tz, float speed) {
        float dx = tx - x;
        float dy = ty - y;
        float dz = tz - z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return ticksForDistance(distance, speed);
    }

    /**
     * Ticks needed to cover {@code distance} at {@code speed}, capped at {@link #MAX_TRAVEL_TICKS}.
     * A non-positive or non-finite speed means "no travel estimate" (0) rather than a division
     * that overflows to {@link Integer#MAX_VALUE}.
     */
    private static int ticksForDistance(float distance, float speed) {
        if (!Float.isFinite(distance) || !Float.isFinite(speed) || distance <= 0f || speed <= 0f) {
            return 0;
        }
        double ticks = Math.ceil((double) distance / (double) speed);
        return (int) Math.min(ticks, MAX_TRAVEL_TICKS);
    }

    /** Keep a tick count inside the sane range, including values read back from older saves. */
    private static int clampTicks(int ticks) {
        return Math.max(0, Math.min(ticks, MAX_TRAVEL_TICKS));
    }

    // ==================== Cost + speed (depend on energy buffer / world) ====================

    /** Move cost per tick — scales with current buffer to drain excess energy faster. */
    public static long moveCost(long bufferAmount, long moveCostDivisor) {
        return (long) Math
                .ceil(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_ARM_ENERGY) + (double) bufferAmount / moveCostDivisor);
    }

    /** Effective arm speed in blocks/tick, including the rain penalty when applicable. */
    public static float effectiveSpeed(long bufferAmount, long moveCostDivisor, Level level, BlockPos quarryPos) {
        long mc = moveCost(bufferAmount, moveCostDivisor);
        float speed = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_ARM_SPEED) + (mc / LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_ARM_SPEED_SCALING));
        if (level != null && level.isRainingAt(quarryPos.above())) {
            speed *= LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_RAIN_PENALTY);
        }
        return speed;
    }

    // ==================== NBT ====================

    public void save(CompoundTag tag) {
        tag.putString("ArmState", state.name());
        tag.putFloat("ArmX", x);
        tag.putFloat("ArmY", y);
        tag.putFloat("ArmZ", z);
        tag.putBoolean("ArmInitialized", initialized);
        tag.putInt("ArmSettlingTicks", settlingTicksRemaining);
        tag.putInt("ArmExpectedTravelTicks", expectedTravelTicks);
        tag.putFloat("ArmSyncedSpeed", syncedSpeed);
    }

    public void load(CompoundTag tag) {
        String armStateName = NbtCompat.getString(tag, "ArmState", "MOVING");
        try {
            state = QuarryArmState.valueOf(armStateName);
        } catch (IllegalArgumentException e) {
            state = QuarryArmState.MOVING;
        }
        x = NbtCompat.getFloat(tag, "ArmX", 0f);
        y = NbtCompat.getFloat(tag, "ArmY", 0f);
        z = NbtCompat.getFloat(tag, "ArmZ", 0f);
        initialized = NbtCompat.getBoolean(tag, "ArmInitialized", false);
        settlingTicksRemaining = clampTicks(NbtCompat.getInt(tag, "ArmSettlingTicks", 0));
        expectedTravelTicks = clampTicks(NbtCompat.getInt(tag, "ArmExpectedTravelTicks", 0));
        syncedSpeed = NbtCompat.getFloat(tag, "ArmSyncedSpeed", 0.0f);
    }
}
