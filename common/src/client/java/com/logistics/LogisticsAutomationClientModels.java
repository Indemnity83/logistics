package com.logistics;

import com.logistics.core.lib.client.model.ClientModelRegistry;

public final class LogisticsAutomationClientModels {
    public static final ClientModelRegistry.ModelKey BEAM =
            ClientModelRegistry.register(LogisticsAutomation.model("marker_beam"));
    public static final ClientModelRegistry.ModelKey CONSTRUCTION_BEAM =
            ClientModelRegistry.register(LogisticsAutomation.model("construction_beam"));
    public static final ClientModelRegistry.ModelKey ARM =
            ClientModelRegistry.register(LogisticsAutomation.model("laser_quarry_gantry_arm"));
    public static final ClientModelRegistry.ModelKey DRILL =
            ClientModelRegistry.register(LogisticsAutomation.model("laser_quarry_drill"));
    public static final ClientModelRegistry.ModelKey LED_GREEN =
            ClientModelRegistry.register(LogisticsAutomation.model("laser_quarry_led_green"));
    public static final ClientModelRegistry.ModelKey LED_RED =
            ClientModelRegistry.register(LogisticsAutomation.model("laser_quarry_led_red"));
    public static final ClientModelRegistry.ModelKey DISPLAY =
            ClientModelRegistry.register(LogisticsAutomation.model("laser_quarry_display"));
    public static final ClientModelRegistry.ModelKey TOP_HATCH =
            ClientModelRegistry.register(LogisticsAutomation.model("laser_quarry_top_hatch"));

    public static void init() {}

    private LogisticsAutomationClientModels() {}
}
