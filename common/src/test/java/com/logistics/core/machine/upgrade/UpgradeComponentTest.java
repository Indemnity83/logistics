package com.logistics.core.machine.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsCore;
import com.logistics.core.lib.storage.ContainerItemStorage;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineComponentContainer;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The upgrade slot, the modifiers it produces, and the change signal machines react to. */
class UpgradeComponentTest extends MinecraftTestEnvironment {

    private static final int SEED_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int UPGRADE_SLOT = 2;

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A farm-shaped machine: one input, one output and one upgrade slot. */
    private static final class UpgradedMachine extends MachineEntity {
        UpgradeComponent upgrades;
        ItemStoreComponent items;
        int syncs;

        UpgradedMachine() {
            super(BlockEntityTypes.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        protected void configure(MachineBuilder machine) {
            items = machine.items("inventory")
                    .slots(SlotRole.INPUT, SlotRole.OUTPUT, SlotRole.UPGRADE)
                    .anyFaceAccess()
                    .build();
            upgrades = machine.upgrades("upgrades", items);
        }

        @Override
        public void sync() {
            syncs++;
            super.sync();
        }

        @Override
        public MenuProvider createMenuProvider() {
            return null;
        }
    }

    private static ItemStack rangeUpgrades(int count) {
        return new ItemStack(LogisticsCore.ITEM.RANGE_UPGRADE, count);
    }

    private static MachineBuilder builder() {
        return new MachineBuilder(new MachineComponentContainer(), () -> {});
    }

    @Test
    void unboundUpgradesAreIdentity() {
        UpgradeComponent upgrades = builder().upgrades("upgrades");

        assertThat(upgrades.modifiers()).isSameAs(MachineModifiers.identity());
    }

    @Test
    void rangeSumsEveryInstalledCopy() {
        UpgradedMachine machine = new UpgradedMachine();
        MachineModifiers modifiers = machine.upgrades.modifiers();

        assertThat(modifiers.range(MachineOperations.WORK_AREA)).isZero();
        machine.setItem(UPGRADE_SLOT, rangeUpgrades(1));
        assertThat(modifiers.range(MachineOperations.WORK_AREA)).isEqualTo(2);
        machine.setItem(UPGRADE_SLOT, rangeUpgrades(2));
        assertThat(modifiers.range(MachineOperations.WORK_AREA)).isEqualTo(4);
    }

    @Test
    void rangeUpgradesLeaveEverythingElseUnchanged() {
        UpgradedMachine machine = new UpgradedMachine();
        machine.setItem(UPGRADE_SLOT, rangeUpgrades(2));
        MachineModifiers modifiers = machine.upgrades.modifiers();
        OperationKey other = () -> "test.other";

        assertThat(modifiers.range(other)).isZero();
        assertThat(modifiers.cost(MachineOperations.WORK_AREA)).isEqualTo(1.0);
        assertThat(modifiers.speed(MachineOperations.WORK_AREA)).isEqualTo(1.0);
        assertThat(modifiers.capacity(10_000)).isEqualTo(10_000);
        assertThat(modifiers.receiveRate(128)).isEqualTo(128);
    }

    @Test
    void copiesBeyondTheSlotMaximumDoNotCount() {
        UpgradedMachine machine = new UpgradedMachine();
        machine.setItem(UPGRADE_SLOT, rangeUpgrades(5));

        assertThat(machine.upgrades.modifiers().range(MachineOperations.WORK_AREA)).isEqualTo(4);
    }

    @Test
    void modifiersIsOneObjectThatFollowsTheSlot() {
        UpgradedMachine machine = new UpgradedMachine();
        MachineModifiers captured = machine.upgrades.modifiers();

        machine.setItem(UPGRADE_SLOT, rangeUpgrades(1));
        assertThat(machine.upgrades.modifiers()).isSameAs(captured);
        assertThat(captured.range(MachineOperations.WORK_AREA)).isEqualTo(2);

        machine.removeItem(UPGRADE_SLOT, 1);
        assertThat(captured.range(MachineOperations.WORK_AREA)).isZero();
    }

    @Test
    void upgradeSlotAcceptsOnlyUpgrades() {
        UpgradedMachine machine = new UpgradedMachine();

        assertThat(machine.canPlaceItem(UPGRADE_SLOT, rangeUpgrades(1))).isTrue();
        assertThat(machine.canPlaceItem(UPGRADE_SLOT, new ItemStack(Items.DIRT))).isFalse();
        // Other slots keep accepting anything.
        assertThat(machine.canPlaceItem(SEED_SLOT, new ItemStack(Items.DIRT))).isTrue();
    }

    @Test
    void upgradeSlotHoldsAtMostTwo() {
        UpgradedMachine machine = new UpgradedMachine();
        IItemKey upgrade = ItemStorageLookup.of(rangeUpgrades(1));

        assertThat(machine.slotStackLimit(UPGRADE_SLOT, rangeUpgrades(1))).isEqualTo(2);
        assertThat(machine.slotStackLimit(SEED_SLOT, rangeUpgrades(1))).isEqualTo(64);

        // Unsided access reaches every slot; the upgrade slot still stops at two.
        ContainerItemStorage unsided = new ContainerItemStorage(machine, null);
        assertThat(unsided.insert(UPGRADE_SLOT, upgrade, 5, false)).isEqualTo(2);
        assertThat(unsided.insert(UPGRADE_SLOT, upgrade, 5, false)).isZero();
        assertThat(machine.getItem(UPGRADE_SLOT).getCount()).isEqualTo(2);
        assertThat(unsided.slotCapacity(UPGRADE_SLOT)).isEqualTo(2);
    }

    @Test
    void noFaceReachesTheUpgradeSlot() {
        UpgradedMachine machine = new UpgradedMachine();
        machine.setItem(UPGRADE_SLOT, rangeUpgrades(2));
        IItemKey upgrade = ItemStorageLookup.of(rangeUpgrades(1));

        for (Direction face : Direction.values()) {
            assertThat(machine.getSlotsForFace(face)).doesNotContain(UPGRADE_SLOT);
            assertThat(new ContainerItemStorage(machine, face).extract(upgrade, 2, true)).isZero();
        }
        assertThat(machine.getItem(OUTPUT_SLOT).isEmpty()).isTrue();
    }

    @Test
    void changesSyncAndNotifyOnTheNextTick() {
        UpgradedMachine machine = new UpgradedMachine();
        int[] fired = {0};
        machine.upgrades.onChange(() -> fired[0]++);
        IItemKey upgrade = ItemStorageLookup.of(rangeUpgrades(1));
        ContainerItemStorage unsided = new ContainerItemStorage(machine, null);

        machine.upgrades.serverTick(machine);
        assertThat(fired[0]).isZero();
        assertThat(machine.syncs).isZero();

        unsided.insert(UPGRADE_SLOT, upgrade, 1, false);
        machine.upgrades.serverTick(machine);
        assertThat(fired[0]).isEqualTo(1);
        assertThat(machine.syncs).isEqualTo(1);

        machine.upgrades.serverTick(machine);
        assertThat(fired[0]).as("an unchanged slot does not re-fire").isEqualTo(1);

        // A count change on the same stack is a change too.
        unsided.insert(UPGRADE_SLOT, upgrade, 1, false);
        machine.upgrades.serverTick(machine);
        assertThat(fired[0]).isEqualTo(2);

        unsided.extract(UPGRADE_SLOT, upgrade, 2, false);
        machine.upgrades.serverTick(machine);
        assertThat(fired[0]).isEqualTo(3);
        assertThat(machine.syncs).isEqualTo(3);
    }

    @Test
    void bindingToItemsWithoutUpgradeSlotsFails() {
        MachineBuilder machine = builder();
        ItemStoreComponent items = machine.items("inventory")
                .slots(SlotRole.INPUT, SlotRole.OUTPUT)
                .anyFaceAccess()
                .build();

        assertThatThrownBy(() -> machine.upgrades("upgrades", items)).isInstanceOf(IllegalStateException.class);
    }
}
