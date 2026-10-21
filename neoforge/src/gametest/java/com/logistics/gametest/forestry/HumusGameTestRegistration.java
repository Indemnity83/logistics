package com.logistics.gametest.forestry;

import com.logistics.gametest.GameTestCase;
import com.logistics.gametest.GameTestRegistrationSupport;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wires {@link HumusGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class HumusGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("forestry/humus_sapling_survives", 60, HumusGameTestBody::testSaplingSurvivesOnHumus),
        new GameTestCase("forestry/humus_oak_grows_small_tree", 60, HumusGameTestBody::testOakGrowsSmallTree),
        new GameTestCase(
            "forestry/humus_dark_oak_grows_from_one_sapling", 60, HumusGameTestBody::testDarkOakGrowsFromOneSapling),
        new GameTestCase("forestry/humus_spruce_grows_small_tree", 60, HumusGameTestBody::testSpruceGrowsSmallTree),
        new GameTestCase("forestry/humus_azalea_grows_small_tree", 60, HumusGameTestBody::testAzaleaGrowsSmallTree),
        new GameTestCase(
            "forestry/humus_mangrove_grows_small_tree_with_grown_propagules", 60,
            HumusGameTestBody::testMangroveGrowsSmallTreeWithGrownPropagules),
        new GameTestCase(
            "forestry/humus_dark_oak_group_grows_four_trees", 60, HumusGameTestBody::testDarkOakGroupGrowsFourTrees),
        new GameTestCase(
            "forestry/humus_spruce_group_grows_four_trees", 60, HumusGameTestBody::testSpruceGroupGrowsFourTrees),
        new GameTestCase(
            "forestry/humus_saplings_off_humus_grow_as_vanilla", 60,
            HumusGameTestBody::testSaplingsOffHumusGrowAsVanilla),
        new GameTestCase(
            "forestry/humus_is_permanent_and_gives_no_boost", 60,
            HumusGameTestBody::testHumusIsPermanentAndGivesNoBoost));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private HumusGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "forestry/humus", TESTS, FUNCTIONS);
    }
}
