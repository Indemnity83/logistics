package com.logistics.core.lib.energy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests for the {@link EnergyPushService} holder. */
class EnergyPushServiceTest {

    // The holder is process-wide, so whatever was registered before this class ran has to go back.
    private EnergyPushService previous;

    @BeforeEach
    void captureRegisteredService() {
        previous = EnergyPushService.get();
    }

    @AfterEach
    void restoreRegisteredService() {
        if (previous == null) {
            EnergyPushService.Holder.clearForTest();
        } else {
            EnergyPushService.set(previous);
        }
    }

    @Test
    void setAndGet_roundTripsAndPushes() {
        EnergyPushService service = (level, target, dir, source, max) -> 7L;
        EnergyPushService.set(service);

        assertSame(service, EnergyPushService.get());
        assertEquals(7L, EnergyPushService.get().push(null, null, null, null, 10));
    }

    @Test
    void set_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> EnergyPushService.set(null));
    }
}
