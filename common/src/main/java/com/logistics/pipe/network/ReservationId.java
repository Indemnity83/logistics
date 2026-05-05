package com.logistics.pipe.network;

import java.util.UUID;

/**
 * Type-safe wrapper around a {@link UUID} that identifies a single item reservation.
 * Prevents accidental confusion with order UUIDs or delivery IDs.
 */
public record ReservationId(UUID value) {
    public ReservationId {
        if (value == null) throw new NullPointerException("value must not be null");
    }

    public static ReservationId random() {
        return new ReservationId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return "Res[" + value.toString().substring(0, 8) + "]";
    }
}
