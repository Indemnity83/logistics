package com.logistics.core.lib.pipe;

/**
 * The distinct cuboid geometries a pipe renders. Every pipe part-model reduces to one of
 * these shapes plus a texture (and optional tint); {@code PipeGeometry} generates the quads
 * for each shape in code, replacing the per-part JSON models.
 */
public enum PipeShape {
    /** Center cube, 4..12 on every axis. */
    CORE,
    /** Connection arm toward NORTH (-Z), 4..12 X/Y, 0..4 Z. Rotated to the target face by the renderer. */
    ARM,
    /** Longer arm for inventory connections, 4..12 X/Y, -2..4 Z. */
    ARM_EXTENDED,
    /** Tinted overlay cube, slightly larger than the core, for pipe markings. */
    MARKINGS
}
