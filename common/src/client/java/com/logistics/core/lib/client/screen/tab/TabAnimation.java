package com.logistics.core.lib.client.screen.tab;

import java.util.function.LongSupplier;

/**
 * Open/close progress of one tab. Progress {@code p} moves linearly toward its target (0 or 1) at
 * {@link #DURATION_MS} per full sweep; {@link #eased()} applies ease-out cubic to it. Retargeting
 * mid-flight continues from the current {@code p}, so the time to reach the new target is
 * proportional to the remaining distance and the displayed value never jumps.
 */
public final class TabAnimation {
    /** Duration of a full 0 → 1 sweep. */
    public static final long DURATION_MS = 180L;

    /** Production clock in milliseconds; monotonic and independent of Minecraft's {@code Util}. */
    public static final LongSupplier SYSTEM_CLOCK = () -> System.nanoTime() / 1_000_000L;

    private final LongSupplier clock;
    private boolean open;
    private double from;
    private long startMs;

    /** Starts closed at rest. */
    public TabAnimation(LongSupplier clock) {
        this.clock = clock;
        this.startMs = clock.getAsLong();
    }

    public static double ease(double p) {
        double inverse = 1.0 - p;
        return 1.0 - inverse * inverse * inverse;
    }

    public boolean targetOpen() {
        return open;
    }

    /** Heads toward {@code open} from the current progress; a no-op when already heading there. */
    public void setTarget(boolean open) {
        if (open == this.open) {
            return;
        }
        long now = clock.getAsLong();
        this.from = progressAt(now);
        this.startMs = now;
        this.open = open;
    }

    /** Jumps to {@code open} with no animation. */
    public void snap(boolean open) {
        this.open = open;
        this.from = open ? 1.0 : 0.0;
        this.startMs = clock.getAsLong();
    }

    /** Linear progress in [0, 1]; exactly 0 or 1 once the target is reached. */
    public double progress() {
        return progressAt(clock.getAsLong());
    }

    public double eased() {
        return ease(progress());
    }

    public boolean isAnimating() {
        return progress() != target();
    }

    public boolean isFullyOpen() {
        return open && progress() == 1.0;
    }

    private double target() {
        return open ? 1.0 : 0.0;
    }

    private double progressAt(long now) {
        double target = target();
        double distance = Math.abs(target - from);
        if (distance == 0.0) {
            return target;
        }
        double moved = Math.max(0L, now - startMs) / (double) DURATION_MS;
        if (moved >= distance) {
            return target;
        }
        return open ? from + moved : from - moved;
    }
}
