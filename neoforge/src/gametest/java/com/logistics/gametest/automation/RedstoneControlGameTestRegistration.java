package com.logistics.gametest.automation;

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
 * Wires {@link RedstoneControlGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class RedstoneControlGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "automation/redstone_ignored_runs_with_and_without_signal",
            180,
            RedstoneControlGameTestBody::testIgnoredRunsWithAndWithoutSignal),
        new GameTestCase(
            "automation/redstone_high_pauses_until_signalled", 220, RedstoneControlGameTestBody::testHighPausesUntilSignalled),
        new GameTestCase(
            "automation/redstone_low_pauses_while_signalled", 220, RedstoneControlGameTestBody::testLowPausesWhileSignalled),
        new GameTestCase(
            "automation/redstone_mode_change_mid_run_freezes_progress",
            240,
            RedstoneControlGameTestBody::testModeChangeMidRunFreezesProgress),
        new GameTestCase(
            "automation/redstone_lever_toggles_pause_and_resume",
            280,
            RedstoneControlGameTestBody::testLeverTogglesPauseAndResume),
        new GameTestCase(
            "automation/redstone_fabricator_high_pauses_until_signalled",
            260,
            RedstoneControlGameTestBody::testFabricatorHighPausesUntilSignalled),
        new GameTestCase(
            "automation/redstone_transposer_high_pauses_until_signalled",
            160,
            RedstoneControlGameTestBody::testTransposerHighPausesUntilSignalled),
        new GameTestCase(
            "automation/redstone_mode_persists_and_default_is_not_saved",
            100,
            RedstoneControlGameTestBody::testModePersistsAndDefaultIsNotSaved));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private RedstoneControlGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/redstone", TESTS, FUNCTIONS);
    }
}
