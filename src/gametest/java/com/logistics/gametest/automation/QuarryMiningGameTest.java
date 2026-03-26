package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryConfig;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Tick-based game tests for LaserQuarry mining behavior.
 *
 * <p>These tests verify the quarry's phase state machine (CLEARING → BUILDING_FRAME → MINING),
 * its response to absent energy, and actual block mining with item output.
 *
 * <p>All tests that require frame building use a 3×3 outer / 1×1 inner custom bounds
 * to keep the frame cost to 28 × 240 = 6 720 RF — within the 7 680 RF buffer.
 *
 * <p>Run all in-game: /test runall
 * Run one test:       /test run logistics-gametest.quarrymininggametest.&lt;methodname&gt;
 */
public class QuarryMiningGameTest {

    /**
     * Verifies that a quarry with no energy stays in CLEARING phase.
     *
     * <p>With energy = 0, {@code tickClearing} returns immediately on every tick.
     * After 20 ticks the phase must still be CLEARING.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 30)
    public void testQuarryStallsWithoutEnergy(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 1, 1);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = (LaserQuarryBlockEntity) context.getBlockEntity(quarryPos);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        context.runAfterDelay(20, () -> {
            if (quarry.getCurrentPhase() != LaserQuarryBlockEntity.Phase.CLEARING) {
                context.fail("Quarry with no energy should stay in CLEARING, got: "
                        + quarry.getCurrentPhase());
            } else {
                context.succeed();
            }
        });
    }

    /**
     * Verifies phase progression from CLEARING → BUILDING_FRAME → MINING.
     *
     * <p>With full energy (7 680 RF) and a 3×3 outer frame:
     * <ul>
     *   <li>CLEARING: ~1 tick (45 air blocks, all skipped in a single scan)
     *   <li>BUILDING_FRAME: 28 ticks (one frame block per tick × 240 RF each = 6 720 RF total)
     *   <li>MINING: entered on tick ~30
     * </ul>
     * The test asserts MINING phase at tick 80 — well past the expected transition.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testQuarryTransitionsThroughPhases(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = (LaserQuarryBlockEntity) context.getBlockEntity(quarryPos);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        // 3×3 outer frame (1×1 inner mining area) centred on the quarry's absolute position
        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() - 1, absPos.getZ() - 1,
                absPos.getX() + 1, absPos.getZ() + 1);

        // Fill energy to full capacity so every frame-build tick can proceed
        try (Transaction tx = Transaction.openOuter()) {
            quarry.energyStorage(Direction.DOWN).insert(LaserQuarryConfig.ENERGY_CAPACITY, tx);
            tx.commit();
        }

        // Check at tick 80 — CLEARING finishes in ~1 tick, BUILDING_FRAME in ~28 ticks
        context.runAfterDelay(80, () -> {
            if (quarry.getCurrentPhase() != LaserQuarryBlockEntity.Phase.MINING) {
                context.fail("Expected MINING phase after 80 ticks with full energy, got: "
                        + quarry.getCurrentPhase());
            } else {
                context.succeed();
            }
        });
    }

    /**
     * Verifies that the quarry mines a block and deposits the drop into a chest above it.
     *
     * <p>Layout (relative coordinates):
     * <pre>
     *   (0,3,0)  [chest]   ← quarry output target (directly above quarry)
     *   (0,2,0)  [quarry]
     *   (2,1,1)  [dirt]    ← inner mining column (absX+2, quarryY−1, absZ+1)
     * </pre>
     *
     * <p>The 3×3 frame bounds extend to the +X/+Z side of the quarry so the quarry column
     * and the chest above it are outside the clearing zone. The inner 1×1 mining column
     * sits at (absX+2, *, absZ+1) = relative (2, *, 1), hitting the dirt at (2, 1, 1).
     *
     * <p>Dirt has hardness 0.5 → break energy = 120 × 1.5 = 180 RF, well within the
     * ~960 RF remaining after frame construction (28 × 240 = 6 720 RF).
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testQuarryOutputsMinedBlockToChest(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(0, 2, 0);
        BlockPos chestPos = new BlockPos(0, 3, 0); // directly above quarry, outside clearing zone
        BlockPos dirtPos = new BlockPos(2, 1, 1);  // inner mining column below frame centre

        // Place dirt first so it is present when mining starts
        context.setBlock(dirtPos, Blocks.DIRT);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        context.setBlock(chestPos, Blocks.CHEST);

        LaserQuarryBlockEntity quarry = (LaserQuarryBlockEntity) context.getBlockEntity(quarryPos);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        // 3×3 outer frame to the +X/+Z side of the quarry; inner 1×1 column at (absX+2, *, absZ+1).
        // The quarry column (absX, absZ) and the chest above it are outside the clearing bounds.
        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() + 1, absPos.getZ(),
                absPos.getX() + 3, absPos.getZ() + 2);

        // 7 680 RF covers 28 frame blocks (6 720 RF) + dirt break (180 RF) with ~780 RF to spare.
        // insert() is capped at maxInsert per call, so loop until storage is full.
        try (Transaction tx = Transaction.openOuter()) {
            var storage = quarry.energyStorage(Direction.DOWN);
            while (storage.insert(LaserQuarryConfig.ENERGY_CAPACITY, tx) > 0) {}
            tx.commit();
        }

        // Succeed as soon as the chest contains dirt (quarry typically mines it around tick 33)
        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIRT));
    }
}
