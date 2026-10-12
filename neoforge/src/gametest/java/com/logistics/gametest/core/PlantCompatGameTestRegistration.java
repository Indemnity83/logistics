package com.logistics.gametest.core;

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
 * Wires {@link PlantCompatGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class PlantCompatGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("core/plant_fertilize_advances_wheat", 40, PlantCompatGameTestBody::testFertilizeAdvancesWheat),
        new GameTestCase(
            "core/plant_fertilize_rejects_non_bonemealable_block",
            40,
            PlantCompatGameTestBody::testFertilizeRejectsNonBonemealableBlock),
        new GameTestCase(
            "core/plant_harvest_berries_from_mature_bush", 40, PlantCompatGameTestBody::testHarvestBerriesFromMatureBush),
        new GameTestCase(
            "core/plant_harvest_berries_from_immature_bush_does_nothing",
            40,
            PlantCompatGameTestBody::testHarvestBerriesFromImmatureBushDoesNothing),
        new GameTestCase(
            "core/plant_harvest_berries_from_cave_vines", 40, PlantCompatGameTestBody::testHarvestBerriesFromCaveVines));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private PlantCompatGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "core/plant_compat", TESTS, FUNCTIONS);
    }
}
