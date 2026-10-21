package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.machine.MachineBuilder;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.farm.crop.CropTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The shared farm menu: shift-click conserves items (mirroring {@code MachineMenuShiftClickTest}),
 * and the synced data decodes to the farm's state. Server-side cases run over a real
 * {@link MachineEntity} built from {@link FarmMachine#assemble}.
 */
@DisplayName("Farm menu")
class FarmMenuTest extends MinecraftTestEnvironment {

    private static final int MACHINE_SLOTS = FarmMachine.SLOT_COUNT;
    private static final int PLAYER_SLOTS = 36;
    private static final int FIRST_MAIN_INVENTORY_SLOT = 9;

    /** Handed to {@link TestFarm#configure}, which runs inside the super constructor before fields exist. */
    private static TendLogic nextLogic;
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
            parts = FarmMachine.assemble(machine, nextLogic, () -> settings, (ctx, owner) -> null);
        }

        @Override
        public MenuProvider createMenuProvider() {
            return null;
        }
    }

    private static TestFarm farm(TendLogic logic, FarmSettings settings) {
        nextLogic = logic;
        nextSettings = settings;
        return new TestFarm();
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

    // ==================== Synced data ====================

    @Test
    @DisplayName("the synced data decodes to the farm's energy, status, plot and flags")
    void syncedData() {
        // Values above a short's range, so the wide slot pairs are exercised.
        FarmSettings settings = FarmSettings.defaults()
                .withTiers(FarmSettings.everyTier(new FarmTierProfile(16, 4, 100_000, 128)));
        TestFarm farm = farm(new CropTendLogic(), settings);
        farm.parts.energy().setAmount(70_000);
        ContainerData data = FarmMenu.dataSource(farm.parts);
        FarmMenu menu = new FarmMenu(1, playerInventory(), farm, data);

        assertThat(data.getCount()).isEqualTo(FarmMenu.DATA_COUNT);
        assertThat(menu.energy()).isEqualTo(70_000);
        assertThat(menu.energyCapacity()).isEqualTo(100_000);
        assertThat(menu.status()).isEqualTo(FarmStatus.IDLE);
        assertThat(menu.reachSize()).as("the Reach's side").isEqualTo(7);
        assertThat(menu.tier()).isEqualTo(MachineTier.BASIC);
        assertThat(menu.plotsTotal()).as("no Plots until a harvest").isZero();
        assertThat(menu.emptyPlots()).isZero();

        farm.parts.tier().setTier(MachineTier.PRECISION);
        assertThat(data.get(FarmMenu.TIER)).isEqualTo(MachineTier.PRECISION.rank());
        assertThat(menu.tier()).isEqualTo(MachineTier.PRECISION);
    }

    @Test
    @DisplayName("the tier and plot counts follow the earlier indices, and an unknown tier rank reads as Basic")
    void tierAndPlotIndices() {
        assertThat(FarmMenu.TIER).isEqualTo(FarmMenu.REACH_SIZE + 1);
        assertThat(FarmMenu.PLOTS_TOTAL).isEqualTo(FarmMenu.TIER + 1);
        assertThat(FarmMenu.PLOTS_EMPTY).isEqualTo(FarmMenu.PLOTS_TOTAL + 1);
        assertThat(FarmMenu.DATA_COUNT).isEqualTo(FarmMenu.PLOTS_EMPTY + 1);

        SimpleContainerData data = new SimpleContainerData(FarmMenu.DATA_COUNT);
        FarmMenu menu = new FarmMenu(1, playerInventory(), new SimpleContainer(FarmMachine.SLOT_COUNT), data);
        data.set(FarmMenu.TIER, MachineTier.INFERNAL.rank());
        data.set(FarmMenu.PLOTS_TOTAL, 72);
        data.set(FarmMenu.PLOTS_EMPTY, 5);
        assertThat(menu.tier()).isEqualTo(MachineTier.INFERNAL);
        assertThat(menu.plotsTotal()).isEqualTo(72);
        assertThat(menu.emptyPlots()).isEqualTo(5);

        data.set(FarmMenu.TIER, 99);
        assertThat(menu.tier()).isEqualTo(MachineTier.BASIC);
    }

    @Test
    @DisplayName("the menu syncs the machine controls after its own data")
    void carriesMachineControls() {
        FarmMenu menu = new FarmMenu(1, playerInventory());

        assertThat(menu.controls().data().getCount()).isEqualTo(MachineControls.COUNT);
        menu.setData(FarmMenu.DATA_COUNT, RedstoneMode.HIGH.syncId());
        assertThat(menu.controls().mode()).isEqualTo(RedstoneMode.HIGH);
    }

    // ==================== Layout ====================

    @Test
    @DisplayName("the menu shows the farm's 15 generic slots as 5 columns × 3 rows, then the player's")
    void slotPositions() {
        FarmMenu menu = new FarmMenu(1, playerInventory());

        assertThat(menu.slots).hasSize(FarmMachine.SLOT_COUNT + PLAYER_SLOTS);
        assertThat(menu.getSlot(0).x).isEqualTo(80);
        assertThat(menu.getSlot(0).y).isEqualTo(17);
        assertThat(menu.getSlot(14).x).isEqualTo(152);
        assertThat(menu.getSlot(14).y).isEqualTo(53);
        assertThat(menu.getSlot(MACHINE_SLOTS).y).isEqualTo(84);
        assertThat(menu.getSlot(hotbar(menu)).y).isEqualTo(142);
    }

    @Test
    @DisplayName("shift-clicking moves anything into the farm and back out of any slot")
    void shiftClickTakesAnythingBothWays() {
        Inventory inventory = playerInventory();
        inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        FarmMenu menu = new FarmMenu(1, inventory);

        menu.quickMoveStack(null, hotbar(menu));
        assertThat(menu.getSlot(0).getItem().is(Items.COBBLESTONE)).isTrue();
        assertThat(menu.getSlot(0).getItem().getCount()).isEqualTo(32);

        menu.getSlot(14).set(new ItemStack(Items.WHEAT, 5));
        menu.quickMoveStack(null, 14);
        assertThat(menu.getSlot(14).getItem().isEmpty()).isTrue();
        assertThat(countOf(inventory, Items.WHEAT)).isEqualTo(5);
    }

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
