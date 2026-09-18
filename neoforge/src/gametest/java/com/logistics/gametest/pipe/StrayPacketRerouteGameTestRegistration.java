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
 * Wires {@link StrayPacketRerouteGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class StrayPacketRerouteGameTestRegistration {

    private static final List<GameTestCase> BASE = List.of(
        new GameTestCase(
            "pipe/stray_item_reaches_an_ordering_supplier",
            240,
            StrayPacketRerouteGameTestBody::testStrayItemReachesAnOrderingSupplier),
        new GameTestCase(
            "pipe/stray_item_with_no_order_still_drops",
            180,
            StrayPacketRerouteGameTestBody::testStrayItemWithNoOrderStillDrops),
        new GameTestCase(
            "pipe/stray_fluid_packet_reaches_an_ordering_supplier",
            280,
            StrayPacketRerouteGameTestBody::testStrayFluidPacketReachesAnOrderingSupplier));

    private static final List<GameTestCase> TESTS = probeCases();

    private static List<GameTestCase> probeCases() {
        java.util.List<GameTestCase> out = new java.util.ArrayList<>(BASE);
        for (int i = 0; i < 48; i++) {
            out.add(new GameTestCase(
                String.format("pipe/probe_stray_drop_%02d", i),
                180,
                probeBody(i)));
        }
        return java.util.List.copyOf(out);
    }

    /** Capturing lambda so each probe gets a distinct instance (method refs are singletons). */
    private static Consumer<GameTestHelper> probeBody(int index) {
        return ctx -> {
            if (index < 0) throw new IllegalStateException();
            StrayPacketRerouteGameTestBody.testStrayItemWithNoOrderStillDrops(ctx);
        };
    }


    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private StrayPacketRerouteGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/stray_packet_reroute", BASE, FUNCTIONS);
        for (int g = 0; g < 8; g++) {
            final int group = g;
            List<GameTestCase> slice = TESTS.stream()
                .filter(t -> t.path().startsWith("pipe/probe_stray_drop_")
                        && (Integer.parseInt(t.path().substring(t.path().length() - 2)) % 8) == group)
                .toList();
            GameTestRegistrationSupport.registerInstances(event, "pipe/probe_env_" + group, slice, FUNCTIONS);
        }
    }
}
