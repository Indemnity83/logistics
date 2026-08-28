package com.logistics.gametest;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wires {@link RecipeLoadingGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class RecipeLoadingGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "recipe/all_logistics_recipe_resources_load", 100, RecipeLoadingGameTestBody::allLogisticsRecipeResourcesLoad));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private RecipeLoadingGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "recipe/loading", TESTS, FUNCTIONS);
    }
}
