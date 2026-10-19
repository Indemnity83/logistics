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
            "forestry/arboretum_plants_saplings_on_lattice", 1200, ArboretumGameTestBody::testPlantsSaplingsOnLattice),
        new GameTestCase(
            "forestry/arboretum_fells_grown_tree_and_replants", 3000,
            ArboretumGameTestBody::testFellsGrownTreeAndReplants),
        new GameTestCase(
            "forestry/arboretum_no_power_plants_nothing", 300, ArboretumGameTestBody::testNoPowerPlantsNothing),
        new GameTestCase(
            "forestry/arboretum_redstone_ignored_by_default", 300, ArboretumGameTestBody::testRedstoneIgnoredByDefault),
        new GameTestCase(
            "forestry/arboretum_signal_pauses_farm_in_low_mode", 600, ArboretumGameTestBody::testSignalPausesFarmInLowMode),
        new GameTestCase(
            "forestry/arboretum_clears_blocks_from_ring", 1200, ArboretumGameTestBody::testClearsBlocksFromRing),
        new GameTestCase(
            "forestry/arboretum_humus_replaces_dirt_across_the_ring", 2000,
            ArboretumGameTestBody::testHumusReplacesDirtAcrossTheRing),
        new GameTestCase(
            "forestry/arboretum_lays_humus_level_with_farm_on_grass", 2000,
            ArboretumGameTestBody::testLaysHumusLevelWithFarmOnGrass),
        new GameTestCase(
            "forestry/arboretum_felled_tree_leaves_sand_that_is_relaid_with_humus", 2000,
            ArboretumGameTestBody::testFelledTreeLeavesSandThatIsRelaidWithHumus),
        new GameTestCase(
            "forestry/arboretum_felled_2x2_tree_leaves_sand_under_all_four_cells", 2000,
            ArboretumGameTestBody::testFelled2x2TreeLeavesSandUnderAllFourCells),
        new GameTestCase(
            "forestry/arboretum_without_humus_sand_is_left_unplanted", 1200,
            ArboretumGameTestBody::testWithoutHumusSandIsLeftUnplanted),
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
