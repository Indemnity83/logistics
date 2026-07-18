package com.logistics.core.lib.power;

public final class EngineCyclePlanner {
    private EngineCyclePlanner() {}

    public static Result advance(
            CyclePhase phase,
            float progress,
            boolean powered,
            float pistonSpeed,
            boolean sendsEnergyContinuously) {
        if (phase == CyclePhase.IDLE && powered) {
            return new Result(CyclePhase.EXPANSION, progress, false);
        }

        if (phase == CyclePhase.IDLE) {
            return new Result(phase, progress, false);
        }

        float nextProgress = progress + pistonSpeed;
        boolean transitionedToCompression = phase == CyclePhase.EXPANSION && nextProgress >= 0.5f;
        CyclePhase nextPhase =
                transitionedToCompression ? CyclePhase.COMPRESSION : phase;
        boolean shouldSendEnergy = sendsEnergyContinuously || transitionedToCompression;

        if (nextProgress >= 1.0f) {
            return new Result(CyclePhase.IDLE, 0.0f, shouldSendEnergy);
        }

        return new Result(nextPhase, nextProgress, shouldSendEnergy);
    }

    public record Result(CyclePhase phase, float progress, boolean shouldSendEnergy) {}
}
