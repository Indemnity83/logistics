package com.logistics.forestry.farm;

/** Pure display math for {@link FarmScreen}: bar fills and the status lamp colour. */
public final class FarmGauges {

    /** Status lamp colours; ordinal order matches the lamp sprites in the GUI texture. */
    public enum Lamp {
        GREEN,
        AMBER,
        RED
    }

    private FarmGauges() {}

    /** Pixels of a {@code length}-pixel bar to fill for {@code amount} of {@code capacity}, floored and clamped. */
    public static int fillPixels(long amount, long capacity, int length) {
        if (capacity <= 0 || amount <= 0 || length <= 0) {
            return 0;
        }
        if (amount >= capacity) {
            return length;
        }
        return (int) (amount * length / capacity);
    }

    public static Lamp lamp(FarmStatus status) {
        return switch (status) {
            case WORKING -> Lamp.GREEN;
            case IDLE, PAUSED -> Lamp.AMBER;
            default -> Lamp.RED;
        };
    }
}
