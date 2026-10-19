package com.logistics.core.lib.client.screen.tab.tabs;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.client.screen.tab.SideTab;
import com.logistics.core.lib.client.screen.tab.TabCanvas;
import com.logistics.core.lib.client.screen.tab.TabSide;
import com.logistics.core.lib.client.screen.tab.TabText;
import com.logistics.core.lib.client.screen.tab.tabs.StatLines.Group;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.core.machine.control.RedstoneView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Right tab for the redstone control: one button per {@link RedstoneMode}, then the control's
 * status. A button click goes to the sender as {@link MachineControls#redstoneButton}; the tab
 * itself never changes the mode, it shows whatever the server syncs back.
 */
public final class RedstoneTab extends SideTab {
    public static final String ID = "redstone";
    public static final String TITLE_KEY = "gui.logistics.tab.redstone.title";
    public static final String STATUS_KEY = "gui.logistics.tab.redstone.status";
    public static final String ENABLED_KEY = "gui.logistics.tab.redstone.enabled";
    public static final String DISABLED_KEY = "gui.logistics.tab.redstone.disabled";
    public static final String SIGNAL_KEY = "gui.logistics.tab.redstone.signal";
    public static final String PRESENT_KEY = "gui.logistics.tab.redstone.present";
    public static final String ABSENT_KEY = "gui.logistics.tab.redstone.absent";
    public static final String REQUIRED_KEY = "gui.logistics.tab.redstone.required";
    public static final String REQUIRED_NONE_KEY = "gui.logistics.tab.redstone.required.none";
    public static final String REQUIRED_LOW_KEY = "gui.logistics.tab.redstone.required.low";
    public static final String REQUIRED_HIGH_KEY = "gui.logistics.tab.redstone.required.high";
    public static final String MODE_KEY_PREFIX = "gui.logistics.tab.redstone.mode.";
    public static final int TINT = 0xFFC03030;
    public static final int PREFERRED_WIDTH = 124;
    /** 1px ring around the selected mode button (the header title's gold). */
    public static final int SELECTED_RING = 0xFFFFD34A;
    public static final int MIN_OPEN_WIDTH = 70;
    public static final int ENABLED_COLOR = 0xFF9CFF9C;
    public static final int DISABLED_COLOR = 0xFFFFC0A0;

    /** Mode buttons, left to right. */
    public static final List<RedstoneMode> BUTTON_ORDER = List.of(RedstoneMode.IGNORED, RedstoneMode.LOW, RedstoneMode.HIGH);
    public static final int BUTTON_SIZE = 18;
    /** Distance between the left edges of neighbouring buttons. */
    public static final int BUTTON_STRIDE = 20;
    public static final int MODE_ICON_SIZE = 16;
    /** Top of the status text, below the button row. */
    public static final int STATUS_Y = BUTTON_SIZE + 6;

    public static final ResourceId ICON = LogisticsMod.modId("tab/redstone");
    public static final ResourceId BUTTON = LogisticsMod.modId("tab/button");
    public static final ResourceId BUTTON_SELECTED = LogisticsMod.modId("tab/button_selected");

    private final RedstoneView redstone;
    private final IntPredicate sender;

    /**
     * @param sender sends a menu-button id to the server; returns whether it was sent
     */
    public RedstoneTab(RedstoneView redstone, IntPredicate sender) {
        this.redstone = redstone;
        this.sender = sender;
    }

    /** Every translation key the tab shows. */
    public static List<String> keys() {
        List<String> keys = new ArrayList<>(List.of(
                TITLE_KEY, STATUS_KEY, ENABLED_KEY, DISABLED_KEY, SIGNAL_KEY, PRESENT_KEY, ABSENT_KEY,
                REQUIRED_KEY, REQUIRED_NONE_KEY, REQUIRED_LOW_KEY, REQUIRED_HIGH_KEY));
        for (RedstoneMode mode : RedstoneMode.values()) {
            keys.add(modeKey(mode));
            keys.add(modeDescriptionKey(mode));
        }
        return keys;
    }

    public static String modeKey(RedstoneMode mode) {
        return MODE_KEY_PREFIX + mode.serializedName();
    }

    public static String modeDescriptionKey(RedstoneMode mode) {
        return modeKey(mode) + ".desc";
    }

    /** The mode icon sprite. */
    public static ResourceId modeIcon(RedstoneMode mode) {
        return LogisticsMod.modId("tab/redstone_" + mode.serializedName());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public TabSide side() {
        return TabSide.RIGHT;
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
        if (!redstone.supported()) {
            return List.of();
        }
        return List.of(
                Component.translatable(modeKey(redstone.mode())).withStyle(ChatFormatting.GRAY),
                enabledText().withColor(enabledColor()));
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
        return STATUS_Y + StatLines.height(text, contentWidth, groups());
    }

    @Override
    public void drawContent(TabCanvas canvas, int x, int y, int contentWidth, int mouseX, int mouseY) {
        boolean supported = redstone.supported();
        RedstoneMode current = redstone.mode();
        for (int i = 0; i < BUTTON_ORDER.size(); i++) {
            RedstoneMode mode = BUTTON_ORDER.get(i);
            int bx = x + i * BUTTON_STRIDE;
            boolean selected = supported && mode == current;
            canvas.sprite(selected ? BUTTON_SELECTED : BUTTON, bx, y, BUTTON_SIZE, BUTTON_SIZE);
            canvas.sprite(modeIcon(mode), bx + 1, y + 1, MODE_ICON_SIZE, MODE_ICON_SIZE);
            if (selected) {
                drawRing(canvas, bx - 1, y - 1, BUTTON_SIZE + 2, SELECTED_RING);
            }
        }
        StatLines.draw(canvas, x, y + STATUS_Y, contentWidth, groups());
    }

    @Override
    public boolean clickContent(double localX, double localY) {
        RedstoneMode mode = buttonAt(localX, localY);
        if (mode == null || !redstone.supported() || mode == redstone.mode()) {
            return false;
        }
        return sender.test(MachineControls.redstoneButton(mode));
    }

    @Override
    public List<Component> contentTooltip(double localX, double localY) {
        RedstoneMode mode = buttonAt(localX, localY);
        if (mode == null) {
            return List.of();
        }
        return List.of(
                Component.translatable(modeKey(mode)),
                Component.translatable(modeDescriptionKey(mode)).withStyle(ChatFormatting.GRAY));
    }

    /** The mode whose button contains the content-local point, or null between and outside the buttons. */
    static @Nullable RedstoneMode buttonAt(double localX, double localY) {
        if (localY < 0 || localY >= BUTTON_SIZE || localX < 0) {
            return null;
        }
        int index = (int) (localX / BUTTON_STRIDE);
        if (index >= BUTTON_ORDER.size() || localX - index * BUTTON_STRIDE >= BUTTON_SIZE) {
            return null;
        }
        return BUTTON_ORDER.get(index);
    }

    /** The status groups below the buttons. */
    List<Group> groups() {
        if (!redstone.supported()) {
            return List.of(
                    new Group(Component.translatable(STATUS_KEY), StatLines.UNKNOWN),
                    new Group(Component.translatable(SIGNAL_KEY), StatLines.UNKNOWN),
                    new Group(Component.translatable(REQUIRED_KEY), StatLines.UNKNOWN));
        }
        return List.of(
                new Group(Component.translatable(STATUS_KEY), enabledText(), enabledColor()),
                new Group(Component.translatable(SIGNAL_KEY),
                        Component.translatable(redstone.signal() ? PRESENT_KEY : ABSENT_KEY)),
                new Group(Component.translatable(REQUIRED_KEY), Component.translatable(requiredKey(redstone.mode()))));
    }

    private MutableComponent enabledText() {
        return Component.translatable(redstone.enabled() ? ENABLED_KEY : DISABLED_KEY);
    }

    private int enabledColor() {
        return redstone.enabled() ? ENABLED_COLOR : DISABLED_COLOR;
    }

    private static String requiredKey(RedstoneMode mode) {
        return switch (mode) {
            case IGNORED -> REQUIRED_NONE_KEY;
            case LOW -> REQUIRED_LOW_KEY;
            case HIGH -> REQUIRED_HIGH_KEY;
        };
    }

    private static void drawRing(TabCanvas canvas, int x, int y, int size, int argb) {
        canvas.fill(x, y, x + size, y + 1, argb);
        canvas.fill(x, y + size - 1, x + size, y + size, argb);
        canvas.fill(x, y + 1, x + 1, y + size - 1, argb);
        canvas.fill(x + size - 1, y + 1, x + size, y + size - 1, argb);
    }
}
