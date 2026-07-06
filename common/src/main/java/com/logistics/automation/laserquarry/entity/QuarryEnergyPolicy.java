package com.logistics.automation.laserquarry.entity;
import com.logistics.LogisticsAutomation;

import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.upgrade.MachineModifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Quarry-specific energy behavior: the action costs (move / break / frame-build) and effective arm
 * speed, each scaled by the machine's {@link MachineModifiers} by operation key, plus thin
 * query/spend access to the energy buffer.
 *
 * <p>Kept separate from energy <em>storage</em>: the storage component holds RF and exposes the
 * capability; this policy decides what quarry actions cost and how fast the arm moves, so upgrades
 * can affect cost/speed without the storage knowing anything quarry-specific.
 */
public final class QuarryEnergyPolicy {

    private static final long FRAME_BUILD_COST = 240L;
    private static final long MOVE_COST_BUFFER_DIVISOR = 10L;

    private final EnergyStorageComponent energy;
    private final MachineModifiers modifiers;
    private final Runnable onConsume;

    private boolean consumedThisTick = false;

    public QuarryEnergyPolicy(EnergyStorageComponent energy, MachineModifiers modifiers, Runnable onConsume) {
        this.energy = energy;
        this.modifiers = modifiers;
        this.onConsume = onConsume;
    }

    // ==================== Storage query / spend ====================

    public long stored() {
        return energy.amount();
    }

    public boolean has(long rf) {
        return energy.amount() >= rf;
    }

    /** Spends RF if the buffer can afford it, flagging consumption this tick and marking dirty. */
    public void consume(long rf) {
        if (energy.amount() >= rf) {
            energy.consume(rf);
            consumedThisTick = true;
            onConsume.run();
        }
    }

    public boolean consumedThisTick() {
        return consumedThisTick;
    }

    public void resetConsumedThisTick() {
        consumedThisTick = false;
    }

    /** Bleed idle RF from the buffer (not "work" — does not set the consumed flag). */
    public void drainIdle(long rf) {
        long amount = energy.amount();
        if (amount > 0) {
            energy.setAmount(Math.max(0, amount - rf));
            onConsume.run();
        }
    }

    // ==================== Action costs (modifier-scaled by operation) ====================

    public long frameBuildCost() {
        return (long) (FRAME_BUILD_COST * modifiers.cost(QuarryOperation.BUILD_FRAME));
    }

    /** Move cost per tick — scales with current buffer to drain excess energy faster. */
    public long moveCost() {
        long base = ArmController.moveCost(energy.amount(), MOVE_COST_BUFFER_DIVISOR);
        return (long) (base * modifiers.cost(QuarryOperation.MOVE_ARM));
    }

    /** Total energy to break a block of the given hardness. */
    public float breakCost(float hardness) {
        return (float) (LogisticsAutomation.energyPerBlockMultiplier()
                * (hardness + 1)
                * modifiers.cost(QuarryOperation.BREAK_BLOCK));
    }

    /** Effective arm speed in blocks/tick, including the rain penalty when applicable. */
    public float effectiveArmSpeed(Level level, BlockPos pos) {
        float base = ArmController.effectiveSpeed(energy.amount(), MOVE_COST_BUFFER_DIVISOR, level, pos);
        return base * (float) modifiers.speed(QuarryOperation.MOVE_ARM);
    }
}
