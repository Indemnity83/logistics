package com.logistics.fluid.pipe;

/**
 * Pure geometry for the fluid drawn inside a pipe.
 *
 * <p>Two shapes share this helper. A <em>horizontal</em> run shows a rising level: the surface is derived
 * from the fill ratio over the <em>core</em> span alone, so it is independent of which arms are connected
 * (a horizontal pipe and a vertical pipe show the same level at the same fill). A <em>vertical</em> run
 * shows an expanding full-height column whose square cross-section grows from the center toward the walls
 * as it fills. Holds no Minecraft types and works in plain block-space coordinates, so it is unit-testable
 * without a renderer.
 */
public final class FluidColumnGeometry {

    private FluidColumnGeometry() {}

    /** The fluid surface height for a horizontal pipe's rising-level fill, in block-space Y. */
    public static float surface(float ratio, float coreBottom, float coreTop) {
        return coreBottom + clamp01(ratio) * (coreTop - coreBottom);
    }

    /**
     * Half-width of the expanding vertical column drawn in a pipe with up/down arms: the column is full
     * height and its square cross-section grows from the center toward the walls. Area-proportional, so the
     * cross-section area tracks the fill — {@code halfWidth ∝ sqrt(ratio)} (a half-full vertical pipe reads
     * as ~71% of the full width = half the volume).
     *
     * @param ratio        fill fraction, 0..1
     * @param maxHalfWidth half-width when full, i.e. half the inset cross-section
     */
    public static float halfWidth(float ratio, float maxHalfWidth) {
        return maxHalfWidth * (float) Math.sqrt(clamp01(ratio));
    }

    private static float clamp01(float value) {
        return value < 0.0F ? 0.0F : Math.min(1.0F, value);
    }
}
