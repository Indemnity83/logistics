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
 * Wires {@link GourdFarmGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class GourdFarmGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "forestry/gourd_farm_harvests_fruit_never_stems_and_hopper_pulls_harvest",
            1200,
            GourdFarmGameTestBody::testHarvestsFruitNeverStemsAndHopperPullsHarvest),
        new GameTestCase(
            "forestry/gourd_farm_full_farm_spills_at_the_stem",
            1200,
            GourdFarmGameTestBody::testFullFarmSpillsAtTheStem),
        new GameTestCase(
            "forestry/gourd_farm_broken_stem_is_re_tilled_and_replanted",
            1200,
            GourdFarmGameTestBody::testBrokenStemIsReTilledAndReplanted));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private GourdFarmGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "forestry/gourd_farm", TESTS, FUNCTIONS);
    }
}
