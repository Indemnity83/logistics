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
 * Wires {@link FluidProviderGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class FluidProviderGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "pipe/fluid_provider_mints_and_delivers_packets",
            180,
            FluidProviderGameTestBody::testFluidProviderMintsAndDeliversPackets),
        new GameTestCase(
            "pipe/fluid_provider_dispatches_below_one_packet_size",
            140,
            FluidProviderGameTestBody::testFluidProviderDispatchesBelowOnePacketSize),
        new GameTestCase(
            "pipe/mixed_full_plus_tail_dispatch_charges_per_physical_packet",
            180,
            FluidProviderGameTestBody::testMixedFullPlusTailDispatchChargesPerPhysicalPacket),
        new GameTestCase(
            "pipe/ten_small_deliveries_cost_ten_times_one_big_delivery",
            320,
            FluidProviderGameTestBody::testTenSmallDeliveriesCostTenTimesOneBigDelivery));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private FluidProviderGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/fluid_provider", TESTS, FUNCTIONS);
    }
}
