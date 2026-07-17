package com.logistics.pipe.tank;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.tank.TankCell;
import com.logistics.core.lib.tank.TankColumn;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TankColumnStorage")
class TankColumnStorageTest extends MinecraftTestEnvironment {

    private static final IFluidKey WATER = SimpleFluidKey.of(Fluids.WATER);
    private static final IFluidKey LAVA = SimpleFluidKey.of(Fluids.LAVA);

    /** In-memory cell holding a fixed fluid/amount, enough to exercise the column view. */
    private static final class FakeCell implements TankCell {
        private final IFluidKey fluid;
        private final long amount;
        private final long capacity;

        FakeCell(long capacity, IFluidKey fluid, long amount) {
            this.capacity = capacity;
            this.fluid = fluid.isBlank() ? SimpleFluidKey.BLANK : fluid;
            this.amount = amount;
        }

        @Override public IFluidKey fluid() { return fluid; }

        @Override public long amount() { return amount; }

        @Override public long capacity() { return capacity; }

        @Override public void setContents(IFluidKey fluid, long amount) {
            throw new UnsupportedOperationException("contents() must not mutate the column");
        }
    }

    private static TankColumnStorage storageOf(FakeCell... cells) {
        return new TankColumnStorage(() -> new TankColumn(List.of(cells), f -> false));
    }

    @Test
    @DisplayName("an empty column reports no contents")
    void emptyColumnHasNoContents() {
        assertThat(storageOf(new FakeCell(100, SimpleFluidKey.BLANK, 0)).contents()).isEmpty();
    }

    @Test
    @DisplayName("a mixed column refuses to report a single view")
    void mixedColumnHasNoContents() {
        assertThat(storageOf(
                        new FakeCell(100, WATER, 100),
                        new FakeCell(100, LAVA, 100))
                .contents())
                .isEmpty();
    }

    @Test
    @DisplayName("a single-fluid column reports one merged view of the whole column")
    void singleFluidColumnReportsMergedView() {
        Iterable<IFluidView> contents = storageOf(
                        new FakeCell(100, WATER, 100),
                        new FakeCell(100, WATER, 40))
                .contents();

        assertThat(contents).singleElement().satisfies(view -> {
            assertThat(view.resource().getFluid()).isEqualTo(Fluids.WATER);
            assertThat(view.amount()).isEqualTo(140);
            assertThat(view.capacity()).isEqualTo(200);
        });
    }
}
