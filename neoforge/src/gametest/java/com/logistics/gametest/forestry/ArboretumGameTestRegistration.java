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
            "forestry/arboretum_fells_humus_tree_whole_and_replants", 2400,
            ArboretumGameTestBody::testFellsHumusTreeWholeAndReplants),
        new GameTestCase(
            "forestry/arboretum_leaves_tree_on_grass_standing", 600,
            ArboretumGameTestBody::testLeavesTreeOnGrassStanding),
        new GameTestCase(
            "forestry/arboretum_does_nothing_without_power", 400, ArboretumGameTestBody::testDoesNothingWithoutPower),
        new GameTestCase(
            "forestry/arboretum_redstone_signal_pauses_farm", 400, ArboretumGameTestBody::testRedstoneSignalPausesFarm));

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
