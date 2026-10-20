package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Farm status lamp")
class FarmLampTest {

    @Test
    @DisplayName("the lamp is green while working")
    void greenWhileWorking() {
        assertThat(FarmLamp.forStatus(FarmStatus.WORKING)).isEqualTo(FarmLamp.GREEN);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, names = {"IDLE", "PAUSED", "SURVEYING"})
    @DisplayName("the lamp is amber while idle, paused or surveying")
    void amberWhileWaiting(FarmStatus status) {
        assertThat(FarmLamp.forStatus(status)).isEqualTo(FarmLamp.AMBER);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"WORKING", "IDLE", "PAUSED", "SURVEYING"})
    @DisplayName("the lamp is red for every problem status")
    void redForProblems(FarmStatus status) {
        assertThat(FarmLamp.forStatus(status)).isEqualTo(FarmLamp.RED);
    }

    @ParameterizedTest
    @EnumSource(FarmStatus.class)
    @DisplayName("no status maps to the unpowered lamp")
    void statusIsNeverOff(FarmStatus status) {
        assertThat(FarmLamp.forStatus(status)).isNotEqualTo(FarmLamp.OFF);
    }

    @Test
    @DisplayName("the lamp's block-state ids are off, red, amber and green")
    void serializedNames() {
        assertThat(FarmLamp.values())
                .extracting(FarmLamp::getSerializedName)
                .containsExactly("off", "red", "amber", "green");
    }
}
