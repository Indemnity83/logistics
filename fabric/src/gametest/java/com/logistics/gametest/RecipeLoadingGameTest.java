package com.logistics.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the recipe-loading GameTest. Test logic lives in
 * {@link RecipeLoadingGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * this method only carries the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class RecipeLoadingGameTest {

    @GameTest
    public void allLogisticsRecipeResourcesLoad(GameTestHelper context) {
        RecipeLoadingGameTestBody.allLogisticsRecipeResourcesLoad(context);
    }
}
