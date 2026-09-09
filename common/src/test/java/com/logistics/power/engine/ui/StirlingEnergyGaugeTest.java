package com.logistics.power.engine.ui;

import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.inventory.ContainerData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Stirling charge gauge must track the buffer's real fill at any configured
 * {@code engines.stirling buffer_capacity}, not only at the 10,000 RF default.
 */
@DisplayName("Stirling Engine charge gauge")
class StirlingEnergyGaugeTest extends MinecraftTestEnvironment {

    /** Gauge sprite height in pixels (ENERGY_HEIGHT in StirlingEngineScreen). */
    private static final int GAUGE_PX = 30;

    /** A ContainerData carrying one synced value at the energy index. */
    private static ContainerData synced(int value) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return index == StirlingEngineBlockEntity.PROPERTY_ENERGY ? value : 0;
            }

            @Override
            public void set(int index, int v) {}

            @Override
            public int getCount() {
                return StirlingEngineBlockEntity.PROPERTY_COUNT;
            }
        };
    }

    /** Server scales the fill, ContainerData syncs it as a short, the screen turns it into pixels. */
    private static int gaugePixels(long stored, long capacity) {
        short overTheWire = (short) StirlingEngineBlockEntity.energyFraction(stored, capacity);
        return StirlingEngineScreenHandler.energyBarHeight(synced(overTheWire), GAUGE_PX);
    }

    @ParameterizedTest(name = "buffer_capacity = {0}")
    @ValueSource(longs = {5_000L, 10_000L, 50_000L, 4_000_000L})
    @DisplayName("gauge is empty at empty, full at full, and half at half")
    void gaugeTracksFillAtAnyCapacity(long capacity) {
        assertThat(gaugePixels(0, capacity)).isZero();
        assertThat(gaugePixels(capacity, capacity)).isEqualTo(GAUGE_PX);
        assertThat(gaugePixels(capacity / 2, capacity)).isBetween(GAUGE_PX / 2 - 1, GAUGE_PX / 2 + 1);
    }

    @ParameterizedTest(name = "buffer_capacity = {0}")
    @ValueSource(longs = {5_000L, 10_000L, 50_000L, 4_000_000L})
    @DisplayName("gauge only reads full when the buffer is full")
    void gaugeIsNotFullBeforeTheBufferIs(long capacity) {
        assertThat(gaugePixels(capacity / 4, capacity)).isLessThan(GAUGE_PX);
        assertThat(gaugePixels(capacity - capacity / 10, capacity)).isLessThan(GAUGE_PX);
    }

    @Test
    @DisplayName("synced fill fits a 16-bit ContainerData slot at any buffer size")
    void syncedFillFitsAShort() {
        for (long capacity : new long[] {5_000L, 10_000L, 50_000L, 4_000_000L, 2_000_000_000L}) {
            for (long stored = 0; stored <= capacity; stored += Math.max(1, capacity / 7)) {
                assertThat(StirlingEngineBlockEntity.energyFraction(stored, capacity))
                        .isBetween(0, (int) Short.MAX_VALUE);
            }
        }
    }
}
