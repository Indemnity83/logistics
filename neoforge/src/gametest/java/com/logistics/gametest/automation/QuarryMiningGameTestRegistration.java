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
 * Wires {@link QuarryMiningGameTestBody}'s methods into MC's data-driven GameTest registries —
 * see {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class QuarryMiningGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "automation/quarry_collects_broken_container_contents",
            30,
            QuarryMiningGameTestBody::testQuarryCollectsBrokenContainerContents),
        new GameTestCase(
            "automation/quarry_leaves_loose_items_when_breaking_a_non_container",
            30,
            QuarryMiningGameTestBody::testQuarryLeavesLooseItemsWhenBreakingANonContainer),
        new GameTestCase("automation/quarry_stalls_without_energy", 50, QuarryMiningGameTestBody::testQuarryStallsWithoutEnergy),
        new GameTestCase(
            "automation/quarry_transitions_through_phases", 220, QuarryMiningGameTestBody::testQuarryTransitionsThroughPhases),
        new GameTestCase(
            "automation/quarry_outputs_mined_block_to_chest", 220, QuarryMiningGameTestBody::testQuarryOutputsMinedBlockToChest),
        new GameTestCase(
            "automation/quarry_mines_and_outputs_via_real_engine", 220, QuarryMiningGameTestBody::testQuarryMinesAndOutputsViaRealEngine),
        new GameTestCase(
            "automation/quarry_treats_lava_as_unminable_and_skips_that_column",
            220,
            QuarryMiningGameTestBody::testQuarryTreatsLavaAsUnminableAndSkipsThatColumn),
        new GameTestCase(
            "automation/quarry_does_not_mine_below_unremoved_lava",
            220,
            QuarryMiningGameTestBody::testQuarryDoesNotMineBelowUnremovedLava),
        new GameTestCase(
            "automation/quarry_tracks_blocked_column_across_zigzag_layer_reflection",
            220,
            QuarryMiningGameTestBody::testQuarryTracksBlockedColumnAcrossZigzagLayerReflection),
        new GameTestCase(
            "automation/quarry_re_mines_block_that_reappears_in_processed_layer",
            220,
            QuarryMiningGameTestBody::testQuarryReMinesBlockThatReappearsInProcessedLayer),
        new GameTestCase(
            "automation/quarry_still_respects_lava_after_reload", 220, QuarryMiningGameTestBody::testQuarryStillRespectsLavaAfterReload),
        new GameTestCase(
            "automation/quarry_re_mines_block_placed_many_layers_behind_cursor",
            220,
            QuarryMiningGameTestBody::testQuarryReMinesBlockPlacedManyLayersBehindCursor));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private QuarryMiningGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/quarry_mining", TESTS, FUNCTIONS);
    }
}
