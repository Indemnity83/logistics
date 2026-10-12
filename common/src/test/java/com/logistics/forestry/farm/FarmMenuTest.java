package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The shared farm menu: shift-click conserves items (mirroring {@code MachineMenuShiftClickTest})
 * and routes them by the farm's per-slot filters, and the synced data decodes to the farm's state.
 * Server-side cases run over a real {@link MachineEntity} built from {@link FarmMachine#assemble},
 * so the menu sees the same {@code canPlaceItem} filters hoppers and pipes do.
 */
@DisplayName("Farm menu")
class FarmMenuTest extends MinecraftTestEnvironment {

    private static final int MACHINE_SLOTS = FarmMachine.SLOT_COUNT;
    private static final int PLAYER_SLOTS = 36;
    private static final int FIRST_MAIN_INVENTORY_SLOT = 9;

    /** Handed to {@link TestFarm#configure}, which runs inside the super constructor before fields exist. */
    private static FarmLogic nextLogic;
    private static FarmSettings nextSettings = FarmHarness.SETTINGS;

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A farm block entity on a vanilla block entity type, so no mod registration is needed. */
    private static final class TestFarm extends MachineEntity {
        private FarmMachine parts;

        TestFarm() {
            super(BlockEntityTypes.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        protected void configure(MachineBuilder machine) {
            FarmSettings settings = nextSettings;
            parts = FarmMachine.assemble(machine, nextLogic, () -> settings, (ctx, owner) -> null, FarmHarness::isFertilizer);
        }

        @Override
        public MenuProvider createMenuProvider() {
            return null;
        }
    }

    private static TestFarm farm(FarmLogic logic) {
        return farm(logic, FarmHarness.SETTINGS);
    }

    private static TestFarm farm(FarmLogic logic, FarmSettings settings) {
        nextLogic = logic;
        nextSettings = settings;
        return new TestFarm();
    }

    private static FarmMenu serverMenu(TestFarm farm, Inventory inventory) {
        return new FarmMenu(1, inventory, farm, FarmMenu.dataSource(farm.parts));
    }

    private static Inventory playerInventory() {
        return new Inventory(null, new EntityEquipment());
    }

    /** The first hotbar slot as a menu index: the last nine menu slots are the hotbar. */
    private static int hotbar(FarmMenu menu) {
        return menu.slots.size() - 9;
    }

    // ==================== Conservation (client-side menu, unfiltered container) ====================

    @Test
    @DisplayName("shift-clicking every player slot in turn neither duplicates nor destroys")
    void shiftClickingEveryPlayerSlotConservesItems() {
        Inventory inventory = playerInventory();
        inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        inventory.setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.COBBLESTONE, 32));
        inventory.setItem(FIRST_MAIN_INVENTORY_SLOT + 1, new ItemStack(Items.IRON_INGOT, 5));
        FarmMenu menu = new FarmMenu(1, inventory);
        int expected = totalItems(menu, inventory);

        for (int pass = 1; pass <= 2; pass++) {
            for (int slot = MACHINE_SLOTS; slot < menu.slots.size(); slot++) {
                menu.quickMoveStack(null, slot);
                assertThat(totalItems(menu, inventory))
                        .as("items after shift-clicking slot %d on pass %d", slot, pass)
                        .isEqualTo(expected);
            }
        }
    }

    @Test
    @DisplayName("shift-clicking a machine slot hands its contents to the player exactly once")
    void shiftClickingAMachineSlotConservesItems() {
        Inventory inventory = playerInventory();
        FarmMenu menu = new FarmMenu(1, inventory);
        for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
            menu.getSlot(slot).set(new ItemStack(Items.COBBLESTONE, 17));
        }
        int expected = totalItems(menu, inventory);

        for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
            menu.quickMoveStack(null, slot);
            assertThat(totalItems(menu, inventory)).as("items after shift-clicking machine slot %d", slot).isEqualTo(expected);
            assertThat(menu.getSlot(slot).getItem().isEmpty()).as("machine slot %d emptied", slot).isTrue();
        }
        assertThat(countOf(inventory, Items.COBBLESTONE)).isEqualTo(expected);
    }

    @Test
    @DisplayName("shift-clicking with nowhere to put the stack leaves everything untouched")
    void shiftClickingWithNoRoomChangesNothing() {
        Inventory inventory = playerInventory();
        for (int slot = 0; slot < PLAYER_SLOTS; slot++) {
            inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        FarmMenu menu = new FarmMenu(1, inventory);
        for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
            menu.getSlot(slot).set(new ItemStack(Items.STONE, 64));
        }
        int expected = totalItems(menu, inventory);

        for (int slot = 0; slot < menu.slots.size(); slot++) {
            assertThat(menu.quickMoveStack(null, slot)).as("slot %d", slot).isEqualTo(ItemStack.EMPTY);
            assertThat(totalItems(menu, inventory)).as("items after a refused shift-click of slot %d", slot).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("a shift-click the destination can only partly absorb keeps the remainder")
    void partiallyAbsorbedShiftClickKeepsTheRemainder() {
        Inventory inventory = playerInventory();
        for (int slot = FIRST_MAIN_INVENTORY_SLOT; slot < PLAYER_SLOTS; slot++) {
            inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, 63));
        }
        inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        FarmMenu menu = new FarmMenu(1, inventory);
        for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
            menu.getSlot(slot).set(new ItemStack(Items.COBBLESTONE, 60));
        }
        int expected = totalItems(menu, inventory);

        menu.quickMoveStack(null, hotbar(menu));

        assertThat(totalItems(menu, inventory)).isEqualTo(expected);
        assertThat(inventory.getItem(0).getCount()).isBetween(1, 63);
    }

    // ==================== Routing (server-side menu over a real farm) ====================

    @Test
    @DisplayName("germlings shift-click into the germling slots")
    void germlingsRouteToGermlingSlots() {
        TestFarm farm = farm(new ScriptedFarmLogic(false, true));
        Inventory inventory = playerInventory();
        inventory.setItem(0, new ItemStack(Items.OAK_SAPLING, 20));
        FarmMenu menu = serverMenu(farm, inventory);

        menu.quickMoveStack(null, hotbar(menu));

        assertThat(farm.getItem(FarmMachine.GERMLING_START).is(Items.OAK_SAPLING)).isTrue();
        assertThat(farm.getItem(FarmMachine.GERMLING_START).getCount()).isEqualTo(20);
        assertThat(inventory.getItem(0).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("fertilizer shift-clicks into the fertilizer slot, not the germling slots")
    void fertilizerRoutesToFertilizerSlot() {
        TestFarm farm = farm(new ScriptedFarmLogic(false, true));
        Inventory inventory = playerInventory();
        inventory.setItem(0, new ItemStack(Items.BONE_MEAL, 70));
        FarmMenu menu = serverMenu(farm, inventory);

        menu.quickMoveStack(null, hotbar(menu));

        assertThat(farm.getItem(FarmMachine.FERTILIZER_SLOT).getCount()).isEqualTo(64);
        assertThat(inventory.getItem(0).getCount()).as("the rest stays: no other slot takes fertilizer").isEqualTo(6);
        for (int slot = FarmMachine.GERMLING_START; slot < FarmMachine.SOIL_END; slot++) {
            assertThat(farm.getItem(slot).isEmpty()).as("slot %d", slot).isTrue();
        }
    }

    @Test
    @DisplayName("soil shift-clicks into the soil slots of a farm that lays soil, and nowhere otherwise")
    void soilRoutesOnlyWhenTheFarmLaysSoil() {
        TestFarm laying = farm(new ScriptedFarmLogic(false, true));
        Inventory inventory = playerInventory();
        inventory.setItem(0, new ItemStack(Items.DIRT, 10));
        FarmMenu menu = serverMenu(laying, inventory);

        menu.quickMoveStack(null, hotbar(menu));

        assertThat(laying.getItem(FarmMachine.SOIL_START).is(Items.DIRT)).isTrue();
        assertThat(menu.getSlot(FarmMachine.SOIL_START).isActive()).isTrue();

        TestFarm notLaying = farm(new ScriptedFarmLogic(false, false));
        Inventory other = playerInventory();
        other.setItem(0, new ItemStack(Items.DIRT, 10));
        FarmMenu otherMenu = serverMenu(notLaying, other);

        assertThat(otherMenu.quickMoveStack(null, hotbar(otherMenu))).isEqualTo(ItemStack.EMPTY);
        assertThat(other.getItem(0).getCount()).isEqualTo(10);
        assertThat(notLaying.isEmpty()).isTrue();
        assertThat(otherMenu.getSlot(FarmMachine.SOIL_START).isActive()).isFalse();
        assertThat(otherMenu.getSlot(FarmMachine.SOIL_START).mayPlace(new ItemStack(Items.DIRT))).isFalse();
    }

    @Test
    @DisplayName("an item no slot accepts stays with the player")
    void unacceptedItemsStay() {
        TestFarm farm = farm(new ScriptedFarmLogic(false, true));
        Inventory inventory = playerInventory();
        inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        FarmMenu menu = serverMenu(farm, inventory);

        assertThat(menu.quickMoveStack(null, hotbar(menu))).isEqualTo(ItemStack.EMPTY);
        assertThat(inventory.getItem(0).getCount()).isEqualTo(10);
        assertThat(farm.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("output slots are take-only and shift-click to the player")
    void outputIsTakeOnly() {
        TestFarm farm = farm(new ScriptedFarmLogic());
        Inventory inventory = playerInventory();
        FarmMenu menu = serverMenu(farm, inventory);
        farm.setItem(FarmMachine.OUTPUT_START, new ItemStack(Items.WHEAT, 12));

        assertThat(menu.getSlot(FarmMachine.OUTPUT_START).mayPlace(new ItemStack(Items.WHEAT))).isFalse();
        menu.quickMoveStack(null, FarmMachine.OUTPUT_START);

        assertThat(farm.getItem(FarmMachine.OUTPUT_START).isEmpty()).isTrue();
        assertThat(countOf(inventory, Items.WHEAT)).isEqualTo(12);
    }

    @Test
    @DisplayName("slots sit at the approved layout positions")
    void slotPositions() {
        FarmMenu menu = new FarmMenu(1, playerInventory());

        assertThat(menu.getSlot(0).x).isEqualTo(42);
        assertThat(menu.getSlot(3).x).isEqualTo(96);
        assertThat(menu.getSlot(3).y).isEqualTo(17);
        assertThat(menu.getSlot(4).y).isEqualTo(35);
        assertThat(menu.getSlot(FarmMachine.FERTILIZER_SLOT).x).isEqualTo(42);
        assertThat(menu.getSlot(FarmMachine.FERTILIZER_SLOT).y).isEqualTo(53);
        assertThat(menu.getSlot(FarmMachine.OUTPUT_START).x).isEqualTo(116);
        assertThat(menu.getSlot(FarmMachine.OUTPUT_START).y).isEqualTo(17);
        assertThat(menu.getSlot(FarmMachine.OUTPUT_END - 1).x).isEqualTo(152);
        assertThat(menu.getSlot(FarmMachine.OUTPUT_END - 1).y).isEqualTo(53);
        assertThat(menu.getSlot(MACHINE_SLOTS).y).isEqualTo(84);
        assertThat(menu.getSlot(hotbar(menu)).y).isEqualTo(142);
    }

    // ==================== Synced data ====================

    @Test
    @DisplayName("the synced data decodes to the farm's energy, water, status, plot and flags")
    void syncedData() {
        // Values above a short's range, so the wide slot pairs are exercised.
        FarmSettings settings = new FarmSettings(9, 10, 100_000, 128, 40, 100, 60, 60, 20, 120, 40_000, 50, 4, 20, 200);
        TestFarm farm = farm(new ScriptedFarmLogic(true, false), settings);
        farm.parts.energy().setAmount(70_000);
        farm.parts.water().tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(33_000), false);
        ContainerData data = FarmMenu.dataSource(farm.parts);
        FarmMenu menu = new FarmMenu(1, playerInventory(), farm, data);

        assertThat(data.getCount()).isEqualTo(FarmMenu.DATA_COUNT);
        assertThat(menu.energy()).isEqualTo(70_000);
        assertThat(menu.energyCapacity()).isEqualTo(100_000);
        assertThat(menu.waterMb()).isEqualTo(33_000);
        assertThat(menu.waterCapacityMb()).isEqualTo(40_000);
        assertThat(menu.status()).isEqualTo(FarmStatus.IDLE);
        assertThat(menu.areaSize()).isEqualTo(9);
        assertThat(menu.usesWater()).isTrue();
        assertThat(menu.laysSoil()).isFalse();
        assertThat(menu.fertilizerCharges()).isZero();
        assertThat(menu.fertilizerChargeCapacity()).isPositive();
    }

    @Test
    @DisplayName("a farm without water syncs an empty, zero-capacity tank")
    void noWaterSyncsZero() {
        TestFarm farm = farm(new ScriptedFarmLogic(false, true));
        FarmMenu menu = serverMenu(farm, playerInventory());

        assertThat(menu.waterMb()).isZero();
        assertThat(menu.waterCapacityMb()).isZero();
        assertThat(menu.usesWater()).isFalse();
        assertThat(menu.laysSoil()).isTrue();
    }

    // ==================== Helpers ====================

    private static int totalItems(FarmMenu menu, Inventory inventory) {
        int total = 0;
        for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
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
}
