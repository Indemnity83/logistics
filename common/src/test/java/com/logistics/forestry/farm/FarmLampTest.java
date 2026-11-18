package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Farm status lamp")
class FarmLampTest {

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, names = {"WORKING", "IDLE"})
    @DisplayName("the lamp is green while the farm works or has nothing ripe")
    void greenWhenAllIsWell(FarmStatus status) {
        assertThat(FarmLamp.forStatus(status)).isEqualTo(FarmLamp.GREEN);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, names = {"WAITING_FOR_SEEDS", "INVENTORY_FULL", "PAUSED"})
    @DisplayName("the lamp is amber while the farm needs attention but is not stopped by a fault")
    void amberForWarnings(FarmStatus status) {
        assertThat(FarmLamp.forStatus(status)).isEqualTo(FarmLamp.AMBER);
    }

    @ParameterizedTest
    @EnumSource(value = FarmStatus.class, names = {"NO_POWER", "BLOCKED"})
    @DisplayName("the lamp is red when the farm cannot work")
    void redForFaults(FarmStatus status) {
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
