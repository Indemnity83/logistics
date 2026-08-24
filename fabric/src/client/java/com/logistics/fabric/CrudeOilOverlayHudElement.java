package com.logistics.fabric;

import com.logistics.core.fluid.CrudeOilOverlayRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier; // raw-id-ok: arbitrary HUD layer key, not a data-driven asset reference

/**
 * Fabric-only wiring for {@link CrudeOilOverlayRenderer} (issue #836) — Fabric API has no equivalent to
 * NeoForge's {@code IClientFluidTypeExtensions#getRenderOverlayTexture}, so the overlay is drawn as its
 * own HUD layer instead. Registered first so vanilla HUD elements (crosshair, hotbar, bars) still draw
 * on top and stay legible, matching how vanilla's own underwater overlay layers.
 */
public final class CrudeOilOverlayHudElement implements HudElement {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("logistics", "crude_oil_overlay"); // raw-id-ok

    public static void register() {
        HudElementRegistry.addFirst(ID, new CrudeOilOverlayHudElement());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, DeltaTracker deltaTracker) {
        if (CrudeOilOverlayRenderer.shouldRender()) {
            CrudeOilOverlayRenderer.render(gui, gui.guiWidth(), gui.guiHeight());
        }
    }
}
