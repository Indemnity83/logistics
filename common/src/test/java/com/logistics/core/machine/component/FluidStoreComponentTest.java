package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

class FluidStoreComponentTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private FluidStoreComponent store() {
        return new FluidStoreComponent("fluid", 10_000, () -> {});
    }

    @Test
    void fluidAccessReturnsTheBackingTank() {
        FluidStoreComponent store = store();
        // The sided fluid accessor and tank() expose the same holder regardless of side.
        assertThat(store.fluid(null)).isSameAs(store.tank());
        assertThat(store.fluid(net.minecraft.core.Direction.UP)).isSameAs(store.tank());
        assertThat(store.tank().getCapacity()).isEqualTo(10_000);
    }

    @Test
    void savesAndLoadsTankContentsRoundTrip() {
        FluidStoreComponent writer = store();
        writer.tank().setContents(SimpleFluidKey.of(Fluids.WATER), 4_000);

        CompoundTag tag = new CompoundTag();
        writer.save(tag, registries);
        assertThat(tag.contains("fluid")).isTrue(); // lock the persisted NBT key

        FluidStoreComponent reader = store();
        reader.load(tag, registries);

        assertThat(reader.tank().isEmpty()).isFalse();
        assertThat(reader.tank().getAmount()).isEqualTo(4_000);
        assertThat(reader.tank().getFluidKey()).isEqualTo(SimpleFluidKey.of(Fluids.WATER));
    }

    @Test
    void loadLegacyReadsTheOldTankRootKey() {
        FluidStoreComponent writer = store();
        writer.tank().setContents(SimpleFluidKey.of(Fluids.LAVA), 250);

        // The component saves under "fluid"; the pre-component format keyed it as "Tank" at the root.
        CompoundTag saved = new CompoundTag();
        writer.save(saved, registries);
        CompoundTag legacyRoot = new CompoundTag();
        legacyRoot.put("Tank", NbtCompat.getCompoundOrEmpty(saved, "fluid"));

        FluidStoreComponent reader = store();
        reader.loadLegacy(legacyRoot, registries);

        assertThat(reader.tank().getAmount()).isEqualTo(250);
        assertThat(reader.tank().getFluidKey()).isEqualTo(SimpleFluidKey.of(Fluids.LAVA));
    }

    private FluidStoreComponent waterOnly() {
        return FluidStoreComponent.inputOnly(
                "water", 10_000, () -> {}, fluid -> fluid.getFluid() == Fluids.WATER);
    }

    @Test
    void inputOnlyAcceptsMatchingFluid() {
        FluidStoreComponent store = waterOnly();
        var exposed = store.fluid(net.minecraft.core.Direction.NORTH);

        assertThat(exposed.insert(SimpleFluidKey.of(Fluids.WATER), 1_000, true)).isEqualTo(1_000);
        assertThat(store.tank().isEmpty()).isTrue(); // simulate changes nothing
        assertThat(exposed.insert(SimpleFluidKey.of(Fluids.WATER), 1_000, false)).isEqualTo(1_000);
        assertThat(store.tank().getAmount()).isEqualTo(1_000);
        assertThat(exposed.contents()).hasSize(1);
    }

    @Test
    void inputOnlyRejectsOtherFluids() {
        FluidStoreComponent store = waterOnly();

        assertThat(store.fluid(null).insert(SimpleFluidKey.of(Fluids.LAVA), 1_000, false)).isZero();
        assertThat(store.tank().isEmpty()).isTrue();
    }

    @Test
    void inputOnlyRefusesExtraction() {
        FluidStoreComponent store = waterOnly();
        store.tank().setContents(SimpleFluidKey.of(Fluids.WATER), 4_000);

        assertThat(store.fluid(net.minecraft.core.Direction.DOWN).extract(SimpleFluidKey.of(Fluids.WATER), 1_000, false))
                .isZero();
        assertThat(store.tank().getAmount()).isEqualTo(4_000);
        // The machine still drains internally.
        assertThat(store.tank().extract(SimpleFluidKey.of(Fluids.WATER), 1_000, false)).isEqualTo(1_000);
    }

    @Test
    void resizeGrowKeepsContents() {
        FluidStoreComponent store = store();
        store.tank().setContents(SimpleFluidKey.of(Fluids.WATER), 4_000);

        store.resize(20_000);

        assertThat(store.tank().getCapacity()).isEqualTo(20_000);
        assertThat(store.tank().getAmount()).isEqualTo(4_000);
        assertThat(store.tank().insert(SimpleFluidKey.of(Fluids.WATER), 50_000, false)).isEqualTo(16_000);
    }

    @Test
    void resizeShrinkClampsContents() {
        int[] changes = {0};
        FluidStoreComponent store = new FluidStoreComponent("fluid", 10_000, () -> changes[0]++);
        store.tank().setContents(SimpleFluidKey.of(Fluids.WATER), 8_000);
        changes[0] = 0;

        store.resize(3_000);

        assertThat(store.tank().getAmount()).isEqualTo(3_000);
        assertThat(store.tank().getFluidKey()).isEqualTo(SimpleFluidKey.of(Fluids.WATER));
        assertThat(store.tank().insert(SimpleFluidKey.of(Fluids.WATER), 1_000, false)).isZero();
        assertThat(changes[0]).isEqualTo(1);
    }

    @Test
    void resizeToZeroEmptiesTheTank() {
        FluidStoreComponent store = store();
        store.tank().setContents(SimpleFluidKey.of(Fluids.LAVA), 2_000);

        store.resize(0);

        assertThat(store.tank().isEmpty()).isTrue();
        assertThat(store.tank().getAmount()).isZero();
        assertThat(store.tank().insert(SimpleFluidKey.of(Fluids.WATER), 1_000, false)).isZero();
    }

    @Test
    void resizeToTheSameCapacityDoesNotMarkDirty() {
        int[] changes = {0};
        FluidStoreComponent store = new FluidStoreComponent("fluid", 10_000, () -> changes[0]++);

        store.resize(10_000);

        assertThat(changes[0]).isZero();
    }

    @Test
    void resizeRejectsNegativeCapacity() {
        FluidStoreComponent store = store();

        assertThatThrownBy(() -> store.resize(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.tank().getCapacity()).isEqualTo(10_000);
    }
}
