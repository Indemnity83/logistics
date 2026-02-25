package com.logistics.power.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("PIDController")
class PIDControllerTest {

    private static final double TOLERANCE = 0.0001;

    // ==================== Proportional Control Tests ====================

    @Test
    @DisplayName("should respond proportionally to error (P-only)")
    void proportionalControl() {
        PIDController pid = new PIDController(2.0, 0.0, 0.0);

        // Error = 10 - 5 = 5, output = 2.0 * 5 = 10
        double output = pid.compute(10.0, 5.0, 0.0, 100.0);

        assertThat(output).isCloseTo(10.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("should clamp output to max range (P-only)")
    void proportionalClampingHigh() {
        PIDController pid = new PIDController(2.0, 0.0, 0.0);

        // Error = 100, output = 200 but clamped to 50
        double output = pid.compute(100.0, 0.0, 0.0, 50.0);

        assertThat(output).isCloseTo(50.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("should clamp output to min range (P-only)")
    void proportionalClampingLow() {
        PIDController pid = new PIDController(2.0, 0.0, 0.0);

        // Error = -100, output = -200 but clamped to -50
        double output = pid.compute(-100.0, 0.0, -50.0, 50.0);

        assertThat(output).isCloseTo(-50.0, within(TOLERANCE));
    }

    // ==================== Integral Control Tests ====================

    @Test
    @DisplayName("should accumulate integral over time (I-only)")
    void integralAccumulation() {
        PIDController pid = new PIDController(0.0, 0.5, 0.0);

        // Error = 10, integral accumulates: 0.5 * 10 = 5 per tick
        pid.compute(10.0, 0.0, 0.0, 100.0);
        double output1 = pid.compute(10.0, 0.0, 0.0, 100.0);
        double output2 = pid.compute(10.0, 0.0, 0.0, 100.0);

        // After 1st: integral = 5, output = 5
        // After 2nd: integral = 10, output = 10
        // After 3rd: integral = 15, output = 15
        assertThat(output1).isCloseTo(10.0, within(TOLERANCE));
        assertThat(output2).isCloseTo(15.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("should prevent integral windup when saturated high")
    void preventHighSaturationWindup() {
        PIDController pid = new PIDController(0.0, 10.0, 0.0);

        // Force saturation - error positive, output at max
        // Each call tries to add 10*100=1000 to integral, but anti-windup prevents it
        pid.compute(100.0, 0.0, 0.0, 50.0);  // Output clamped to 50, integral = 1000 (first call)
        pid.compute(100.0, 0.0, 0.0, 50.0);  // Saturated, integral rejected, stays at 1000
        pid.compute(100.0, 0.0, 0.0, 50.0);  // Saturated, integral rejected, stays at 1000

        // Get the integral value - should be capped, not growing infinitely
        double integral = pid.getIntegral();

        // If anti-windup is working, integral should be close to the saturation point (50.0)
        // not accumulated to 3000 (3 ticks * 1000)
        assertThat(integral).isLessThan(150.0);
    }

    @Test
    @DisplayName("should prevent integral windup when saturated low")
    void preventLowSaturationWindup() {
        PIDController pid = new PIDController(0.0, 10.0, 0.0);

        // Force saturation - error negative, output at min
        // Each call tries to add 10*(-100)=-1000 to integral, but anti-windup prevents it
        pid.compute(-100.0, 0.0, -50.0, 50.0);  // Output clamped to -50, integral = -1000
        pid.compute(-100.0, 0.0, -50.0, 50.0);  // Saturated, integral rejected, stays at -1000
        pid.compute(-100.0, 0.0, -50.0, 50.0);  // Saturated, integral rejected, stays at -1000

        // Get the integral value - should be capped, not growing infinitely negative
        double integral = pid.getIntegral();

        // If anti-windup is working, integral should be close to the saturation point (-50.0)
        // not accumulated to -3000 (3 ticks * -1000)
        assertThat(integral).isGreaterThan(-150.0);
    }

    @Test
    @DisplayName("should allow integral to grow when not saturated")
    void integralGrowthWhenNotSaturated() {
        PIDController pid = new PIDController(0.0, 1.0, 0.0);

        // Normal operation - not saturated
        double output1 = pid.compute(10.0, 0.0, 0.0, 100.0);
        double output2 = pid.compute(10.0, 0.0, 0.0, 100.0);
        double output3 = pid.compute(10.0, 0.0, 0.0, 100.0);

        // Integral should grow: 10, 20, 30
        assertThat(output1).isCloseTo(10.0, within(TOLERANCE));
        assertThat(output2).isCloseTo(20.0, within(TOLERANCE));
        assertThat(output3).isCloseTo(30.0, within(TOLERANCE));
    }

    // ==================== Derivative Control Tests ====================

    @Test
    @DisplayName("should respond to error rate of change (D-only)")
    void derivativeResponse() {
        PIDController pid = new PIDController(0.0, 0.0, 1.0);

        // First call establishes lastError, no derivative yet
        pid.compute(10.0, 0.0, 0.0, 100.0);

        // Error changes from 10 to 5 (error decreased by 5)
        // Derivative = kd * (currentError - lastError) = 1.0 * (5 - 10) = -5
        double output = pid.compute(10.0, 5.0, -100.0, 100.0);

        assertThat(output).isCloseTo(-5.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("should not compute derivative on first run")
    void noDerivativeOnFirstRun() {
        PIDController pid = new PIDController(0.0, 0.0, 1.0);

        // First call - no previous error, derivative should be 0
        double output = pid.compute(10.0, 0.0, 0.0, 100.0);

        assertThat(output).isCloseTo(0.0, within(TOLERANCE));
    }

    // ==================== Deadband Tests ====================

    @Test
    @DisplayName("should treat small errors as zero within deadband")
    void deadbandSuppression() {
        PIDController pid = new PIDController(1.0, 0.0, 0.0, 2.0);

        // Error = 100 - 99 = 1.0, within deadband of 2.0
        double output = pid.compute(100.0, 99.0, 0.0, 200.0);

        assertThat(output).isEqualTo(0.0);
    }

    @Test
    @DisplayName("should respond to errors outside deadband")
    void errorOutsideDeadband() {
        PIDController pid = new PIDController(1.0, 0.0, 0.0, 2.0);

        // Error = 100 - 95 = 5.0, outside deadband of 2.0
        double output = pid.compute(100.0, 95.0, 0.0, 200.0);

        assertThat(output).isCloseTo(5.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("should handle negative deadband values gracefully")
    void negativeDeadband() {
        PIDController pid = new PIDController(1.0, 0.0, 0.0, -5.0);

        // Negative deadband should be treated as 0 (Math.max(0.0, deadband))
        double output = pid.compute(10.0, 9.0, 0.0, 100.0);

        // Error = 1.0, should produce output since deadband is 0
        assertThat(output).isCloseTo(1.0, within(TOLERANCE));
    }

    // ==================== State Management Tests ====================

    @Test
    @DisplayName("should reset integral and derivative state")
    void stateReset() {
        PIDController pid = new PIDController(1.0, 1.0, 1.0);

        // Accumulate some state
        pid.compute(10.0, 0.0, 0.0, 100.0);
        pid.compute(10.0, 0.0, 0.0, 100.0);

        // Reset
        pid.reset();

        // After reset, integral should be 0 and derivative should be 0 (first run)
        double output = pid.compute(10.0, 0.0, 0.0, 100.0);

        // After reset: P=10, I=10 (reset to 0, then add 1*10), D=0 (first run) = 20
        assertThat(output).isCloseTo(20.0, within(TOLERANCE));

        // Verify integral was reset
        assertThat(pid.getIntegral()).isCloseTo(10.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("should persist and restore integral value")
    void integralPersistence() {
        PIDController pid = new PIDController(0.0, 1.0, 0.0);

        // Accumulate integral
        pid.compute(10.0, 0.0, 0.0, 100.0);
        pid.compute(10.0, 0.0, 0.0, 100.0);

        // Save integral
        double savedIntegral = pid.getIntegral();
        assertThat(savedIntegral).isCloseTo(20.0, within(TOLERANCE));

        // Create new PID and restore
        PIDController restoredPid = new PIDController(0.0, 1.0, 0.0);
        restoredPid.setIntegral(savedIntegral);

        // Next computation should use restored integral
        double output = restoredPid.compute(10.0, 0.0, 0.0, 100.0);
        assertThat(output).isCloseTo(30.0, within(TOLERANCE)); // 20 + (1.0 * 10)
    }

    // ==================== Combined PID Tests ====================

    @Test
    @DisplayName("should combine P, I, and D terms correctly")
    void combinedPID() {
        PIDController pid = new PIDController(1.0, 0.5, 0.2);

        // First call: P=10, I=5, D=0 (first run) -> output = 15
        double output1 = pid.compute(10.0, 0.0, 0.0, 100.0);
        assertThat(output1).isCloseTo(15.0, within(TOLERANCE));

        // Second call: error still 10
        // P=10, I=10 (accumulated 5+5), D=0 (error unchanged) -> output = 20
        double output2 = pid.compute(10.0, 0.0, 0.0, 100.0);
        assertThat(output2).isCloseTo(20.0, within(TOLERANCE));

        // Third call: error changes to 5
        // P=5, I=12.5 (10+2.5), D=0.2*(5-10)=-1 -> output = 16.5
        double output3 = pid.compute(10.0, 5.0, 0.0, 100.0);
        assertThat(output3).isCloseTo(16.5, within(TOLERANCE));
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("should throw exception when minOutput > maxOutput")
    void invalidRange() {
        PIDController pid = new PIDController(1.0, 0.0, 0.0);

        assertThatThrownBy(() -> pid.compute(10.0, 0.0, 100.0, 50.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minOutput must be <= maxOutput");
    }

    @Test
    @DisplayName("should handle zero range (min == max)")
    void zeroRange() {
        PIDController pid = new PIDController(1.0, 0.0, 0.0);

        double output = pid.compute(10.0, 0.0, 50.0, 50.0);

        assertThat(output).isCloseTo(50.0, within(TOLERANCE));
    }

    // ==================== Getter Tests ====================

    @Test
    @DisplayName("should expose gain values via getters")
    void gainGetters() {
        PIDController pid = new PIDController(1.5, 2.5, 3.5, 0.5);

        assertThat(pid.getKp()).isCloseTo(1.5, within(TOLERANCE));
        assertThat(pid.getKi()).isCloseTo(2.5, within(TOLERANCE));
        assertThat(pid.getKd()).isCloseTo(3.5, within(TOLERANCE));
    }

    @Test
    @DisplayName("should track last error value")
    void lastErrorTracking() {
        PIDController pid = new PIDController(1.0, 0.0, 0.0);

        pid.compute(10.0, 5.0, 0.0, 100.0);

        assertThat(pid.getLastError()).isCloseTo(5.0, within(TOLERANCE));
    }
}
