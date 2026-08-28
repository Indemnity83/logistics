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
 * Wires {@link GlassTankBucketGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class GlassTankBucketGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "pipe/empty_bucket_drains_glass_tank_in_survival",
            100,
            GlassTankBucketGameTestBody::emptyBucketDrainsGlassTankInSurvival),
        // The body doesn't call succeed() itself — Fabric's wrapper adds an extra assertion
        // afterward (see GlassTankBucketGameTestBody), so NeoForge finishes it here instead.
        new GameTestCase("pipe/empty_bucket_drains_glass_tank_in_creative", 100, context -> {
            GlassTankBucketGameTestBody.emptyBucketDrainsGlassTankInCreative(context);
            context.succeed();
        }));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private GlassTankBucketGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/glass_tank_bucket", TESTS, FUNCTIONS);
    }
}
