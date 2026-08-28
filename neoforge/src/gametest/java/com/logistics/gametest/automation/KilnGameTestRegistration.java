package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link KilnGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class KilnGameTestRegistration {

    private KilnGameTestRegistration() {}

    /**
    * Test that kiln can be placed and creates block entity.
    */
    @GameTest(template = "empty", batch = "kiln")
    public static void testKilnPlacement(GameTestHelper context) {
        KilnGameTestBody.testKilnPlacement(context);
    }

    /**
    * Wiki claim (Usage): "Input is accepted from the top and sides; output is drawn from the
    * bottom, the same as a vanilla furnace."
    *
    * <p>NOTE: the wiki overstates this — the code (and this test) enforce input from the top only,
    * matching vanilla furnace side-access rules. See WIKI_DISCREPANCIES.md § Kiln.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Kiln#Usage">wiki/Kiln.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "kiln")
    public static void testKilnInventoryAccess(GameTestHelper context) {
        KilnGameTestBody.testKilnInventoryAccess(context);
    }

    /**
    * Wiki claim (Usage): "Input is accepted from the top and sides..."
    *
    * <p>NOTE: the wiki overstates this — the code (and this test) enforce input from the top only.
    * See WIKI_DISCREPANCIES.md § Kiln.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Kiln#Usage">wiki/Kiln.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "kiln")
    public static void testKilnInputAccess(GameTestHelper context) {
        KilnGameTestBody.testKilnInputAccess(context);
    }

    /**
    * Wiki claim (Usage): "...output is drawn from the bottom, the same as a vanilla furnace."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Kiln#Usage">wiki/Kiln.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "kiln")
    public static void testKilnOutputExtraction(GameTestHelper context) {
        KilnGameTestBody.testKilnOutputExtraction(context);
    }

    /**
    * Test that kiln has correct initial state.
    */
    @GameTest(template = "empty", batch = "kiln")
    public static void testKilnInitialState(GameTestHelper context) {
        KilnGameTestBody.testKilnInitialState(context);
    }

    /**
    * Test that kiln provides item storage capability.
    */
    @GameTest(template = "empty", batch = "kiln")
    public static void testKilnItemStorage(GameTestHelper context) {
        KilnGameTestBody.testKilnItemStorage(context);
    }

    /**
    * Wiki claim (Power): "It holds 10,000 RF and accepts up to 128 RF/tick. Each smelt draws RF
    * over the same time a vanilla furnace would take — about 4,000 RF for a standard 10-second
    * smelt (20 RF/tick while active)."
    *
    * <p>NOTE: the code computes cookingTime(200) * KILN_RF_PER_COOK_TICK(10) = 2,000 RF, drained at
    * 20 RF/t = 100 ticks (5s) — half the wiki's claimed cost and time. This test asserts the code's
    * actual behavior; see WIKI_DISCREPANCIES.md § Kiln for the tracked mismatch.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Kiln#Power">wiki/Kiln.txt § Power</a>
    */
    @GameTest(template = "empty", batch = "kiln", timeoutTicks = 130)
    public static void testKilnSmeltsWithEnergy(GameTestHelper context) {
        KilnGameTestBody.testKilnSmeltsWithEnergy(context);
    }

    /**
    * Wiki claim (Setup): "The Kiln smelts continuously as long as it has power and input items."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Kiln#Setup">wiki/Kiln.txt § Setup</a>
    */
    @GameTest(template = "empty", batch = "kiln", timeoutTicks = 230)
    public static void testKilnSmeltsContinuously(GameTestHelper context) {
        KilnGameTestBody.testKilnSmeltsContinuously(context);
    }

    /**
    * Wiki claim (Usage/Power): "Input is accepted from the top... connect a Stirling Engine or
    * any RF source." The tests above prove the recipe math and energy contract by manipulating
    * the kiln's storages directly; this one proves the whole feature as a player actually wires
    * it up — a real engine delivering power with no cable in between, and a real hopper pushing
    * the ingredient in and another pulling the result out — with no direct capability calls.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Kiln#Usage">wiki/Kiln.txt § Usage</a>
    * @see <a href="https://logistics.fandom.com/wiki/Kiln#Power">wiki/Kiln.txt § Power</a>
    */
    @GameTest(template = "empty", batch = "kiln", timeoutTicks = 220)
    public static void testKilnSmeltsViaRealEngineAndHoppers(GameTestHelper context) {
        KilnGameTestBody.testKilnSmeltsViaRealEngineAndHoppers(context);
    }

    /**
    * Test that kiln block state has correct FACING property.
    */
    @GameTest(template = "empty", batch = "kiln")
    public static void testKilnFacing(GameTestHelper context) {
        KilnGameTestBody.testKilnFacing(context);
    }
}
