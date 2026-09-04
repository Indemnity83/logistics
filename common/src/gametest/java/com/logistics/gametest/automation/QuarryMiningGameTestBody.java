package com.logistics.gametest.automation;

import com.logistics.automation.laserquarry.entity.QuarryEnergy;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPower;
import com.logistics.automation.laserquarry.LaserQuarryGeometry;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.automation.laserquarry.entity.QuarryBlockBreaker;
import com.logistics.automation.laserquarry.entity.QuarryOutput;
import com.logistics.automation.laserquarry.entity.QuarryPhase;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.core.lib.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Shared tick-based GameTest bodies for LaserQuarry mining behavior, compiled directly into both
 * loaders' {@code gametest} source sets (see {@code common/build.gradle}). Loader-specific glue
 * wires these into each loader's own registration mechanism: Fabric's {@code @GameTest}-annotated
 * {@code QuarryMiningGameTest} delegates to these methods, and NeoForge's
 * {@code QuarryMiningGameTestRegistration} references them directly as
 * {@code Consumer<GameTestHelper>} method references.
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
public class QuarryMiningGameTestBody {

    /**
     * Wiki claim (Power): "...the quarry stops entirely without power."
     *
     * <p>With energy = 0, {@code tickClearing} returns immediately on every tick.
     * After 20 ticks the phase must still be CLEARING.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Power">wiki/Laser Quarry.txt § Power</a>
     */
    public static void testQuarryStallsWithoutEnergy(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 1, 1);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        context.runAfterDelay(20, () -> {
            if (quarry.getCurrentPhase() != QuarryPhase.CLEARING) {
                context.fail("Quarry with no energy should stay in CLEARING, got: "
                        + quarry.getCurrentPhase());
            } else {
                context.succeed();
            }
        });
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
    public static void testQuarryTransitionsThroughPhases(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);

