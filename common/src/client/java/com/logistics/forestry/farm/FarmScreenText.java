package com.logistics.forestry.farm;

import com.logistics.core.machine.tier.MachineTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** The farm screen's title, status-lamp tooltip, button text and side-tab key. */
final class FarmScreenText {

    static final String TITLE = "gui.logistics.farm.title";
    static final String TOOLTIP_AREA = "tooltip.logistics.farm.area";
    static final String TOOLTIP_PLOTS = "tooltip.logistics.farm.plots";
    static final String BUTTON_START = "gui.logistics.farm.start";
    static final String BUTTON_SHOW_AREA = "gui.logistics.farm.show_area";
    static final String BUTTON_HIDE_AREA = "gui.logistics.farm.hide_area";
    static final String TOOLTIP_START = "tooltip.logistics.farm.start";
    static final String BLOCK_KEY_PREFIX = "block.logistics.";
    static final String FALLBACK_TAB_KEY = "farm";

    private FarmScreenText() {}

    /** "Crop Farm (Industrial)". */
    static Component title(Component blockName, MachineTier tier) {
        return Component.translatable(TITLE, blockName, Component.translatable(tier.translationKey()));
    }

    /** The status, then the tier and footprint, then the plot counts. */
    static List<Component> statusTooltip(FarmStatus status, MachineTier tier, int areaSize, int usable, int blocked) {
        return List.of(
                Component.translatable(status.translationKey()),
                Component.translatable(
                                TOOLTIP_AREA,
                                Component.translatable(tier.translationKey()),
                                areaSize,
                                FarmGeometry.COURTYARD)
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable(TOOLTIP_PLOTS, usable, blocked).withStyle(ChatFormatting.GRAY));
    }

    /** "Start" while surveying; once working, "Hide area" while the preview shows and "Show area" otherwise. */
    static Component buttonLabel(FarmState state, boolean previewShown) {
        if (state == FarmState.SURVEY) {
            return Component.translatable(BUTTON_START);
        }
        return Component.translatable(previewShown ? BUTTON_HIDE_AREA : BUTTON_SHOW_AREA);
    }

    /** The button id {@link FarmMenu#clickMenuButton} expects in {@code state}. */
    static int buttonId(FarmState state) {
        return state == FarmState.SURVEY ? FarmMenu.BUTTON_START : FarmMenu.BUTTON_SHOW_AREA;
    }

    /**
     * The side-tab key for the farm whose menu opened under {@code blockName}: the last segment of
     * its block's translation key ({@code block.logistics.forestry.crop_farm} gives {@code crop_farm}).
     * Every farm shares one menu type, and the block name is the only per-farm detail a client screen
     * has when it is built.
     */
    static String tabKey(Component blockName) {
        if (blockName.getContents() instanceof TranslatableContents name && name.getKey().startsWith(BLOCK_KEY_PREFIX)) {
            String key = name.getKey();
            return key.substring(key.lastIndexOf('.') + 1);
        }
        return FALLBACK_TAB_KEY;
    }

    /** What pressing Start will clear and skip. */
    static Component startTooltip(int clear, int flagged, int skipped) {
        return Component.translatable(TOOLTIP_START, clear, flagged, skipped);
    }
}
