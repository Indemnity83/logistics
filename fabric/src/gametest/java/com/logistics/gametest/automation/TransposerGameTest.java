package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.automation.transposer.TransposerBlockEntity;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Gametests for the Transposer: bucket ⇄ fluid conversion, RF-gating, and atomicity — a blocked output,
 * a fluid mismatch, or an empty energy buffer must leave both the input item and the tank untouched.
 */
public class TransposerGameTest {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    // The transposer is precharged to capacity before each positive-path test.
    private static final long FULL_ENERGY = 1_000_000L;
    private static final int COMPLETE_DELAY = 45;

    private static TransposerBlockEntity place(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsAutomation.BLOCK.TRANSPOSER);
        TransposerBlockEntity be = context.getBlockEntity(pos, TransposerBlockEntity.class);
        if (be == null) {
            context.fail("Transposer should create TransposerBlockEntity");
        }
        return be;
    }

    /** Fills the buffer to capacity, looping since {@code insert} is rate-limited per call by {@code maxInsert}. */
    private static void chargeFully(TransposerBlockEntity be) {
        var energy = be.energyStorage(null);
        if (energy == null) {
            throw new IllegalStateException("Transposer should expose an energy capability");
        }
        for (int i = 0; i < 1000 && energy.insert(FULL_ENERGY, false) > 0; i++) {
            // keep inserting until the buffer stops accepting
        }
    }

    private static Fluid liquidRedstone() {
        return BuiltInRegistries.FLUID.getValue(LogisticsCore.resource("liquid_redstone").toIdentifier());
    }

    private static Fluid seedOil() {
        return BuiltInRegistries.FLUID.getValue(LogisticsCore.resource("seed_oil").toIdentifier());
    }

    @GameTest
    public void placement(GameTestHelper context) {
        place(context, new BlockPos(1, 1, 1));
        context.succeed();
    }

    /**
     * Wiki claim (Usage): "An empty bucket plus at least 1,000 mB in the tank becomes a filled
     * bucket of that fluid; the tank loses 1,000 mB." (Power): "A bucket fill/empty costs 800 RF."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
     */
    @GameTest(maxTicks = 60)
    public void fillFromLava(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        be.tank().insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(1_000), false);
        be.setItem(INPUT_SLOT, new ItemStack(Items.BUCKET));
        chargeFully(be);
        long filledEnergy = be.energyStorage(null).getAmount();

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(OUTPUT_SLOT).is(Items.LAVA_BUCKET)) {
                context.fail("Expected a lava bucket in the output, got: " + be.getItem(OUTPUT_SLOT));
                return;
            }
            if (!be.getItem(INPUT_SLOT).isEmpty()) {
                context.fail("Input bucket should have been consumed");
                return;
            }
            if (be.tank().getAmount() != 0) {
                context.fail("Tank should be empty after filling one bucket, got: " + be.tank().getAmount());
                return;
            }
            long spent = filledEnergy - be.energyStorage(null).getAmount();
            if (spent != 800) {
                context.fail("Filling a bucket should cost exactly 800 RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }

    /** Empty bucket + tank of a custom mod fluid + full energy → that fluid's bucket out, tank drained. */
    @GameTest(maxTicks = 60)
    public void fillFromCustomFluid(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        Fluid fluid = liquidRedstone();
        Item bucket = LogisticsCore.BUCKET.forFluid("liquid_redstone");
        be.tank().insert(SimpleFluidKey.of(fluid), FluidUnits.mb(1_000), false);
        be.setItem(INPUT_SLOT, new ItemStack(Items.BUCKET));
        chargeFully(be);

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(OUTPUT_SLOT).is(bucket)) {
                context.fail("Expected a liquid redstone bucket in the output, got: " + be.getItem(OUTPUT_SLOT));
                return;
            }
            if (!be.getItem(INPUT_SLOT).isEmpty()) {
                context.fail("Input bucket should have been consumed");
                return;
            }
            if (be.tank().getAmount() != 0) {
                context.fail("Tank should be empty after filling one bucket, got: " + be.tank().getAmount());
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Usage): "A filled bucket — water, lava, or any Logistics fluid bucket — plus room
     * for 1,000 mB becomes a plain empty bucket; the tank gains 1,000 mB."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
     */
    @GameTest(maxTicks = 60)
    public void emptyCustomFluidBucket(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        Fluid fluid = liquidRedstone();
        Item bucket = LogisticsCore.BUCKET.forFluid("liquid_redstone");
        be.setItem(INPUT_SLOT, new ItemStack(bucket));
        chargeFully(be);

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(OUTPUT_SLOT).is(Items.BUCKET)) {
                context.fail("Expected an empty bucket in the output, got: " + be.getItem(OUTPUT_SLOT));
                return;
            }
            if (!be.getItem(INPUT_SLOT).isEmpty()) {
                context.fail("Filled bucket should have been consumed");
                return;
            }
            if (be.tank().getAmount() != FluidUnits.mb(1_000) || be.tank().getFluidKey().getFluid() != fluid) {
                context.fail("Tank should hold 1000 mB of liquid redstone, got: " + be.tank().getAmount());
                return;
            }
            context.succeed();
        });
    }

    /** Empty bucket + tank of seed oil + full energy → seed oil bucket out, tank drained by 1000 mB. */
    @GameTest(maxTicks = 60)
    public void fillSeedOilBucket(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        Fluid fluid = seedOil();
        Item bucket = LogisticsCore.BUCKET.forFluid("seed_oil");
        be.tank().insert(SimpleFluidKey.of(fluid), FluidUnits.mb(1_000), false);
        be.setItem(INPUT_SLOT, new ItemStack(Items.BUCKET));
        chargeFully(be);

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(OUTPUT_SLOT).is(bucket)) {
                context.fail("Expected a seed oil bucket in the output, got: " + be.getItem(OUTPUT_SLOT));
                return;
            }
            if (!be.getItem(INPUT_SLOT).isEmpty()) {
                context.fail("Input bucket should have been consumed");
                return;
            }
            if (be.tank().getAmount() != 0) {
                context.fail("Tank should be empty after filling one bucket, got: " + be.tank().getAmount());
                return;
            }
            context.succeed();
        });
    }

    /** Seed oil bucket + room in the tank + full energy → empty bucket out, tank gains 1000 mB of seed oil. */
    @GameTest(maxTicks = 60)
    public void emptySeedOilBucket(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        Fluid fluid = seedOil();
        Item bucket = LogisticsCore.BUCKET.forFluid("seed_oil");
        be.setItem(INPUT_SLOT, new ItemStack(bucket));
        chargeFully(be);

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(OUTPUT_SLOT).is(Items.BUCKET)) {
                context.fail("Expected an empty bucket in the output, got: " + be.getItem(OUTPUT_SLOT));
                return;
            }
            if (!be.getItem(INPUT_SLOT).isEmpty()) {
                context.fail("Filled bucket should have been consumed");
                return;
            }
            if (be.tank().getAmount() != FluidUnits.mb(1_000) || be.tank().getFluidKey().getFluid() != fluid) {
                context.fail("Tank should hold 1000 mB of seed oil, got: " + be.tank().getAmount());
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Usage): "The conversion is atomic: if the output slot can't hold the result or
     * there isn't enough RF banked, nothing is consumed and the tank is untouched." An under-full
     * tank is the fluid-side equivalent — there isn't a valid result to produce, so nothing runs.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
     */
    @GameTest(maxTicks = 60)
    public void insufficientTankAmountRejected(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        be.tank().insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(500), false);
        be.setItem(INPUT_SLOT, new ItemStack(Items.BUCKET));
        chargeFully(be);

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(INPUT_SLOT).is(Items.BUCKET)) {
                context.fail("Empty bucket should be untouched when the tank has too little fluid");
                return;
            }
            if (!be.getItem(OUTPUT_SLOT).isEmpty()) {
                context.fail("Nothing should have been produced from an under-full tank");
                return;
            }
            if (be.tank().getAmount() != FluidUnits.mb(500) || be.tank().getFluidKey().getFluid() != Fluids.LAVA) {
                context.fail("Tank should still hold 500 mB of lava, got: " + be.tank().getAmount());
                return;
            }
            context.succeed();
        });
    }

    /** Lava bucket + room in the tank + full energy → empty bucket out, tank gains 1000 mB of lava. */
    @GameTest(maxTicks = 60)
    public void emptyLavaBucket(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        be.setItem(INPUT_SLOT, new ItemStack(Items.LAVA_BUCKET));
        chargeFully(be);

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(OUTPUT_SLOT).is(Items.BUCKET)) {
                context.fail("Expected an empty bucket in the output, got: " + be.getItem(OUTPUT_SLOT));
                return;
            }
            if (!be.getItem(INPUT_SLOT).isEmpty()) {
                context.fail("Filled bucket should have been consumed");
                return;
            }
            if (be.tank().getAmount() != FluidUnits.mb(1_000) || be.tank().getFluidKey().getFluid() != Fluids.LAVA) {
                context.fail("Tank should hold 1000 mB of lava, got: " + be.tank().getAmount());
                return;
            }
            context.succeed();
        });
    }

    /** Filled bucket of one fluid against a tank holding another + full energy → rejected: untouched. */
    @GameTest(maxTicks = 60)
    public void fluidMismatchRejected(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        be.tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(1_000), false);
        be.setItem(INPUT_SLOT, new ItemStack(Items.LAVA_BUCKET));
        chargeFully(be);

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(INPUT_SLOT).is(Items.LAVA_BUCKET)) {
                context.fail("Lava bucket should be untouched on a fluid mismatch");
                return;
            }
            if (!be.getItem(OUTPUT_SLOT).isEmpty()) {
                context.fail("Nothing should have been produced on a fluid mismatch");
                return;
            }
            if (be.tank().getAmount() != FluidUnits.mb(1_000) || be.tank().getFluidKey().getFluid() != Fluids.WATER) {
                context.fail("Tank should still hold 1000 mB of water, got: " + be.tank().getAmount());
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Usage): "The conversion is atomic: if the output slot can't hold the result...
     * nothing is consumed and the tank is untouched." Neither the input bucket nor the tank may
     * change when the output is blocked.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
     */
    @GameTest(maxTicks = 60)
    public void blockedOutputIsNoOp(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        be.tank().insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(1_000), false);
        be.setItem(INPUT_SLOT, new ItemStack(Items.BUCKET));
        // Output blocked by an incompatible item, so the lava bucket result cannot be placed.
        be.setItem(OUTPUT_SLOT, new ItemStack(Items.STONE));
        chargeFully(be);

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(INPUT_SLOT).is(Items.BUCKET)) {
                context.fail("Input empty bucket must be untouched when the output is blocked");
                return;
            }
            if (!be.getItem(OUTPUT_SLOT).is(Items.STONE)) {
                context.fail("Blocking item must be untouched");
                return;
            }
            if (be.tank().getAmount() != FluidUnits.mb(1_000)) {
                context.fail("Tank must be unchanged when the output is blocked, got: " + be.tank().getAmount());
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Usage): "...if there isn't enough RF banked, nothing is consumed and the tank is
     * untouched." A valid fill recipe must still not run without energy to spend.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
     */
    @GameTest(maxTicks = 60)
    public void noEnergyIsNoOp(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        be.tank().insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(1_000), false);
        be.setItem(INPUT_SLOT, new ItemStack(Items.BUCKET));
        // No chargeFully(be) — the buffer starts empty.

        context.runAfterDelay(COMPLETE_DELAY, () -> {
            if (!be.getItem(INPUT_SLOT).is(Items.BUCKET)) {
                context.fail("Input empty bucket must be untouched with no energy to spend");
                return;
            }
            if (!be.getItem(OUTPUT_SLOT).isEmpty()) {
                context.fail("Nothing should have been produced with an empty energy buffer");
                return;
            }
            if (be.tank().getAmount() != FluidUnits.mb(1_000)) {
                context.fail("Tank must be unchanged with no energy to spend, got: " + be.tank().getAmount());
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Usage/Power): "An empty bucket plus at least 1,000 mB in the tank becomes a
     * filled bucket... A bucket fill/empty costs 800 RF." The test above proves the RF cost and
     * atomicity by manipulating storages directly; this one proves the whole feature as a player
     * actually wires it up — a real engine (no cable) delivering power, and a real hopper pushing
     * the empty bucket in and another pulling the filled bucket out. The tank is preloaded
     * directly since fluid-pipe connectivity to the transposer isn't the claim under test here.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
     */
    @GameTest(maxTicks = 100)
    public void fillsBucketViaRealEngineAndHoppers(GameTestHelper context) {
        BlockPos transposerPos = new BlockPos(1, 1, 1);
        BlockPos enginePos = new BlockPos(0, 1, 1);
        BlockPos redstoneBlockPos = new BlockPos(-1, 1, 1);
        BlockPos inputHopperPos = transposerPos.above();
        BlockPos outputHopperPos = transposerPos.below();

        TransposerBlockEntity be = place(context, transposerPos);
        context.setBlock(redstoneBlockPos, Blocks.REDSTONE_BLOCK);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        context.setBlock(inputHopperPos, Blocks.HOPPER);
        context.setBlock(outputHopperPos, Blocks.HOPPER);

        CreativeEngineBlockEntity engine = context.getBlockEntity(enginePos, CreativeEngineBlockEntity.class);
        HopperBlockEntity inputHopper = context.getBlockEntity(inputHopperPos, HopperBlockEntity.class);
        if (be == null || engine == null || inputHopper == null) {
            context.fail("Expected transposer, engine, and input hopper block entities");
            return;
        }

        be.tank().insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(1_000), false);
        // Cycle 20 -> 40 -> 80 -> 160 RF/t, comfortably above the transposer's 128 RF/t input cap.
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();

        inputHopper.setItem(0, new ItemStack(Items.BUCKET));

        context.succeedWhen(() -> context.assertContainerContains(outputHopperPos, Items.LAVA_BUCKET));
    }
}
