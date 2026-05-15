package com.logistics.neoforge.client.render;

import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.cable.CableTier;
import com.logistics.power.render.model.CableGeometry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.jetbrains.annotations.Nullable;

public final class NeoForgeCableModel implements DynamicBlockStateModel {
    private final Identifier textureId;
    private volatile TextureAtlasSprite spriteCache;

    public NeoForgeCableModel(CableTier tier) {
        this.textureId = Identifier.fromNamespaceAndPath("logistics", "block/power/" + tier.id());
    }

    @Override
    public Material.Baked particleMaterial() {
        return new Material.Baked(sprite(), false);
    }

    @Override
    public int materialFlags() {
        return 0;
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> parts) {
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return;
        }

        TextureAtlasSprite sprite = sprite();
        CablePartBuilder builder = new CablePartBuilder(sprite, particleMaterial(), materialFlags());
        CableGeometry.emit(builder, cable.getRenderConnectionMask(), CableGeometry.TextureUvs.fromSprite(sprite), direction -> true);
        parts.add(builder.build());
    }

    private TextureAtlasSprite sprite() {
        TextureAtlasSprite sprite = spriteCache;
        if (sprite == null) {
            sprite = Minecraft.getInstance().getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(textureId);
            spriteCache = sprite;
        }
        return sprite;
    }

    private static final class CablePartBuilder implements CableGeometry.QuadSink {
        private final TextureAtlasSprite sprite;
        private final Material.Baked particleMaterial;
        private final int materialFlags;
        private final List<BakedQuad> unculledQuads = new ArrayList<>();
        private final EnumMap<Direction, List<BakedQuad>> culledQuads = new EnumMap<>(Direction.class);

        private CablePartBuilder(TextureAtlasSprite sprite, Material.Baked particleMaterial, int materialFlags) {
            this.sprite = sprite;
            this.particleMaterial = particleMaterial;
            this.materialFlags = materialFlags;
        }

        @Override
        public void emitQuad(Direction nominalFace, @Nullable Direction cullFace,
                float x0, float y0, float z0, float u0, float v0,
                float x1, float y1, float z1, float u1, float v1,
                float x2, float y2, float z2, float u2, float v2,
                float x3, float y3, float z3, float u3, float v3) {
            MutableQuad quad = new MutableQuad();
            quad.setPosition(0, x0, y0, z0).setUv(0, u0, v0);
            quad.setPosition(1, x1, y1, z1).setUv(1, u1, v1);
            quad.setPosition(2, x2, y2, z2).setUv(2, u2, v2);
            quad.setPosition(3, x3, y3, z3).setUv(3, u3, v3);
            quad.setDirection(nominalFace);
            quad.setSprite(sprite, ChunkSectionLayer.CUTOUT, Sheets.cutoutBlockSheet());
            quad.setColor(0xFFFFFFFF);
            quad.recomputeNormals(false);

            if (cullFace == null) {
                unculledQuads.add(quad.toBakedQuad());
            } else {
                culledQuads.computeIfAbsent(cullFace, ignored -> new ArrayList<>()).add(quad.toBakedQuad());
            }
        }

        private BlockStateModelPart build() {
            return new CablePart(unculledQuads, culledQuads, particleMaterial, materialFlags);
        }
    }

    private record CablePart(
            List<BakedQuad> unculledQuads,
            EnumMap<Direction, List<BakedQuad>> culledQuads,
            Material.Baked particleMaterial,
            int materialFlags) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            if (direction == null) {
                return unculledQuads;
            }
            return culledQuads.getOrDefault(direction, List.of());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }
    }
}
