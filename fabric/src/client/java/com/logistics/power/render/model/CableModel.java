package com.logistics.power.render.model;

import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.cable.CableTier;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public final class CableModel implements BlockStateModel, FabricBlockStateModel {
    private final Identifier textureId;
    private TextureAtlasSprite spriteCache;

    public CableModel(CableTier tier) {
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
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {}

    @Override
    public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
        if (!(world.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return Integer.valueOf(0);
        }
        return Integer.valueOf(cable.getRenderConnectionMask());
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<Direction> cullTest) {
        if (!(world.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return;
        }

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
                    emitter.chunkLayer(ChunkSectionLayer.CUTOUT);
                    emitter.emit();
                },
                cable.getRenderConnectionMask(),
                CableGeometry.TextureUvs.fromSprite(sprite),
                cullTest);
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
}
