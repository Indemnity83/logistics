package com.logistics.power.render.model;

import com.logistics.power.cable.CableBlock;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

public final class CableGeometry {
    private CableGeometry() {}

    public interface QuadSink {
        void emitQuad(Direction nominalFace, @Nullable Direction cullFace,
                float x0, float y0, float z0, float u0, float v0,
                float x1, float y1, float z1, float u1, float v1,
                float x2, float y2, float z2, float u2, float v2,
                float x3, float y3, float z3, float u3, float v3);
    }

    public record TextureUvs(
            float spriteU0, float spriteV0, float spriteUSpan, float spriteVSpan,
            float armSideU0, float armSideV0, float armSideU1, float armSideV1,
            float armFrontU0, float armFrontV0, float armFrontU1, float armFrontV1,
            float armTopU0, float armTopV0, float armTopU1, float armTopV1,
            float nubU0, float nubV0, float nubU1, float nubV1,
            float plugNorthU0, float plugNorthV0, float plugNorthU1, float plugNorthV1,
            float plugEastU0, float plugEastV0, float plugEastU1, float plugEastV1,
            float plugSouthU0, float plugSouthV0, float plugSouthU1, float plugSouthV1,
            float plugWestU0, float plugWestV0, float plugWestU1, float plugWestV1,
            float plugUpU0, float plugUpV0, float plugUpU1, float plugUpV1,
            float plugDownU0, float plugDownV0, float plugDownU1, float plugDownV1) {
        public static TextureUvs fromSprite(TextureAtlasSprite sprite) {
            float spriteU0 = sprite.getU0();
            float spriteV0 = sprite.getV0();
            float spriteUSpan = sprite.getU1() - spriteU0;
            float spriteVSpan = sprite.getV1() - spriteV0;
            return new TextureUvs(
                    spriteU0, spriteV0, spriteUSpan, spriteVSpan,
                    spriteU0 + spriteUSpan * (1f / 16f), spriteV0 + spriteVSpan * (12f / 16f),
                    spriteU0 + spriteUSpan * (7f / 16f), spriteV0 + spriteVSpan,
                    spriteU0 + spriteUSpan * (12f / 16f), spriteV0,
                    spriteU0 + spriteUSpan, spriteV0 + spriteVSpan * (4f / 16f),
                    spriteU0 + spriteUSpan * (12f / 16f), spriteV0 + spriteVSpan * (5f / 16f),
                    spriteU0 + spriteUSpan, spriteV0 + spriteVSpan * (11f / 16f),
                    spriteU0 + spriteUSpan * (13f / 16f), spriteV0 + spriteVSpan * (1f / 16f),
                    spriteU0 + spriteUSpan * (15f / 16f), spriteV0 + spriteVSpan * (3f / 16f),
                    spriteU0 + spriteUSpan * (12f / 16f), spriteV0 + spriteVSpan * (12f / 16f),
                    spriteU0 + spriteUSpan, spriteV0 + spriteVSpan,
                    spriteU0 + spriteUSpan * (12f / 16f), spriteV0,
                    spriteU0 + spriteUSpan * (15f / 16f), spriteV0 + spriteVSpan * (4f / 16f),
                    spriteU0 + spriteUSpan * (12f / 16f), spriteV0,
                    spriteU0 + spriteUSpan, spriteV0 + spriteVSpan * (4f / 16f),
                    spriteU0 + spriteUSpan * (13f / 16f), spriteV0,
                    spriteU0 + spriteUSpan, spriteV0 + spriteVSpan * (4f / 16f),
                    spriteU0 + spriteUSpan * (12f / 16f), spriteV0 + spriteVSpan * (1f / 16f),
                    spriteU0 + spriteUSpan, spriteV0 + spriteVSpan * (4f / 16f),
                    spriteU0 + spriteUSpan * (12f / 16f), spriteV0,
                    spriteU0 + spriteUSpan, spriteV0 + spriteVSpan * (3f / 16f));
        }
    }

