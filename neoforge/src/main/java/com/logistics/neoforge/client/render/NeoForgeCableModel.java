package com.logistics.neoforge.client.render;

import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.cable.CableTier;
import com.logistics.power.render.model.CableGeometry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

public final class NeoForgeCableModel implements DynamicBlockStateModel {
    private final Identifier textureId;
    private volatile TextureAtlasSprite spriteCache;

    public NeoForgeCableModel(CableTier tier) {
        this.textureId = Identifier.fromNamespaceAndPath("logistics", "block/power/" + tier.id());
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return sprite();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts) {
        // DynamicBlockStateModel: no-op for the static variant
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockModelPart> parts) {
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return;
        }

        TextureAtlasSprite sprite = sprite();
        CablePartBuilder builder = new CablePartBuilder(sprite);
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
        private final List<BakedQuad> unculledQuads = new ArrayList<>();
        private final EnumMap<Direction, List<BakedQuad>> culledQuads = new EnumMap<>(Direction.class);

        private CablePartBuilder(TextureAtlasSprite sprite) {
            this.sprite = sprite;
        }

        @Override
        public void emitQuad(Direction nominalFace, @Nullable Direction cullFace,
                float x0, float y0, float z0, float u0, float v0,
                float x1, float y1, float z1, float u1, float v1,
                float x2, float y2, float z2, float u2, float v2,
                float x3, float y3, float z3, float u3, float v3) {
            QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
            baker.setDirection(nominalFace);
            baker.setSprite(sprite);
            baker.setShade(true);
            baker.setHasAmbientOcclusion(true);
            baker.addVertex(x0, y0, z0).setUv(u0, v0).setColor(-1);
            baker.addVertex(x1, y1, z1).setUv(u1, v1).setColor(-1);
            baker.addVertex(x2, y2, z2).setUv(u2, v2).setColor(-1);
            baker.addVertex(x3, y3, z3).setUv(u3, v3).setColor(-1);
            BakedQuad quad = baker.bakeQuad();

            if (cullFace == null) {
                unculledQuads.add(quad);
            } else {
                culledQuads.computeIfAbsent(cullFace, ignored -> new ArrayList<>()).add(quad);
            }
        }

        private BlockModelPart build() {
            return new CablePart(unculledQuads, culledQuads, sprite);
        }
    }

    private record CablePart(
            List<BakedQuad> unculledQuads,
            EnumMap<Direction, List<BakedQuad>> culledQuads,
            TextureAtlasSprite sprite) implements BlockModelPart {
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

        @Override
        public TextureAtlasSprite particleIcon() {
            return sprite;
        }
    }
}
