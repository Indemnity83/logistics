package com.logistics.core.lib.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests for the {@link EnergyPushService} holder. */
class EnergyPushServiceTest {

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
