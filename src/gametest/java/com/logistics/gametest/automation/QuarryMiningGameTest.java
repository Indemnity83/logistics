package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryConfig;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import team.reborn.energy.api.EnergyStorage;

/**
 * Tick-based game tests for LaserQuarry mining behavior.
 *
 * <p>These tests verify the quarry's phase state machine (CLEARING → BUILDING_FRAME → MINING),
 * its response to absent energy, and actual block mining with item output.
 *
 * <p>All tests that require frame building use a 3×3 outer / 1×1 inner custom bounds
 * placed directly in front of the quarry (+Z direction) to keep the frame cost to
 * 28 × 240 = 6 720 RF — within the 7 680 RF buffer. The quarry itself sits outside
 * the bounds (at the −Z edge), matching the real-gameplay placement where markers
 * define the region to mine and the quarry is placed adjacent to it.
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
    @GameTest(maxTicks = 30)
    public void testQuarryStallsWithoutEnergy(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 1, 1);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
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
     *   <li>CLEARING: 1 tick (45 pre-cleared air blocks, all skipped in a single scan)
     *   <li>BUILDING_FRAME: 28 ticks (one frame block per tick × 240 RF each = 6 720 RF total)
     *   <li>MINING: entered on tick ~30
     * </ul>
     * The test asserts MINING phase at tick 80 — well past the expected transition.
     *
     * <p>The clearing volume (3×3 × 5 Y levels, in front of the quarry) is pre-filled
     * with air to avoid underground terrain blocks, which would stall a quarry that has
     * only a few hundred RF to spare for stone-breaking.
     */
    @GameTest(maxTicks = 200)
    public void testQuarryTransitionsThroughPhases(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);

        // Pre-clear the bounds column (dz = +1..+3 relative to the quarry) from quarryY
        // to quarryY+Y_OFFSET_ABOVE so the clearing phase encounters only air and
        // completes in a single tick regardless of what terrain the game-test world has.
        for (int dy = 0; dy <= LaserQuarryConfig.Y_OFFSET_ABOVE; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }

        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        // 3×3 outer frame (1×1 inner mining area) placed in front of the quarry (+Z).
        // The quarry sits at absZ, which is outside the bounds [absZ+1, absZ+3], matching
        // real gameplay where the quarry is placed adjacent to the marked region.
        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() - 1, absPos.getZ() + 1,
                absPos.getX() + 1, absPos.getZ() + 3);

        // SimpleEnergyStorage.insert() is rate-limited by MAX_ENERGY_INPUT (1 000 RF/call),
        // so loop until the battery is full.
        try (Transaction tx = Transaction.openOuter()) {
            EnergyStorage es = quarry.energyStorage(Direction.DOWN);
            long remaining = LaserQuarryConfig.ENERGY_CAPACITY;
            while (remaining > 0) {
                long inserted = es.insert(remaining, tx);
                if (inserted == 0) break;
                remaining -= inserted;
            }
            tx.commit();
        }

        // Check at tick 80 — CLEARING finishes in 1 tick, BUILDING_FRAME in ~28 ticks
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
     */
    @GameTest(maxTicks = 200)
    public void testQuarryOutputsMinedBlockToChest(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        BlockPos chestPos = new BlockPos(1, 3, 1); // quarryPos.above() — quarry output target
        BlockPos dirtPos = new BlockPos(1, 1, 3);  // inside the 1×1 inner mining area

        // Pre-clear the bounds column (dz = +1..+3) from quarryY to quarryY+Y_OFFSET_ABOVE
        // so the clearing phase encounters only air and completes in a single tick.
        for (int dy = 0; dy <= LaserQuarryConfig.Y_OFFSET_ABOVE; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }

        // Dirt is below the inner mining area (not in the clearing range) — place it now.
        // Chest is above the quarry (outside the clearing X/Z range) — also safe upfront.
        context.setBlock(dirtPos, Blocks.DIRT);
        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        // 3×3 outer frame (1×1 inner) placed in front of the quarry (+Z).
        // Inner mining column: (absX, Y, absZ+2) → relative (1, Y, 3) — exactly dirtPos X/Z.
        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() - 1, absPos.getZ() + 1,
                absPos.getX() + 1, absPos.getZ() + 3);

        // SimpleEnergyStorage.insert() is rate-limited by MAX_ENERGY_INPUT (1 000 RF/call);
        // loop to fill the full 7 680 RF capacity.
        try (Transaction tx = Transaction.openOuter()) {
            EnergyStorage es = quarry.energyStorage(Direction.DOWN);
            long remaining = LaserQuarryConfig.ENERGY_CAPACITY;
            while (remaining > 0) {
                long inserted = es.insert(remaining, tx);
                if (inserted == 0) break;
                remaining -= inserted;
            }
            tx.commit();
        }

        // Succeed as soon as the chest contains dirt (quarry mines dirt around tick ~33)
        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIRT));
    }
}
