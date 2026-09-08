package com.logistics.pipe.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.pipe.modules.SupplierModule;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A configured supply target must survive a GUI round-trip.
 *
 * <p>Regression cover for #930: the ghost slot's count is a display of the target capped at one
 * stack, so a target of 500 reopens as a 64-count ghost. Reading that back as the current target
 * silently rewrote the configuration — the first click after reopening dropped a 500-item target
 * to {@code 64 + cursor}.
 */
@DisplayName("Supplier stock target")
class SupplierTargetRoundTripTest extends MinecraftTestEnvironment {

    private static final String COBBLESTONE = "minecraft:cobblestone";
    private static final int CONFIGURED_TARGET = 500;

    @Test
    @DisplayName("accumulates from the configured target, not the capped ghost count")
    void firstClickAfterReopeningKeepsTheConfiguredTarget() {
        TestMenu menu = reopenedMenuWith(COBBLESTONE, CONFIGURED_TARGET);

        // The reopened GUI can only show one stack of the 500 it is configured for.
        assertThat(menu.getSlot(0).getItem().getCount()).isEqualTo(64);

        menu.setCarried(new ItemStack(Items.COBBLESTONE, 10));
        menu.handleCustomSlotClick(0, 0, false, null);

        assertThat(menu.savedItem(0)).isEqualTo(COBBLESTONE);
        assertThat(menu.savedAmount(0)).isEqualTo(510);
    }

    @Test
    @DisplayName("keeps accumulating across further clicks in the same session")
    void laterClicksAccumulateFromTheSavedTarget() {
        TestMenu menu = reopenedMenuWith(COBBLESTONE, CONFIGURED_TARGET);

        menu.setCarried(new ItemStack(Items.COBBLESTONE, 10));
        menu.handleCustomSlotClick(0, 0, false, null);
        menu.setCarried(new ItemStack(Items.COBBLESTONE, 10));
        menu.handleCustomSlotClick(0, 0, false, null);

        assertThat(menu.savedAmount(0)).isEqualTo(520);
    }

    @Test
    @DisplayName("caps at 999 rather than growing without bound")
    void accumulationStopsAtTheMaximum() {
        TestMenu menu = reopenedMenuWith(COBBLESTONE, 995);

        menu.setCarried(new ItemStack(Items.COBBLESTONE, 32));
        menu.handleCustomSlotClick(0, 0, false, null);

        assertThat(menu.savedAmount(0)).isEqualTo(999);
    }

    @Test
    @DisplayName("starts over at the cursor count when a different item is placed")
    void aDifferentItemReplacesTheTarget() {
        TestMenu menu = reopenedMenuWith(COBBLESTONE, CONFIGURED_TARGET);

        menu.setCarried(new ItemStack(Items.DIAMOND, 5));
        menu.handleCustomSlotClick(0, 0, false, null);

        assertThat(menu.savedItem(0)).isEqualTo("minecraft:diamond");
        assertThat(menu.savedAmount(0)).isEqualTo(5);
    }

    /** A menu whose ghost slots were just loaded from a configuration saved in an earlier session. */
    private static TestMenu reopenedMenuWith(String itemId, int amount) {
        TestMenu menu = new TestMenu();
        menu.saveSupplySlot(0, itemId, amount);
        menu.supplyInventory().loadFromItem(moduleItemWith(itemId, amount));
        return menu;
    }

    private static ItemStack moduleItemWith(String itemId, int amount) {
        CompoundTag slotTag = new CompoundTag();
        slotTag.putString("item", itemId);
        slotTag.putInt("amount", amount);
        CompoundTag supplies = new CompoundTag();
        supplies.put("0", slotTag);
        CompoundTag tag = new CompoundTag();
        tag.put(SupplierModule.SUPPLIES, supplies);

        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /**
     * Menu with the supply configuration held in memory, standing in for the pipe module state or the
     * module item's NBT that a real menu reads and writes.
     */
    private static final class TestMenu extends SupplierScreenHandler {
        private final String[] items = new String[SupplierModule.MAX_SUPPLY_SLOTS];
        private final int[] amounts = new int[SupplierModule.MAX_SUPPLY_SLOTS];

        TestMenu() {
            super(1, new SimpleContainer(36));
        }

        @Override
        int configuredAmount(int slotIndex) {
            return amounts[slotIndex];
        }

        @Override
        void saveSupplySlot(int slotIndex, String itemId, int amount) {
            items[slotIndex] = itemId;
            amounts[slotIndex] = amount;
        }

        String savedItem(int slotIndex) {
            return items[slotIndex];
        }

        int savedAmount(int slotIndex) {
            return amounts[slotIndex];
        }
    }
}