        // Pre-clear the bounds column (dz = +1..+3 relative to the quarry) from quarryY
        // to quarryY+Y_OFFSET_ABOVE so the clearing phase encounters only air and
        // completes in a single tick regardless of what terrain the game-test world has.
        for (int dy = 0; dy <= LaserQuarryGeometry.Y_OFFSET_ABOVE; dy++) {
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

        // EnergyComponent.insert() is rate-limited by MAX_ENERGY_INPUT (1 000 RF/call),
        // so loop until the battery is full.
        IEnergyStorage es = quarry.energyStorage(Direction.DOWN);
        long remaining = QuarryEnergy.energyCapacity();
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted == 0) {
                context.fail("Failed to fill quarry energy: insert returned 0 with " + remaining + " RF remaining");
                return;
            }
            remaining -= inserted;
        }

        // Check at tick 80 — CLEARING finishes in 1 tick, BUILDING_FRAME in ~28 ticks
        context.runAfterDelay(80, () -> {
            if (quarry.getCurrentPhase() != QuarryPhase.MINING) {
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
     *
     * <p>Wiki claim (Item collection): "Mined items output from the top of the quarry into any
     * connected inventory or pipe (no extractor needed)." No extractor is used here — the chest
     * receives items directly.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Item_collection">wiki/Laser Quarry.txt § Item collection</a>
     */
    public static void testQuarryOutputsMinedBlockToChest(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        BlockPos chestPos = new BlockPos(1, 3, 1); // quarryPos.above() — quarry output target
        BlockPos dirtPos = new BlockPos(1, 1, 3);  // inside the 1×1 inner mining area

        // Pre-clear the bounds column (dz = +1..+3) from quarryY to quarryY+Y_OFFSET_ABOVE
        // so the clearing phase encounters only air and completes in a single tick.
        for (int dy = 0; dy <= LaserQuarryGeometry.Y_OFFSET_ABOVE; dy++) {
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

        // EnergyComponent.insert() is rate-limited by MAX_ENERGY_INPUT (1 000 RF/call);
        // loop to fill the full 7 680 RF capacity.
        IEnergyStorage es = quarry.energyStorage(Direction.DOWN);
        long remaining = QuarryEnergy.energyCapacity();
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted == 0) {
                context.fail("Failed to fill quarry energy: insert returned 0 with " + remaining + " RF remaining");
                return;
            }
            remaining -= inserted;
        }

        // Succeed as soon as the chest contains dirt (quarry mines dirt around tick ~33)
        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIRT));
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
    public static void testQuarryMinesAndOutputsViaRealEngine(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        BlockPos chestPos = new BlockPos(1, 3, 1);
        BlockPos dirtPos = new BlockPos(1, 1, 3);
        BlockPos enginePos = new BlockPos(0, 2, 1);
        BlockPos redstoneBlockPos = new BlockPos(-1, 2, 1);

        for (int dy = 0; dy <= LaserQuarryGeometry.Y_OFFSET_ABOVE; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }

        context.setBlock(dirtPos, Blocks.DIRT);
        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        context.setBlock(redstoneBlockPos, Blocks.REDSTONE_BLOCK);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
        CreativeEngineBlockEntity engine = context.getBlockEntity(enginePos, CreativeEngineBlockEntity.class);
        if (quarry == null || engine == null) {
            context.fail("Expected quarry and engine block entities");
            return;
        }

        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() - 1, absPos.getZ() + 1,
                absPos.getX() + 1, absPos.getZ() + 3);

        // Cycle to the max output level (1280 RF/t); the quarry's own per-call cap (1 000 RF)
        // still throttles what it actually receives each tick.
        for (int i = 0; i < 6; i++) {
            engine.cycleOutputLevel();
        }

        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIRT));
    }

    /** Verifies lava is treated as unminable — like bedrock: never mined, never replaced — and the quarry still finishes. */
    public static void testQuarryTreatsLavaAsUnminableAndSkipsThatColumn(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        BlockPos lavaPos = new BlockPos(1, 1, 3); // inside the 1×1 inner mining area

        for (int dy = 0; dy <= LaserQuarryGeometry.Y_OFFSET_ABOVE; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }

        // The mining region defaults to solid stone (not air) so the lava source is fully
        // enclosed and can't spread via ordinary fluid physics.
        for (int dy = -1; dy >= -3; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.STONE);
                }
            }
        }

        context.setBlock(lavaPos, Blocks.LAVA);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() - 1, absPos.getZ() + 1,
                absPos.getX() + 1, absPos.getZ() + 3);

        IEnergyStorage es = quarry.energyStorage(Direction.DOWN);
        long remaining = QuarryEnergy.energyCapacity();
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted == 0) {
                context.fail("Failed to fill quarry energy: insert returned 0 with " + remaining + " RF remaining");
                return;
            }
            remaining -= inserted;
        }

        // Succeeds once the quarry finishes, as long as the lava was never disturbed along the way.
        context.succeedWhen(() -> {
            if (context.getBlockState(lavaPos).getFluidState().isEmpty()) {
                throw context.assertionException(
                        "Quarry must never mine or replace lava — it should be left in place like bedrock at "
                                + lavaPos);
            }
            if (!quarry.isFinished()) {
                throw context.assertionException(
                        "Quarry should finish by treating the lava-blocked column like bedrock, not stall on it");
            }
        });
    }

    /** Verifies the quarry never mines ground beneath lava it never removed, even after giving up waiting on that column. */
    public static void testQuarryDoesNotMineBelowUnremovedLava(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        BlockPos lavaPos = new BlockPos(1, 1, 3); // inside the 1×1 inner mining area
        BlockPos belowLavaPos = new BlockPos(1, 0, 3); // one layer directly below the lava

        for (int dy = 0; dy <= LaserQuarryGeometry.Y_OFFSET_ABOVE; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }

        // The mining region defaults to solid stone (not air) so the lava pocket is fully
        // enclosed and can't spread via ordinary fluid physics — an unconfined source block would
        // flow into any open neighbor, which could let it (or its output) end up somewhere this
        // test doesn't check.
        for (int dy = -1; dy >= -3; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.STONE);
                }
            }
        }

        context.setBlock(lavaPos, Blocks.LAVA);
        context.setBlock(belowLavaPos, Blocks.STONE);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() - 1, absPos.getZ() + 1,
                absPos.getX() + 1, absPos.getZ() + 3);

        IEnergyStorage es = quarry.energyStorage(Direction.DOWN);
        long remaining = QuarryEnergy.energyCapacity();
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted == 0) {
                context.fail("Failed to fill quarry energy: insert returned 0 with " + remaining + " RF remaining");
                return;
            }
            remaining -= inserted;
        }

        // Lava is never removed in this test. Even once the quarry has moved on past this
        // permanently blocked column, the stone directly beneath must stay untouched.
        context.succeedWhen(() -> {
            if (context.getBlockState(belowLavaPos).getBlock() != Blocks.STONE) {
                throw context.assertionException(
                        "Quarry must not mine ground below lava it never removed at " + belowLavaPos);
            }
            if (!quarry.isFinished()) {
                throw context.assertionException("Quarry should still finish despite the permanently blocked column");
            }
        });
    }

    /**
     * Verifies a blocked column stays tied to its real-world position across the mining zigzag's
     * layer-to-layer reflection, rather than to the scan-order grid index it happened to be found at.
     */
    public static void testQuarryTracksBlockedColumnAcrossZigzagLayerReflection(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        BlockPos lavaPos = new BlockPos(1, 1, 3); // world column A, shallow mining layer
        BlockPos stoneBelowLava = new BlockPos(1, 0, 3); // world column A, one layer deeper

        for (int dy = 0; dy <= LaserQuarryGeometry.Y_OFFSET_ABOVE; dy++) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }

        // The mining region defaults to solid stone (not air) so the lava pocket below is fully
        // enclosed on every side and can't spread into column B — a source block left with any
        // open neighbor spreads via ordinary fluid physics, which would contaminate the "control"
        // column and mask the very bug this test exists to catch.
        for (int dy = -1; dy >= -3; dy--) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.STONE);
                }
            }
        }

        context.setBlock(lavaPos, Blocks.LAVA);
        context.setBlock(stoneBelowLava, Blocks.STONE);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        // 4-wide outer frame -> 2-wide inner (world columns A, B); 1-deep inner in Z.
        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() - 1, absPos.getZ() + 1,
                absPos.getX() + 2, absPos.getZ() + 3);

        IEnergyStorage es = quarry.energyStorage(Direction.DOWN);
        long remaining = QuarryEnergy.energyCapacity();
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted == 0) {
                context.fail("Failed to fill quarry energy: insert returned 0 with " + remaining + " RF remaining");
                return;
            }
            remaining -= inserted;
        }

        // Never remove the lava. Keep topping up energy — the wider frame costs more than the
        // single up-front fill covers.
        context.succeedWhen(() -> {
            es.insert(1000, false);
            if (context.getBlockState(stoneBelowLava).getBlock() != Blocks.STONE) {
                throw context.assertionException(
                        "Quarry must not mine ground below lava it never removed at " + stoneBelowLava);
            }
            if (!quarry.isFinished()) {
                throw context.assertionException("Quarry should still finish despite the permanently blocked column");
            }
        });
    }

    /**
     * Verifies the quarry returns to break a block that reappears in an already-processed cell
     * before it commits to descending to the next layer.
     */
    public static void testQuarryReMinesBlockThatReappearsInProcessedLayer(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        BlockPos chestPos = new BlockPos(1, 3, 1);
        BlockPos cellA = new BlockPos(1, 1, 3); // mined/passed first
        BlockPos cellB = new BlockPos(1, 1, 4); // mined second — keeps the layer "in progress"

        for (int dy = 0; dy <= LaserQuarryGeometry.Y_OFFSET_ABOVE; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 1; dz <= 4; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }

        // cellA starts empty so the very first mining scan passes over it as already-clear.
        context.setBlock(cellB, Blocks.DIRT);
        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        // 4-deep outer frame -> 2-deep inner (cellA, cellB); 1-wide inner in X.
        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() - 1, absPos.getZ() + 1,
                absPos.getX() + 1, absPos.getZ() + 4);

        IEnergyStorage es = quarry.energyStorage(Direction.DOWN);
        long remaining = QuarryEnergy.energyCapacity();
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted == 0) {
                context.fail("Failed to fill quarry energy: insert returned 0 with " + remaining + " RF remaining");
                return;
            }
            remaining -= inserted;
        }

        // isArmInitialized() confirms the live scan already passed cellA, not just MINING starting.
        boolean[] placed = {false};
        context.succeedWhen(() -> {
            es.insert(1000, false);

            if (!placed[0] && quarry.isArmInitialized()) {
                context.setBlock(cellA, Blocks.DIRT);
                placed[0] = true;
            }
            if (!placed[0]) {
                throw context.assertionException("Waiting for the arm to initialize on its first mining target");
            }
            if (!context.getBlockState(cellA).isAir()) {
                throw context.assertionException(
                        "Quarry must re-mine a block that reappeared in an already-processed cell at " + cellA);
            }
        });
    }

    /**
     * Verifies the quarry still respects unremoved lava after a save/load round trip, even once
     * the mining cursor has already moved past it.
     */
    public static void testQuarryStillRespectsLavaAfterReload(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        BlockPos lavaPos = new BlockPos(1, 1, 3); // world column A — permanently blocked
        BlockPos stoneBelowLava = new BlockPos(1, 0, 3); // world column A, one layer deeper
        BlockPos columnB = new BlockPos(2, 1, 3); // world column B — reachable, gives the arm somewhere to go

        for (int dy = 0; dy <= LaserQuarryGeometry.Y_OFFSET_ABOVE; dy++) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }

        for (int dy = -1; dy >= -3; dy--) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.STONE);
                }
            }
        }

        context.setBlock(lavaPos, Blocks.LAVA);
        context.setBlock(stoneBelowLava, Blocks.STONE);
        context.setBlock(columnB, Blocks.STONE);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity[] quarry = {context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class)};
        if (quarry[0] == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        // 4-wide outer frame -> 2-wide inner (world columns A, B); 1-deep inner in Z.
        BlockPos absPos = context.absolutePos(quarryPos);
        quarry[0].setCustomBounds(
                absPos.getX() - 1, absPos.getZ() + 1,
                absPos.getX() + 2, absPos.getZ() + 3);

        IEnergyStorage es = quarry[0].energyStorage(Direction.DOWN);
        long remaining = QuarryEnergy.energyCapacity();
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted == 0) {
                context.fail("Failed to fill quarry energy: insert returned 0 with " + remaining + " RF remaining");
                return;
            }
            remaining -= inserted;
        }

        HolderLookup.Provider registries = context.getLevel().registryAccess();

        // Wait until column B's shallow block (same layer as the lava) is actually mined — not
        // just until the arm reaches it — so the mining cursor has genuinely moved on to a
        // deeper layer before the reload, matching the reported bug: the quarry only loses track
        // of a blocked column once it has already progressed past the layer the lava sits on.
        // Then simulate a real world reload — not just a save/load call on the live object, which
        // wouldn't reset anything the old object was still holding onto in memory — by destroying
        // the block and reconstructing a genuinely new instance from the saved NBT, exactly like
        // a chunk unload/reload does. Then keep checking the invariant holds afterward.
        boolean[] reloaded = {false};
        context.succeedWhen(() -> {
            es.insert(1000, false);

            if (!reloaded[0]) {
                if (!context.getBlockState(columnB).isAir()) {
                    throw context.assertionException("Waiting for column B's shallow block to be mined");
                }
                CompoundTag saved = quarry[0].saveCustomOnly(registries);
                context.setBlock(quarryPos, Blocks.AIR);
                context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);
                quarry[0] = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
                if (quarry[0] == null) {
                    throw context.assertionException("Expected a fresh LaserQuarryBlockEntity after reload at " + quarryPos);
                }
                quarry[0].loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));
                reloaded[0] = true;
            }

            if (context.getBlockState(stoneBelowLava).getBlock() != Blocks.STONE) {
                throw context.assertionException(
                        "Quarry must not mine ground below lava it never removed at " + stoneBelowLava);
            }
            if (!quarry[0].isFinished()) {
                throw context.assertionException("Quarry should still finish despite the never-removed lava");
            }
        });
    }

    /**
     * Verifies a block placed back into an already-mined cell gets re-mined even after the cursor
     * has moved on several layers further, not just the one layer immediately behind it.
     */
    public static void testQuarryReMinesBlockPlacedManyLayersBehindCursor(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 2, 1);
        BlockPos firstLayerPos = new BlockPos(1, 1, 3); // mined first, directly below the quarry
        BlockPos secondLayerPos = new BlockPos(1, 0, 3);
        BlockPos thirdLayerPos = new BlockPos(1, -1, 3);

        for (int dy = 0; dy <= LaserQuarryGeometry.Y_OFFSET_ABOVE; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 1; dz <= 3; dz++) {
                    context.setBlock(quarryPos.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }

        context.setBlock(firstLayerPos, Blocks.STONE);
        context.setBlock(secondLayerPos, Blocks.STONE);
        context.setBlock(thirdLayerPos, Blocks.STONE);
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        LaserQuarryBlockEntity quarry = context.getBlockEntity(quarryPos, LaserQuarryBlockEntity.class);
        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity at " + quarryPos);
            return;
        }

        BlockPos absPos = context.absolutePos(quarryPos);
        quarry.setCustomBounds(
                absPos.getX() - 1, absPos.getZ() + 1,
                absPos.getX() + 1, absPos.getZ() + 3);

        IEnergyStorage es = quarry.energyStorage(Direction.DOWN);
        long remaining = QuarryEnergy.energyCapacity();
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted == 0) {
                context.fail("Failed to fill quarry energy: insert returned 0 with " + remaining + " RF remaining");
                return;
            }
            remaining -= inserted;
        }

        // Wait until the cursor has mined three layers deep (proving it moved well past the first
        // one, not just one layer behind), then place a block back into that first layer — a spot
        // a recheck scoped to only "the layer just left" could never look at again.
        boolean[] placed = {false};
        context.succeedWhen(() -> {
            es.insert(1000, false);

            if (!placed[0]) {
                if (!context.getBlockState(thirdLayerPos).isAir()) {
                    throw context.assertionException("Waiting for the cursor to mine three layers deep");
                }
                context.setBlock(firstLayerPos, Blocks.STONE);
                placed[0] = true;
            }

            if (!context.getBlockState(firstLayerPos).isAir()) {
                throw context.assertionException(
                        "Quarry must re-mine a block placed back several layers behind the cursor at " + firstLayerPos);
            }
        });
    }

    /**
     * Breaking a container spills its contents as loose items before the quarry ever sees them, so
     * the quarry sweeps the area around what it just mined. That sweep must still work.
     *
     * <p>Drives {@link QuarryBlockBreaker#mineBlock} directly rather than running the phase machine:
     * the contract under test is what one break does to the items around it, and the state machine
     * is already covered above.
     */
    public static void testQuarryCollectsBrokenContainerContents(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 1, 1);
        BlockPos outputPos = quarryPos.above();
        BlockPos containerPos = new BlockPos(3, 1, 1);

        context.setBlock(outputPos, Blocks.CHEST);
        context.setBlock(containerPos, Blocks.CHEST);

        ChestBlockEntity container = context.getBlockEntity(containerPos, ChestBlockEntity.class);
        if (container == null) {
            context.fail("Expected a chest block entity at " + containerPos);
            return;
        }
        container.setItem(0, new ItemStack(Items.DIAMOND));

        mine(context, quarryPos, containerPos);

        context.assertContainerContains(outputPos, Items.DIAMOND);
        context.succeed();
    }

    /**
     * Breaking anything that is not a container must leave the ground alone. A quarry frame block is
     * used as the no-drop block because it has no loot table at all, so the case is deterministic;
     * in real play the common triggers are leaves failing their sapling roll, grass and fire.
     */
    public static void testQuarryLeavesLooseItemsWhenBreakingANonContainer(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 1, 1);
        BlockPos outputPos = quarryPos.above();
        BlockPos targetPos = new BlockPos(3, 1, 1);
        BlockPos droppedPos = new BlockPos(3, 1, 2);

        context.setBlock(outputPos, Blocks.CHEST);
        context.setBlock(targetPos, LogisticsAutomation.BLOCK.LASER_QUARRY_FRAME);
        context.spawnItem(Items.DIAMOND, droppedPos);

        mine(context, quarryPos, targetPos);

        context.assertItemEntityPresent(Items.DIAMOND, droppedPos, 1.0);
        context.assertContainerEmpty(outputPos);
        context.succeed();
    }

    /**
     * A container's spilled contents are the quarry's; anything that was already lying beside it is
     * not. The same distinction covers the block entities that spill nothing at all — a bed, sign or
     * spawner mined next to a dropped stack adds no items, so there is nothing for the quarry to take.
     */
    public static void testQuarryLeavesLooseItemsLyingBesideABrokenContainer(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(1, 1, 1);
        BlockPos outputPos = quarryPos.above();
        BlockPos containerPos = new BlockPos(3, 1, 1);
        BlockPos droppedPos = new BlockPos(3, 1, 2);

        context.setBlock(outputPos, Blocks.CHEST);
        context.setBlock(containerPos, Blocks.CHEST);

        ChestBlockEntity container = context.getBlockEntity(containerPos, ChestBlockEntity.class);
        if (container == null) {
            context.fail("Expected a chest block entity at " + containerPos);
            return;
        }
        container.setItem(0, new ItemStack(Items.DIAMOND));
        context.spawnItem(Items.EMERALD, droppedPos);

        mine(context, quarryPos, containerPos);

        context.assertContainerContains(outputPos, Items.DIAMOND);
        context.assertItemEntityPresent(Items.EMERALD, droppedPos, 1.0);
        context.succeed();
    }

    /** Mines {@code targetPos} exactly as the quarry would, routing output through {@code quarryPos}. */
    private static void mine(GameTestHelper context, BlockPos quarryPos, BlockPos targetPos) {
        ServerLevel level = context.getLevel();
        BlockPos absTarget = context.absolutePos(targetPos);
        QuarryBlockBreaker.mineBlock(
                level, absTarget, level.getBlockState(absTarget), new QuarryOutput(context.absolutePos(quarryPos)));
    }
}
