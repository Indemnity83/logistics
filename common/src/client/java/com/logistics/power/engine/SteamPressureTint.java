package com.logistics.power.engine;

/**
 * Maps a Steam Engine's stored-pressure fraction (0..1) to an ARGB tint for the piston shaft: a cool
 * blue while building, bright white through the operating band, warming to amber near the top. Shared
 * by both loaders' engine block-entity renderers.
 */
public final class SteamPressureTint {

    private SteamPressureTint() {}

    // Gradient stops (r, g, b): low pressure -> operating -> high pressure.
    private static final int[] LOW = {0x6F, 0xA8, 0xDC}; // soft blue
    private static final int[] MID = {0xFF, 0xFF, 0xFF}; // white
    private static final int[] HIGH = {0xFF, 0x9B, 0x3D}; // amber

    /** Opaque ARGB tint for the given pressure fraction (clamped to [0,1]). */
    public static int color(double fraction) {
        double f = Math.clamp(fraction, 0.0, 1.0);
        int r;
        int g;
        int b;
        if (f < 0.5) {
            double t = f / 0.5;
            r = lerp(LOW[0], MID[0], t);
            g = lerp(LOW[1], MID[1], t);
            b = lerp(LOW[2], MID[2], t);
        } else {
            double t = (f - 0.5) / 0.5;
            r = lerp(MID[0], HIGH[0], t);
            g = lerp(MID[1], HIGH[1], t);
            b = lerp(MID[2], HIGH[2], t);
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }
}
