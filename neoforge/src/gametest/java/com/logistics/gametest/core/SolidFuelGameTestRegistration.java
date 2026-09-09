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
 * Wires {@link SolidFuelGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class SolidFuelGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("core/peat_burns", 10, SolidFuelGameTestBody::testPeatBurns),
        new GameTestCase("core/bitumen_burns", 10, SolidFuelGameTestBody::testBitumenBurns),
        new GameTestCase("core/tar_burns", 10, SolidFuelGameTestBody::testTarBurns));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private SolidFuelGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "core/solid_fuel", TESTS, FUNCTIONS);
    }
}
