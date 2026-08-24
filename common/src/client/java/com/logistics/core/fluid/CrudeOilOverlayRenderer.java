package com.logistics.core.fluid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws the camera-submersion overlay for Crude Oil (issue #836) — a full-screen darkening fill while
 * the local player's eyes are inside it, so vision is heavily obscured rather than clear like water.
 *
 * <p>{@link GuiGraphicsExtractor} is vanilla (not loader-specific), so the draw itself is shared; only
 * the per-loader hook that calls it each frame lives outside this module (a {@code HudElement} on
 * Fabric, native {@code IClientFluidTypeExtensions} on NeoForge — see {@code NeoForgeClientSetup}).
 */
public final class CrudeOilOverlayRenderer {
    // ~80% opaque black. Deliberately a flat fill rather than a texture/vignette — the issue explicitly
    // allows a simple overlay ("does not need to reproduce Minecraft's normal underwater rendering").
    private static final int OVERLAY_ARGB = 0xCC000000;

    private CrudeOilOverlayRenderer() {}

    /** {@code true} when the overlay should be drawn this frame. */
    public static boolean shouldRender() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && CrudeOilSubmersion.isEyeInCrudeOil(mc.player);
    }

    public static void render(GuiGraphicsExtractor gui, int width, int height) {
        gui.fill(0, 0, width, height, OVERLAY_ARGB);
    }
}
