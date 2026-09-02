package com.logistics.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link ServerDataLoadingGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class ServerDataLoadingGameTestRegistration {

    private ServerDataLoadingGameTestRegistration() {}

    /**
    * Verifies every shipped loot table deserializes into the live loot registry.
    *
    * <p>Run in-game: /test run logistics-gametest.serverdataloadinggametest.alllogisticsloottablesload
    */
    @GameTest(template = "empty", batch = "serverdata", timeoutTicks = 100)
    public static void allLogisticsLootTablesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsLootTablesLoad(context);
    }

    /**
    * Verifies every registered block that declares a loot table has that table actually loaded.
    *
    * <p>Run in-game: /test run logistics-gametest.serverdataloadinggametest.everylogisticsblockloottableisloaded
    */
    @GameTest(template = "empty", batch = "serverdata", timeoutTicks = 100)
    public static void everyLogisticsBlockLootTableIsLoaded(GameTestHelper context) {
        ServerDataLoadingGameTestBody.everyLogisticsBlockLootTableIsLoaded(context);
    }

    /**
    * Verifies every shipped configured feature deserializes into the live worldgen registry.
    *
    * <p>Run in-game: /test run logistics-gametest.serverdataloadinggametest.alllogisticsconfiguredfeaturesload
    */
    @GameTest(template = "empty", batch = "serverdata", timeoutTicks = 100)
    public static void allLogisticsConfiguredFeaturesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsConfiguredFeaturesLoad(context);
    }

    /**
    * Verifies every shipped placed feature deserializes into the live worldgen registry.
    *
    * <p>Run in-game: /test run logistics-gametest.serverdataloadinggametest.alllogisticsplacedfeaturesload
    */
    @GameTest(template = "empty", batch = "serverdata", timeoutTicks = 100)
    public static void allLogisticsPlacedFeaturesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsPlacedFeaturesLoad(context);
    }

    /**
    * Verifies every logistics item a shipped tag lists survives into the loaded tag.
    *
    * <p>Run in-game: /test run logistics-gametest.serverdataloadinggametest.alllogisticsitemtagentriesload
    */
    @GameTest(template = "empty", batch = "serverdata", timeoutTicks = 100)
    public static void allLogisticsItemTagEntriesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsItemTagEntriesLoad(context);
    }

    /**
    * Verifies every logistics block a shipped tag lists survives into the loaded tag.
    *
    * <p>Run in-game: /test run logistics-gametest.serverdataloadinggametest.alllogisticsblocktagentriesload
    */
    @GameTest(template = "empty", batch = "serverdata", timeoutTicks = 100)
    public static void allLogisticsBlockTagEntriesLoad(GameTestHelper context) {
        ServerDataLoadingGameTestBody.allLogisticsBlockTagEntriesLoad(context);
    }
}
