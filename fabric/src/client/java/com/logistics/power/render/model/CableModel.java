package com.logistics.power.render.model;

import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.cable.CableTier;
import java.util.Collections;
import java.util.List;
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
        CableGeometry.emit(
                (nominalFace, quadCullFace,
                        x0, y0, z0, u0, v0,
                        x1, y1, z1, u1, v1,
                        x2, y2, z2, u2, v2,
                        x3, y3, z3, u3, v3) -> {
                    emitter.pos(0, x0, y0, z0).uv(0, u0, v0);
                    emitter.pos(1, x1, y1, z1).uv(1, u1, v1);
                    emitter.pos(2, x2, y2, z2).uv(2, u2, v2);
                    emitter.pos(3, x3, y3, z3).uv(3, u3, v3);
                    emitter.nominalFace(nominalFace);
                    emitter.cullFace(quadCullFace);
                    emitter.color(-1, -1, -1, -1);
                    emitter.emit();
                },
                cable.getRenderConnectionMask(),
                CableGeometry.TextureUvs.fromSprite(sprite),
                context::isFaceCulled);
    }

    private TextureAtlasSprite sprite() {
        TextureAtlasSprite sprite = spriteCache;
        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(textureId);
            spriteCache = sprite;
        }
        return sprite;
    }
}
