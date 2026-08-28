package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link SequentialFabricatorGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class SequentialFabricatorGameTestRegistration {

    private SequentialFabricatorGameTestRegistration() {}

    @GameTest(template = "empty", batch = "sequentialfabricator")
    public static void testPlacement(GameTestHelper context) {
        SequentialFabricatorGameTestBody.testPlacement(context);
    }

    /**
    * Wiki claim (Usage): "Choose the chipset to build from the machine's GUI, then feed it the
    * ingredients; the Sequential Fabricator consumes them in order and produces the selected
    * chipset." (Recipes): "Redstone -> Redstone Chipset, 10,000 RF."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Usage">wiki/Sequential Fabricator.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "sequentialfabricator", timeoutTicks = 140)
    public static void testBuildsSelectedChipset(GameTestHelper context) {
        SequentialFabricatorGameTestBody.testBuildsSelectedChipset(context);
    }

    /**
    * NOTE: the wiki's Usage section reads as one selection at a time ("the selected chipset"), but
    * the machine actually supports queuing several chipsets simultaneously and cycles through them
    * round-robin. See WIKI_DISCREPANCIES.md § Sequential Fabricator.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Usage">wiki/Sequential Fabricator.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "sequentialfabricator", timeoutTicks = 360)
    public static void testCyclesThroughMultipleSelectedChipsets(GameTestHelper context) {
        SequentialFabricatorGameTestBody.testCyclesThroughMultipleSelectedChipsets(context);
    }

    /**
    * Wiki claim (Usage/Power): "...feed it the ingredients... connect a strong RF source."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Usage">wiki/Sequential Fabricator.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "sequentialfabricator", timeoutTicks = 200)
    public static void testBuildsChipsetViaRealEngineAndHoppers(GameTestHelper context) {
        SequentialFabricatorGameTestBody.testBuildsChipsetViaRealEngineAndHoppers(context);
    }
}
