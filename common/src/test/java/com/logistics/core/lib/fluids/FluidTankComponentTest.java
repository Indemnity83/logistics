package com.logistics.core.lib.fluids;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FluidTankComponent")
class FluidTankComponentTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("new tanks start empty")
    void newTank_isEmpty() {
        FluidTankComponent tank = tank(1000, () -> {});

        assertThat(tank.isEmpty()).isTrue();
        assertThat(tank.getAmount()).isZero();
        assertThat(tank.getCapacity()).isEqualTo(1000);
        assertThat(tank.contents()).isEmpty();
        assertThat(tank.getFluidKey().isBlank()).isTrue();
    }

    @Test
    @DisplayName("insert commits fluid up to capacity")
    void insert_commitsUpToCapacity() {
        AtomicInteger changes = new AtomicInteger();
        FluidTankComponent tank = tank(1000, changes::incrementAndGet);

        assertThat(tank.insert(water(), 600, false)).isEqualTo(600);
        assertThat(tank.insert(water(), 600, false)).isEqualTo(400);

        assertThat(tank.getAmount()).isEqualTo(1000);
        assertThat(tank.getFluidKey()).isEqualTo(water());
        assertThat(changes).hasValue(2);
    }

    @Test
    @DisplayName("insert simulation does not mutate")
    void insert_simulate_doesNotMutate() {
        AtomicInteger changes = new AtomicInteger();
        FluidTankComponent tank = tank(1000, changes::incrementAndGet);

        assertThat(tank.insert(water(), 500, true)).isEqualTo(500);

        assertThat(tank.isEmpty()).isTrue();
        assertThat(changes).hasValue(0);
    }

    @Test
    @DisplayName("insert rejects blank, non-positive, and mismatched fluids")
    void insert_rejectsInvalidInputs() {
        FluidTankComponent tank = tank(1000, () -> {});
        tank.insert(water(), 500, false);

        assertThat(tank.insert(blank(), 100, false)).isZero();
        assertThat(tank.insert(water(), 0, false)).isZero();
        assertThat(tank.insert(water(), -1, false)).isZero();
        assertThat(tank.insert(lava(), 100, false)).isZero();
        assertThat(tank.getAmount()).isEqualTo(500);
    }

    @Test
    @DisplayName("extract commits and clears key when emptied")
    void extract_commitsAndClearsWhenEmpty() {
        AtomicInteger changes = new AtomicInteger();
        FluidTankComponent tank = tank(1000, changes::incrementAndGet);
        tank.insert(water(), 700, false);

        assertThat(tank.extract(water(), 300, false)).isEqualTo(300);
        assertThat(tank.getAmount()).isEqualTo(400);
        assertThat(tank.extract(water(), 1000, false)).isEqualTo(400);

        assertThat(tank.isEmpty()).isTrue();
        assertThat(tank.getFluidKey().isBlank()).isTrue();
        assertThat(changes).hasValue(3);
    }

    @Test
    @DisplayName("extract simulation and invalid inputs do not mutate")
    void extract_simulateAndInvalidInputs_doNotMutate() {
        AtomicInteger changes = new AtomicInteger();
        FluidTankComponent tank = tank(1000, changes::incrementAndGet);
        tank.insert(water(), 700, false);

        assertThat(tank.extract(water(), 200, true)).isEqualTo(200);
        assertThat(tank.extract(lava(), 200, false)).isZero();
        assertThat(tank.extract(blank(), 200, false)).isZero();
        assertThat(tank.extract(water(), 0, false)).isZero();
        assertThat(tank.extract(water(), -1, false)).isZero();

        assertThat(tank.getAmount()).isEqualTo(700);
        assertThat(changes).hasValue(1);
    }

    @Test
    @DisplayName("contents exposes current fluid only when non-empty")
    void contents_exposesCurrentFluidOnlyWhenNonEmpty() {
        FluidTankComponent tank = tank(1000, () -> {});
        tank.insert(water(), 250, false);

        assertThat(tank.contents())
                .singleElement()
                .satisfies(view -> {
                    assertThat(view.resource()).isEqualTo(water());
                    assertThat(view.amount()).isEqualTo(250);
                });

        tank.extract(water(), 250, false);
        assertThat(tank.contents()).isEmpty();
    }

    @Test
    @DisplayName("NBT round trip preserves fluid and amount")
    void nbt_roundTrip_preservesFluidAndAmount() {
        FluidTankComponent written = tank(1000, () -> {});
        written.insert(water(), 750, false);
        CompoundTag nbt = new CompoundTag();

        written.writeNbt(nbt, "tank");
        FluidTankComponent read = tank(1000, () -> {});
        read.readNbt(nbt, "tank");

        assertThat(read.getFluidKey()).isEqualTo(water());
        assertThat(read.getAmount()).isEqualTo(750);
    }

    private static FluidTankComponent tank(long capacity, Runnable onChanged) {
        return new FluidTankComponent(capacity, onChanged);
    }

    private static IFluidKey water() {
        return key(Fluids.WATER);
    }

    private static IFluidKey lava() {
        return key(Fluids.LAVA);
    }

    private static IFluidKey blank() {
        return key(Fluids.EMPTY);
    }

    private static IFluidKey key(Fluid fluid) {
        return new IFluidKey() {
            @Override public Fluid getFluid() { return fluid; }
            @Override public DataComponentPatch getComponents() { return DataComponentPatch.EMPTY; }
            @Override public boolean equals(Object o) {
                return o instanceof IFluidKey other
                        && fluid == other.getFluid()
                        && getComponents().equals(other.getComponents());
            }
            @Override public int hashCode() { return 31 * fluid.hashCode() + getComponents().hashCode(); }
        };
    }
}
