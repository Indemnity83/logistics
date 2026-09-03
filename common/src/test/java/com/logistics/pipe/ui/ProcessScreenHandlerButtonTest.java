package com.logistics.pipe.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Button ids arrive straight from the client as an unconstrained int, so every id
 * must resolve without throwing on the server thread.
 */
@DisplayName("ProcessScreenHandler satellite buttons")
class ProcessScreenHandlerButtonTest {

    /** What {@code getAvailableSatelliteIds()} returns: Off, then registered ids. */
    private static final List<Integer> AVAILABLE = List.of(0, 3, 7);

    /** Configured satellite that is no longer registered — {@code indexOf} returns -1. */
    private static final int UNREGISTERED_ID = 5;

    @ParameterizedTest(name = "button {0}")
    @ValueSource(ints = {-2147483648, -1, 2, 3, 99, 2147483647})
    @DisplayName("unknown button id is a harmless no-op with an unregistered satellite")
    void unknownButtonId_isNoOp(int button) {
        assertThatCode(() -> ProcessScreenHandler.resolveSatelliteId(AVAILABLE, UNREGISTERED_ID, button))
                .doesNotThrowAnyException();
        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, UNREGISTERED_ID, button))
                .isEqualTo(UNREGISTERED_ID);
    }

    @ParameterizedTest(name = "button {0}")
    @ValueSource(ints = {-1, 2, 99})
    @DisplayName("unknown button id is a harmless no-op with a registered satellite")
    void unknownButtonId_withKnownSatellite_isNoOp(int button) {
        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, 3, button)).isEqualTo(3);
    }

    @Test
    @DisplayName("prev and next still cycle Off + registered ids")
    void prevAndNext_cycleAvailableIds() {
        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, 0, 1)).isEqualTo(3);
        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, 3, 1)).isEqualTo(7);
        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, 7, 1)).isEqualTo(0);

        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, 0, 0)).isEqualTo(7);
        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, 7, 0)).isEqualTo(3);
        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, 3, 0)).isEqualTo(0);
    }

    @Test
    @DisplayName("prev and next recover from an unregistered satellite")
    void prevAndNext_recoverFromUnregisteredSatellite() {
        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, UNREGISTERED_ID, 0)).isEqualTo(7);
        assertThat(ProcessScreenHandler.resolveSatelliteId(AVAILABLE, UNREGISTERED_ID, 1)).isEqualTo(0);
    }

    @Test
    @DisplayName("no registered satellites leaves only Off")
    void noRegisteredSatellites_leavesOnlyOff() {
        assertThat(ProcessScreenHandler.resolveSatelliteId(List.of(0), 0, 0)).isZero();
        assertThat(ProcessScreenHandler.resolveSatelliteId(List.of(0), 0, 1)).isZero();
        assertThat(ProcessScreenHandler.resolveSatelliteId(List.of(0), UNREGISTERED_ID, 1)).isZero();
    }

    @Test
    @DisplayName("an empty id list never throws")
    void emptyIdList_neverThrows() {
        assertThat(ProcessScreenHandler.resolveSatelliteId(List.of(), UNREGISTERED_ID, 0))
                .isEqualTo(UNREGISTERED_ID);
        assertThat(ProcessScreenHandler.resolveSatelliteId(List.of(), UNREGISTERED_ID, 1))
                .isEqualTo(UNREGISTERED_ID);
    }
}
