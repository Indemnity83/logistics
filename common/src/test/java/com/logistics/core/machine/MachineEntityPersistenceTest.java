package com.logistics.core.machine;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Test;

/**
 * Persistence of a {@link MachineEntity} composed from the real wrapper components, including
 * migration from the pre-component save format (root {@code Inventory}/{@code Energy} keys) that
 * machines like the Macerator used before this refactor.
 */
class MachineEntityPersistenceTest extends MinecraftTestEnvironment {

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    /** A processing-machine shell backed by a vanilla BE type so no mod registration is needed. */
    private static final class TestMachine extends MachineEntity {
        TestMachine() {
            super(BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        protected void configure(MachineBuilder machine) {
            machine.energy("energy").capacity(10_000).maxInput(128).providesDemand().build();
            machine.items("inventory").slots(SlotRole.INPUT, SlotRole.OUTPUT).bottomOutAccess().build();
        }

        void save(CompoundTag tag) {
            saveLogisticsData(tag, registries());
        }

        void load(CompoundTag tag) {
            loadLogisticsData(tag, registries());
        }

        @Override
        public MenuProvider createMenuProvider() {
            return null;
        }
    }

    @Test
    void newFormatRoundTripsEnergyAndItems() {
        TestMachine writer = new TestMachine();
        writer.energyStorage(null).insert(100, false); // a single insert is capped at maxInsert (128)
        writer.setItem(0, new ItemStack(Items.RAW_IRON, 3));

        CompoundTag saved = new CompoundTag();
        writer.save(saved);
        assertThat(saved.getCompound("components")).isPresent();

        TestMachine reader = new TestMachine();
        reader.load(saved);

        assertThat(reader.energyStorage(null).getAmount()).isEqualTo(100);
        assertThat(reader.getItem(0).getItem()).isEqualTo(Items.RAW_IRON);
        assertThat(reader.getItem(0).getCount()).isEqualTo(3);
    }

    @Test
    void migratesLegacyRootKeys() {
        // Build a pre-component save: Inventory + Energy + ProcessProgress at the LogisticsData root.
        CompoundTag legacy = new CompoundTag();
        EnergyComponent energy = new EnergyComponent(10_000, 128, 0, () -> {});
        energy.setAmount(4_200);
        energy.writeNbt(legacy, "Energy");
        ItemInventoryComponent inventory = new ItemInventoryComponent(2, () -> {});
        inventory.setItem(0, new ItemStack(Items.RAW_IRON, 7));
        inventory.writeNbt(legacy, "Inventory", registries);
        legacy.putInt("ProcessProgress", 5); // dead under RF-cost; must be ignored

        TestMachine reader = new TestMachine();
        reader.load(legacy);

        assertThat(reader.energyStorage(null).getAmount()).isEqualTo(4_200);
        assertThat(reader.getItem(0).getItem()).isEqualTo(Items.RAW_IRON);
        assertThat(reader.getItem(0).getCount()).isEqualTo(7);
    }
}
