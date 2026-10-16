package com.logistics.forestry.farm;

import com.logistics.core.machine.tier.MachineTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** The farm screen's title and status-lamp tooltip text. */
final class FarmScreenText {

    static final String TITLE = "gui.logistics.farm.title";
    static final String TOOLTIP_AREA = "tooltip.logistics.farm.area";
    static final String TOOLTIP_PLOTS = "tooltip.logistics.farm.plots";

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
}