    public static void emit(QuadSink sink, int connectionMask, TextureUvs uvs, java.util.function.Predicate<Direction> cullTest) {
        int armMask = armMask(connectionMask);

        if (armMask == 0) {
            emitStubCore(sink, uvs.spriteU0(), uvs.spriteV0(), uvs.spriteUSpan(), uvs.spriteVSpan());
            emitNub(sink, Direction.NORTH, uvs.nubU0(), uvs.nubV0(), uvs.nubU1(), uvs.nubV1());
            emitNub(sink, Direction.SOUTH, uvs.nubU0(), uvs.nubV0(), uvs.nubU1(), uvs.nubV1());
        } else {
            for (Direction dir : Direction.values()) {
                if (isConnected(armMask, dir)) continue;

                int tile = TILE_FOR_EDGE_MASK[edgeMaskForFace(armMask, dir)];
                int tx = (tile % 4) * 4;
                int ty = (tile / 4) * 4;
                float u0 = uvs.spriteU0() + uvs.spriteUSpan() * (tx / 16f);
                float u1 = uvs.spriteU0() + uvs.spriteUSpan() * ((tx + 4) / 16f);
                float v0 = uvs.spriteV0() + uvs.spriteVSpan() * (ty / 16f);
                float v1 = uvs.spriteV0() + uvs.spriteVSpan() * ((ty + 4) / 16f);
                emitFace(sink, dir, null,
                        CORE_MIN, CORE_MIN, CORE_MIN,
                        CORE_MAX, CORE_MAX, CORE_MAX,
                        u0, v0, u1, v1);
            }
        }

        for (Direction dir : Direction.values()) {
            if (!isConnected(armMask, dir)) continue;
            emitArm(sink, dir, cullTest,
                    uvs.armSideU0(), uvs.armSideV0(), uvs.armSideU1(), uvs.armSideV1(),
                    uvs.armFrontU0(), uvs.armFrontV0(), uvs.armFrontU1(), uvs.armFrontV1(),
                    uvs.armTopU0(), uvs.armTopV0(), uvs.armTopU1(), uvs.armTopV1());
        }

        if (Integer.bitCount(armMask) == 1) {
            Direction armDir = Direction.from3DDataValue(Integer.numberOfTrailingZeros(armMask));
            emitNub(sink, armDir.getOpposite(), uvs.nubU0(), uvs.nubV0(), uvs.nubU1(), uvs.nubV1());
        }

        for (Direction dir : Direction.values()) {
            if (connectionType(connectionMask, dir) == CableBlock.ConnectionType.DEVICE) {
                emitPlug(sink, dir, uvs);
            }
        }
    }

    private static void emitStubCore(QuadSink sink, float spriteU0, float spriteV0, float spriteUSpan, float spriteVSpan) {
        emitStubCoreFace(sink, Direction.NORTH, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 12, 0, 16, 4);
        emitStubCoreFace(sink, Direction.EAST, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 2, 12, 6, 16);
        emitStubCoreFace(sink, Direction.SOUTH, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 12, 0, 16, 4);
        emitStubCoreFace(sink, Direction.WEST, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 2, 12, 6, 16);
        emitStubCoreFace(sink, Direction.UP, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 12, 4, 16, 8);
        emitStubCoreFace(sink, Direction.DOWN, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 12, 4, 16, 8);
    }

    private static void emitStubCoreFace(QuadSink sink, Direction face,
            float spriteU0, float spriteV0, float spriteUSpan, float spriteVSpan,
            int tx0, int ty0, int tx1, int ty1) {
        emitFace(sink, face, null,
                CORE_MIN, CORE_MIN, CORE_MIN,
                CORE_MAX, CORE_MAX, CORE_MAX,
                spriteU0 + spriteUSpan * (tx0 / 16f),
                spriteV0 + spriteVSpan * (ty0 / 16f),
                spriteU0 + spriteUSpan * (tx1 / 16f),
                spriteV0 + spriteVSpan * (ty1 / 16f));
    }

