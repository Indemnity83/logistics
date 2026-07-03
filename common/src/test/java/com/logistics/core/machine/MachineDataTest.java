package com.logistics.core.machine;

import net.minecraft.world.inventory.ContainerData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MachineData")
class MachineDataTest {

    /** A ContainerData whose single slot returns a fixed value. */
    private static ContainerData slot(int value) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return value;
            }

            @Override
            public void set(int index, int v) {}

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    @Test
    @DisplayName("fraction is 0 at empty and SCALE at full")
    void fractionBounds() {
        assertThat(MachineData.fraction(0, 40_000)).isZero();
        assertThat(MachineData.fraction(40_000, 40_000)).isEqualTo(MachineData.SCALE);
        assertThat(MachineData.fraction(20_000, 40_000)).isEqualTo(MachineData.SCALE / 2);
    }

    @Test
    @DisplayName("fraction stays short-safe for RF values far above 32,767 (the sync overflow bug)")
    void fractionNeverOverflowsAShort() {
        // Crucible-scale numbers: a 40,000 RF buffer, recipes to 300,000 RF — all past a short's 32,767.
        for (long value = 0; value <= 300_000; value += 12_345) {
            int f = MachineData.fraction(value, 300_000);
            assertThat(f).isBetween(0, MachineData.SCALE);
            assertThat(f).isLessThanOrEqualTo(Short.MAX_VALUE);
        }
        assertThat(MachineData.fraction(300_000, 300_000)).isEqualTo(MachineData.SCALE);
    }

    @Test
    @DisplayName("fraction clamps a value above its max and guards a non-positive max")
    void fractionEdgeCases() {
        assertThat(MachineData.fraction(50_000, 40_000)).isEqualTo(MachineData.SCALE);
        assertThat(MachineData.fraction(100, 0)).isZero();
        assertThat(MachineData.fraction(100, -5)).isZero();
    }

    @Test
    @DisplayName("barPixels scales the fraction to the sprite and clamps to its width")
    void barPixels() {
        assertThat(MachineData.barPixels(slot(0), 0, 24)).isZero();
        assertThat(MachineData.barPixels(slot(MachineData.SCALE), 0, 24)).isEqualTo(24);
        assertThat(MachineData.barPixels(slot(MachineData.SCALE / 2), 0, 24)).isEqualTo(12);
        // A fraction reported past SCALE never renders past the sprite.
        assertThat(MachineData.barPixels(slot(MachineData.SCALE * 2), 0, 30)).isEqualTo(30);
    }
}
