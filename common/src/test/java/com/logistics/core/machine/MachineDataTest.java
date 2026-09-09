package com.logistics.core.machine;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
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

    /**
     * The client's view of {@code slots} once vanilla has synced them:
     * {@code ClientboundContainerSetDataPacket} writes and reads every value with
     * {@code writeShort}/{@code readShort}, so only the low 16 bits, sign-extended, survive the trip.
     */
    private static ContainerData afterSync(int... slots) {
        SimpleContainerData client = new SimpleContainerData(slots.length);
        for (int i = 0; i < slots.length; i++) {
            client.set(i, (short) slots[i]);
        }
        return client;
    }

    /** {@code value} published as a wide slot pair and synced to the client. */
    private static ContainerData syncedWide(long value) {
        int[] slots = new int[MachineData.WIDE_SLOTS];
        for (int half = 0; half < slots.length; half++) {
            slots[half] = MachineData.wideSlot(value, half);
        }
        return afterSync(slots);
    }

    @Test
    @DisplayName("a wide value survives the sync past 32,767 (the tank-amount overflow bug)")
    void wideSurvivesTheShortSync() {
        // Tank millibuckets. Raised past a short, a single slot used to wrap negative (the gauge pinned
        // empty at 40,000 mB) and then back to a plausible wrong number (70,000 mB read as 4,464 mB).
        assertThat(MachineData.wide(syncedWide(0), 0)).isZero();
        assertThat(MachineData.wide(syncedWide(16_000), 0)).isEqualTo(16_000);
        assertThat(MachineData.wide(syncedWide(32_768), 0)).isEqualTo(32_768);
        assertThat(MachineData.wide(syncedWide(40_000), 0)).isEqualTo(40_000);
        assertThat(MachineData.wide(syncedWide(65_536), 0)).isEqualTo(65_536);
        assertThat(MachineData.wide(syncedWide(70_000), 0)).isEqualTo(70_000);
        assertThat(MachineData.wide(syncedWide(Integer.MAX_VALUE), 0)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("a wide value clamps to 0..Integer.MAX_VALUE")
    void wideClampsOutOfRangeValues() {
        assertThat(MachineData.wide(syncedWide(-1), 0)).isZero();
        assertThat(MachineData.wide(syncedWide(Long.MAX_VALUE), 0)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("a wide value reads from its own slot pair, not from slot 0")
    void wideReadsAtItsOwnIndex() {
        SimpleContainerData data = new SimpleContainerData(2 + MachineData.WIDE_SLOTS);
        data.set(0, MachineData.SCALE);
        data.set(1, -1);
        for (int half = 0; half < MachineData.WIDE_SLOTS; half++) {
            data.set(2 + half, (short) MachineData.wideSlot(70_000, half));
        }
        assertThat(MachineData.wide(data, 2)).isEqualTo(70_000);
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
