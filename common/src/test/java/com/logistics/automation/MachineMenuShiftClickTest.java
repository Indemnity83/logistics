package com.logistics.automation;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.alloysmelter.AlloySmelterScreenHandler;
import com.logistics.automation.crucible.CrucibleScreenHandler;
import com.logistics.automation.fabricator.SequentialFabricatorScreenHandler;
import com.logistics.automation.kiln.KilnScreenHandler;
import com.logistics.automation.macerator.MaceratorScreenHandler;
import com.logistics.automation.refinery.RefineryScreenHandler;
import com.logistics.automation.sawmill.SawmillScreenHandler;
import com.logistics.automation.transposer.TransposerScreenHandler;
import com.logistics.core.lib.block.MachineResultSlot;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Shift-clicking in a machine GUI must conserve items: whatever leaves a slot has to turn up in
 * exactly one other slot, never in two and never nowhere.
 *
 * <p>Regression cover for #846, where the Refinery — the one machine with no player-fillable slot —
 * passed the player-inventory range to {@code moveItemStackTo} as both the implicit source and the
 * destination. Vanilla's merge loop reached the slot the stack came from and grew that live
 * {@code ItemStack} into itself, doubling the count on every shift-click. The contract is asserted
 * across every machine menu rather than only the one that broke, because the defect is a property of
 * the slot ranges each {@code quickMoveStack} passes, and every menu writes those ranges by hand.
 *
 * <p>These drive the menus headlessly: the client-side constructor builds its own backing container,
 * and {@code quickMoveStack} needs no player except through {@link MachineResultSlot#onTake}, which
 * awards banked smelting XP and so requires a live level. Those two result slots (Kiln, Macerator)
 * are therefore exercised from the player side only.
 */
@DisplayName("Machine menu shift-click")
class MachineMenuShiftClickTest extends MinecraftTestEnvironment {

    private static final int PLAYER_SLOTS = 36;
    /** Hotbar occupies inventory indices 0-8; the main inventory 9-35. */
    private static final int FIRST_MAIN_INVENTORY_SLOT = 9;

    static Stream<MenuCase> menus() {
        return Stream.of(
                new MenuCase("kiln", inventory -> new KilnScreenHandler(1, inventory)),
                new MenuCase("macerator", inventory -> new MaceratorScreenHandler(1, inventory)),
                new MenuCase("sawmill", inventory -> new SawmillScreenHandler(1, inventory)),
                new MenuCase("alloy smelter", inventory -> new AlloySmelterScreenHandler(1, inventory)),
                new MenuCase("crucible", inventory -> new CrucibleScreenHandler(1, inventory)),
                new MenuCase("transposer", inventory -> new TransposerScreenHandler(1, inventory)),
                new MenuCase("refinery", inventory -> new RefineryScreenHandler(1, inventory)),
                new MenuCase("sequential fabricator",
                        inventory -> new SequentialFabricatorScreenHandler(1, inventory)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("menus")
    @DisplayName("shift-clicking every player slot in turn neither duplicates nor destroys")
    void shiftClickingEveryPlayerSlotConservesItems(MenuCase menuCase) {
        Inventory inventory = playerInventory();
        // Partial stacks in both halves of the player inventory, so a same-item merge target exists
        // inside the source's own range as well as outside it — the shape that doubled in #846.
        inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        inventory.setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.COBBLESTONE, 32));
        inventory.setItem(FIRST_MAIN_INVENTORY_SLOT + 1, new ItemStack(Items.IRON_INGOT, 5));
        AbstractContainerMenu menu = menuCase.open(inventory);
        int expected = totalItems(menu, inventory);

        // Twice over: the second pass shift-clicks whatever the first pass left in each slot.
        for (int pass = 1; pass <= 2; pass++) {
            for (int slot = machineSlotCount(menu); slot < menu.slots.size(); slot++) {
                menu.quickMoveStack(null, slot);
                assertThat(totalItems(menu, inventory))
                        .as("items after shift-clicking slot %d on pass %d", slot, pass)
                        .isEqualTo(expected);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("menus")
    @DisplayName("shift-clicking a machine slot hands its contents to the player exactly once")
    void shiftClickingAMachineSlotConservesItems(MenuCase menuCase) {
        Inventory inventory = playerInventory();
        AbstractContainerMenu menu = menuCase.open(inventory);
        for (int slot = 0; slot < machineSlotCount(menu); slot++) {
            if (isExperienceResultSlot(menu, slot)) continue;
            menu.getSlot(slot).set(new ItemStack(Items.COBBLESTONE, 17));
        }
        int expected = totalItems(menu, inventory);

        for (int slot = 0; slot < machineSlotCount(menu); slot++) {
            if (isExperienceResultSlot(menu, slot)) continue;
            menu.quickMoveStack(null, slot);
            assertThat(totalItems(menu, inventory))
                    .as("items after shift-clicking machine slot %d", slot)
                    .isEqualTo(expected);
            assertThat(menu.getSlot(slot).getItem().isEmpty())
                    .as("machine slot %d emptied into the player inventory", slot)
                    .isTrue();
        }
        assertThat(countOf(inventory, Items.COBBLESTONE)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("menus")
    @DisplayName("shift-clicking with nowhere to put the stack leaves everything untouched")
    void shiftClickingWithNoRoomChangesNothing(MenuCase menuCase) {
        Inventory inventory = playerInventory();
        for (int slot = 0; slot < PLAYER_SLOTS; slot++) {
            inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        AbstractContainerMenu menu = menuCase.open(inventory);
        for (int slot = 0; slot < machineSlotCount(menu); slot++) {
            menu.getSlot(slot).set(new ItemStack(Items.STONE, 64));
        }
        int expected = totalItems(menu, inventory);

        for (int slot = 0; slot < menu.slots.size(); slot++) {
            if (isExperienceResultSlot(menu, slot)) continue;
            assertThat(menu.quickMoveStack(null, slot))
                    .as("shift-clicking slot %d with no room reports nothing moved", slot)
                    .isEqualTo(ItemStack.EMPTY);
            assertThat(totalItems(menu, inventory))
                    .as("items after a refused shift-click of slot %d", slot)
                    .isEqualTo(expected);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("menus")
    @DisplayName("a shift-click the destination can only partly absorb keeps the remainder")
    void partiallyAbsorbedShiftClickKeepsTheRemainder(MenuCase menuCase) {
        Inventory inventory = playerInventory();
        // Every destination is nearly full, so the move stops part-way through the stack. Clearing the
        // source slot for a move that only partly succeeded would destroy whatever did not fit.
        for (int slot = FIRST_MAIN_INVENTORY_SLOT; slot < PLAYER_SLOTS; slot++) {
            inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, 63));
        }
        inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        AbstractContainerMenu menu = menuCase.open(inventory);
        for (int slot = 0; slot < machineSlotCount(menu); slot++) {
            menu.getSlot(slot).set(new ItemStack(Items.COBBLESTONE, 60));
        }
        int expected = totalItems(menu, inventory);

        // The first hotbar slot: the last nine menu slots are the hotbar.
        menu.quickMoveStack(null, menu.slots.size() - 9);

        assertThat(totalItems(menu, inventory)).as("items after a partly absorbed shift-click").isEqualTo(expected);
        assertThat(inventory.getItem(0).getCount())
                .as("the destinations absorb part of the stack and no more — otherwise this case proves nothing")
                .isBetween(1, 63);
    }

    @Test
    @DisplayName("refinery: a main-inventory stack moves to the hotbar rather than doubling in place")
    void refineryShiftClickDoesNotDoubleTheStack() {
        Inventory inventory = playerInventory();
        inventory.setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.COBBLESTONE, 32));
        RefineryScreenHandler menu = new RefineryScreenHandler(1, inventory);

        menu.quickMoveStack(null, machineSlotCount(menu));

        assertThat(countOf(inventory, Items.COBBLESTONE)).as("cobblestone in the player inventory").isEqualTo(32);
        assertThat(inventory.getItem(FIRST_MAIN_INVENTORY_SLOT).isEmpty()).as("source slot emptied").isTrue();
        assertThat(inventory.getItem(0).getCount()).as("stack landed in the hotbar").isEqualTo(32);
    }

    // ==================== Helpers ====================

    /** A headless player inventory — nothing on the shift-click path reads the owning player. */
    private static Inventory playerInventory() {
        return new Inventory(null, new EntityEquipment());
    }

    /** Machine slots come first; the trailing 36 are views of the player inventory. */
    private static int machineSlotCount(AbstractContainerMenu menu) {
        return menu.slots.size() - PLAYER_SLOTS;
    }

    /** Result slots that award banked XP dereference the player, so they need a live level. */
    private static boolean isExperienceResultSlot(AbstractContainerMenu menu, int slot) {
        return menu.getSlot(slot) instanceof MachineResultSlot;
    }

    /** Every item the player can reach: the machine's own slots plus the whole player inventory. */
    private static int totalItems(AbstractContainerMenu menu, Inventory inventory) {
        int total = 0;
        for (int slot = 0; slot < machineSlotCount(menu); slot++) {
            total += menu.getSlot(slot).getItem().getCount();
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            total += inventory.getItem(slot).getCount();
        }
        return total;
    }

    private static int countOf(Inventory inventory, Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    /** One machine GUI under test, opened over a supplied player inventory. */
    record MenuCase(String name, Function<Inventory, AbstractContainerMenu> factory) {
        AbstractContainerMenu open(Inventory inventory) {
            return factory.apply(inventory);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