    private static void emitArm(QuadSink sink, Direction dir, java.util.function.Predicate<Direction> cullTest,
            float sideU0, float sideV0, float sideU1, float sideV1,
            float frontU0, float frontV0, float frontU1, float frontV1,
            float topU0, float topV0, float topU1, float topV1) {
        for (Direction face : Direction.values()) {
            if (face == Direction.SOUTH) continue;
            Direction rotatedFace = rotateFromNorth(face, dir);
            if (face == Direction.NORTH) {
                if (!cullTest.test(rotatedFace)) continue;
                emitRotatedBoxFace(sink, face, dir,
                        ARM_MODEL_MIN_X, ARM_MODEL_MIN_Y, ARM_MODEL_MIN_Z,
                        ARM_MODEL_MAX_X, ARM_MODEL_MAX_Y, ARM_MODEL_MAX_Z,
                        frontU0, frontV0, frontU1, frontV1, true);
            } else if (face == Direction.UP || face == Direction.DOWN) {
                emitRotatedBoxFace(sink, face, dir,
                        ARM_MODEL_MIN_X, ARM_MODEL_MIN_Y, ARM_MODEL_MIN_Z,
                        ARM_MODEL_MAX_X, ARM_MODEL_MAX_Y, ARM_MODEL_MAX_Z,
                        topU0, topV0, topU1, topV1, false);
            } else {
                emitRotatedBoxFace(sink, face, dir,
                        ARM_MODEL_MIN_X, ARM_MODEL_MIN_Y, ARM_MODEL_MIN_Z,
                        ARM_MODEL_MAX_X, ARM_MODEL_MAX_Y, ARM_MODEL_MAX_Z,
                        sideU0, sideV0, sideU1, sideV1, false);
            }
        }
    }

    private static void emitNub(QuadSink sink, Direction dir, float u0, float v0, float u1, float v1) {
        for (Direction face : Direction.values()) {
            if (face == Direction.SOUTH) continue;
            emitRotatedBoxFace(sink, face, dir,
                    NUB_MODEL_MIN_X, NUB_MODEL_MIN_Y, NUB_MODEL_MIN_Z,
                    NUB_MODEL_MAX_X, NUB_MODEL_MAX_Y, NUB_MODEL_MAX_Z,
                    u0, v0, u1, v1, false);
        }
    }

    private static void emitPlug(QuadSink sink, Direction dir, TextureUvs uvs) {
        emitRotatedBoxFace(sink, Direction.NORTH, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                uvs.plugNorthU0(), uvs.plugNorthV0(), uvs.plugNorthU1(), uvs.plugNorthV1(), false);
        emitRotatedBoxFace(sink, Direction.EAST, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                uvs.plugEastU0(), uvs.plugEastV0(), uvs.plugEastU1(), uvs.plugEastV1(), false);
        emitRotatedBoxFace(sink, Direction.SOUTH, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                uvs.plugSouthU0(), uvs.plugSouthV0(), uvs.plugSouthU1(), uvs.plugSouthV1(), false);
        emitRotatedBoxFace(sink, Direction.WEST, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                uvs.plugWestU0(), uvs.plugWestV0(), uvs.plugWestU1(), uvs.plugWestV1(), false);
        emitRotatedBoxFace(sink, Direction.UP, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                uvs.plugUpU0(), uvs.plugUpV0(), uvs.plugUpU1(), uvs.plugUpV1(), false);
        emitRotatedBoxFace(sink, Direction.DOWN, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                uvs.plugDownU0(), uvs.plugDownV0(), uvs.plugDownU1(), uvs.plugDownV1(), false);
    }

    private static void emitRotatedBoxFace(QuadSink sink, Direction face, Direction targetDir,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float u0, float v0, float u1, float v1, boolean cullable) {
        float[] p0 = boxVertex(face, 0, x0, y0, z0, x1, y1, z1);
        float[] p1 = boxVertex(face, 1, x0, y0, z0, x1, y1, z1);
        float[] p2 = boxVertex(face, 2, x0, y0, z0, x1, y1, z1);
        float[] p3 = boxVertex(face, 3, x0, y0, z0, x1, y1, z1);
        Direction rotatedFace = rotateFromNorth(face, targetDir);
        emitRotatedQuad(sink, targetDir, rotatedFace, cullable ? rotatedFace : null,
                p3, u0, v1, p2, u1, v1, p1, u1, v0, p0, u0, v0);
    }

