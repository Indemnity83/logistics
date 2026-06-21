package com.logistics.neoforge.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

/**
 * NeoForge 21.1 custom geometry loader for power cables.
 *
 * <p>Implements {@link IGeometryLoader} to deserialize cable model JSON and returns a
 * {@link NeoForgeCableModel} as the baked model. The cable sprite is resolved from the model's
 * {@code "cable"} texture variable through the baking context, so it is stitched into the block
 * atlas like any normal model texture.
 *
 * <p>The loader ID is {@code logistics:cable_model}. Reference it from the cable block model JSON
 * using {@code "loader": "logistics:cable_model"}.
 */
public final class NeoForgeCableBlockModelDefinition
        implements IGeometryLoader<NeoForgeCableBlockModelDefinition.CableGeometry> {

    public static final NeoForgeCableBlockModelDefinition INSTANCE =
            new NeoForgeCableBlockModelDefinition();

    private NeoForgeCableBlockModelDefinition() {}

    @Override
    public CableGeometry read(JsonObject jsonObject, JsonDeserializationContext context) {
        return new CableGeometry();
    }

    /** Unbaked geometry for a cable. The tier is encoded in the model's texture, not here. */
    public static final class CableGeometry implements IUnbakedGeometry<CableGeometry> {
        private CableGeometry() {}

        @Override
        public BakedModel bake(
                IGeometryBakingContext context,
                ModelBaker baker,
                Function<Material, TextureAtlasSprite> spriteGetter,
                ModelState modelState,
                ItemOverrides overrides) {
            TextureAtlasSprite sprite = spriteGetter.apply(context.getMaterial("cable"));
            return new NeoForgeCableModel(sprite);
        }
    }
}
