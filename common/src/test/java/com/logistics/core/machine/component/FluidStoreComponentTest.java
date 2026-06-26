package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

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
}