    private static float[] boxVertex(Direction face, int index,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (face) {
            case DOWN -> switch (index) {
                case 0 -> new float[]{x0, y0, z1};
                case 1 -> new float[]{x1, y0, z1};
                case 2 -> new float[]{x1, y0, z0};
                default -> new float[]{x0, y0, z0};
            };
            case UP -> switch (index) {
                case 0 -> new float[]{x0, y1, z0};
                case 1 -> new float[]{x1, y1, z0};
                case 2 -> new float[]{x1, y1, z1};
                default -> new float[]{x0, y1, z1};
            };
            case NORTH -> switch (index) {
                case 0 -> new float[]{x1, y1, z0};
                case 1 -> new float[]{x0, y1, z0};
                case 2 -> new float[]{x0, y0, z0};
                default -> new float[]{x1, y0, z0};
            };
            case SOUTH -> switch (index) {
                case 0 -> new float[]{x0, y1, z1};
                case 1 -> new float[]{x1, y1, z1};
                case 2 -> new float[]{x1, y0, z1};
                default -> new float[]{x0, y0, z1};
            };
            case WEST -> switch (index) {
                case 0 -> new float[]{x0, y1, z0};
                case 1 -> new float[]{x0, y1, z1};
                case 2 -> new float[]{x0, y0, z1};
                default -> new float[]{x0, y0, z0};
            };
            case EAST -> switch (index) {
                case 0 -> new float[]{x1, y1, z1};
                case 1 -> new float[]{x1, y1, z0};
                case 2 -> new float[]{x1, y0, z0};
                default -> new float[]{x1, y0, z1};
            };
        };
    }

    private static void emitRotatedQuad(QuadSink sink, Direction targetDir, Direction nominalFace, @Nullable Direction cullFace,
            float[] p0, float u0, float v0, float[] p1, float u1, float v1,
            float[] p2, float u2, float v2, float[] p3, float u3, float v3) {
        float[] r0 = rotateVertex(targetDir, p0);
        float[] r1 = rotateVertex(targetDir, p1);
        float[] r2 = rotateVertex(targetDir, p2);
        float[] r3 = rotateVertex(targetDir, p3);
        sink.emitQuad(nominalFace, cullFace,
                r0[0], r0[1], r0[2], u0, v0,
                r1[0], r1[1], r1[2], u1, v1,
                r2[0], r2[1], r2[2], u2, v2,
                r3[0], r3[1], r3[2], u3, v3);
    }

    private static float[] rotateVertex(Direction targetDir, float[] p) {
        float dx = p[0] - 0.5f;
        float dy = p[1] - 0.5f;
        float dz = p[2] - 0.5f;
        float rx = dx;
        float ry = dy;
        float rz = dz;

        switch (targetDir) {
            case EAST -> { rx = -dz; rz = dx; }
            case SOUTH -> { rx = -dx; rz = -dz; }
            case WEST -> { rx = dz; rz = -dx; }
            case UP -> { ry = -dz; rz = dy; }
            case DOWN -> { ry = dz; rz = -dy; }
            default -> { }
        }

        return new float[]{rx + 0.5f, ry + 0.5f, rz + 0.5f};
    }

    private static void emitFace(QuadSink sink, Direction face, @Nullable Direction cullFace,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float u0, float v0, float u1, float v1) {
        float[] p0 = boxVertex(face, 0, x0, y0, z0, x1, y1, z1);
        float[] p1 = boxVertex(face, 1, x0, y0, z0, x1, y1, z1);
        float[] p2 = boxVertex(face, 2, x0, y0, z0, x1, y1, z1);
        float[] p3 = boxVertex(face, 3, x0, y0, z0, x1, y1, z1);
        sink.emitQuad(face, cullFace,
                p3[0], p3[1], p3[2], u0, v1,
                p2[0], p2[1], p2[2], u1, v1,
                p1[0], p1[1], p1[2], u1, v0,
                p0[0], p0[1], p0[2], u0, v0);
    }

    private static Direction rotateFromNorth(Direction face, Direction targetDir) {
        int x = directionX(face);
        int y = directionY(face);
        int z = directionZ(face);
        int rx = x;
        int ry = y;
        int rz = z;

        switch (targetDir) {
            case EAST -> { rx = -z; rz = x; }
            case SOUTH -> { rx = -x; rz = -z; }
            case WEST -> { rx = z; rz = -x; }
            case UP -> { ry = -z; rz = y; }
            case DOWN -> { ry = z; rz = -y; }
            default -> { }
        }

        return directionFromVector(rx, ry, rz);
    }

    private static int directionX(Direction direction) {
        return switch (direction) {
            case EAST -> 1;
            case WEST -> -1;
            default -> 0;
        };
    }

    private static int directionY(Direction direction) {
        return switch (direction) {
            case UP -> 1;
            case DOWN -> -1;
            default -> 0;
        };
    }

