package com.logistics.core.lib.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Builds vanilla {@link BakedQuad}s from raw vertex floats and groups them into a single
 * {@link BlockModelPart}, using only loader-agnostic Minecraft APIs (no Fabric FRAPI,
 * no NeoForge quad builders).
 *
 * <p>Procedural geometry emitters (e.g. {@code CableGeometry}, {@code PipeGeometry}) feed
 * quads through {@link #quad}, which has the same shape as their {@code QuadSink} functional
 * interface, so it can be passed as a method reference ({@code baker::quad}). The caller wraps
 * {@link #toPart()} in a {@code BlockStateModel} and submits it via
 * {@code SubmitNodeCollector.submitBlockModel}.
 *
 * <p>Conventions, matching the geometry emitters: vertex positions are block-space (0..1) and
 * UVs are already atlas coordinates (mapped through the sprite), so UVs are packed directly.
 * The cull face is ignored — a block-entity renderer is not part of the chunk mesh, so its
 * faces are not neighbor-culled; every emitted quad is kept as a general (null-direction) quad.
 * The render layer is chosen by the caller at submit time, so it is not carried on the quad.
 */
public final class VanillaQuadBaker {
    private final TextureAtlasSprite sprite;
    private final int tintIndex;
    private final boolean shade;
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
        quads.add(new BakedQuad(
                new Vector3f(x0, y0, z0),
                new Vector3f(x1, y1, z1),
                new Vector3f(x2, y2, z2),
                new Vector3f(x3, y3, z3),
                UVPair.pack(u0, v0),
                UVPair.pack(u1, v1),
                UVPair.pack(u2, v2),
                UVPair.pack(u3, v3),
                tintIndex,
                nominalFace,
                sprite,
                shade,
                lightEmission));
    }

    public boolean isEmpty() {
        return quads.isEmpty();
    }

    public BlockModelPart toPart() {
        return new SimplePart(List.copyOf(quads), sprite);
    }

    private record SimplePart(List<BakedQuad> quads, TextureAtlasSprite sprite) implements BlockModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return sprite;
        }
    }
}
