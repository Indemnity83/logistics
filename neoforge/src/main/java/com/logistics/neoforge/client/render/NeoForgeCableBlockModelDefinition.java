package com.logistics.neoforge.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.logistics.power.cable.CableTier;
import java.util.Locale;
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
 * <p>Implements {@link IGeometryLoader} to deserialize cable blockstate model JSON
 * and returns a {@link NeoForgeCableModel} as the baked model. Registered via
 * {@link net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders}.
 *
 * <p>The loader ID is {@code logistics:cable_model}. Reference it from the cable
 * blockstate model JSON using {@code "loader": "logistics:cable_model"}.
 */
public final class NeoForgeCableBlockModelDefinition
        implements IGeometryLoader<NeoForgeCableBlockModelDefinition.CableGeometry> {

    public static final NeoForgeCableBlockModelDefinition INSTANCE =
            new NeoForgeCableBlockModelDefinition();

    private NeoForgeCableBlockModelDefinition() {}

    @Override
    public CableGeometry read(JsonObject jsonObject, JsonDeserializationContext context) {
        String tierStr = jsonObject.has("tier")
                ? jsonObject.get("tier").getAsString()
                : "copper";
        return new CableGeometry(tierFromId(tierStr));
    }

    private static CableTier tierFromId(String id) {
        for (CableTier tier : CableTier.values()) {
            if (tier.id().equals(id) || tier.name().toLowerCase(Locale.ROOT).equals(id)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Unknown cable tier: " + id);
    }

    /** Unbaked geometry for a specific cable tier. */
    public static final class CableGeometry
            implements IUnbakedGeometry<CableGeometry> {
        private final CableTier tier;

        private CableGeometry(CableTier tier) {
            this.tier = tier;
        }

        @Override
        public BakedModel bake(
                IGeometryBakingContext context,
                ModelBaker baker,
                Function<Material, TextureAtlasSprite> spriteGetter,
                ModelState modelState,
                ItemOverrides overrides) {
            return new NeoForgeCableModel(tier);
        }
    }
}
