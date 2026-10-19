package com.logistics.core.machine.control;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RedstoneModeTest {

    @ParameterizedTest
    @CsvSource({
        "IGNORED, false, true",
        "IGNORED, true, true",
        "LOW, false, true",
        "LOW, true, false",
        "HIGH, false, false",
        "HIGH, true, true"
    })
    void allowsFollowsTheTruthTable(RedstoneMode mode, boolean signal, boolean expected) {
        assertThat(mode.allows(signal)).isEqualTo(expected);
    }

    /** Names are persisted in NBT and sync ids travel over the menu protocol: both must never change. */
    @Test
    void namesAndSyncIdsAreStable() {
        assertThat(RedstoneMode.values()).containsExactly(RedstoneMode.IGNORED, RedstoneMode.LOW, RedstoneMode.HIGH);
        assertThat(RedstoneMode.IGNORED.serializedName()).isEqualTo("ignored");
        assertThat(RedstoneMode.IGNORED.syncId()).isZero();
        assertThat(RedstoneMode.LOW.serializedName()).isEqualTo("low");
        assertThat(RedstoneMode.LOW.syncId()).isEqualTo(1);
        assertThat(RedstoneMode.HIGH.serializedName()).isEqualTo("high");
        assertThat(RedstoneMode.HIGH.syncId()).isEqualTo(2);
    }

    @Test
    void byNameRoundTripsAndFallsBackForUnknownNames() {
        for (RedstoneMode mode : RedstoneMode.values()) {
            assertThat(RedstoneMode.byName(mode.serializedName(), RedstoneMode.IGNORED)).isEqualTo(mode);
        }
        assertThat(RedstoneMode.byName("bogus", RedstoneMode.HIGH)).isEqualTo(RedstoneMode.HIGH);
        assertThat(RedstoneMode.byName("HIGH", RedstoneMode.LOW)).isEqualTo(RedstoneMode.LOW);
        assertThat(RedstoneMode.byName(null, RedstoneMode.LOW)).isEqualTo(RedstoneMode.LOW);
    }

    @Test
    void bySyncIdRoundTripsAndIsEmptyForUnknownIds() {
        for (RedstoneMode mode : RedstoneMode.values()) {
            assertThat(RedstoneMode.bySyncId(mode.syncId())).contains(mode);
        }
        assertThat(RedstoneMode.bySyncId(3)).isEmpty();
        assertThat(RedstoneMode.bySyncId(-1)).isEmpty();
    }
}
