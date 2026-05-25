package com.logistics.neoforge.client.render;

import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.cable.CableTier;
import com.logistics.power.render.model.CableGeometry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

public final class NeoForgeCableModel implements IDynamicBakedModel {
    /** Stores the cable connection bitmask from the block entity. */
    public static final ModelProperty<Integer> CONNECTION_MASK = new ModelProperty<>();

    private final ResourceLocation textureId;
    private volatile TextureAtlasSprite spriteCache;

    public NeoForgeCableModel(CableTier tier) {
        this.textureId = ResourceLocation.fromNamespaceAndPath("logistics", "block/power/" + tier.id());
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return sprite();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return sprite();
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
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public ModelData getModelData(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            ModelData modelData) {
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            return modelData.derive().with(CONNECTION_MASK, cable.getRenderConnectionMask()).build();
        }
        return modelData;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource random,
            ModelData modelData,
            @Nullable RenderType renderType) {
        Integer mask = modelData.get(CONNECTION_MASK);
        if (mask == null) {
            return List.of();
        }

        TextureAtlasSprite sprite = sprite();
        CablePartBuilder builder = new CablePartBuilder(sprite);
        CableGeometry.emit(
                builder,
                mask,
                CableGeometry.TextureUvs.fromSprite(sprite),
                direction -> true);
        return side == null ? builder.getUnculled() : builder.getCulled(side);
    }

    private TextureAtlasSprite sprite() {
        TextureAtlasSprite sprite = spriteCache;
        if (sprite == null) {
            sprite = Minecraft.getInstance().getModelManager()
                    .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(textureId);
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
            baker.addVertex(x0, y0, z0).setUv(u0, v0).setColor(255, 255, 255, 255);
            baker.addVertex(x1, y1, z1).setUv(u1, v1).setColor(255, 255, 255, 255);
            baker.addVertex(x2, y2, z2).setUv(u2, v2).setColor(255, 255, 255, 255);
            baker.addVertex(x3, y3, z3).setUv(u3, v3).setColor(255, 255, 255, 255);
            BakedQuad quad = baker.bakeQuad();

            if (cullFace == null) {
                unculledQuads.add(quad);
            } else {
                culledQuads.computeIfAbsent(cullFace, d -> new ArrayList<>()).add(quad);
            }
        }

        List<BakedQuad> getUnculled() {
            return unculledQuads;
        }

        List<BakedQuad> getCulled(Direction face) {
            return culledQuads.getOrDefault(face, List.of());
        }
    }
}
