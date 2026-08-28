package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Sequential Fabricator GameTests. Test logic lives in
 * {@link SequentialFabricatorGameTestBody} (shared with NeoForge — see
 * {@code common/src/gametest}); these methods only carry the {@code @GameTest} annotation
 * Fabric's reflection-based test discovery requires.
 */
public class SequentialFabricatorGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testPlacement(GameTestHelper context) {
        SequentialFabricatorGameTestBody.testPlacement(context);
    }

    /**
    * Wiki claim (Usage): "Choose the chipset to build from the machine's GUI, then feed it the
    * ingredients; the Sequential Fabricator consumes them in order and produces the selected
    * chipset." (Recipes): "Redstone -> Redstone Chipset, 10,000 RF."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Usage">wiki/Sequential Fabricator.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 140)
    public void testBuildsSelectedChipset(GameTestHelper context) {
        SequentialFabricatorGameTestBody.testBuildsSelectedChipset(context);
    }

    /**
    * NOTE: the wiki's Usage section reads as one selection at a time ("the selected chipset"), but
    * the machine actually supports queuing several chipsets simultaneously and cycles through them
    * round-robin. See WIKI_DISCREPANCIES.md § Sequential Fabricator.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Usage">wiki/Sequential Fabricator.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 360)
    public void testCyclesThroughMultipleSelectedChipsets(GameTestHelper context) {
        SequentialFabricatorGameTestBody.testCyclesThroughMultipleSelectedChipsets(context);
    }

    /**
    * Wiki claim (Usage/Power): "...feed it the ingredients... connect a strong RF source."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Usage">wiki/Sequential Fabricator.txt § Usage</a>
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testBuildsChipsetViaRealEngineAndHoppers(GameTestHelper context) {
        SequentialFabricatorGameTestBody.testBuildsChipsetViaRealEngineAndHoppers(context);
    }
}
