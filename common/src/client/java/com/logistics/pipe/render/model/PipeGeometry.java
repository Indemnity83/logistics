package com.logistics.pipe.render.model;

import com.logistics.core.lib.client.render.QuadSink;
import com.logistics.core.lib.pipe.PipeShape;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Generates pipe geometry in code, replacing the per-part JSON models. Each {@link PipeShape}
 * is an axis-aligned cuboid with per-face UVs (and rotations) lifted verbatim from the original
 * model JSONs. Arms are emitted in their base NORTH orientation; the block-entity renderer
 * rotates them to the connected face.
 *
 * <p>The vertex winding and UV-corner convention match the (visually verified) cable path
 * (see {@code CableGeometry}/{@code VanillaQuadBaker}), so rotation-0 faces render identically
 * to the baked models.
 */
public final class PipeGeometry {
    private PipeGeometry() {}

    /** Emit the quads for {@code shape}, textured from {@code sprite} (atlas-mapped UVs). */
    public static void emit(QuadSink sink, PipeShape shape, TextureAtlasSprite sprite) {
        Box box = boxFor(shape);
        for (Face f : facesFor(shape)) {
            emitFace(sink, f.dir(), box, f.u0(), f.v0(), f.u1(), f.v1(), f.rotation(), sprite);
        }
    }

    /** Cuboid bounds in 0..16 model space. */
    private record Box(float x0, float y0, float z0, float x1, float y1, float z1) {}

    /** One face: direction, local UV rect in 0..16, and texture rotation (0/90/180/270). */
    private record Face(Direction dir, float u0, float v0, float u1, float v1, int rotation) {}

    private static Box boxFor(PipeShape shape) {
        return switch (shape) {
            case CORE -> new Box(4, 4, 4, 12, 12, 12);
            case ARM -> new Box(4, 4, 0, 12, 12, 4);
            case ARM_EXTENDED -> new Box(4, 4, -2, 12, 12, 4);
            case MARKINGS -> new Box(3.999f, 3.999f, 3.999f, 12.001f, 12.001f, 12.001f);
        };
    }

    private static Face[] facesFor(PipeShape shape) {
        return switch (shape) {
            case CORE -> uniformFaces(0, 0, 16, 16, 0);
            case MARKINGS -> uniformFaces(0, 4, 8, 12, 0);
            case ARM -> new Face[] {
                new Face(Direction.NORTH, 0, 0, 2, 2, 0),
                new Face(Direction.EAST, 0, 8, 16, 16, 90),
                new Face(Direction.SOUTH, 0, 0, 2, 2, 0),
                new Face(Direction.WEST, 0, 8, 16, 16, 270),
                new Face(Direction.UP, 0, 8, 16, 16, 0),
                new Face(Direction.DOWN, 0, 8, 16, 16, 180),
            };
            case ARM_EXTENDED -> new Face[] {
                new Face(Direction.NORTH, 0, 0, 2, 2, 0),
                new Face(Direction.EAST, 0, 4, 16, 16, 90),
                new Face(Direction.SOUTH, 0, 0, 2, 2, 0),
                new Face(Direction.WEST, 0, 4, 16, 16, 270),
                new Face(Direction.UP, 0, 4, 16, 16, 0),
                new Face(Direction.DOWN, 0, 16, 16, 4, 0),
            };
        };
    }

    private static Face[] uniformFaces(float u0, float v0, float u1, float v1, int rotation) {
        Direction[] dirs = Direction.values();
        Face[] faces = new Face[dirs.length];
        for (int i = 0; i < dirs.length; i++) {
            faces[i] = new Face(dirs[i], u0, v0, u1, v1, rotation);
        }
        return faces;
    }

    private static void emitFace(QuadSink sink, Direction face, Box box,
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

        // Per-vertex UVs in the order (p3, p2, p1, p0) — same convention as CableGeometry.emitFace
        // for rotation 0; texture rotation cyclically shifts the assignment.
        float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
        int shift = ((rotation / 90) % 4 + 4) % 4;

        float[] p0 = boxVertex(face, 0, box);
        float[] p1 = boxVertex(face, 1, box);
        float[] p2 = boxVertex(face, 2, box);
        float[] p3 = boxVertex(face, 3, box);
        float[] a = uv[shift % 4];
        float[] b = uv[(1 + shift) % 4];
        float[] c = uv[(2 + shift) % 4];
        float[] d = uv[(3 + shift) % 4];
        sink.emitQuad(face, null,
                p3[0], p3[1], p3[2], a[0], a[1],
                p2[0], p2[1], p2[2], b[0], b[1],
                p1[0], p1[1], p1[2], c[0], c[1],
                p0[0], p0[1], p0[2], d[0], d[1]);
    }

    private static float[] boxVertex(Direction face, int index, Box box) {
        float x0 = box.x0() / 16f, y0 = box.y0() / 16f, z0 = box.z0() / 16f;
        float x1 = box.x1() / 16f, y1 = box.y1() / 16f, z1 = box.z1() / 16f;
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
