package com.logistics.core.lib.client.screen.tab;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.client.screen.tab.TabFixtures.RecordingCanvas;
import com.logistics.core.lib.client.screen.tab.TabFixtures.WrappedCall;
import com.logistics.core.lib.client.screen.tab.tabs.EnergyTab;
import com.logistics.core.machine.control.EnergyView;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Energy tab")
class EnergyTabTest {
    private static final int WIDTH = 98;
    private static final int LABEL = 0xFFDDE4F0;
    private static final int VALUE = 0xFFFFFFFF;

    static final class FakeEnergy implements EnergyView {
        boolean hasBuffer = true;
        long stored;
        long capacity;
        long rate;
        long maxRate;

        FakeEnergy(long stored, long capacity, long rate, long maxRate) {
            this.stored = stored;
            this.capacity = capacity;
            this.rate = rate;
            this.maxRate = maxRate;
        }

        @Override
        public boolean hasBuffer() {
            return hasBuffer;
        }

        @Override
        public long stored() {
            return stored;
        }

        @Override
        public long capacity() {
            return capacity;
        }

        @Override
        public long rate() {
            return rate;
        }

        @Override
        public long maxRate() {
            return maxRate;
        }
    }

    private static List<WrappedCall> draw(EnergyTab tab) {
        RecordingCanvas canvas = new RecordingCanvas(new EnglishLang.Text());
        tab.drawContent(canvas, 10, 20, WIDTH, -1, -1);
        return canvas.wrapped;
    }

    private static List<String> lines(EnergyTab tab) {
        return draw(tab).stream().map(call -> EnglishLang.resolve(call.text())).toList();
    }

    @Nested
    @DisplayName("usage (machines)")
    class Usage {
        @Test
        @DisplayName("shows usage, max input and the buffer, with grouped numbers")
        void groups() {
            EnergyTab tab = new EnergyTab(new FakeEnergy(12_345, 100_000, 1_280, 128), EnergyTab.Mode.USAGE);

            assertThat(lines(tab)).containsExactly(
                    "Usage:", "1,280 RF/t",
                    "Max Input:", "128 RF/t",
                    "Stored:", "12,345 / 100,000 RF");
        }

        @Test
        @DisplayName("shows the buffer even if the view reports none")
        void bufferAlwaysShown() {
            FakeEnergy energy = new FakeEnergy(0, 10_000, 0, 128);
            energy.hasBuffer = false;

            assertThat(lines(new EnergyTab(energy, EnergyTab.Mode.USAGE))).contains("Stored:", "0 / 10,000 RF");
        }
    }

    @Nested
    @DisplayName("generation (engines)")
    class Generation {
        @Test
        @DisplayName("shows generation, max output and the buffer when there is one")
        void withBuffer() {
            EnergyTab tab = new EnergyTab(new FakeEnergy(5_000, 10_000, 40, 50), EnergyTab.Mode.GENERATION);

            assertThat(lines(tab)).containsExactly(
                    "Generation:", "40 RF/t",
                    "Max Output:", "50 RF/t",
                    "Stored:", "5,000 / 10,000 RF");
        }

        @Test
        @DisplayName("hides the buffer group for a bufferless engine")
        void withoutBuffer() {
            FakeEnergy energy = new FakeEnergy(0, 0, 2_000_000, 2_500_000);
            energy.hasBuffer = false;
            EnergyTab tab = new EnergyTab(energy, EnergyTab.Mode.GENERATION);

            assertThat(lines(tab)).containsExactly(
                    "Generation:", "2,000,000 RF/t",
                    "Max Output:", "2,500,000 RF/t");
            assertThat(tab.contentHeight(new EnglishLang.Text(), WIDTH))
                    .isEqualTo(4 * 9 + 1);
        }
    }

    @Test
    @DisplayName("labels sit at the content edge and values one line below, indented, all with a shadow")
    void styling() {
        List<WrappedCall> calls = draw(new EnergyTab(new FakeEnergy(1, 2, 3, 4), EnergyTab.Mode.USAGE));

        assertThat(calls).allSatisfy(call -> assertThat(call.shadow()).isTrue());
        assertThat(calls).extracting(WrappedCall::argb).containsExactly(LABEL, VALUE, LABEL, VALUE, LABEL, VALUE);
        assertThat(calls).extracting(WrappedCall::x).containsExactly(10, 14, 10, 14, 10, 14);
        assertThat(calls).extracting(WrappedCall::y).containsExactly(20, 29, 39, 48, 58, 67);
        assertThat(calls).extracting(WrappedCall::maxWidth).containsExactly(WIDTH, WIDTH - 4, WIDTH, WIDTH - 4, WIDTH, WIDTH - 4);
    }

    @Test
    @DisplayName("content height counts every line plus the spacing between groups")
    void contentHeight() {
        EnergyTab tab = new EnergyTab(new FakeEnergy(1, 2, 3, 4), EnergyTab.Mode.USAGE);

        assertThat(tab.contentHeight(new EnglishLang.Text(), WIDTH)).isEqualTo(6 * 9 + 2);
    }

    @Test
    @DisplayName("a value too wide for the tab wraps, and the height grows to match")
    void wrapsLongValues() {
        EnergyTab tab = new EnergyTab(new FakeEnergy(100_000, 100_000, 3, 4), EnergyTab.Mode.USAGE);
        EnglishLang.Text text = new EnglishLang.Text();

        // "100,000 / 100,000 RF" is 20 characters, 120px at 6px each: two lines in the 66px value column.
        assertThat(tab.contentHeight(text, 70)).isEqualTo(tab.contentHeight(text, 400) + 9);
    }

    @Test
    @DisplayName("the collapsed tooltip shows the current rate")
    void statusLines() {
        EnergyTab tab = new EnergyTab(new FakeEnergy(0, 0, 12_500, 0), EnergyTab.Mode.USAGE);

        assertThat(tab.statusLines()).extracting(EnglishLang::resolve).containsExactly("12,500 RF/t");
    }

    @Test
    @DisplayName("is a left tab with its own identity")
    void identity() {
        EnergyTab tab = new EnergyTab(new FakeEnergy(0, 0, 0, 0), EnergyTab.Mode.USAGE);

        assertThat(tab.id()).isEqualTo("energy");
        assertThat(tab.side()).isEqualTo(TabSide.LEFT);
        assertThat(tab.tint()).isEqualTo(0xFF3F6FD0);
        assertThat(tab.icon().toString()).isEqualTo("logistics:tab/energy");
        assertThat(EnglishLang.resolve(tab.title())).isEqualTo("Energy");
        assertThat(tab.preferredWidth()).isEqualTo(110);
        assertThat(tab.minOpenWidth()).isEqualTo(70);
        assertThat(tab.clickContent(5, 5)).isFalse();
        assertThat(tab.contentTooltip(5, 5)).isEqualTo(List.<Component>of());
    }
}
