package com.logistics.pipe.runtime;

/**
 * Encapsulates physics calculations for traveling items in pipes.
 * Handles acceleration, drag, and physics-based deceleration to target speeds.
 */
public class TravelingItemPhysics {
    private final float minSpeed;

    /**
     * Create a physics calculator with the specified minimum speed.
     *
     * @param minSpeed Minimum allowed speed (items will not slow below this value)
     */
    public TravelingItemPhysics(float minSpeed) {
        this.minSpeed = minSpeed;
    }

    /**
     * Update speed for a traveling item based on current state and pipe properties.
     * Simulates physics (deceleration/acceleration/drag) and clamps result to valid range.
     *
     * @param currentSpeed Current speed in blocks per tick
     * @param currentProgress Current progress along pipe segment (0.0 to 1.0)
     * @param accelerationRate Rate of acceleration/deceleration (blocks/tick²)
     * @param dragCoefficient Fraction of speed lost per tick (0.0 to 1.0)
     * @param maxSpeed Maximum allowed speed for this segment
     * @return Next speed value, clamped to valid range
     */
    public float updateSpeed(
            float currentSpeed,
            float currentProgress,
            float accelerationRate,
            float dragCoefficient,
            float maxSpeed) {
        float speed = simulate(currentSpeed, currentProgress, accelerationRate, dragCoefficient, maxSpeed);
        return clampSpeed(speed, maxSpeed, currentSpeed > maxSpeed);
    }

    /**
     * Calculate next speed based on current physics mode (priority: deceleration > acceleration > drag).
     *
     * @param currentSpeed Current speed in blocks per tick
     * @param currentProgress Current progress along pipe segment (0.0 to 1.0)
     * @param accelerationRate Rate of acceleration (blocks/tick²)
     * @param dragCoefficient Fraction of speed lost per tick (0.0 to 1.0)
     * @param maxSpeed Maximum allowed speed for this segment
     * @return Unclamped next speed value
     */
    private float simulate(
            float currentSpeed,
            float currentProgress,
            float accelerationRate,
            float dragCoefficient,
            float maxSpeed) {
        boolean isOverspeed = currentSpeed > maxSpeed;
        boolean hasAcceleration = accelerationRate != 0f;
        boolean hasDrag = dragCoefficient != 0f;

        if (isOverspeed) return decelerateToTarget(currentSpeed, currentProgress, maxSpeed);
        if (hasAcceleration) return currentSpeed + accelerationRate;
        if (hasDrag) return applyDrag(currentSpeed, dragCoefficient);
        return currentSpeed;
    }

    /**
     * Calculate deceleration needed to reach target speed exactly at segment exit.
     * Uses kinematic equation: v² = u² + 2as, solved for a (deceleration).
     *
     * @param currentSpeed Current speed
     * @param currentProgress Current progress (0.0 to 1.0)
     * @param targetSpeed Target speed to reach at exit
     * @return New speed after applying calculated deceleration
     */
    private float decelerateToTarget(float currentSpeed, float currentProgress, float targetSpeed) {
        float remaining = Math.max(1.0e-4f, 1.0f - currentProgress);
        float targetSquared = targetSpeed * targetSpeed;
        float currentSquared = currentSpeed * currentSpeed;
        float deceleration = (targetSquared - currentSquared) / (2.0f * remaining);

        float newSpeed = currentSpeed + deceleration;
        return Math.max(newSpeed, targetSpeed);
    }

    /**
     * Apply drag to current speed as a fraction lost per tick.
     *
     * @param currentSpeed Current speed
     * @param dragCoefficient Fraction of speed to remove (0.0 to 1.0)
     * @return Speed after drag
     */
    private float applyDrag(float currentSpeed, float dragCoefficient) {
        return currentSpeed - (currentSpeed * dragCoefficient);
    }

    /**
     * Clamp speed to valid range [minSpeed, maxSpeed].
     * When decelerating to max, allows temporary overshoot above maxSpeed.
     *
     * @param speed Speed to clamp
     * @param maxSpeed Maximum allowed speed
     * @param isDeceleratingToMax Whether currently decelerating from overspeed
     * @return Clamped speed value
     */
    private float clampSpeed(float speed, float maxSpeed, boolean isDeceleratingToMax) {
        boolean isBelowMinimum = speed < minSpeed;
        boolean isAboveMaximumWhileNotDecelerating = !isDeceleratingToMax && speed > maxSpeed;

        if (isBelowMinimum) return minSpeed;
        if (isAboveMaximumWhileNotDecelerating) return maxSpeed;
        return speed;
    }
}
