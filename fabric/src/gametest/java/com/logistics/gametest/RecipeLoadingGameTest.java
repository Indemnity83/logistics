package com.logistics.gametest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Verifies that every recipe resource shipped by Logistics is loaded by Minecraft. */
public class RecipeLoadingGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void allLogisticsRecipeResourcesLoad(GameTestHelper context) {
        var recipes = context.getLevel().getServer().getRecipeManager().getRecipes();
        try {
            URL rootUrl = RecipeLoadingGameTest.class.getClassLoader().getResource("data/logistics/recipe");
            if (rootUrl == null) {
                context.fail("Logistics recipe resource root is missing");
                return;
            }
            Path root = Paths.get(rootUrl.toURI());
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                    String relative = root.relativize(path).toString().replace('\\', '/');
                    String id = "logistics:" + relative.substring(0, relative.length() - ".json".length());
                    if (recipes.stream().noneMatch(holder -> holder.id().toString().equals(id))) {
                        context.fail("Logistics recipe failed to load: " + id);
                        return;
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            context.fail("Could not inspect Logistics recipe resources: " + e.getMessage());
            return;
        }
        context.succeed();
    }
}
