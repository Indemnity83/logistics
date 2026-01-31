package com.logistics.power.engine;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * PID (Proportional-Integral-Derivative) controller for engine power output regulation.
 * Used by the Stirling Engine to match BuildCraft's PID-controlled output behavior.
 *
 * <p>The controller adjusts power output to maintain temperature at a target level:
 * <ul>
 *   <li>P (Proportional): Responds to current error magnitude</li>
 *   <li>I (Integral): Eliminates steady-state error over time</li>
 *   <li>D (Derivative): Dampens oscillations by responding to rate of change</li>
 * </ul>
 *
 * <p>Note: gain values are highly dependent on units and system dynamics.
 */
public class PIDController {
    // ==================== Tuning Logging ====================
    // Set to true to enable CSV logging for PID tuning analysis
    private static final boolean LOGGING_ENABLED = false;
    private static final String LOG_FILE = "pid_tuning.csv";
    private static PrintWriter logWriter = null;
    private static long logTick = 0;
    private static boolean logHeaderWritten = false;

    private final double kp; // Proportional gain
    private final double ki; // Integral gain (per-tick accumulation)
    private final double kd; // Derivative gain (response to error change rate)

    /**
     * Error deadband in measured units. If |error| <= deadband, error is treated as 0
     * to reduce output jitter when the load is quantized.
     */
    private final double deadband;

    private double integral = 0.0;
    private double lastError = 0.0;
    private boolean firstRun = true;

    // Last computed values for logging
    private double lastSetpoint;
    private double lastMeasured;
    private double lastProportional;
    private double lastDerivative;
    private double lastOutput;

    /**
     * Creates a new PI controller with the specified gains (no derivative).
     *
     * @param kp proportional gain (response to current error)
     * @param ki integral gain (response to accumulated error)
     */
    public PIDController(double kp, double ki) {
        this(kp, ki, 0.0, 0.0);
    }

    /**
     * Creates a new PID controller with the specified gains.
     *
     * @param kp proportional gain (response to current error)
     * @param ki integral gain (response to accumulated error)
     * @param kd derivative gain (response to error rate of change)
     */
    public PIDController(double kp, double ki, double kd) {
        this(kp, ki, kd, 0.0);
    }

    public PIDController(double kp, double ki, double kd, double deadband) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.deadband = Math.max(0.0, deadband);
    }

    /**
     * Computes the control output based on the error between setpoint and measured value.
     *
     * @param setpoint the target value
     * @param measured the current measured value
     * @param minOutput minimum allowed output
     * @param maxOutput maximum allowed output
     * @return the computed output, clamped to [minOutput, maxOutput]
     */
    public double compute(double setpoint, double measured, double minOutput, double maxOutput) {
        double error = setpoint - measured;

        // Optional deadband around the setpoint to reduce jitter.
        if (deadband > 0.0 && Math.abs(error) <= deadband) {
            error = 0.0;
        }

        // Proportional term
        double proportional = kp * error;

        // Derivative term (rate of error change)
        double derivative = 0.0;
        if (!firstRun) {
            derivative = kd * (error - lastError);
        }
        firstRun = false;
        lastError = error;

        // Propose an updated integral (per-tick accumulation).
        double proposedIntegral = integral + (ki * error);

        // Compute the unclamped output using all three terms.
        double unclamped = proportional + proposedIntegral + derivative;

        // Clamp to output range.
        double output = unclamped;
        if (output > maxOutput) {
            output = maxOutput;
        } else if (output < minOutput) {
            output = minOutput;
        }

        // Conditional integration anti-windup:
        // If we saturated and the error would push further into saturation, do NOT accept the integral update.
        boolean saturatedHigh = output >= maxOutput && unclamped > maxOutput;
        boolean saturatedLow = output <= minOutput && unclamped < minOutput;

        if ((saturatedHigh && error > 0.0) || (saturatedLow && error < 0.0)) {
            // Reject the integral update to avoid windup.
            // Keep the previous integral.
        } else {
            integral = proposedIntegral;
        }

        // Store values for logging
        lastSetpoint = setpoint;
        lastMeasured = measured;
        lastProportional = proportional;
        lastDerivative = derivative;
        lastOutput = output;

        logData();

        return output;
    }

    /**
     * Resets the controller's state (integral and derivative tracking).
     * Should be called when the controlled system changes state significantly
     * (e.g., fuel runs out, engine stops).
     */
    public void reset() {
        integral = 0.0;
        lastError = 0.0;
        firstRun = true;
    }

    /**
     * Gets the current integral accumulator value.
     * Used for NBT persistence.
     *
     * @return the integral accumulator
     */
    public double getIntegral() {
        return integral;
    }

    /**
     * Sets the integral accumulator value.
     * Used for NBT persistence.
     *
     * @param integral the integral accumulator value to restore
     */
    public void setIntegral(double integral) {
        this.integral = integral;
    }

    /**
     * Gets the proportional gain.
     *
     * @return the kp value
     */
    public double getKp() {
        return kp;
    }

    /**
     * Gets the integral gain.
     *
     * @return the ki value
     */
    public double getKi() {
        return ki;
    }

    /**
     * Gets the derivative gain.
     *
     * @return the kd value
     */
    public double getKd() {
        return kd;
    }

    /**
     * Gets the last error value (for logging/debugging).
     *
     * @return the last error
     */
    public double getLastError() {
        return lastError;
    }

    // ==================== Logging Methods ====================

    private void logData() {
        if (!LOGGING_ENABLED) {
            return;
        }

        try {
            if (logWriter == null) {
                logWriter = new PrintWriter(new FileWriter(LOG_FILE, false));
                logHeaderWritten = false;
                logTick = 0;
            }

            if (!logHeaderWritten) {
                logWriter.println("tick,setpoint,measured,error,kp_term,ki_term,kd_term,output");
                logHeaderWritten = true;
            }

            logWriter.printf(
                    "%d,%.2f,%.2f,%.2f,%.4f,%.4f,%.4f,%.4f%n",
                    logTick,
                    lastSetpoint,
                    lastMeasured,
                    lastSetpoint - lastMeasured,
                    lastProportional,
                    integral,
                    lastDerivative,
                    lastOutput);

            logWriter.flush();
            logTick++;

        } catch (IOException e) {
            // Silently ignore logging errors
        }
    }

    /**
     * Closes the log file. Call when done with tuning session.
     */
    public static void closeLog() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
            logHeaderWritten = false;
            logTick = 0;
        }
    }

    /**
     * Resets the log to start a fresh tuning session.
     */
    public static void resetLog() {
        closeLog();
    }
}
