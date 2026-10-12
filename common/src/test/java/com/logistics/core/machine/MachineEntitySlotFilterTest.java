package com.logistics.core.machine;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.storage.ContainerItemStorage;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Test;

/**
 * Per-slot filters and any-face access on a real {@link MachineEntity}, driven through
 * {@link ContainerItemStorage} the way pipes and capability adapters reach it.
 */
class MachineEntitySlotFilterTest extends MinecraftTestEnvironment {

    private static final int SEED_SLOT = 0;
    private static final int BONE_MEAL_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int UPGRADE_SLOT = 4;

    private static final IItemKey SEEDS = ItemStorageLookup.of(new ItemStack(Items.WHEAT_SEEDS));
    private static final IItemKey BONE_MEAL = ItemStorageLookup.of(new ItemStack(Items.BONE_MEAL));
    private static final IItemKey DIRT = ItemStorageLookup.of(new ItemStack(Items.DIRT));
    private static final IItemKey WHEAT = ItemStorageLookup.of(new ItemStack(Items.WHEAT));

    /** Seeds slot, bone meal slot, two outputs and a generic slot no face reaches. */
    private static final class FarmShell extends MachineEntity {
        FarmShell() {
            super(BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        protected void configure(MachineBuilder machine) {
            machine.items("inventory")
                    .slots(SlotRole.INPUT, SlotRole.INPUT, SlotRole.OUTPUT, SlotRole.OUTPUT, SlotRole.GENERIC)
                    .anyFaceAccess()
                    .slotFilter(SEED_SLOT, SEED_SLOT + 1, stack -> stack.is(Items.WHEAT_SEEDS))
                    .slotFilter(BONE_MEAL_SLOT, BONE_MEAL_SLOT + 1, stack -> stack.is(Items.BONE_MEAL))
                    .build();
        }

        @Override
        public MenuProvider createMenuProvider() {
            return null;
        }
    }

    /** A furnace-access machine with no slot filters, as every machine was before them. */
    private static final class PlainMachine extends MachineEntity {
        PlainMachine() {
            super(BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        protected void configure(MachineBuilder machine) {
            machine.items("inventory").slots(SlotRole.INPUT, SlotRole.OUTPUT).furnaceAccess().build();
        }

        @Override
        public MenuProvider createMenuProvider() {
            return null;
        }
    }

    @Test
    void insertionRoutesEachItemToItsSlotFromEveryFace() {
        for (Direction face : Direction.values()) {
            FarmShell machine = new FarmShell();
            ContainerItemStorage storage = new ContainerItemStorage(machine, face);

            assertThat(storage.insert(BONE_MEAL, 5, false)).isEqualTo(5);
            assertThat(storage.insert(SEEDS, 3, false)).isEqualTo(3);

            assertThat(machine.getItem(BONE_MEAL_SLOT).is(Items.BONE_MEAL)).isTrue();
            assertThat(machine.getItem(BONE_MEAL_SLOT).getCount()).isEqualTo(5);
            assertThat(machine.getItem(SEED_SLOT).is(Items.WHEAT_SEEDS)).isTrue();
            assertThat(machine.getItem(SEED_SLOT).getCount()).isEqualTo(3);
        }
    }

    @Test
    void itemsNoSlotAcceptsAreRefused() {
        FarmShell machine = new FarmShell();
        ContainerItemStorage storage = new ContainerItemStorage(machine, Direction.UP);

        assertThat(storage.insert(DIRT, 10, false)).isZero();
        assertThat(machine.isEmpty()).isTrue();
    }

    @Test
    void extractionReachesOnlyOutputSlots() {
        FarmShell machine = new FarmShell();
        machine.setItem(SEED_SLOT, new ItemStack(Items.WHEAT_SEEDS, 4));
        machine.setItem(OUTPUT_SLOT, new ItemStack(Items.WHEAT, 6));
        machine.setItem(UPGRADE_SLOT, new ItemStack(Items.WHEAT, 1));

        for (Direction face : Direction.values()) {
            ContainerItemStorage storage = new ContainerItemStorage(machine, face);
            assertThat(storage.extract(SEEDS, 4, true)).isZero();
            assertThat(storage.extract(WHEAT, 64, true)).isEqualTo(6);
        }
        assertThat(new ContainerItemStorage(machine, Direction.NORTH).extract(WHEAT, 64, false))
                .isEqualTo(6);
        assertThat(machine.getItem(UPGRADE_SLOT).getCount()).isEqualTo(1);
    }

    @Test
    void canPlaceItemAppliesTheSlotFilter() {
        FarmShell machine = new FarmShell();

        assertThat(machine.canPlaceItem(SEED_SLOT, new ItemStack(Items.WHEAT_SEEDS))).isTrue();
        assertThat(machine.canPlaceItem(SEED_SLOT, new ItemStack(Items.BONE_MEAL))).isFalse();
        assertThat(machine.canPlaceItem(BONE_MEAL_SLOT, new ItemStack(Items.BONE_MEAL))).isTrue();
        assertThat(machine.canPlaceItem(BONE_MEAL_SLOT, new ItemStack(Items.DIRT))).isFalse();
        // Unfiltered slots accept anything, as Container#canPlaceItem does by default.
        assertThat(machine.canPlaceItem(UPGRADE_SLOT, new ItemStack(Items.DIRT))).isTrue();
    }

    @Test
    void machinesWithoutSlotFiltersKeepTheirAccess() {
        PlainMachine machine = new PlainMachine();

        assertThat(machine.canPlaceItem(0, new ItemStack(Items.DIRT))).isTrue();
        assertThat(machine.canPlaceItem(1, new ItemStack(Items.DIRT))).isTrue();
        assertThat(new ContainerItemStorage(machine, Direction.UP).insert(DIRT, 3, false)).isEqualTo(3);
        assertThat(new ContainerItemStorage(machine, Direction.NORTH).insert(DIRT, 3, false)).isZero();
        assertThat(machine.getItem(0).getCount()).isEqualTo(3);
    }
}
