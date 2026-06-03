package com.logistics.core.lib.client.render;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-agnostic sink for procedurally generated quads. Geometry emitters (e.g.
 * {@code PipeGeometry}) push quads here; {@link VanillaQuadBaker#quad} matches this shape, so
 * {@code baker::quad} can be passed as the sink.
 *
 * <p>Vertices are block-space (0..1); UVs are atlas coordinates. {@code cullFace} may be null.
 */
@FunctionalInterface
public interface QuadSink {
    void emitQuad(Direction nominalFace, @Nullable Direction cullFace,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3);
}
