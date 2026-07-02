package com.logistics.fabric;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class FabricBiomeModifications {
    private FabricBiomeModifications() {}

    public static void register() {
        // Tin ore worldgen: separate features for stone (rare) and deepslate (abundant)
        // Stone: ~7% of copper's effective rate (1 vein/chunk vs copper's 16)
        // Deepslate: ~80% of copper's rate (13 veins/chunk)
        // Use LogisticsMod.modId() to avoid core/ prefix
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("tin_ore_stone").toIdentifier())
        );
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("tin_ore_deepslate").toIdentifier())
        );

        // Apatite ore worldgen: large veins (up to 48 blocks) spawning above Y 60
        // 2 veins per chunk, Y 60-256 (uniform distribution)
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("apatite_ore_stone").toIdentifier())
        );

        // Bog earth: clay-style disks on the submerged swamp floor (matches the NeoForge biome modifier
        // on #c:is_swamp; same step as vanilla's disk_clay/disk_grass)
        BiomeModifications.addFeature(
            BiomeSelectors.tag(TagKey.create(Registries.BIOME, ResourceId.in("c", "is_swamp").toIdentifier())),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("bog_earth").toIdentifier())
        );

        // Crude oil seeps (matches the NeoForge biome modifiers): surface pools that pick their ore shell
        // by biome. One roll across the overworld (like lava lakes), plus a bonus roll in arid biomes so
        // deserts + badlands are a bit oilier. Oil shale stays underground gravel veins in the dry biomes.
        var isDesert = TagKey.create(Registries.BIOME, ResourceId.in("c", "is_desert").toIdentifier());
        var isBadlands = TagKey.create(Registries.BIOME, ResourceId.in("c", "is_badlands").toIdentifier());
        var isSavanna = TagKey.create(Registries.BIOME, ResourceId.in("c", "is_savanna").toIdentifier());
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.LAKES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("oil_seep").toIdentifier())
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(isDesert).or(BiomeSelectors.tag(isBadlands)),
            GenerationStep.Decoration.LAKES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("oil_seep_arid").toIdentifier())
        );
        BiomeModifications.addFeature(
            BiomeSelectors.tag(isDesert).or(BiomeSelectors.tag(isBadlands)).or(BiomeSelectors.tag(isSavanna)),
            GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(Registries.PLACED_FEATURE, LogisticsMod.modId("oil_shale").toIdentifier())
        );
    }
}
