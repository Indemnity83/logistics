package com.logistics.gametest;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.kiln.KilnBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shared datapack-reload lifecycle GameTest body, compiled directly into both loaders'
 * {@code gametest} source sets (see {@code common/build.gradle}).
 *
 * <p>A reload swaps every recipe, loot table, and tag while the world keeps running, so a machine
 * holding a resolved recipe mid-run is exposed to it. The risk is not a crash but a quiet accounting
 * error: progress silently restarting (the player pays twice), or a run completing twice (the player
 * gets a free ingot). {@code RecipeProcessorComponent} compares the resolved plan by value rather
 * than identity, which is what makes a fresh-but-equivalent plan after a reload continue the run
 * instead of resetting it — these tests pin that behaviour.
 *
 * <p>Reloading does not disturb the rest of the suite, so these are ordinary shared feature tests
 * and need no isolated run; see "A datapack reload does not need an isolated lane" in TESTING.md.
 */
public class ReloadLifecycleGameTestBody {

    private static final BlockPos KILN_POS = new BlockPos(1, 1, 1);

    /** Raw iron costs cookingTime(200) * KILN_RF_PER_COOK_TICK(10), drained at 20 RF/t over 100 ticks. */
    private static final long SMELT_COST = 2_000L;

    private static final long FULL_CHARGE = 10_000L;

    /**
     * A smelt already in flight when the datapack reloads finishes exactly once, for exactly its
     * normal cost.
     *
     * <p>The energy assertion is what distinguishes "continued" from "restarted": a reset run would
     * still finish eventually, just later and after spending more, so asserting only on the output
     * item would let a silent progress reset through.
     */
    public static void kilnCompletesInFlightSmeltAcrossReload(GameTestHelper context) {
        KilnBlockEntity kiln = placeChargedKiln(context);
        if (kiln == null) {
            return;
        }
        var energy = kiln.energyStorage(null);
        kiln.setItem(0, new ItemStack(Items.RAW_IRON));

        // Reload halfway through the 100-tick smelt.
        context.runAfterDelay(50, () -> {
            long spent = FULL_CHARGE - energy.getAmount();
            // Guard against a vacuous pass: reloading before the smelt started would prove nothing.
            if (spent <= 0 || spent >= SMELT_COST) {
                context.fail("Expected the kiln to be mid-smelt before reloading, but it had spent " + spent + " RF");
                return;
            }
            if (!reloadDatapacks(context)) {
                context.fail("The reload did not replace the recipe manager, so this proves nothing");
                return;
            }
            if (kiln.getItem(0).isEmpty()) {
                context.fail("Reloading cleared the kiln's input");
            }
        });

        // The smelt would have finished at tick 100 undisturbed; a restart at tick 50 would push it
        // to 150 and still be running here.
        context.runAfterDelay(120, () -> {
            if (!kiln.getItem(0).isEmpty()) {
                context.fail("Input should be consumed once the smelt completes; the reload restarted it");
                return;
            }
            ItemStack output = kiln.getItem(1);
            if (!output.is(Items.IRON_INGOT) || output.getCount() != 1) {
                context.fail("Expected exactly one iron ingot after one smelt, got: " + output);
                return;
            }
            long spent = FULL_CHARGE - energy.getAmount();
            if (spent != SMELT_COST) {
                context.fail("A smelt spanning a reload should still cost exactly " + SMELT_COST
                    + " RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }

    /**
     * The kiln still resolves recipes after a reload.
     *
     * <p>Covers the opposite failure from the test above: rather than corrupting a run already in
     * flight, a reload could leave the resolver pointing at a discarded recipe manager so that no
     * <em>new</em> run ever starts — a machine that simply stops working, with no error.
     */
    public static void kilnStartsNewSmeltAfterReload(GameTestHelper context) {
        KilnBlockEntity kiln = placeChargedKiln(context);
        if (kiln == null) {
            return;
        }
        var energy = kiln.energyStorage(null);

        // Reload while idle, then feed it.
        context.runAfterDelay(5, () -> {
            if (!reloadDatapacks(context)) {
                context.fail("The reload did not replace the recipe manager, so this proves nothing");
                return;
            }
            kiln.setItem(0, new ItemStack(Items.RAW_IRON));
        });

        context.runAfterDelay(130, () -> {
            ItemStack output = kiln.getItem(1);
            if (!output.is(Items.IRON_INGOT) || output.getCount() != 1) {
                context.fail("Kiln should smelt normally after a reload, got: " + output);
                return;
            }
            long spent = FULL_CHARGE - energy.getAmount();
            if (spent != SMELT_COST) {
                context.fail("A post-reload smelt should cost exactly " + SMELT_COST + " RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }

    /**
     * Reloads the datapacks currently selected, blocking until the swap has happened.
     *
     * <p>Returns whether the reload actually replaced the server's recipe manager. Without this the
     * tests would pass just as happily against a reload that silently did nothing, which is the one
     * way they could be worthless.
     */
    private static boolean reloadDatapacks(GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        Object before = server.getRecipeManager();
        server.reloadResources(server.getPackRepository().getSelectedIds()).join();
        return server.getRecipeManager() != before;
    }

    /** Places a kiln charged to its 10,000 RF capacity, so energy spent is measurable against it. */
    private static KilnBlockEntity placeChargedKiln(GameTestHelper context) {
        context.setBlock(KILN_POS, LogisticsAutomation.BLOCK.KILN);
        KilnBlockEntity kiln = context.getBlockEntity(KILN_POS, KilnBlockEntity.class);
        if (kiln == null) {
            context.fail("Expected KilnBlockEntity");
            return null;
        }
        var energy = kiln.energyStorage(null);
        // insert() caps each call at KILN_MAX_ENERGY_INPUT(128), so fill in repeated calls.
        for (int i = 0; i < 80 && energy.getAmount() < FULL_CHARGE; i++) {
            energy.insert(128, false);
        }
        if (energy.getAmount() != FULL_CHARGE) {
            context.fail("Expected a full 10,000 RF kiln, got: " + energy.getAmount());
            return null;
        }
        return kiln;
    }
}
