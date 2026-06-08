package com.logistics.core.lib.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Builds vanilla {@link BakedQuad}s from raw vertex floats, using only loader-agnostic Minecraft
 * APIs (no Fabric FRAPI, no NeoForge quad builders).
 *
 * <p><b>1.21.1 variant.</b> Unlike the newer branches — whose {@code BakedQuad} is a
 * {@code Vector3f}-positions record wrapped in a {@code BlockStateModel}/{@code BlockModelPart}
 * and submitted via {@code SubmitNodeCollector.submitBlockModel} — MC 1.21.1's {@code BakedQuad}
 * is the classic {@code int[]}-vertex form drawn directly with {@code VertexConsumer.putBulkData}.
 * So this baker emits {@code int[32]} quads (the {@code DefaultVertexFormat.BLOCK} layout vanilla's
 * {@code FaceBakery} produces) and exposes the {@link #toQuads() quad list} for the renderer's
 * existing {@code putBulkData} loop.
 *
 * <p>Procedural geometry emitters (e.g. {@code PipeGeometry}) feed quads through {@link #quad},
 * which has the same shape as their {@code QuadSink} functional interface, so it can be passed as a
 * method reference ({@code baker::quad}).
 *
 * <p>Conventions, matching the geometry emitters: vertex positions are block-space (0..1) and UVs
 * are already atlas coordinates (mapped through the sprite), so they are packed directly. The cull
 * face is ignored — a block-entity renderer is not part of the chunk mesh, so its faces are not
 * neighbor-culled. The render layer and tint color are applied by the caller at draw time.
 *
 * <p>Per-vertex layout (8 ints, mirroring {@code FaceBakery}): {@code [xBits, yBits, zBits,
 * color=-1 (white), uBits, vBits, light=0, normal=0]}. Light and normal are left 0 exactly as
 * vanilla baked quads are — {@code putBulkData} supplies the light and derives the normal.
 */
public final class VanillaQuadBaker {
    private static final int INTS_PER_VERTEX = 8;

    private final TextureAtlasSprite sprite;
    private final int tintIndex;
    private final boolean shade;
    // Kept for call-site parity with the newer branches; MC 1.21.1's BakedQuad has no light-emission field.
    private final int lightEmission;
    private final List<BakedQuad> quads = new ArrayList<>();

    public VanillaQuadBaker(TextureAtlasSprite sprite, int tintIndex, boolean shade, int lightEmission) {
        this.sprite = sprite;
        this.tintIndex = tintIndex;
        this.shade = shade;
        this.lightEmission = lightEmission;
    }

    /** {@code QuadSink}-shaped entry point; {@code cullFace} is intentionally ignored (see class doc). */
    public void quad(Direction nominalFace, @Nullable Direction cullFace,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3) {
        int[] v = new int[INTS_PER_VERTEX * 4];
        putVertex(v, 0, x0, y0, z0, u0, v0);
        putVertex(v, 1, x1, y1, z1, u1, v1);
        putVertex(v, 2, x2, y2, z2, u2, v2);
        putVertex(v, 3, x3, y3, z3, u3, v3);
        quads.add(new BakedQuad(v, tintIndex, nominalFace, sprite, shade));
    }

    private static void putVertex(int[] data, int vertex, float x, float y, float z, float u, float w) {
        int base = vertex * INTS_PER_VERTEX;
        data[base] = Float.floatToRawIntBits(x);
        data[base + 1] = Float.floatToRawIntBits(y);
        data[base + 2] = Float.floatToRawIntBits(z);
        data[base + 3] = -1; // packed white; per-quad tint is applied at draw time
        data[base + 4] = Float.floatToRawIntBits(u);
        data[base + 5] = Float.floatToRawIntBits(w);
        // data[base + 6] (light) and data[base + 7] (normal) stay 0 — same as vanilla baked quads.
    }

    public boolean isEmpty() {
        return quads.isEmpty();
    }

    public List<BakedQuad> toQuads() {
        return quads;
    }
}
