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

    @Test
    @DisplayName("NBT read clamps a saved amount that exceeds the current capacity")
    void nbt_read_clampsOverCapacityAmount() {
        // A capacity shrink (config, tier rework) since the tank was saved can leave the persisted
        // amount above what the tank can currently hold. Loading it must clamp, not trust it blindly —
        // an unclamped over-capacity amount crashes the column's next rebalance() (settle asserts
        // total <= capacity), which is exactly what happened without this clamp.
        FluidTankComponent written = tank(1000, () -> {});
        written.insert(water(), 1000, false);
        CompoundTag nbt = new CompoundTag();
        written.writeNbt(nbt, "tank");

        FluidTankComponent shrunk = tank(200, () -> {});
        shrunk.readNbt(nbt, "tank");

        assertThat(shrunk.getAmount()).isEqualTo(200);
        assertThat(shrunk.getAmount()).isLessThanOrEqualTo(shrunk.getCapacity());
    }

    @Test
    @DisplayName("reading empty NBT clears stale contents (emptied-tank sync)")
    void nbt_empty_clearsStaleContents() {
        // An emptied tank writes no key; reading that must reset, not retain old fluid — otherwise the
        // client keeps rendering fluid after the server tank has drained to zero.
        FluidTankComponent emptied = tank(1000, () -> {});
        CompoundTag emptyNbt = new CompoundTag();
        emptied.writeNbt(emptyNbt, "tank");
        // The key must be present even when empty, so the owning BE always emits its data tag.
        assertThat(emptyNbt.contains("tank")).isTrue();

        FluidTankComponent stale = tank(1000, () -> {});
        stale.insert(water(), 500, false);
        stale.readNbt(emptyNbt, "tank");

        assertThat(stale.isEmpty()).isTrue();
        assertThat(stale.getAmount()).isZero();
        assertThat(stale.getFluidKey().isBlank()).isTrue();
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
