package com.logistics.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the recipe-loading GameTest. Test logic lives in
 * {@link RecipeLoadingGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * this method only carries the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class RecipeLoadingGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void allLogisticsRecipeResourcesLoad(GameTestHelper context) {
        RecipeLoadingGameTestBody.allLogisticsRecipeResourcesLoad(context);
    }
}
