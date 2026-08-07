package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.automation.transposer.TransposerBlockEntity;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Gametests for the Transposer: bucket ⇄ fluid conversion, RF-gating, and atomicity — a blocked output,
 * a fluid mismatch, or an empty energy buffer must leave both the input item and the tank untouched.
 */
public class TransposerGameTest {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    // Default bucket recipes cost 800 RF at 20 RF/t (see LogisticsAutomation.CONFIG.TRANSPOSER_*):
    // 40 ticks to complete. Seeding the buffer to capacity before each positive-path test keeps the
    // delay generous without coupling the test to the exact default cost.
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

    /**
     * Fills the buffer to capacity. {@code insert} is rate-limited per call by {@code maxInsert} (the
     * same limit a cable would hit), so this calls it repeatedly until the buffer stops accepting —
     * either capacity is reached, or (if {@code maxInsert} were ever configured to 0) it never was.
     */
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

    @GameTest
    public void placement(GameTestHelper context) {
        place(context, new BlockPos(1, 1, 1));
        context.succeed();
    }

    /** Empty bucket + tank of lava + a full energy buffer → lava bucket out, tank drained by 1000 mB. */
    @GameTest(maxTicks = 60)
    public void fillFromLava(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TransposerBlockEntity be = place(context, pos);

        be.tank().insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(1_000), false);
        be.setItem(INPUT_SLOT, new ItemStack(Items.BUCKET));
        chargeFully(be);

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

    /** Filled bucket of a custom mod fluid + room in the tank + full energy → empty bucket out, tank gains 1000 mB. */
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

    /** Empty bucket + tank holding less than 1000 mB + full energy → rejected: input and tank untouched. */
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

    /** The atomicity test: output blocked → no-op. Neither the input bucket nor the tank may change. */
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

    /** No RF in the buffer → no-op. A valid fill recipe must still not run without energy to spend. */
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
}
