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
 * Wires {@link ServerDataLoadingGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class ServerDataLoadingGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "server_data/all_logistics_loot_tables_load", 100, ServerDataLoadingGameTestBody::allLogisticsLootTablesLoad),
        new GameTestCase(
            "server_data/every_logistics_block_loot_table_is_loaded",
            100,
            ServerDataLoadingGameTestBody::everyLogisticsBlockLootTableIsLoaded),
        new GameTestCase(
            "server_data/all_logistics_configured_features_load",
            100,
            ServerDataLoadingGameTestBody::allLogisticsConfiguredFeaturesLoad),
        new GameTestCase(
            "server_data/all_logistics_placed_features_load",
            100,
            ServerDataLoadingGameTestBody::allLogisticsPlacedFeaturesLoad));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private ServerDataLoadingGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "server_data/loading", TESTS, FUNCTIONS);
    }
}
