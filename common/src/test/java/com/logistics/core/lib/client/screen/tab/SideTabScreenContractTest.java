package com.logistics.core.lib.client.screen.tab;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.alloysmelter.AlloySmelterScreen;
import com.logistics.automation.alloysmelter.AlloySmelterScreenHandler;
import com.logistics.automation.crucible.CrucibleScreen;
import com.logistics.automation.crucible.CrucibleScreenHandler;
import com.logistics.automation.fabricator.SequentialFabricatorScreen;
import com.logistics.automation.fabricator.SequentialFabricatorScreenHandler;
import com.logistics.automation.kiln.KilnScreen;
import com.logistics.automation.kiln.KilnScreenHandler;
import com.logistics.automation.macerator.MaceratorScreen;
import com.logistics.automation.macerator.MaceratorScreenHandler;
import com.logistics.automation.refinery.RefineryScreen;
import com.logistics.automation.refinery.RefineryScreenHandler;
import com.logistics.automation.sawmill.SawmillScreen;
import com.logistics.automation.sawmill.SawmillScreenHandler;
import com.logistics.automation.transposer.TransposerScreen;
import com.logistics.automation.transposer.TransposerScreenHandler;
import com.logistics.core.lib.client.screen.tab.TabFixtures.FakeClock;
import com.logistics.core.lib.client.screen.tab.TabFixtures.RecordingCanvas;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.power.engine.ui.FuelEngineScreenHandler;
import com.logistics.power.engine.ui.MagmaticEngineScreenHandler;
import com.logistics.power.engine.ui.ReactionEngineScreenHandler;
import com.logistics.power.engine.ui.SteamEngineScreenHandler;
import com.logistics.power.engine.ui.StirlingEngineScreenHandler;
import com.logistics.power.screen.FuelEngineScreen;
import com.logistics.power.screen.MagmaticEngineScreen;
import com.logistics.power.screen.ReactionEngineScreen;
import com.logistics.power.screen.SteamEngineScreen;
import com.logistics.power.screen.StirlingEngineScreen;
import com.logistics.test.MinecraftTestEnvironment;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every screen carrying side tabs honours the integration contract: it exposes its host, it treats
 * the tab rects as inside the GUI when a carried item is released, and no tab, collapsed, animating
 * or fully open, ever lies over a slot or reaches into the GUI image at all.
 *
 * <p>Tabs draw below slots and do not stop a slot from being hovered, so a tab over a slot would
 * show a highlight and take key presses through the tab. Adding a screen is one line in
 * {@link #screens()}.
 */
@DisplayName("Side tab screen contract")
class SideTabScreenContractTest extends MinecraftTestEnvironment {

    /** GUI-scaled screen sizes: 1280×720 and 1920×1080 at scales 3 and 4, plus a 4:3 window. */
    private static final int[][] SCREEN_SIZES = {{427, 240}, {480, 270}, {640, 360}, {320, 240}};
    private static final long FRAME_MS = 15L;

    static Stream<ScreenCase> screens() {
        return Stream.of(
                machine("macerator", MaceratorScreen.class, 176, 166,
                        inventory -> new MaceratorScreenHandler(1, inventory), MaceratorScreenHandler::controls),
                machine("kiln", KilnScreen.class, 176, 166,
                        inventory -> new KilnScreenHandler(1, inventory), KilnScreenHandler::controls),
                machine("sawmill", SawmillScreen.class, 176, 166,
                        inventory -> new SawmillScreenHandler(1, inventory), SawmillScreenHandler::controls),
                machine("alloy_smelter", AlloySmelterScreen.class, 176, 166,
                        inventory -> new AlloySmelterScreenHandler(1, inventory), AlloySmelterScreenHandler::controls),
                machine("crucible", CrucibleScreen.class, 176, 166,
                        inventory -> new CrucibleScreenHandler(1, inventory), CrucibleScreenHandler::controls),
                machine("refinery", RefineryScreen.class, 176, 166,
                        inventory -> new RefineryScreenHandler(1, inventory), RefineryScreenHandler::controls),
                machine("sequential_fabricator", SequentialFabricatorScreen.class, 176, 186,
                        inventory -> new SequentialFabricatorScreenHandler(1, inventory),
                        SequentialFabricatorScreenHandler::controls),
                machine("transposer", TransposerScreen.class, 176, 166,
                        inventory -> new TransposerScreenHandler(1, inventory), TransposerScreenHandler::controls),
                engine("stirling_engine", StirlingEngineScreen.class, 176, 166,
                        inventory -> new StirlingEngineScreenHandler(1, inventory), StirlingEngineScreenHandler::controls),
                engine("fuel_engine", FuelEngineScreen.class, 176, 166,
                        inventory -> new FuelEngineScreenHandler(1, inventory), FuelEngineScreenHandler::controls),
                engine("magmatic_engine", MagmaticEngineScreen.class, 176, 166,
                        inventory -> new MagmaticEngineScreenHandler(1, inventory), MagmaticEngineScreenHandler::controls),
                engine("steam_engine", SteamEngineScreen.class, 176, 166,
                        inventory -> new SteamEngineScreenHandler(1, inventory), SteamEngineScreenHandler::controls),
                engine("reaction_engine", ReactionEngineScreen.class, 176, 166,
                        inventory -> new ReactionEngineScreenHandler(1, inventory), ReactionEngineScreenHandler::controls));
    }

    @BeforeEach
    @AfterEach
    void clearMemory() {
        TabMemory.clear();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("screens")
    @DisplayName("implements SideTabScreen and overrides hasClickedOutside")
    void integratesWithTheHost(ScreenCase screen) throws NoSuchMethodException {
        assertThat(SideTabScreen.class).isAssignableFrom(screen.screenClass());
        Method override = screen.screenClass()
                .getDeclaredMethod("hasClickedOutside", double.class, double.class, int.class, int.class, int.class);
        assertThat(override.getDeclaringClass()).isEqualTo(screen.screenClass());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("screens")
    @DisplayName("no tab ever covers a slot or reaches into the GUI image")
    void tabsStayClearOfSlots(ScreenCase screen) {
        AbstractContainerMenu menu = screen.menu().apply(new Inventory(null));
        assertThat(menu.slots).as("menu slots").isNotEmpty();

        for (int[] size : SCREEN_SIZES) {
            Geometry geometry = new Geometry(screen, size[0], size[1]);
            List<TabRect> slots = slotRects(menu, geometry);
            String where = screen.key() + " at " + size[0] + "x" + size[1];
            TabMemory.clear();

            List<TabRect> collapsed = frame(screen, menu, geometry, new FakeClock());
            assertThat(collapsed).as(where + ": visible tabs").isNotEmpty();
            assertClear(collapsed, slots, geometry, where + ", collapsed");

            int opened = 0;
            for (int i = 0; i < collapsed.size(); i++) {
                TabMemory.clear();
                FakeClock clock = new FakeClock();
                SideTabHost host = screen.host(menu, geometry.anchor(), clock);
                host.attach();
                RecordingCanvas canvas = new RecordingCanvas();
                host.render(canvas, -1000, -1000);
                TabRect target = collapsed.get(i);
                host.mouseClicked(target.x() + target.w() / 2.0, target.y() + target.h() / 2.0, true);

                List<TabRect> rects = host.exclusionAreas();
                for (long t = 0; t <= TabAnimation.DURATION_MS; t += FRAME_MS) {
                    clock.advance(FRAME_MS);
                    host.render(canvas, -1000, -1000);
                    rects = host.exclusionAreas();
                    assertClear(rects, slots, geometry, where + ", tab " + i + " opening at " + t + "ms");
                }
                if (!rects.equals(collapsed)) {
                    opened++;
                }
            }
            assertThat(opened).as(where + ": tabs that opened (an all-collapsed sweep proves nothing)")
                    .isPositive();
        }
    }

    private static List<TabRect> frame(ScreenCase screen, AbstractContainerMenu menu, Geometry geometry, FakeClock clock) {
        SideTabHost host = screen.host(menu, geometry.anchor(), clock);
        host.attach();
        host.render(new RecordingCanvas(), -1000, -1000);
        return host.exclusionAreas();
    }

    private static void assertClear(List<TabRect> tabs, List<TabRect> slots, Geometry geometry, String where) {
        TabRect image = geometry.image();
        for (TabRect tab : tabs) {
            for (TabRect slot : slots) {
                assertThat(tab.intersects(slot)).as(where + ": tab " + tab + " over slot " + slot).isFalse();
            }
            assertThat(tab.intersects(image)).as(where + ": tab " + tab + " reaches into the GUI " + image).isFalse();
        }
    }

    /** Each slot's hover area, {@code isHovering(x, y, 16, 16)}: one pixel beyond the item on every side. */
    private static List<TabRect> slotRects(AbstractContainerMenu menu, Geometry geometry) {
        List<TabRect> rects = new ArrayList<>();
        for (Slot slot : menu.slots) {
            rects.add(new TabRect(geometry.left + slot.x - 1, geometry.top + slot.y - 1, 18, 18));
        }
        return rects;
    }

    private static <M extends AbstractContainerMenu> ScreenCase machine(
            String key,
            Class<? extends AbstractContainerScreen<?>> screenClass,
            int imageWidth,
            int imageHeight,
            Function<Inventory, M> menu,
            Function<M, MachineControls> controls) {
        return new ScreenCase(key, screenClass, menu::apply, imageWidth, imageHeight, (m, anchor, clock) ->
                MachineTabs.machine(key, m, controls.apply(cast(m)), anchor, new EnglishLang.Text(), clock));
    }

    private static <M extends AbstractContainerMenu> ScreenCase engine(
            String key,
            Class<? extends AbstractContainerScreen<?>> screenClass,
            int imageWidth,
            int imageHeight,
            Function<Inventory, M> menu,
            Function<M, MachineControls> controls) {
        return new ScreenCase(key, screenClass, menu::apply, imageWidth, imageHeight, (m, anchor, clock) ->
                MachineTabs.engine(key, m, controls.apply(cast(m)), anchor, new EnglishLang.Text(), clock));
    }

    @SuppressWarnings("unchecked")
    private static <M extends AbstractContainerMenu> M cast(AbstractContainerMenu menu) {
        return (M) menu;
    }

    @FunctionalInterface
    interface HostFactory {
        SideTabHost create(AbstractContainerMenu menu, TabAnchor anchor, FakeClock clock);
    }

    record ScreenCase(
            String key,
            Class<? extends AbstractContainerScreen<?>> screenClass,
            Function<Inventory, AbstractContainerMenu> menu,
            int imageWidth,
            int imageHeight,
            HostFactory hosts) {

        SideTabHost host(AbstractContainerMenu menu, TabAnchor anchor, FakeClock clock) {
            return hosts.create(menu, anchor, clock);
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /** The screen's layout as vanilla computes it: the image centred, as {@code AbstractContainerScreen.init} does. */
    private static final class Geometry {
        final int width;
        final int height;
        final int imageWidth;
        final int imageHeight;
        final int left;
        final int top;

        Geometry(ScreenCase screen, int width, int height) {
            this.width = width;
            this.height = height;
            this.imageWidth = screen.imageWidth();
            this.imageHeight = screen.imageHeight();
            this.left = (width - imageWidth) / 2;
            this.top = (height - imageHeight) / 2;
        }

        TabAnchor anchor() {
            return new TabAnchor(() -> left, () -> top, () -> imageWidth, () -> width, () -> height);
        }

        TabRect image() {
            return new TabRect(left, top, imageWidth, imageHeight);
        }
    }
}
