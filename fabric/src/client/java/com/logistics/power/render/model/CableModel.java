package com.logistics.power.render.model;

import com.logistics.power.cable.CableBlock;
import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.cable.CableTier;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class CableModel implements BakedModel, FabricBakedModel {
    private final ResourceLocation textureId;
    private TextureAtlasSprite spriteCache;

    public CableModel(CableTier tier) {
        this.textureId = ResourceLocation.fromNamespaceAndPath("logistics", "block/power/" + tier.id());
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return sprite();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter world, BlockState state, BlockPos pos,
            Supplier<RandomSource> randomSupplier, RenderContext context) {
        if (!(world.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return;
        }

        QuadEmitter emitter = context.getEmitter();
        TextureAtlasSprite sprite = sprite();
        float spriteU0 = sprite.getU0();
        float spriteV0 = sprite.getV0();
        float spriteUSpan = sprite.getU1() - spriteU0;
        float spriteVSpan = sprite.getV1() - spriteV0;

        int connectionMask = cable.getRenderConnectionMask();
        int armMask = armMask(connectionMask);

        float armSideU0 = spriteU0 + spriteUSpan * (1f / 16f);
        float armSideU1 = spriteU0 + spriteUSpan * (7f / 16f);
        float armSideV0 = spriteV0 + spriteVSpan * (12f / 16f);
        float armSideV1 = spriteV0 + spriteVSpan;
        float armFrontU0 = spriteU0 + spriteUSpan * (12f / 16f);
        float armFrontU1 = spriteU0 + spriteUSpan;
        float armFrontV0 = spriteV0;
        float armFrontV1 = spriteV0 + spriteVSpan * (4f / 16f);
        float armTopU0 = spriteU0 + spriteUSpan * (12f / 16f);
        float armTopU1 = spriteU0 + spriteUSpan;
        float armTopV0 = spriteV0 + spriteVSpan * (5f / 16f);
        float armTopV1 = spriteV0 + spriteVSpan * (11f / 16f);
        float nubU0 = spriteU0 + spriteUSpan * (13f / 16f);
        float nubU1 = spriteU0 + spriteUSpan * (15f / 16f);
        float nubV0 = spriteV0 + spriteVSpan * (1f / 16f);
        float nubV1 = spriteV0 + spriteVSpan * (3f / 16f);
        float plugNorthU0 = spriteU0 + spriteUSpan * (12f / 16f);
        float plugNorthU1 = spriteU0 + spriteUSpan;
        float plugNorthV0 = spriteV0 + spriteVSpan * (12f / 16f);
        float plugNorthV1 = spriteV0 + spriteVSpan;
        float plugEastU0 = spriteU0 + spriteUSpan * (12f / 16f);
        float plugEastU1 = spriteU0 + spriteUSpan * (15f / 16f);
        float plugEastV0 = spriteV0;
        float plugEastV1 = spriteV0 + spriteVSpan * (4f / 16f);
        float plugSouthU0 = spriteU0 + spriteUSpan * (12f / 16f);
        float plugSouthU1 = spriteU0 + spriteUSpan;
        float plugSouthV0 = spriteV0;
        float plugSouthV1 = spriteV0 + spriteVSpan * (4f / 16f);
        float plugWestU0 = spriteU0 + spriteUSpan * (13f / 16f);
        float plugWestU1 = spriteU0 + spriteUSpan;
        float plugWestV0 = spriteV0;
        float plugWestV1 = spriteV0 + spriteVSpan * (4f / 16f);
        float plugUpU0 = spriteU0 + spriteUSpan * (12f / 16f);
        float plugUpU1 = spriteU0 + spriteUSpan;
        float plugUpV0 = spriteV0 + spriteVSpan * (1f / 16f);
        float plugUpV1 = spriteV0 + spriteVSpan * (4f / 16f);
        float plugDownU0 = spriteU0 + spriteUSpan * (12f / 16f);
        float plugDownU1 = spriteU0 + spriteUSpan;
        float plugDownV0 = spriteV0;
        float plugDownV1 = spriteV0 + spriteVSpan * (3f / 16f);

        if (armMask == 0) {
            emitStubCore(emitter, spriteU0, spriteV0, spriteUSpan, spriteVSpan);
            emitNub(emitter, Direction.NORTH, nubU0, nubV0, nubU1, nubV1);
            emitNub(emitter, Direction.SOUTH, nubU0, nubV0, nubU1, nubV1);
        } else {
            for (Direction dir : Direction.values()) {
                if (isConnected(armMask, dir)) continue;

                int tile = TILE_FOR_EDGE_MASK[edgeMaskForFace(armMask, dir)];
                int tx = (tile % 4) * 4;
                int ty = (tile / 4) * 4;
                float u0 = spriteU0 + spriteUSpan * (tx / 16f);
                float u1 = spriteU0 + spriteUSpan * ((tx + 4) / 16f);
                float v0 = spriteV0 + spriteVSpan * (ty / 16f);
                float v1 = spriteV0 + spriteVSpan * ((ty + 4) / 16f);
                emitFace(emitter, dir,
                        CORE_MIN, CORE_MIN, CORE_MIN,
                        CORE_MAX, CORE_MAX, CORE_MAX,
                        u0, v0, u1, v1);
            }
        }

        for (Direction dir : Direction.values()) {
            if (!isConnected(armMask, dir)) continue;
            emitArm(emitter, dir, context,
                    armSideU0, armSideV0, armSideU1, armSideV1,
                    armFrontU0, armFrontV0, armFrontU1, armFrontV1,
                    armTopU0, armTopV0, armTopU1, armTopV1);
        }

        if (Integer.bitCount(armMask) == 1) {
            Direction armDir = Direction.from3DDataValue(Integer.numberOfTrailingZeros(armMask));
            emitNub(emitter, armDir.getOpposite(), nubU0, nubV0, nubU1, nubV1);
        }

        for (Direction dir : Direction.values()) {
            if (connectionType(connectionMask, dir) == CableBlock.ConnectionType.DEVICE) {
                emitPlug(emitter, dir,
                        plugNorthU0, plugNorthV0, plugNorthU1, plugNorthV1,
                        plugEastU0, plugEastV0, plugEastU1, plugEastV1,
                        plugSouthU0, plugSouthV0, plugSouthU1, plugSouthV1,
                        plugWestU0, plugWestV0, plugWestU1, plugWestV1,
                        plugUpU0, plugUpV0, plugUpU1, plugUpV1,
                        plugDownU0, plugDownV0, plugDownU1, plugDownV1);
            }
        }
    }

    private void emitStubCore(QuadEmitter emitter, float spriteU0, float spriteV0, float spriteUSpan, float spriteVSpan) {
        emitStubCoreFace(emitter, Direction.NORTH, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 12, 0, 16, 4);
        emitStubCoreFace(emitter, Direction.EAST, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 2, 12, 6, 16);
        emitStubCoreFace(emitter, Direction.SOUTH, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 12, 0, 16, 4);
        emitStubCoreFace(emitter, Direction.WEST, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 2, 12, 6, 16);
        emitStubCoreFace(emitter, Direction.UP, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 12, 4, 16, 8);
        emitStubCoreFace(emitter, Direction.DOWN, spriteU0, spriteV0, spriteUSpan, spriteVSpan, 12, 4, 16, 8);
    }

    private void emitStubCoreFace(QuadEmitter emitter, Direction face,
            float spriteU0, float spriteV0, float spriteUSpan, float spriteVSpan,
            int tx0, int ty0, int tx1, int ty1) {
        emitFace(emitter, face,
                CORE_MIN, CORE_MIN, CORE_MIN,
                CORE_MAX, CORE_MAX, CORE_MAX,
                spriteU0 + spriteUSpan * (tx0 / 16f),
                spriteV0 + spriteVSpan * (ty0 / 16f),
                spriteU0 + spriteUSpan * (tx1 / 16f),
                spriteV0 + spriteVSpan * (ty1 / 16f));
    }

    private void emitArm(QuadEmitter emitter, Direction dir, RenderContext context,
            float sideU0, float sideV0, float sideU1, float sideV1,
            float frontU0, float frontV0, float frontU1, float frontV1,
            float topU0, float topV0, float topU1, float topV1) {
        for (Direction face : Direction.values()) {
            if (face == Direction.SOUTH) continue;
            Direction rotatedFace = rotateFromNorth(face, dir);
            if (face == Direction.NORTH) {
                if (!context.isFaceCulled(rotatedFace)) continue;
                emitRotatedBoxFace(emitter, face, dir,
                        ARM_MODEL_MIN_X, ARM_MODEL_MIN_Y, ARM_MODEL_MIN_Z,
                        ARM_MODEL_MAX_X, ARM_MODEL_MAX_Y, ARM_MODEL_MAX_Z,
                        frontU0, frontV0, frontU1, frontV1, true);
            } else if (face == Direction.UP || face == Direction.DOWN) {
                emitRotatedBoxFace(emitter, face, dir,
                        ARM_MODEL_MIN_X, ARM_MODEL_MIN_Y, ARM_MODEL_MIN_Z,
                        ARM_MODEL_MAX_X, ARM_MODEL_MAX_Y, ARM_MODEL_MAX_Z,
                        topU0, topV0, topU1, topV1, false);
            } else {
                emitRotatedBoxFace(emitter, face, dir,
                        ARM_MODEL_MIN_X, ARM_MODEL_MIN_Y, ARM_MODEL_MIN_Z,
                        ARM_MODEL_MAX_X, ARM_MODEL_MAX_Y, ARM_MODEL_MAX_Z,
                        sideU0, sideV0, sideU1, sideV1, false);
            }
        }
    }

    private void emitNub(QuadEmitter emitter, Direction dir, float u0, float v0, float u1, float v1) {
        for (Direction face : Direction.values()) {
            if (face == Direction.SOUTH) continue;
            emitRotatedBoxFace(emitter, face, dir,
                    NUB_MODEL_MIN_X, NUB_MODEL_MIN_Y, NUB_MODEL_MIN_Z,
                    NUB_MODEL_MAX_X, NUB_MODEL_MAX_Y, NUB_MODEL_MAX_Z,
                    u0, v0, u1, v1, false);
        }
    }

    private void emitPlug(QuadEmitter emitter, Direction dir,
            float northU0, float northV0, float northU1, float northV1,
            float eastU0, float eastV0, float eastU1, float eastV1,
            float southU0, float southV0, float southU1, float southV1,
            float westU0, float westV0, float westU1, float westV1,
            float upU0, float upV0, float upU1, float upV1,
            float downU0, float downV0, float downU1, float downV1) {
        emitRotatedBoxFace(emitter, Direction.NORTH, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                northU0, northV0, northU1, northV1, false);
        emitRotatedBoxFace(emitter, Direction.EAST, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                eastU0, eastV0, eastU1, eastV1, false);
        emitRotatedBoxFace(emitter, Direction.SOUTH, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                southU0, southV0, southU1, southV1, false);
        emitRotatedBoxFace(emitter, Direction.WEST, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                westU0, westV0, westU1, westV1, false);
        emitRotatedBoxFace(emitter, Direction.UP, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                upU0, upV0, upU1, upV1, false);
        emitRotatedBoxFace(emitter, Direction.DOWN, dir,
                PLUG_MODEL_MIN_X, PLUG_MODEL_MIN_Y, PLUG_MODEL_MIN_Z,
                PLUG_MODEL_MAX_X, PLUG_MODEL_MAX_Y, PLUG_MODEL_MAX_Z,
                downU0, downV0, downU1, downV1, false);
    }

    private static void emitRotatedBoxFace(QuadEmitter emitter, Direction face, Direction targetDir,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float u0, float v0, float u1, float v1, boolean cullable) {
        float p0x, p0y, p0z, p1x, p1y, p1z, p2x, p2y, p2z, p3x, p3y, p3z;
        switch (face) {
            case DOWN -> {
                p0x = x0; p0y = y0; p0z = z1;
                p1x = x1; p1y = y0; p1z = z1;
                p2x = x1; p2y = y0; p2z = z0;
                p3x = x0; p3y = y0; p3z = z0;
            }
            case UP -> {
                p0x = x0; p0y = y1; p0z = z0;
                p1x = x1; p1y = y1; p1z = z0;
                p2x = x1; p2y = y1; p2z = z1;
                p3x = x0; p3y = y1; p3z = z1;
            }
            case NORTH -> {
                p0x = x1; p0y = y1; p0z = z0;
                p1x = x0; p1y = y1; p1z = z0;
                p2x = x0; p2y = y0; p2z = z0;
                p3x = x1; p3y = y0; p3z = z0;
            }
            case SOUTH -> {
                p0x = x0; p0y = y1; p0z = z1;
                p1x = x1; p1y = y1; p1z = z1;
                p2x = x1; p2y = y0; p2z = z1;
                p3x = x0; p3y = y0; p3z = z1;
            }
            case WEST -> {
                p0x = x0; p0y = y1; p0z = z0;
                p1x = x0; p1y = y1; p1z = z1;
                p2x = x0; p2y = y0; p2z = z1;
                p3x = x0; p3y = y0; p3z = z0;
            }
            case EAST -> {
                p0x = x1; p0y = y1; p0z = z1;
                p1x = x1; p1y = y1; p1z = z0;
                p2x = x1; p2y = y0; p2z = z0;
                p3x = x1; p3y = y0; p3z = z1;
            }
            default -> {
                return;
            }
        }

        emitRotatedVertex(emitter, 0, targetDir, p3x, p3y, p3z, u0, v1);
        emitRotatedVertex(emitter, 1, targetDir, p2x, p2y, p2z, u1, v1);
        emitRotatedVertex(emitter, 2, targetDir, p1x, p1y, p1z, u1, v0);
        emitRotatedVertex(emitter, 3, targetDir, p0x, p0y, p0z, u0, v0);
        Direction rotatedFace = rotateFromNorth(face, targetDir);
        emitter.nominalFace(rotatedFace);
        emitter.cullFace(cullable ? rotatedFace : null);
        emitter.color(-1, -1, -1, -1);
        emitter.emit();
    }

    private static void emitRotatedVertex(QuadEmitter emitter, int vertexIndex, Direction targetDir,
            float x, float y, float z, float u, float v) {
        float dx = x - 0.5f;
        float dy = y - 0.5f;
        float dz = z - 0.5f;
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

        emitter.pos(vertexIndex, rx + 0.5f, ry + 0.5f, rz + 0.5f).uv(vertexIndex, u, v);
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

    private TextureAtlasSprite sprite() {
        TextureAtlasSprite sprite = spriteCache;
        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(textureId);
            spriteCache = sprite;
        }
        return sprite;
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

    private static void emitFace(QuadEmitter emitter, Direction face,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float u0, float v0, float u1, float v1) {
        float p0x, p0y, p0z, p1x, p1y, p1z, p2x, p2y, p2z, p3x, p3y, p3z;
        switch (face) {
            case DOWN -> {
                p0x = x0; p0y = y0; p0z = z1;
                p1x = x1; p1y = y0; p1z = z1;
                p2x = x1; p2y = y0; p2z = z0;
                p3x = x0; p3y = y0; p3z = z0;
            }
            case UP -> {
                p0x = x0; p0y = y1; p0z = z0;
                p1x = x1; p1y = y1; p1z = z0;
                p2x = x1; p2y = y1; p2z = z1;
                p3x = x0; p3y = y1; p3z = z1;
            }
            case NORTH -> {
                p0x = x1; p0y = y1; p0z = z0;
                p1x = x0; p1y = y1; p1z = z0;
                p2x = x0; p2y = y0; p2z = z0;
                p3x = x1; p3y = y0; p3z = z0;
            }
            case SOUTH -> {
                p0x = x0; p0y = y1; p0z = z1;
                p1x = x1; p1y = y1; p1z = z1;
                p2x = x1; p2y = y0; p2z = z1;
                p3x = x0; p3y = y0; p3z = z1;
            }
            case WEST -> {
                p0x = x0; p0y = y1; p0z = z0;
                p1x = x0; p1y = y1; p1z = z1;
                p2x = x0; p2y = y0; p2z = z1;
                p3x = x0; p3y = y0; p3z = z0;
            }
            case EAST -> {
                p0x = x1; p0y = y1; p0z = z1;
                p1x = x1; p1y = y1; p1z = z0;
                p2x = x1; p2y = y0; p2z = z0;
                p3x = x1; p3y = y0; p3z = z1;
            }
            default -> {
                return;
            }
        }

        emitter.pos(0, p3x, p3y, p3z).uv(0, u0, v1);
        emitter.pos(1, p2x, p2y, p2z).uv(1, u1, v1);
        emitter.pos(2, p1x, p1y, p1z).uv(2, u1, v0);
        emitter.pos(3, p0x, p0y, p0z).uv(3, u0, v0);
        emitter.nominalFace(face);
        emitter.cullFace(null);
        emitter.color(-1, -1, -1, -1);
        emitter.emit();
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
