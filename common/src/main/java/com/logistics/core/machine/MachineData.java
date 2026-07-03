package com.logistics.core.machine;

import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.RecipeProcessorComponent;
import net.minecraft.world.inventory.ContainerData;

/**
 * Shared {@link ContainerData} plumbing for component machines.
 *
 * <p>{@code ContainerData} syncs each value to the client as a signed 16-bit short (−32,768..32,767).
 * Syncing raw RF overflows once a buffer or recipe exceeds 32,767, so progress and energy fill are
 * synced as a {@code 0..}{@link #SCALE} fraction instead — correct at any energy magnitude.
 */
public final class MachineData {
    /** Fixed-point denominator for synced fractions. */
    public static final int SCALE = 10_000;

    /** Data index: recipe progress fill (0..{@link #SCALE}). */
    public static final int PROGRESS = 0;
    /** Data index: energy buffer fill (0..{@link #SCALE}). */
    public static final int ENERGY = 1;
    /** Slot count for a machine syncing only progress + energy. */
    public static final int COUNT = 2;

    private MachineData() {}

    /** Active-recipe progress as a 0..{@link #SCALE} fraction. */
    public static int progressFraction(RecipeProcessorComponent processor) {
        return Math.round(processor.progress() * SCALE);
    }

    /** Energy fill as a 0..{@link #SCALE} fraction of the buffer capacity. */
    public static int energyFraction(EnergyStorageComponent energy, long capacity) {
        return fraction(energy.amount(), capacity);
    }

    /**
     * {@code value} as a 0..{@link #SCALE} fraction of {@code max}. Bounded to {@link #SCALE} (which
     * fits a 16-bit short) regardless of magnitude — this is what keeps raw RF from overflowing sync.
     */
    public static int fraction(long value, long max) {
        return max <= 0 ? 0 : (int) (Math.min(value, max) * SCALE / max);
    }

    /**
     * Server-side {@link ContainerData} syncing only {@link #PROGRESS} and {@link #ENERGY} as
     * fractions. The client uses a {@code SimpleContainerData} of {@link #COUNT} slots.
     */
    public static ContainerData source(
            RecipeProcessorComponent processor, EnergyStorageComponent energy, long capacity) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case PROGRESS -> progressFraction(processor);
                    case ENERGY -> energyFraction(energy, capacity);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server-side source only; the client uses a SimpleContainerData populated by sync.
            }

            @Override
            public int getCount() {
                return COUNT;
            }
        };
    }

    /** Bar fill in pixels (0..{@code spritePx}, clamped) from the 0..{@link #SCALE} fraction at {@code index}. */
    public static int barPixels(ContainerData data, int index, int spritePx) {
        return Math.min(spritePx, spritePx * data.get(index) / SCALE);
    }
}
