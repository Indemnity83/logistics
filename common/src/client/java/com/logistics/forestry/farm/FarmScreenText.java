package com.logistics.forestry.farm;

import com.logistics.core.machine.tier.MachineTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** The farm screen's title, status-lamp tooltip and button text. */
final class FarmScreenText {

    static final String TITLE = "gui.logistics.farm.title";
    static final String TOOLTIP_AREA = "tooltip.logistics.farm.area";
    static final String TOOLTIP_PLOTS = "tooltip.logistics.farm.plots";
    static final String BUTTON_START = "gui.logistics.farm.start";
    static final String BUTTON_SHOW_AREA = "gui.logistics.farm.show_area";
    static final String TOOLTIP_START = "tooltip.logistics.farm.start";

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

    /** "Start" while surveying, "Show area" once working. */
    static Component buttonLabel(FarmState state) {
        return Component.translatable(state == FarmState.SURVEY ? BUTTON_START : BUTTON_SHOW_AREA);
    }

    /** The button id {@link FarmMenu#clickMenuButton} expects in {@code state}. */
    static int buttonId(FarmState state) {
        return state == FarmState.SURVEY ? FarmMenu.BUTTON_START : FarmMenu.BUTTON_SHOW_AREA;
    }

    /** What pressing Start will clear and skip. */
    static Component startTooltip(int clear, int flagged, int skipped) {
        return Component.translatable(TOOLTIP_START, clear, flagged, skipped);
    }
}
