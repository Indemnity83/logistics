package com.logistics.gametest.automation;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Alloy Smelter GameTests. Test logic lives in
 * {@link AlloySmelterGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class AlloySmelterGameTest {

    @GameTest
    public void testPlacement(GameTestHelper context) {
        AlloySmelterGameTestBody.testPlacement(context);
    }

    /**
     * Wiki claim (Usage): "The two inputs are order-independent." Both (ore, sand) and (sand, ore)
     * must start drawing energy — i.e. both resolve to the same recipe.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Usage">wiki/Alloy Smelter.txt § Usage</a>
     */
    @GameTest(maxTicks = 40)
    public void testInputsAreOrderIndependent(GameTestHelper context) {
        AlloySmelterGameTestBody.testInputsAreOrderIndependent(context);
    }

    /**
     * Wiki claim (Ore processing): "Iron Ore;Deepslate Iron Ore + Sand;Red Sand -> Iron Ingot,2,
     * Byproduct: Rich Slag, 5% chance." (Power): "Each recipe carries its own RF cost."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Ore_processing">wiki/Alloy Smelter.txt § Ore processing</a>
     */
    @GameTest(maxTicks = 220)
    public void testSmeltsIronOreWithSandFlux(GameTestHelper context) {
        AlloySmelterGameTestBody.testSmeltsIronOreWithSandFlux(context);
    }

    /**
     * Wiki claim (Alloying): "Bronze is smelted from three copper and one tin, in ingot...form" —
     * "Copper Ingot,InputCount=3 + Tin Ingot -> Bronze Ingot,4."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Alloying">wiki/Alloy Smelter.txt § Alloying</a>
     */
    @GameTest(maxTicks = 220)
    public void testAlloysCopperAndTinIntoBronze(GameTestHelper context) {
        AlloySmelterGameTestBody.testAlloysCopperAndTinIntoBronze(context);
    }

    /**
     * Wiki claim (Usage/Power): "The two inputs are order-independent... connect a Stirling Engine
     * or any RF source."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Usage">wiki/Alloy Smelter.txt § Usage</a>
     */
    @GameTest(maxTicks = 260)
    public void testSmeltsIronOreViaRealEngineAndHoppers(GameTestHelper context) {
        AlloySmelterGameTestBody.testSmeltsIronOreViaRealEngineAndHoppers(context);
    }
}
