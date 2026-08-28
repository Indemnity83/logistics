package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link AlloySmelterGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class AlloySmelterGameTestRegistration {

    private AlloySmelterGameTestRegistration() {}

    @GameTest(template = "empty", batch = "alloysmelter")
    public static void testPlacement(GameTestHelper context) {
        AlloySmelterGameTestBody.testPlacement(context);
    }

    /**
    * Wiki claim (Usage): "The two inputs are order-independent." Both (ore, sand) and (sand, ore)
    * must start drawing energy — i.e. both resolve to the same recipe.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Usage">wiki/Alloy Smelter.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "alloysmelter", timeoutTicks = 40)
    public static void testInputsAreOrderIndependent(GameTestHelper context) {
        AlloySmelterGameTestBody.testInputsAreOrderIndependent(context);
    }

    /**
    * Wiki claim (Ore processing): "Iron Ore;Deepslate Iron Ore + Sand;Red Sand -> Iron Ingot,2,
    * Byproduct: Rich Slag, 5% chance." (Power): "Each recipe carries its own RF cost."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Ore_processing">wiki/Alloy Smelter.txt § Ore processing</a>
    */
    @GameTest(template = "empty", batch = "alloysmelter", timeoutTicks = 220)
    public static void testSmeltsIronOreWithSandFlux(GameTestHelper context) {
        AlloySmelterGameTestBody.testSmeltsIronOreWithSandFlux(context);
    }

    /**
    * Wiki claim (Alloying): "Bronze is smelted from three copper and one tin, in ingot...form" —
    * "Copper Ingot,InputCount=3 + Tin Ingot -> Bronze Ingot,4."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Alloying">wiki/Alloy Smelter.txt § Alloying</a>
    */
    @GameTest(template = "empty", batch = "alloysmelter", timeoutTicks = 220)
    public static void testAlloysCopperAndTinIntoBronze(GameTestHelper context) {
        AlloySmelterGameTestBody.testAlloysCopperAndTinIntoBronze(context);
    }

    /**
    * Wiki claim (Usage/Power): "The two inputs are order-independent... connect a Stirling Engine
    * or any RF source."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Usage">wiki/Alloy Smelter.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "alloysmelter", timeoutTicks = 260)
    public static void testSmeltsIronOreViaRealEngineAndHoppers(GameTestHelper context) {
        AlloySmelterGameTestBody.testSmeltsIronOreViaRealEngineAndHoppers(context);
    }
}
