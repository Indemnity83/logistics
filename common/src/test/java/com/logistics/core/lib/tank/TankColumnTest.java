package com.logistics.core.lib.tank;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TankColumn")
class TankColumnTest extends MinecraftTestEnvironment {

    private static final IFluidKey WATER = SimpleFluidKey.of(Fluids.WATER);
    private static final IFluidKey LAVA = SimpleFluidKey.of(Fluids.LAVA);

    /** Simple in-memory cell for testing column math in native units. */
    private static final class FakeCell implements TankCell {
        private IFluidKey fluid = SimpleFluidKey.BLANK;
        private long amount;
        private final long capacity;

        FakeCell(long capacity) {
            this.capacity = capacity;
        }

        @Override public IFluidKey fluid() { return fluid; }
        @Override public long amount() { return amount; }
        @Override public long capacity() { return capacity; }

        @Override
        public void setContents(IFluidKey fluid, long amount) {
            if (fluid == null || fluid.isBlank() || amount <= 0) {
                this.fluid = SimpleFluidKey.BLANK;
                this.amount = 0;
            } else {
                this.fluid = fluid;
                this.amount = Math.min(amount, capacity);
            }
        }
    }

    private static TankColumn liquidColumn(FakeCell... cells) {
        return new TankColumn(List.of(cells), f -> false);
    }

    @Test
    @DisplayName("insert fills the column bottom-up")
    void insertFillsBottomUp() {
        FakeCell bottom = new FakeCell(100);
        FakeCell top = new FakeCell(100);

        long inserted = liquidColumn(bottom, top).insert(WATER, 150, false);

        assertThat(inserted).isEqualTo(150);
        assertThat(bottom.amount()).isEqualTo(100);
        assertThat(top.amount()).isEqualTo(50);
    }

    @Test
    @DisplayName("insert simulate does not mutate")
    void insertSimulate() {
        FakeCell bottom = new FakeCell(100);
        FakeCell top = new FakeCell(100);

        long inserted = liquidColumn(bottom, top).insert(WATER, 150, true);

        assertThat(inserted).isEqualTo(150);
        assertThat(bottom.amount()).isZero();
        assertThat(top.amount()).isZero();
    }

    @Test
    @DisplayName("extract drains the column")
    void extractDrains() {
        FakeCell bottom = new FakeCell(100);
        FakeCell top = new FakeCell(100);
        TankColumn column = liquidColumn(bottom, top);
        column.insert(WATER, 150, false);

        long extracted = column.extract(WATER, 80, false);

        assertThat(extracted).isEqualTo(80);
        assertThat(column.total()).isEqualTo(70);
        // Remaining settles to the bottom.
        assertThat(bottom.amount()).isEqualTo(70);
        assertThat(top.amount()).isZero();
    }

    @Test
    @DisplayName("a different fluid is rejected without loss")
    void differentFluidRejected() {
        FakeCell cell = new FakeCell(1000);
        TankColumn column = liquidColumn(cell);
        column.insert(WATER, 500, false);

        assertThat(column.insert(LAVA, 100, false)).isZero();
        assertThat(column.extract(LAVA, 100, false)).isZero();
        assertThat(cell.amount()).isEqualTo(500);
        assertThat(cell.fluid()).isEqualTo(WATER);
    }

    @Test
    @DisplayName("a full column creates backpressure with no loss")
    void backpressure() {
        FakeCell cell = new FakeCell(100);
        TankColumn column = liquidColumn(cell);
        column.insert(WATER, 100, false);

        assertThat(column.insert(WATER, 50, false)).isZero();
        assertThat(cell.amount()).isEqualTo(100);
    }

    @Test
    @DisplayName("a mixed column refuses all operations")
    void mixedColumnRefuses() {
        FakeCell bottom = new FakeCell(100);
        FakeCell top = new FakeCell(100);
        bottom.setContents(WATER, 50);
        top.setContents(LAVA, 50);
        TankColumn column = liquidColumn(bottom, top);

        assertThat(column.mixed()).isTrue();
        assertThat(column.insert(WATER, 10, false)).isZero();
        assertThat(column.extract(WATER, 10, false)).isZero();
        assertThat(bottom.amount()).isEqualTo(50);
        assertThat(top.amount()).isEqualTo(50);
    }

    @Test
    @DisplayName("rebalance settles an unsettled column and reports changed cells")
    void rebalanceSettles() {
        FakeCell bottom = new FakeCell(100);
        FakeCell top = new FakeCell(100);
        // Unsettled: fluid sits in the top cell only.
        top.setContents(WATER, 60);
        TankColumn column = liquidColumn(bottom, top);

        List<TankCell> changed = column.rebalance();

        assertThat(bottom.amount()).isEqualTo(60);
        assertThat(top.amount()).isZero();
        assertThat(changed).containsExactlyInAnyOrder(bottom, top);
    }

    @Test
    @DisplayName("deposit then extract a bottle moves one third of a bucket")
    void bottleRoundTrip() {
        FakeCell cell = new FakeCell(81_000); // a bucket in droplets-scale units
        TankColumn column = liquidColumn(cell);

        // Native bottle size depends on the platform unit; assert the round trip is conservative.
        long bottle = 27_000;
        long before = column.total();
        boolean deposited = column.depositBottle(WATER, bottle);
        assertThat(deposited).isTrue();
        assertThat(column.total()).isEqualTo(before + bottle);

        boolean extracted = column.extractBottle(WATER, bottle);
        assertThat(extracted).isTrue();
        assertThat(column.total()).isEqualTo(before);
    }

    @Test
    @DisplayName("gases settle to the top")
    void gasesSettleTop() {
        FakeCell bottom = new FakeCell(100);
        FakeCell top = new FakeCell(100);
        TankColumn gasColumn = new TankColumn(new ArrayList<>(List.of(bottom, top)), f -> true);

        gasColumn.insert(WATER, 60, false); // treat WATER as gas via the predicate

        assertThat(top.amount()).isEqualTo(60);
        assertThat(bottom.amount()).isZero();
    }
}
