package com.logistics.forestry.farm;

import com.logistics.core.machine.tier.MachineTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** The farm screen's title, status readout, status-lamp tooltip and side-tab key. */
final class FarmScreenText {

    static final String TITLE = "gui.logistics.farm.title";
    static final String TOOLTIP_REACH = "tooltip.logistics.farm.reach";
    static final String TOOLTIP_PLOTS = "tooltip.logistics.farm.plots";
    static final String BLOCK_KEY_PREFIX = "block.logistics.";
    static final String FALLBACK_TAB_KEY = "farm";

    private FarmScreenText() {}

    /** "Crop Farm (Industrial)". */
    static Component title(Component blockName, MachineTier tier) {
        return Component.translatable(TITLE, blockName, Component.translatable(tier.translationKey()));
    }

    /** The status as the screen and Jade show it; waiting for seeds carries the Empty Plot count. */
    static Component status(FarmStatus status, int emptyPlots) {
        return status == FarmStatus.WAITING_FOR_SEEDS
                ? Component.translatable(status.translationKey(), emptyPlots)
                : Component.translatable(status.translationKey());
    }

    /** The status, then the Reach, then the Plot and Empty Plot counts. */
    static List<Component> statusTooltip(FarmStatus status, int reachSize, int plots, int emptyPlots) {
        return List.of(
                status(status, emptyPlots),
                Component.translatable(TOOLTIP_REACH, reachSize).withStyle(ChatFormatting.GRAY),
                Component.translatable(TOOLTIP_PLOTS, plots, emptyPlots).withStyle(ChatFormatting.GRAY));
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
}
