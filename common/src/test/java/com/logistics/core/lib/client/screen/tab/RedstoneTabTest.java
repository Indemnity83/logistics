package com.logistics.core.lib.client.screen.tab;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.client.screen.tab.TabFixtures.RecordingCanvas;
import com.logistics.core.lib.client.screen.tab.TabFixtures.SpriteCall;
import com.logistics.core.lib.client.screen.tab.TabFixtures.WrappedCall;
import com.logistics.core.lib.client.screen.tab.tabs.RedstoneTab;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.core.machine.control.RedstoneView;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Redstone tab")
class RedstoneTabTest {
    private static final int WIDTH = 100;

    static final class FakeRedstone implements RedstoneView {
        boolean supported = true;
        RedstoneMode mode = RedstoneMode.IGNORED;
        boolean signal;
        boolean enabled = true;

        @Override
        public boolean supported() {
            return supported;
        }

        @Override
        public RedstoneMode mode() {
            return mode;
        }

        @Override
        public boolean signal() {
            return signal;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }
    }

    private final FakeRedstone redstone = new FakeRedstone();
    private final List<Integer> sent = new ArrayList<>();
    private boolean sendResult = true;
    private RedstoneTab tab;

    @BeforeEach
    void setUp() {
        tab = new RedstoneTab(redstone, id -> {
            sent.add(id);
            return sendResult;
        });
    }

    /** Centre of the {@code index}-th button, content-local. */
    private static double buttonX(int index) {
        return index * 20 + 9;
    }

    @Nested
    @DisplayName("clicks")
    class Clicks {
        @ParameterizedTest(name = "button {0} sends {1}")
        @CsvSource({"0, IGNORED", "1, LOW", "2, HIGH"})
        @DisplayName("a button for another mode sends that mode's id")
        void sendsTheMode(int index, RedstoneMode mode) {
            redstone.mode = mode == RedstoneMode.HIGH ? RedstoneMode.LOW : RedstoneMode.HIGH;

            assertThat(tab.clickContent(buttonX(index), 9)).isTrue();
            assertThat(sent).containsExactly(MachineControls.redstoneButton(mode));
        }

        @Test
        @DisplayName("reports whether the sender sent the click")
        void reportsTheSender() {
            sendResult = false;

            assertThat(tab.clickContent(buttonX(2), 9)).isFalse();
            assertThat(sent).containsExactly(MachineControls.redstoneButton(RedstoneMode.HIGH));
        }

        @Test
        @DisplayName("the selected mode's button sends nothing")
        void sameMode() {
            redstone.mode = RedstoneMode.LOW;

            assertThat(tab.clickContent(buttonX(1), 9)).isFalse();
            assertThat(sent).isEmpty();
        }

        @Test
        @DisplayName("nothing is sent before the status syncs")
        void unsupported() {
            redstone.supported = false;

            assertThat(tab.clickContent(buttonX(2), 9)).isFalse();
            assertThat(sent).isEmpty();
        }

        @ParameterizedTest(name = "({0}, {1})")
        @CsvSource({
            "-1, 9", "18, 9", "19, 9", "38, 9", "39, 9", "58, 9", "70, 9",
            "9, -1", "9, 18", "29, 30", "9, 40"
        })
        @DisplayName("gaps between buttons and points outside the row do nothing")
        void misses(double x, double y) {
            redstone.mode = RedstoneMode.HIGH;

            assertThat(tab.clickContent(x, y)).isFalse();
            assertThat(tab.contentTooltip(x, y)).isEmpty();
            assertThat(sent).isEmpty();
        }

        @ParameterizedTest(name = "({0}, {1}) is {2}")
        @CsvSource({"0, 0, IGNORED", "17.9, 17.9, IGNORED", "20, 0, LOW", "37, 17, LOW", "40, 0, HIGH", "57.5, 10, HIGH"})
        @DisplayName("each button's edges belong to it")
        void edges(double x, double y, RedstoneMode mode) {
            redstone.mode = mode == RedstoneMode.HIGH ? RedstoneMode.LOW : RedstoneMode.HIGH;

            assertThat(tab.clickContent(x, y)).isTrue();
            assertThat(sent).containsExactly(MachineControls.redstoneButton(mode));
        }
    }

    @Test
    @DisplayName("a button's tooltip names its mode and describes it")
    void tooltips() {
        assertThat(tab.contentTooltip(buttonX(0), 9)).extracting(EnglishLang::resolve)
                .containsExactly("Ignored", "Always runs, regardless of redstone.");
        assertThat(tab.contentTooltip(buttonX(1), 9)).extracting(EnglishLang::resolve)
                .containsExactly("Low", "Runs only without a redstone signal.");
        assertThat(tab.contentTooltip(buttonX(2), 9)).extracting(EnglishLang::resolve)
                .containsExactly("High", "Runs only with a redstone signal.");
    }

    @Nested
    @DisplayName("drawing")
    class Drawing {
        private RecordingCanvas draw() {
            RecordingCanvas canvas = new RecordingCanvas(new EnglishLang.Text());
            tab.drawContent(canvas, 10, 20, WIDTH, -1, -1);
            return canvas;
        }

        private List<String> buttons(RecordingCanvas canvas) {
            return canvas.sprites.stream()
                    .filter(call -> call.w() == 18)
                    .map(call -> call.sprite().toString())
                    .toList();
        }

