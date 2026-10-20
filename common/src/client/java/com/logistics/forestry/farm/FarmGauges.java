package com.logistics.forestry.farm;

/** Pure display math for {@link FarmScreen}: bar fills and the status lamp sprite. */
public final class FarmGauges {

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

    /** The status lamp sprite for {@code status}, counted along the GUI texture's row: green, amber, red. */
    public static int lampSprite(FarmStatus status) {
        return switch (FarmLamp.forStatus(status)) {
            case GREEN -> 0;
            case AMBER -> 1;
            case RED -> 2;
            case OFF -> throw new IllegalArgumentException("The GUI has no unpowered lamp sprite");
        };
    }
}
