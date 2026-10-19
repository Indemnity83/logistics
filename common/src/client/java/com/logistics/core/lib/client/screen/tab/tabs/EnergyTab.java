package com.logistics.core.lib.client.screen.tab.tabs;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.screen.tab.SideTab;
import com.logistics.core.lib.client.screen.tab.TabCanvas;
import com.logistics.core.lib.client.screen.tab.TabSide;
import com.logistics.core.lib.client.screen.tab.TabText;
import com.logistics.core.lib.client.screen.tab.tabs.StatLines.Group;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.control.EnergyView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Left tab with the host's synced energy telemetry: its rate, its maximum rate and its buffer. */
public final class EnergyTab extends SideTab {
    public static final String ID = "energy";
    public static final String TITLE_KEY = "gui.logistics.tab.energy.title";
    public static final String USAGE_KEY = "gui.logistics.tab.energy.usage";
    public static final String GENERATION_KEY = "gui.logistics.tab.energy.generation";
    public static final String MAX_INPUT_KEY = "gui.logistics.tab.energy.max_input";
    public static final String MAX_OUTPUT_KEY = "gui.logistics.tab.energy.max_output";
    public static final String STORED_KEY = "gui.logistics.tab.energy.stored";
    /** {@code %s RF/t}. */
    public static final String RATE_KEY = "gui.logistics.tab.energy.rate";
    /** {@code %s / %s RF}. */
    public static final String AMOUNT_KEY = "gui.logistics.tab.energy.amount";
    public static final List<String> KEYS = List.of(
            TITLE_KEY, USAGE_KEY, GENERATION_KEY, MAX_INPUT_KEY, MAX_OUTPUT_KEY, STORED_KEY, RATE_KEY, AMOUNT_KEY);
    public static final int TINT = 0xFF3F6FD0;
    public static final int PREFERRED_WIDTH = 110;
    public static final int MIN_OPEN_WIDTH = 70;

    private static final ResourceId ICON = LogisticsMod.modId("tab/energy");

    /** Whether the rate is energy spent (machines) or energy produced (engines). */
    public enum Mode {
        USAGE(USAGE_KEY, MAX_INPUT_KEY),
        GENERATION(GENERATION_KEY, MAX_OUTPUT_KEY);

        private final String rateLabel;
        private final String maxRateLabel;

        Mode(String rateLabel, String maxRateLabel) {
            this.rateLabel = rateLabel;
            this.maxRateLabel = maxRateLabel;
        }
    }

    private final EnergyView energy;
    private final Mode mode;

    public EnergyTab(EnergyView energy, Mode mode) {
        this.energy = energy;
        this.mode = mode;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public TabSide side() {
        return TabSide.LEFT;
    }

    @Override
    public int tint() {
        return TINT;
    }

    @Override
    public ResourceId icon() {
        return ICON;
    }

    @Override
    public Component title() {
        return Component.translatable(TITLE_KEY);
    }

    @Override
    public List<Component> statusLines() {
        return List.of(rate(energy.rate()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public int preferredWidth() {
        return PREFERRED_WIDTH;
    }

    @Override
    public int minOpenWidth() {
        return MIN_OPEN_WIDTH;
    }

    @Override
    public int contentHeight(TabText text, int contentWidth) {
        return StatLines.height(text, contentWidth, groups());
    }

    @Override
    public void drawContent(TabCanvas canvas, int x, int y, int contentWidth, int mouseX, int mouseY) {
        StatLines.draw(canvas, x, y, contentWidth, groups());
    }

    /** The label/value groups shown, in order. */
    List<Group> groups() {
        List<Group> groups = new ArrayList<>(3);
        groups.add(new Group(Component.translatable(mode.rateLabel), rate(energy.rate())));
        groups.add(new Group(Component.translatable(mode.maxRateLabel), rate(energy.maxRate())));
        if (mode == Mode.USAGE || energy.hasBuffer()) {
            groups.add(new Group(
                    Component.translatable(STORED_KEY),
                    Component.translatable(
                            AMOUNT_KEY, StatLines.number(energy.stored()), StatLines.number(energy.capacity()))));
        }
        return groups;
    }

    private static MutableComponent rate(long rate) {
        return Component.translatable(RATE_KEY, StatLines.number(rate));
    }
}