        @Test
        @DisplayName("lays out three buttons with their mode icons inset by one pixel")
        void layout() {
            redstone.mode = RedstoneMode.LOW;

            assertThat(draw().sprites).containsExactly(
                    new SpriteCall(RedstoneTab.BUTTON, 10, 20, 18, 18),
                    new SpriteCall(RedstoneTab.modeIcon(RedstoneMode.IGNORED), 11, 21, 16, 16),
                    new SpriteCall(RedstoneTab.BUTTON_SELECTED, 30, 20, 18, 18),
                    new SpriteCall(RedstoneTab.modeIcon(RedstoneMode.LOW), 31, 21, 16, 16),
                    new SpriteCall(RedstoneTab.BUTTON, 50, 20, 18, 18),
                    new SpriteCall(RedstoneTab.modeIcon(RedstoneMode.HIGH), 51, 21, 16, 16));
            assertThat(RedstoneTab.modeIcon(RedstoneMode.LOW).toString()).isEqualTo("logistics:tab/redstone_low");
        }

        @Test
        @DisplayName("highlights the current mode once synced")
        void selected() {
            redstone.mode = RedstoneMode.HIGH;

            assertThat(buttons(draw())).containsExactly(
                    "logistics:tab/button", "logistics:tab/button", "logistics:tab/button_selected");
        }

        @Test
        @DisplayName("highlights nothing before the status syncs")
        void unsupportedUnselected() {
            redstone.supported = false;
            redstone.mode = RedstoneMode.IGNORED;

            assertThat(buttons(draw())).containsOnly("logistics:tab/button");
        }

        @Test
        @DisplayName("rings the selected button in gold, and nothing before the status syncs")
        void selectedRing() {
            redstone.mode = RedstoneMode.LOW;
            var drawn = draw();
            assertThat(drawn.fills).isNotEmpty().allMatch(f -> f.argb() == RedstoneTab.SELECTED_RING);
            // The ring hugs the LOW button (x 30..47, y 20..37), one pixel outside it.
            assertThat(drawn.fills).extracting(f -> f.x0()).containsOnly(29, 48);
            assertThat(drawn.fills).allMatch(f -> f.y0() >= 19 && f.y1() <= 39);

            redstone.supported = false;
            assertThat(draw().fills).isEmpty();
        }

        @Test
        @DisplayName("shows the control status, the signal and the signal required below the buttons")
        void status() {
            redstone.mode = RedstoneMode.HIGH;
            redstone.signal = false;
            redstone.enabled = false;

            List<WrappedCall> calls = draw().wrapped;
            assertThat(calls).extracting(call -> EnglishLang.resolve(call.text())).containsExactly(
                    "Control Status:", "Disabled", "Signal:", "Absent", "Signal Required:", "High");
            assertThat(calls.get(0).y()).isEqualTo(20 + 24);
            assertThat(calls.get(1).argb()).isEqualTo(RedstoneTab.DISABLED_COLOR);
            assertThat(calls.get(3).argb()).isEqualTo(0xFFFFFFFF);
            assertThat(calls).allSatisfy(call -> assertThat(call.shadow()).isTrue());
        }

        @Test
        @DisplayName("an enabled control with a signal and no requirement")
        void enabled() {
            redstone.mode = RedstoneMode.IGNORED;
            redstone.signal = true;

            List<WrappedCall> calls = draw().wrapped;
            assertThat(calls).extracting(call -> EnglishLang.resolve(call.text())).containsExactly(
                    "Control Status:", "Enabled", "Signal:", "Present", "Signal Required:", "None");
            assertThat(calls.get(1).argb()).isEqualTo(RedstoneTab.ENABLED_COLOR);
        }

        @Test
        @DisplayName("shows dashes before the status syncs")
        void unsupportedStatus() {
            redstone.supported = false;

            assertThat(draw().wrapped).extracting(call -> EnglishLang.resolve(call.text())).containsExactly(
                    "Control Status:", "—", "Signal:", "—", "Signal Required:", "—");
        }
    }

    @Test
    @DisplayName("content height covers the button row and the status groups")
    void contentHeight() {
        assertThat(tab.contentHeight(new EnglishLang.Text(), WIDTH)).isEqualTo(24 + 6 * 9 + 2);
    }

    @Test
    @DisplayName("the collapsed tooltip shows the mode and whether the machine may run")
    void statusLines() {
        redstone.mode = RedstoneMode.LOW;
        redstone.enabled = false;

        assertThat(tab.statusLines()).extracting(EnglishLang::resolve).containsExactly("Low", "Disabled");

        redstone.supported = false;
        assertThat(tab.statusLines()).isEmpty();
    }

    @Test
    @DisplayName("is a right tab with its own identity")
    void identity() {
        assertThat(tab.id()).isEqualTo("redstone");
        assertThat(tab.side()).isEqualTo(TabSide.RIGHT);
        assertThat(tab.tint()).isEqualTo(0xFFC03030);
        assertThat(tab.icon().toString()).isEqualTo("logistics:tab/redstone");
        assertThat(EnglishLang.resolve(tab.title())).isEqualTo("Redstone Control");
        assertThat(tab.preferredWidth()).isEqualTo(124);
        assertThat(tab.minOpenWidth()).isEqualTo(70);
    }
}
