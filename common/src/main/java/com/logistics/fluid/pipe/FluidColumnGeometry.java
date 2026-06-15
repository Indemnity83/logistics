package com.logistics.fluid.pipe;

/**
 * Pure geometry for the fluid column drawn inside a pipe.
 *
 * <p>The fluid surface is derived from the fill ratio over the <em>core</em> span alone, so it is independent
 * of which arms are connected — a horizontal pipe and an up/down pipe show the same level at the same fill.
 * A down arm sits below the surface and is full whenever the pipe holds any fluid (fluid settles); an up arm
 * only fills once the pipe is full (fluid only climbs under pressure). Holds no Minecraft types and works in
 * plain block-space coordinates, so it is unit-testable without a renderer.
 */
public final class FluidColumnGeometry {

    private FluidColumnGeometry() {}

    /** Vertical extent of the rendered column, plus the core surface height (for horizontal arms). */
    public record Column(float bottom, float top, float surface) {}

    /**
     * @param ratio      fill fraction, 0..1
     * @param down       whether a down arm is connected
     * @param up         whether an up arm is connected
     * @param coreBottom bottom of the core fluid (block-space Y)
     * @param coreTop    top of the core fluid (block-space Y)
     * @param floor      bottom of a down arm (block-space Y)
     * @param ceiling    top of an up arm (block-space Y)
     */
    public static Column of(
            float ratio, boolean down, boolean up, float coreBottom, float coreTop, float floor, float ceiling) {
        float surface = coreBottom + ratio * (coreTop - coreBottom);
        float bottom = down ? floor : coreBottom;
        float top = up && ratio >= 1.0F ? ceiling : surface;
        return new Column(bottom, top, surface);
    }
}
