package com.logistics.core.lib.power;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EngineCyclePlannerTest {
    @Test
    void advance_startsExpansionWhenIdleAndPowered() {
        EngineCyclePlanner.Result result = EngineCyclePlanner.advance(
                CyclePhase.IDLE,
                0.0f,
                true,
                0.08f,
                false);

        assertThat(result.phase()).isEqualTo(CyclePhase.EXPANSION);
        assertThat(result.progress()).isZero();
        assertThat(result.shouldSendEnergy()).isFalse();
    }

    @Test
    void advance_keepsIdleEngineStoppedWhenUnpowered() {
        EngineCyclePlanner.Result result = EngineCyclePlanner.advance(
                CyclePhase.IDLE,
                0.0f,
                false,
                0.08f,
                false);

        assertThat(result.phase()).isEqualTo(CyclePhase.IDLE);
        assertThat(result.progress()).isZero();
        assertThat(result.shouldSendEnergy()).isFalse();
    }

    @Test
    void advance_movesExpansionForwardWithoutSendingBeforeCompression() {
        EngineCyclePlanner.Result result = EngineCyclePlanner.advance(
                CyclePhase.EXPANSION,
                0.20f,
                true,
                0.08f,
                false);

        assertThat(result.phase()).isEqualTo(CyclePhase.EXPANSION);
        assertThat(result.progress()).isEqualTo(0.28f);
        assertThat(result.shouldSendEnergy()).isFalse();
    }

    @Test
    void advance_entersCompressionAndRequestsPulsedEnergySend() {
        EngineCyclePlanner.Result result = EngineCyclePlanner.advance(
                CyclePhase.EXPANSION,
                0.48f,
                true,
                0.04f,
                false);

        assertThat(result.phase()).isEqualTo(CyclePhase.COMPRESSION);
        assertThat(result.progress()).isEqualTo(0.52f);
        assertThat(result.shouldSendEnergy()).isTrue();
    }

    @Test
    void advance_atExactBoundary_progressIsExactlyPointFive() {
        EngineCyclePlanner.Result result = EngineCyclePlanner.advance(
                CyclePhase.EXPANSION,
                0.49f,
                true,
                0.01f,
                false);

        assertThat(result.phase()).isEqualTo(CyclePhase.COMPRESSION);
        assertThat(result.progress()).isEqualTo(0.5f);
        assertThat(result.shouldSendEnergy()).isTrue();
    }

    @Test
    void advance_requestsContinuousEnergySendEveryActiveTick() {
        EngineCyclePlanner.Result result = EngineCyclePlanner.advance(
                CyclePhase.EXPANSION,
                0.20f,
                true,
                0.08f,
                true);

        assertThat(result.phase()).isEqualTo(CyclePhase.EXPANSION);
        assertThat(result.progress()).isEqualTo(0.28f);
        assertThat(result.shouldSendEnergy()).isTrue();
    }

    @Test
    void advance_resetsToIdleAtEndOfCycle() {
        EngineCyclePlanner.Result result = EngineCyclePlanner.advance(
                CyclePhase.COMPRESSION,
                0.96f,
                true,
                0.08f,
                false);

        assertThat(result.phase()).isEqualTo(CyclePhase.IDLE);
        assertThat(result.progress()).isZero();
        assertThat(result.shouldSendEnergy()).isFalse();
    }
}
