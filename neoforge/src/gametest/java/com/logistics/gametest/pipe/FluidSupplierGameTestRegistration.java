package com.logistics.gametest.pipe;

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
 * Wires {@link FluidSupplierGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class FluidSupplierGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "pipe/dry_restock_fills_to_target", 280, FluidSupplierGameTestBody::testDryRestockFillsToTarget),
        new GameTestCase(
            "pipe/exact_non_round_deficit_is_ordered_and_filled",
            280,
            FluidSupplierGameTestBody::testExactNonRoundDeficitIsOrderedAndFilled),
        new GameTestCase(
            "pipe/never_orders_more_than_buffer_can_hold",
            220,
            FluidSupplierGameTestBody::testNeverOrdersMoreThanBufferCanHold),
        new GameTestCase(
            "pipe/insufficient_energy_holds_fluid_then_deposits_when_powered",
            420,
            FluidSupplierGameTestBody::testInsufficientEnergyHoldsFluidThenDepositsWhenPowered),
        new GameTestCase(
            "pipe/stops_ordering_when_tank_has_no_real_room",
            220,
            FluidSupplierGameTestBody::testStopsOrderingWhenTankHasNoRealRoom));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private FluidSupplierGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/fluid_supplier", TESTS, FUNCTIONS);
    }
}
