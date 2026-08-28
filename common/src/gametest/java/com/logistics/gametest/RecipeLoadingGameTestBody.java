package com.logistics.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * Shared recipe-loading GameTest body, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires this into each
 * loader's own registration mechanism: Fabric's {@code @GameTest}-annotated
 * {@code RecipeLoadingGameTest} delegates to this method, and NeoForge's
 * {@code RecipeLoadingGameTestRegistration} references it directly as a
 * {@code Consumer<GameTestHelper>} method reference.
 */
public class RecipeLoadingGameTestBody {

    public static void allLogisticsRecipeResourcesLoad(GameTestHelper context) {
        // FileToIdConverter walks the live datapack stack the same way RecipeManager's own reload
        // does (not the raw classpath), so it works the same regardless of how each loader's dev
        // environment lays out mod resources on disk — unlike a ClassLoader.getResource() +
        // Files.walk() approach, which depends on resources being an unpacked directory on the
        // classpath at a specific location.
        ResourceManager resources = context.getLevel().getServer().getResourceManager();
        RecipeManager recipes = context.getLevel().getServer().getRecipeManager();

        var converter = FileToIdConverter.json("recipe");
        for (Identifier fileId : converter.listMatchingResources(resources).keySet()) {
            if (!fileId.getNamespace().equals("logistics")) {
                continue;
            }
            Identifier id = converter.fileToId(fileId);
            if (recipes.getRecipes().stream().noneMatch(holder -> holder.id().identifier().equals(id))) {
                context.fail("Logistics recipe failed to load: " + id);
                return;
            }
        }
        context.succeed();
    }
}
