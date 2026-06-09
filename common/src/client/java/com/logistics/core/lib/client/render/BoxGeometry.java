package com.logistics.core.lib.client.render;

import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Generic code generator for axis-aligned box models (single texture), replacing simple
 * authored JSON block models. Each {@link Element} is a cuboid with a subset of faces; each
 * {@link Face} carries its local UV rect (0..16) and texture rotation (0/90/180/270), lifted
 * verbatim from the model JSON. Vanilla ignores {@code texture_size}, so UVs map to the sprite
 * as {@code uv/16 * span} regardless of the texture's pixel dimensions.
 *
 * <p>Vertex winding and UV convention match the (visually verified) cable/pipe path. Geometry
 * is emitted in the model's authored orientation; renderers apply any animation via the pose stack.
 */
public final class BoxGeometry {
    private BoxGeometry() {}

    /** A single cuboid face: direction, local UV rect in 0..16 (corners may be reversed to flip), and rotation. */
    public record Face(Direction dir, float u0, float v0, float u1, float v1, int rotation) {}

    /** A cuboid in 0..16 model space with an explicit list of faces (only the faces that exist are drawn). */
    public record Element(float x0, float y0, float z0, float x1, float y1, float z1, List<Face> faces) {}

    /** Emit all elements' faces into {@code sink}, textured from {@code sprite}. */
    public static void emit(QuadSink sink, List<Element> elements, TextureAtlasSprite sprite) {
        for (Element e : elements) {
            for (Face f : e.faces()) {
                emitFace(sink, f.dir(), e, f.u0(), f.v0(), f.u1(), f.v1(), f.rotation(), sprite);
            }
        }
    }

    private static void emitFace(QuadSink sink, Direction face, Element b,
            float localU0, float localV0, float localU1, float localV1, int rotation,
            TextureAtlasSprite sprite) {
        float spriteU0 = sprite.getU0();
        float spriteV0 = sprite.getV0();
        float spriteUSpan = sprite.getU1() - spriteU0;
        float spriteVSpan = sprite.getV1() - spriteV0;
        float u0 = spriteU0 + spriteUSpan * (localU0 / 16f);
        float u1 = spriteU0 + spriteUSpan * (localU1 / 16f);
        float v0 = spriteV0 + spriteVSpan * (localV0 / 16f);
        float v1 = spriteV0 + spriteVSpan * (localV1 / 16f);

        float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
        int shift = ((rotation / 90) % 4 + 4) % 4;
        float[] a = uv[shift % 4];
        float[] bb = uv[(1 + shift) % 4];
        float[] c = uv[(2 + shift) % 4];
        float[] d = uv[(3 + shift) % 4];

        float[] p0 = vertex(face, 0, b);
        float[] p1 = vertex(face, 1, b);
        float[] p2 = vertex(face, 2, b);
        float[] p3 = vertex(face, 3, b);
        sink.emitQuad(face, null,
                p3[0], p3[1], p3[2], a[0], a[1],
                p2[0], p2[1], p2[2], bb[0], bb[1],
                p1[0], p1[1], p1[2], c[0], c[1],
                p0[0], p0[1], p0[2], d[0], d[1]);
    }

    private static float[] vertex(Direction face, int index, Element b) {
        float x0 = b.x0() / 16f, y0 = b.y0() / 16f, z0 = b.z0() / 16f;
        float x1 = b.x1() / 16f, y1 = b.y1() / 16f, z1 = b.z1() / 16f;
        return switch (face) {
            case DOWN -> switch (index) {
                case 0 -> new float[] {x0, y0, z1};
                case 1 -> new float[] {x1, y0, z1};
                case 2 -> new float[] {x1, y0, z0};
                default -> new float[] {x0, y0, z0};
            };
            case UP -> switch (index) {
                case 0 -> new float[] {x0, y1, z0};
                case 1 -> new float[] {x1, y1, z0};
                case 2 -> new float[] {x1, y1, z1};
                default -> new float[] {x0, y1, z1};
            };
            case NORTH -> switch (index) {
                case 0 -> new float[] {x1, y1, z0};
                case 1 -> new float[] {x0, y1, z0};
                case 2 -> new float[] {x0, y0, z0};
                default -> new float[] {x1, y0, z0};
            };
            case SOUTH -> switch (index) {
                case 0 -> new float[] {x0, y1, z1};
                case 1 -> new float[] {x1, y1, z1};
                case 2 -> new float[] {x1, y0, z1};
                default -> new float[] {x0, y0, z1};
            };
            case WEST -> switch (index) {
                case 0 -> new float[] {x0, y1, z0};
                case 1 -> new float[] {x0, y1, z1};
                case 2 -> new float[] {x0, y0, z1};
                default -> new float[] {x0, y0, z0};
            };
            case EAST -> switch (index) {
                case 0 -> new float[] {x1, y1, z1};
                case 1 -> new float[] {x1, y1, z0};
                case 2 -> new float[] {x1, y0, z0};
                default -> new float[] {x1, y0, z1};
            };
        };
    }
}
