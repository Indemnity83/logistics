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
 * Wires {@link ArboretumGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class ArboretumGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "forestry/arboretum_plants_saplings_on_lattice", 600, ArboretumGameTestBody::testPlantsSaplingsOnLattice),
        new GameTestCase(
            "forestry/arboretum_fells_grown_tree_and_replants", 2000,
            ArboretumGameTestBody::testFellsGrownTreeAndReplants),
        new GameTestCase(
            "forestry/arboretum_no_power_plants_nothing", 300, ArboretumGameTestBody::testNoPowerPlantsNothing),
        new GameTestCase(
            "forestry/arboretum_redstone_pauses_farm", 300, ArboretumGameTestBody::testRedstonePausesFarm),
        new GameTestCase(
            "forestry/arboretum_leaves_stone_and_player_builds_alone", 300,
            ArboretumGameTestBody::testLeavesStoneAndPlayerBuildsAlone),
        new GameTestCase(
            "forestry/arboretum_humus_replaces_dirt_under_planting_corners", 600,
            ArboretumGameTestBody::testHumusReplacesDirtUnderPlantingCorners),
        new GameTestCase(
            "forestry/arboretum_every_germling_grows_on_arboretum_soils", 40,
            ArboretumGameTestBody::testEveryGermlingGrowsOnArboretumSoils));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private ArboretumGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "forestry/arboretum", TESTS, FUNCTIONS);
    }
}