    private static int directionZ(Direction direction) {
        return switch (direction) {
            case SOUTH -> 1;
            case NORTH -> -1;
            default -> 0;
        };
    }

    private static Direction directionFromVector(int x, int y, int z) {
        if (x > 0) return Direction.EAST;
        if (x < 0) return Direction.WEST;
        if (y > 0) return Direction.UP;
        if (y < 0) return Direction.DOWN;
        if (z > 0) return Direction.SOUTH;
        return Direction.NORTH;
    }

    private static boolean isConnected(int mask, Direction dir) {
        return (mask & (1 << dir.get3DDataValue())) != 0;
    }

    private static int armMask(int connectionMask) {
        int armMask = 0;
        for (Direction dir : Direction.values()) {
            if (connectionType(connectionMask, dir) != CableBlock.ConnectionType.NONE) {
                armMask |= 1 << dir.get3DDataValue();
            }
        }
        return armMask;
    }

    private static CableBlock.ConnectionType connectionType(int connectionMask, Direction dir) {
        int ordinal = (connectionMask >> (dir.get3DDataValue() * 2)) & 0b11;
        CableBlock.ConnectionType[] values = CableBlock.ConnectionType.values();
        return ordinal < values.length ? values[ordinal] : CableBlock.ConnectionType.NONE;
    }

    private static int edgeMaskForFace(int connectionMask, Direction face) {
        Direction top, right, bottom, left;
        switch (face) {
            case DOWN -> { top = Direction.SOUTH; right = Direction.EAST; bottom = Direction.NORTH; left = Direction.WEST; }
            case UP -> { top = Direction.NORTH; right = Direction.EAST; bottom = Direction.SOUTH; left = Direction.WEST; }
            case NORTH -> { top = Direction.UP; right = Direction.WEST; bottom = Direction.DOWN; left = Direction.EAST; }
            case SOUTH -> { top = Direction.UP; right = Direction.EAST; bottom = Direction.DOWN; left = Direction.WEST; }
            case WEST -> { top = Direction.UP; right = Direction.SOUTH; bottom = Direction.DOWN; left = Direction.NORTH; }
            case EAST -> { top = Direction.UP; right = Direction.NORTH; bottom = Direction.DOWN; left = Direction.SOUTH; }
            default -> {
                return 0;
            }
        }

        int mask = 0;
        if (isConnected(connectionMask, top)) mask |= 1;
        if (isConnected(connectionMask, right)) mask |= 2;
        if (isConnected(connectionMask, bottom)) mask |= 4;
        if (isConnected(connectionMask, left)) mask |= 8;
        return mask;
    }

    private static final float CORE_MIN = 6f / 16f;
    private static final float CORE_MAX = 10f / 16f;
    private static final float ARM_MODEL_MIN_X = 6f / 16f;
    private static final float ARM_MODEL_MAX_X = 10f / 16f;
    private static final float ARM_MODEL_MIN_Y = 6f / 16f;
    private static final float ARM_MODEL_MAX_Y = 10f / 16f;
    private static final float ARM_MODEL_MIN_Z = 0f;
    private static final float ARM_MODEL_MAX_Z = 6f / 16f;
    private static final float NUB_MODEL_MIN_X = 7f / 16f;
    private static final float NUB_MODEL_MAX_X = 9f / 16f;
    private static final float NUB_MODEL_MIN_Y = 7f / 16f;
    private static final float NUB_MODEL_MAX_Y = 9f / 16f;
    private static final float NUB_MODEL_MIN_Z = 4f / 16f;
    private static final float NUB_MODEL_MAX_Z = 6f / 16f;
    private static final float PLUG_MODEL_MIN_X = 5f / 16f;
    private static final float PLUG_MODEL_MAX_X = 11f / 16f;
    private static final float PLUG_MODEL_MIN_Y = 5f / 16f;
    private static final float PLUG_MODEL_MAX_Y = 11f / 16f;
    private static final float PLUG_MODEL_MIN_Z = 0f;
    private static final float PLUG_MODEL_MAX_Z = 3f / 16f;

    private static final int[] TILE_FOR_EDGE_MASK = {
            3, 11, 12, 8,
            11, 11, 0, 4,
            13, 10, 12, 9,
            2, 6, 1, 5
    };
}
