package com.logistics.gametest.automation;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the tick-based LaserQuarry mining GameTests. Test logic lives in
 * {@link QuarryMiningGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 *
 * <p>Run all in-game: /test runall
 * Run one test:       /test run logistics-gametest.quarrymininggametest.&lt;methodname&gt;
 */
public class QuarryMiningGameTest {

    /**
     * Wiki claim (Power): "...the quarry stops entirely without power."
     *
     * <p>With energy = 0, {@code tickClearing} returns immediately on every tick.
     * After 20 ticks the phase must still be CLEARING.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Power">wiki/Laser Quarry.txt § Power</a>
     */
    @GameTest(maxTicks = 30)
    public void testQuarryStallsWithoutEnergy(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryStallsWithoutEnergy(context);
    }

    /**
     * Wiki claim (Usage): "...the Laser Quarry constructs a mining frame around the target area,
     * then excavates layer by layer down to bedrock..."
     *
     * <p>Verifies the quarry reaches MINING phase by the expected tick. It does not sample
     * BUILDING_FRAME along the way or assert the frame blocks themselves were placed.
     *
     * <p>With full energy (7 680 RF) and a 3×3 outer frame:
     * <ul>
     *   <li>CLEARING: 1 tick (45 pre-cleared air blocks, all skipped in a single scan)
     *   <li>BUILDING_FRAME: 28 ticks (one frame block per tick × 240 RF each = 6 720 RF total)
     *   <li>MINING: entered on tick ~30
     * </ul>
     * The test asserts MINING phase at tick 80 — well past the expected transition.
     *
     * <p>The clearing volume (3×3 × 5 Y levels, in front of the quarry) is pre-filled
     * with air to avoid underground terrain blocks, which would stall a quarry that has
     * only a few hundred RF to spare for stone-breaking.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Usage">wiki/Laser Quarry.txt § Usage</a>
     */
    @GameTest(maxTicks = 200)
    public void testQuarryTransitionsThroughPhases(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryTransitionsThroughPhases(context);
    }

    /**
     * Verifies that the quarry mines a block and deposits the drop into a chest above it.
     *
     * <p>Layout (relative coordinates):
     * <pre>
     *   z=1, y=3  [chest]   ← quarry output target ({@code quarryPos.above()})
     *   z=1, y=2  [quarry]  ← outside the custom bounds (bounds start at z=2)
     *   z=3, y=1  [dirt]    ← first mining target, inside the 1×1 inner area
     * </pre>
     *
     * <p>Dirt has hardness 0.5 → break energy = 120 × 1.5 = 180 RF, well within the
     * ~960 RF remaining after frame construction. Dirt drops dirt regardless of tool,
     * so {@code Block.getDrops} with an empty tool stack returns 1× dirt.
     *
     * <p>The chest is placed upfront because the clearing scan only covers the bounds
     * X/Z range (dz = +1..+3 relative to the quarry). The chest at dz = 0 (directly
     * above the quarry) is outside the clearing zone and will not be removed.
     *
     * <p>Wiki claim (Item collection): "Mined items output from the top of the quarry into any
     * connected inventory or pipe (no extractor needed)." No extractor is used here — the chest
     * receives items directly.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Item_collection">wiki/Laser Quarry.txt § Item collection</a>
     */
    @GameTest(maxTicks = 200)
    public void testQuarryClearsBlockDroppedIntoFrameSlot(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryClearsBlockDroppedIntoFrameSlot(context);
    }

    @GameTest(maxTicks = 200)
    public void testQuarryRepairsFrameBrokenByPlayer(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryRepairsFrameBrokenByPlayer(context);
    }

    @GameTest(maxTicks = 200)
    public void testQuarryOutputsMinedBlockToChest(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryOutputsMinedBlockToChest(context);
    }

    /**
     * Same setup as {@link #testQuarryOutputsMinedBlockToChest}, but powered by a real engine
     * instead of pre-filling the energy buffer directly — the phase-machine tests above isolate
     * mining logic from power delivery on purpose (frame + mining costs thousands of RF, so
     * pre-charging keeps their tick budgets tight); this test proves power delivery itself works
     * end to end, the way a player would actually wire the quarry up.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Power">wiki/Laser Quarry.txt § Power</a>
     */
    @GameTest(maxTicks = 200)
    public void testQuarryMinesAndOutputsViaRealEngine(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryMinesAndOutputsViaRealEngine(context);
    }

    /** Verifies lava is treated as unminable — like bedrock: never mined, never replaced — and the quarry still finishes. */
    @GameTest(maxTicks = 200)
    public void testQuarryTreatsLavaAsUnminableAndSkipsThatColumn(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryTreatsLavaAsUnminableAndSkipsThatColumn(context);
    }

    /** Verifies the quarry never mines ground beneath lava it never removed, even after giving up waiting on that column. */
    @GameTest(maxTicks = 200)
    public void testQuarryDoesNotMineBelowUnremovedLava(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryDoesNotMineBelowUnremovedLava(context);
    }

    /**
     * Verifies a blocked column stays tied to its real-world position across the mining zigzag's
     * layer-to-layer reflection, rather than to the scan-order grid index it happened to be found at.
     */
    @GameTest(maxTicks = 200)
    public void testQuarryTracksBlockedColumnAcrossZigzagLayerReflection(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryTracksBlockedColumnAcrossZigzagLayerReflection(context);
    }

    /**
     * Verifies the quarry returns to break a block that reappears in an already-processed cell
     * before it commits to descending to the next layer.
     */
    @GameTest(maxTicks = 200)
    public void testQuarryReMinesBlockThatReappearsInProcessedLayer(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryReMinesBlockThatReappearsInProcessedLayer(context);
    }

    /**
     * Verifies the quarry still respects unremoved lava after a save/load round trip, even once
     * the mining cursor has already moved past it.
     */
    @GameTest(maxTicks = 200)
    public void testQuarryStillRespectsLavaAfterReload(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryStillRespectsLavaAfterReload(context);
    }

    /**
     * Verifies a block placed back into an already-mined cell gets re-mined even after the cursor
     * has moved on several layers further, not just the one layer immediately behind it.
     */
    @GameTest(maxTicks = 200)
    public void testQuarryReMinesBlockPlacedManyLayersBehindCursor(GameTestHelper context) {
        QuarryMiningGameTestBody.testQuarryReMinesBlockPlacedManyLayersBehindCursor(context);
    }
}
