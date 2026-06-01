package com.logistics.core.lib.power;

final class EngineCyclePlanner {
    private EngineCyclePlanner() {}

    static Result advance(
            AbstractEngineBlockEntity.CyclePhase phase,
            float progress,
            boolean powered,
            float pistonSpeed,
            boolean sendsEnergyContinuously) {
        if (phase == AbstractEngineBlockEntity.CyclePhase.IDLE && powered) {
            return new Result(AbstractEngineBlockEntity.CyclePhase.EXPANSION, progress, false);
        }

        if (phase == AbstractEngineBlockEntity.CyclePhase.IDLE) {
            return new Result(phase, progress, false);
        }

        float nextProgress = progress + pistonSpeed;
        boolean transitionedToCompression = phase == AbstractEngineBlockEntity.CyclePhase.EXPANSION && nextProgress > 0.5f;
        AbstractEngineBlockEntity.CyclePhase nextPhase =
                transitionedToCompression ? AbstractEngineBlockEntity.CyclePhase.COMPRESSION : phase;
        boolean shouldSendEnergy = sendsEnergyContinuously || transitionedToCompression;

        if (nextProgress >= 1.0f) {
            return new Result(AbstractEngineBlockEntity.CyclePhase.IDLE, 0.0f, shouldSendEnergy);
        }

        return new Result(nextPhase, nextProgress, shouldSendEnergy);
    }

    record Result(AbstractEngineBlockEntity.CyclePhase phase, float progress, boolean shouldSendEnergy) {}
}
