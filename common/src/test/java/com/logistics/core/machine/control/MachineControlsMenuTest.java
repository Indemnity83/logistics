package com.logistics.core.machine.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.automation.alloysmelter.AlloySmelterScreenHandler;
import com.logistics.automation.crucible.CrucibleBlockEntity;
import com.logistics.automation.crucible.CrucibleScreenHandler;
import com.logistics.automation.fabricator.SequentialFabricatorScreenHandler;
import com.logistics.automation.kiln.KilnScreenHandler;
import com.logistics.automation.macerator.MaceratorScreenHandler;
import com.logistics.automation.refinery.RefineryBlockEntity;
import com.logistics.automation.refinery.RefineryScreenHandler;
import com.logistics.automation.sawmill.SawmillScreenHandler;
import com.logistics.automation.transposer.TransposerBlockEntity;
import com.logistics.automation.transposer.TransposerScreenHandler;
import com.logistics.core.machine.MachineData;
import com.logistics.power.engine.block.entity.FuelEngineBlockEntity;
import com.logistics.power.engine.block.entity.MagmaticEngineBlockEntity;
import com.logistics.power.engine.block.entity.ReactionEngineBlockEntity;
import com.logistics.power.engine.block.entity.SteamEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.power.engine.ui.FuelEngineScreenHandler;
import com.logistics.power.engine.ui.MagmaticEngineScreenHandler;
import com.logistics.power.engine.ui.ReactionEngineScreenHandler;
import com.logistics.power.engine.ui.SteamEngineScreenHandler;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import com.logistics.test.MinecraftTestEnvironment;
import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every machine and engine menu carries a {@link MachineControls} block registered after its own data,
 * so the existing data indices keep their meaning and the controls occupy exactly
 * {@link MachineControls#COUNT} further slots.
 *
 * <p>Server menus whose public constructor takes a block entity (Sequential Fabricator, Stirling, Steam,
 * Reaction) cannot be built without a level; the {@code machine_controls_server_menus} GameTest opens
 * all 13 server menus through their real menu providers.
 */
@DisplayName("Machine controls in every menu")
class MachineControlsMenuTest extends MinecraftTestEnvironment {

    record MenuCase(
            String name,
            int ownDataCount,
            Function<Inventory, AbstractContainerMenu> client,
            @Nullable BiFunction<Inventory, MachineControls, AbstractContainerMenu> server) {
        @Override
        public String toString() {
            return name;
        }
    }

    // Package-private in their own packages; read reflectively rather than widened for a test.
    private static final int CRUCIBLE_DATA = packagePrivateCount(CrucibleBlockEntity.class);
    private static final int REFINERY_DATA = packagePrivateCount(RefineryBlockEntity.class);
    private static final int TRANSPOSER_DATA = packagePrivateCount(TransposerBlockEntity.class);

    private static int packagePrivateCount(Class<?> owner) {
        try {
            Field field = owner.getDeclaredField("DATA_COUNT");
            field.setAccessible(true);
            return field.getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static Stream<MenuCase> menus() {
        return Stream.of(
                new MenuCase("macerator", MachineData.COUNT, inv -> new MaceratorScreenHandler(1, inv),
                        (inv, c) -> new MaceratorScreenHandler(1, inv, new SimpleContainer(3),
                                new SimpleContainerData(MachineData.COUNT), c)),
                new MenuCase("kiln", MachineData.COUNT, inv -> new KilnScreenHandler(1, inv),
                        (inv, c) -> new KilnScreenHandler(1, inv, new SimpleContainer(2),
                                new SimpleContainerData(MachineData.COUNT), c)),
                new MenuCase("sawmill", MachineData.COUNT, inv -> new SawmillScreenHandler(1, inv),
                        (inv, c) -> new SawmillScreenHandler(1, inv, new SimpleContainer(3),
                                new SimpleContainerData(MachineData.COUNT), c)),
                new MenuCase("alloy smelter", MachineData.COUNT, inv -> new AlloySmelterScreenHandler(1, inv),
                        (inv, c) -> new AlloySmelterScreenHandler(1, inv, new SimpleContainer(4),
                                new SimpleContainerData(MachineData.COUNT), c)),
                new MenuCase("crucible", CRUCIBLE_DATA, inv -> new CrucibleScreenHandler(1, inv),
                        (inv, c) -> new CrucibleScreenHandler(1, inv, new SimpleContainer(1),
                                new SimpleContainerData(CRUCIBLE_DATA), c)),
                new MenuCase("refinery", REFINERY_DATA, inv -> new RefineryScreenHandler(1, inv),
                        (inv, c) -> new RefineryScreenHandler(1, inv, new SimpleContainer(1),
                                new SimpleContainerData(REFINERY_DATA), c)),
                new MenuCase("transposer", TRANSPOSER_DATA, inv -> new TransposerScreenHandler(1, inv),
                        (inv, c) -> new TransposerScreenHandler(1, inv, new SimpleContainer(2),
                                new SimpleContainerData(TRANSPOSER_DATA), c)),
                new MenuCase("sequential fabricator", MachineData.COUNT,
                        inv -> new SequentialFabricatorScreenHandler(1, inv), null),
                new MenuCase("stirling engine", StirlingEngineBlockEntity.PROPERTY_COUNT,
                        inv -> new StirlingEngineScreenHandler(1, inv), null),
                new MenuCase("fuel engine", FuelEngineBlockEntity.DATA_COUNT, inv -> new FuelEngineScreenHandler(1, inv),
                        (inv, c) -> new FuelEngineScreenHandler(1, inv,
                                new SimpleContainerData(FuelEngineBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL, c)),
                new MenuCase("magmatic engine", MagmaticEngineBlockEntity.DATA_COUNT,
                        inv -> new MagmaticEngineScreenHandler(1, inv),
                        (inv, c) -> new MagmaticEngineScreenHandler(1, inv,
                                new SimpleContainerData(MagmaticEngineBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL, c)),
                new MenuCase("steam engine", SteamEngineBlockEntity.DATA_COUNT,
                        inv -> new SteamEngineScreenHandler(1, inv), null),
                new MenuCase("reaction engine", ReactionEngineBlockEntity.DATA_COUNT,
                        inv -> new ReactionEngineScreenHandler(1, inv), null));
    }

    static Stream<MenuCase> serverMenus() {
        return menus().filter(m -> m.server() != null);
    }

    private static Inventory inventory() {
        return new Inventory(null, new EntityEquipment());
    }

    static MachineControls controlsOf(AbstractContainerMenu menu) {
        try {
            return (MachineControls) menu.getClass().getMethod("controls").invoke(menu);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(menu.getClass().getSimpleName() + " has no public controls()", e);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("menus")
    @DisplayName("the client menu registers the controls after its own data, exactly COUNT slots")
    void clientMenuRegistersControlsAfterOwnData(MenuCase menuCase) {
        AbstractContainerMenu menu = menuCase.client().apply(inventory());
        MachineControls controls = controlsOf(menu);
        assertThat(controls).isNotNull();
        assertThat(controls.data().getCount()).isEqualTo(MachineControls.COUNT);

        // Sync addresses data slots by menu-wide index: the controls start right after the menu's own.
        for (int i = 0; i < MachineControls.COUNT; i++) {
            menu.setData(menuCase.ownDataCount() + i, 1000 + i);
        }
        for (int i = 0; i < MachineControls.COUNT; i++) {
            assertThat(controls.data().get(i)).isEqualTo(1000 + i);
        }
        int pastTheEnd = menuCase.ownDataCount() + MachineControls.COUNT;
        assertThatThrownBy(() -> menu.setData(pastTheEnd, 1)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("menus")
    @DisplayName("the client menu validates a redstone click only once synced")
    void clientMenuDelegatesClicks(MenuCase menuCase) {
        AbstractContainerMenu menu = menuCase.client().apply(inventory());
        int high = MachineControls.redstoneButton(RedstoneMode.HIGH);
        assertThat(menu.clickMenuButton(null, high)).isFalse();

        menu.setData(menuCase.ownDataCount() + MachineControls.STATUS,
                MachineControls.pack(RedstoneMode.IGNORED, false, true, true, true));
        assertThat(menu.clickMenuButton(null, high)).isTrue();
        assertThat(controlsOf(menu).mode()).isEqualTo(RedstoneMode.IGNORED);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serverMenus")
    @DisplayName("the server menu exposes the controls it was given")
    void serverMenuExposesItsControls(MenuCase menuCase) {
        MachineControls controls = MachineControls.server(new MachineControlsTest.FakeStatus());
        AbstractContainerMenu menu = menuCase.server().apply(inventory(), controls);

        assertThat(controlsOf(menu)).isSameAs(controls);
        assertThat(menu.clickMenuButton(null, 0)).isFalse();
    }
}
