package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the kiln GameTests. Test logic lives in {@link KilnGameTestBody}
 * (shared with NeoForge — see {@code common/src/gametest}); these methods only carry the
 * {@code @GameTest} annotation Fabric's reflection-based test discovery requires.
 */
public class KilnGameTest {

    /**
    * Test that kiln can be placed and creates block entity.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnPlacement(GameTestHelper context) {
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
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnInventoryAccess(GameTestHelper context) {
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
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnInputAccess(GameTestHelper context) {
        KilnGameTestBody.testKilnInputAccess(context);
    }

    /**
    * Wiki claim (Usage): "...output is drawn from the bottom, the same as a vanilla furnace."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Kiln#Usage">wiki/Kiln.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnOutputExtraction(GameTestHelper context) {
        KilnGameTestBody.testKilnOutputExtraction(context);
    }

    /**
    * Test that kiln has correct initial state.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnInitialState(GameTestHelper context) {
        KilnGameTestBody.testKilnInitialState(context);
    }

    /**
    * Test that kiln provides item storage capability.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnItemStorage(GameTestHelper context) {
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
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 130)
    public void testKilnSmeltsWithEnergy(GameTestHelper context) {
        KilnGameTestBody.testKilnSmeltsWithEnergy(context);
    }

    /**
    * Wiki claim (Setup): "The Kiln smelts continuously as long as it has power and input items."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Kiln#Setup">wiki/Kiln.txt § Setup</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 230)
    public void testKilnSmeltsContinuously(GameTestHelper context) {
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
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 220)
    public void testKilnSmeltsViaRealEngineAndHoppers(GameTestHelper context) {
        KilnGameTestBody.testKilnSmeltsViaRealEngineAndHoppers(context);
    }

    /**
    * Test that kiln block state has correct FACING property.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testKilnFacing(GameTestHelper context) {
        KilnGameTestBody.testKilnFacing(context);
    }
}
