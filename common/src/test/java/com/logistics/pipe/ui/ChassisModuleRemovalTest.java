package com.logistics.pipe.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Taking a module out of a chassis pipe must run its teardown, whichever way the player takes it.
 *
 * <p>Regression cover for #929: shift-click emptied the slot's stack in place, so the removal was
 * invisible to {@link ChassisInventory#setItem} and the module's {@code onDetach} never ran — the
 * pipe stayed registered as a sink/provider for goods it no longer accepted. {@code ChassisInventory}
 * is the only caller of {@code Module#onDetach} in the mod, so this is the single point that decides
 * whether module teardown happens at all — and it must happen exactly once, never twice.
 */
@DisplayName("Chassis module removal")
class ChassisModuleRemovalTest extends MinecraftTestEnvironment {

    private static final Item MODULE = Items.STICK;
    private static final int CHASSIS_SLOTS = 1;
    private static final int PLAYER_SLOTS = 36;

    @Test
    @DisplayName("shift-clicking a module out detaches it exactly once")
    void shiftClickDetachesTheModule() {
        RecordingInventory inventory = new RecordingInventory();
        inventory.setItem(0, moduleStack());
        ChassisScreenHandler menu = menuFor(inventory, new SimpleContainer(PLAYER_SLOTS));

        menu.quickMoveStack(null, 0);

        assertThat(inventory.getItem(0).isEmpty()).as("chassis slot is empty after shift-click").isTrue();
        assertThat(playerHolds(menu)).as("module handed to the player").isTrue();
        assertThat(inventory.detached).as("modules detached").hasSize(1);
    }

    @Test
    @DisplayName("picking a module up by hand detaches it exactly once")
    void normalPickupDetachesTheModule() {
        RecordingInventory inventory = new RecordingInventory();
        inventory.setItem(0, moduleStack());
        ChassisScreenHandler menu = menuFor(inventory, new SimpleContainer(PLAYER_SLOTS));

        // What a plain left-click on a filled slot does: take the stack out of the container.
        ItemStack taken = menu.getSlot(0).remove(1);

        assertThat(taken.getItem()).isEqualTo(MODULE);
        assertThat(inventory.getItem(0).isEmpty()).as("chassis slot is empty after pickup").isTrue();
        assertThat(inventory.detached).as("modules detached").hasSize(1);
    }

    @Test
    @DisplayName("leaves the module attached when there is no room to shift-click it out")
    void noRoomLeavesTheModuleAttached() {
        RecordingInventory inventory = new RecordingInventory();
        inventory.setItem(0, moduleStack());
        SimpleContainer playerInventory = new SimpleContainer(PLAYER_SLOTS);
        for (int slot = 0; slot < PLAYER_SLOTS; slot++) {
            playerInventory.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        ChassisScreenHandler menu = menuFor(inventory, playerInventory);

        menu.quickMoveStack(null, 0);

        assertThat(inventory.getItem(0).isEmpty()).as("chassis slot still holds the module").isFalse();
        assertThat(inventory.detached).as("modules detached").isEmpty();
    }

    // ==================== Helpers ====================

    /**
     * Stands in for a module item. The domain's real module items are registered after this
     * environment binds data components, so an {@code ItemStack} cannot be built from one; the
     * removal path keys off the container slot rather than the item type, and the teardown hook is
     * recorded directly, so any single item serves. Same stand-in as {@code PipeModuleHelperTest}.
     */
    private static ItemStack moduleStack() {
        return new ItemStack(MODULE, 1);
    }

    private static ChassisScreenHandler menuFor(ChassisInventory inventory, SimpleContainer playerInventory) {
        return new ChassisScreenHandler(1, playerInventory, CHASSIS_SLOTS, null, inventory);
    }

    private static boolean playerHolds(ChassisScreenHandler menu) {
        for (int slot = CHASSIS_SLOTS; slot < CHASSIS_SLOTS + PLAYER_SLOTS; slot++) {
            if (menu.getSlot(slot).getItem().getItem() == MODULE) return true;
        }
        return false;
    }

    /**
     * A chassis inventory with no pipe entity behind it — every hook that needs a live world is a
     * no-op — that records the module teardown calls the menu drives.
     */
    private static final class RecordingInventory extends ChassisInventory {
        private final List<ItemStack> detached = new ArrayList<>();

        RecordingInventory() {
            super((PipeBlockEntity) null);
        }

        @Override
        void detachModule(ItemStack stack) {
            detached.add(stack.copy());
        }
    }
}
