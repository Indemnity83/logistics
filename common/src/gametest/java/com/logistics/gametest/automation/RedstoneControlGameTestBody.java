package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import com.logistics.automation.fabricator.SequentialFabricatorBlockEntity;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.automation.transposer.TransposerBlockEntity;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.MachineEntity;
import com.logistics.core.machine.control.RedstoneMode;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared machine redstone-control GameTest bodies, compiled directly into both loaders' {@code
 * gametest} source sets (see {@code common/build.gradle}). Fabric's {@code @GameTest}-annotated
 * {@code RedstoneControlGameTest} delegates to these methods, and NeoForge's {@code
 * RedstoneControlGameTestRegistration} references them directly as {@code Consumer<GameTestHelper>}
 * method references.
 *
 * <p>The signal is always a real block beside the machine — a redstone block or a lever — so each test
 * exercises the vanilla neighbor-signal rule the machine reads.
 */
public class RedstoneControlGameTestBody {

    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
    private static final BlockPos ABOVE = MACHINE.above();
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    // Long enough that an unpaused run would visibly progress (every machine here spends RF each tick).
    private static final int HOLD_TICKS = 40;
    // Oak planks cost 1,000 RF in the macerator.
    private static final long PLANKS_ENERGY = 1_000;

    // ==================== Setup helpers ====================

    private static void chargeFully(IEnergyStorage energy) {
        for (int i = 0; i < 1000 && energy.insert(1_000_000L, false) > 0; i++) {
            // keep inserting until the buffer stops accepting
        }
    }

    /** A charged macerator with one oak plank queued (1,000 RF of work). */
    private static MaceratorBlockEntity loadedMacerator(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsAutomation.BLOCK.MACERATOR);
        MaceratorBlockEntity macerator = (MaceratorBlockEntity) context.getBlockEntity(pos);
        if (macerator == null) {
            throw new GameTestAssertException("Expected a macerator block entity at " + pos);
        }
        chargeFully(macerator.energyStorage(null));
        macerator.setItem(INPUT_SLOT, new ItemStack(Items.OAK_PLANKS));
        return macerator;
    }

    private static boolean maceratorFinished(MaceratorBlockEntity macerator) {
        return macerator.getItem(INPUT_SLOT).isEmpty() && !macerator.getItem(OUTPUT_SLOT).isEmpty();
    }

    private static boolean isLit(GameTestHelper context, BlockPos pos) {
        BlockState state = context.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    /** Places an unpowered lever on the floor east of the machine, on a block it can stay attached to. */
    private static BlockPos placeLever(GameTestHelper context) {
        BlockPos lever = MACHINE.east();
        context.setBlock(lever.below(), Blocks.STONE);
        context.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
        return lever;
    }

    // ==================== Shared scenario ====================

    /**
     * Asserts the loaded machine does nothing for {@link #HOLD_TICKS} — no progress, no RF spent, not
     * lit, no output — then applies {@code release} and waits for the run to finish.
     */
    private static void heldUntilReleased(
            GameTestHelper context, MachineEntity machine, BooleanSupplier finished, Runnable release) {
        IEnergyStorage energy = machine.energyStorage(null);
        long charged = energy.getAmount();
        context.startSequence()
                .thenExecuteAfter(HOLD_TICKS, () -> {
                    context.assertTrue(machine.processProgress() == 0f,
                            "A paused machine should make no progress, got: " + machine.processProgress());
                    context.assertTrue(energy.getAmount() == charged,
                            "A paused machine should spend no RF, spent: " + (charged - energy.getAmount()));
                    context.assertFalse(isLit(context, MACHINE), "A paused machine should not be lit");
                    context.assertFalse(finished.getAsBoolean(), "A paused machine should produce nothing");
                })
                .thenExecute(release)
                .thenWaitUntil(() -> context.assertTrue(
                        finished.getAsBoolean(), "Machine should finish its run once redstone permits it"))
                .thenSucceed();
    }

    // ==================== Macerator ====================

    /** The default mode ignores redstone: a macerator runs with no signal and with one. */
    public static void testIgnoredRunsWithAndWithoutSignal(GameTestHelper context) {
        BlockPos signalledPos = new BlockPos(4, 1, 1);
        MaceratorBlockEntity unsignalled = loadedMacerator(context, MACHINE);
        MaceratorBlockEntity signalled = loadedMacerator(context, signalledPos);
        context.setBlock(signalledPos.above(), Blocks.REDSTONE_BLOCK);

        for (MaceratorBlockEntity macerator : new MaceratorBlockEntity[] {unsignalled, signalled}) {
            if (macerator.redstoneControl() == null || macerator.redstoneControl().mode() != RedstoneMode.IGNORED) {
                context.fail("A macerator should default to IGNORED redstone mode");
                return;
            }
        }
        if (unsignalled.hasRedstoneSignal() || !signalled.hasRedstoneSignal()) {
            context.fail("Only the macerator under the redstone block should see a signal");
            return;
        }

        context.succeedWhen(() -> {
            context.assertTrue(maceratorFinished(unsignalled), "Unsignalled macerator should finish");
            context.assertTrue(maceratorFinished(signalled), "Signalled macerator should finish");
        });
    }

    /** HIGH holds the run with no signal; a redstone block placed beside it lets the run finish. */
    public static void testHighPausesUntilSignalled(GameTestHelper context) {
        MaceratorBlockEntity macerator = loadedMacerator(context, MACHINE);
        macerator.setRedstoneMode(RedstoneMode.HIGH);

        heldUntilReleased(context, macerator, () -> maceratorFinished(macerator),
                () -> context.setBlock(ABOVE, Blocks.REDSTONE_BLOCK));
    }

    /** LOW holds the run while a redstone block is beside it; removing the block lets it finish. */
    public static void testLowPausesWhileSignalled(GameTestHelper context) {
        context.setBlock(ABOVE, Blocks.REDSTONE_BLOCK);
        MaceratorBlockEntity macerator = loadedMacerator(context, MACHINE);
        macerator.setRedstoneMode(RedstoneMode.LOW);

        heldUntilReleased(context, macerator, () -> maceratorFinished(macerator),
                () -> context.setBlock(ABOVE, Blocks.AIR));
    }

    /**
     * Switching to a mode that forbids work mid-run freezes the run exactly where it was — progress kept,
     * no RF spent — and switching back finishes it for the recipe's cost and no more.
     */
    public static void testModeChangeMidRunFreezesProgress(GameTestHelper context) {
        MaceratorBlockEntity macerator = loadedMacerator(context, MACHINE);
        IEnergyStorage energy = macerator.energyStorage(null);
        long charged = energy.getAmount();
        float[] frozenProgress = new float[1];
        long[] frozenEnergy = new long[1];

        context.startSequence()
                .thenWaitUntil(() -> context.assertTrue(
                        macerator.processProgress() > 0f, "Macerator should start grinding"))
                .thenExecute(() -> {
                    macerator.setRedstoneMode(RedstoneMode.HIGH);
                    frozenProgress[0] = macerator.processProgress();
                    frozenEnergy[0] = energy.getAmount();
                    context.assertTrue(frozenProgress[0] < 1f, "Run should still be in progress when paused");
                })
                .thenExecuteAfter(HOLD_TICKS, () -> {
                    context.assertTrue(macerator.processProgress() == frozenProgress[0],
                            "Paused progress should stay at " + frozenProgress[0] + ", got: "
                                    + macerator.processProgress());
                    context.assertTrue(energy.getAmount() == frozenEnergy[0],
                            "A paused run should spend no RF, spent: " + (frozenEnergy[0] - energy.getAmount()));
                    context.assertFalse(isLit(context, MACHINE), "A paused macerator should not be lit");
                    context.assertFalse(maceratorFinished(macerator), "A paused run should not finish");
                })
                .thenExecute(() -> macerator.setRedstoneMode(RedstoneMode.IGNORED))
                .thenWaitUntil(() -> context.assertTrue(
                        maceratorFinished(macerator), "Macerator should finish once the mode permits it"))
                .thenExecute(() -> context.assertTrue(charged - energy.getAmount() == PLANKS_ENERGY,
                        "The paused-and-resumed run should cost exactly " + PLANKS_ENERGY + " RF, spent: "
                                + (charged - energy.getAmount())))
                .thenSucceed();
    }

    /**
     * With no screen open, flipping a lever beside a HIGH macerator starts, pauses (keeping progress) and
     * resumes it.
     */
    public static void testLeverTogglesPauseAndResume(GameTestHelper context) {
        BlockPos lever = placeLever(context);
        MaceratorBlockEntity macerator = loadedMacerator(context, MACHINE);
        macerator.setRedstoneMode(RedstoneMode.HIGH);
        IEnergyStorage energy = macerator.energyStorage(null);
        long charged = energy.getAmount();
        float[] frozenProgress = new float[1];
        long[] frozenEnergy = new long[1];

        context.startSequence()
                .thenExecuteAfter(HOLD_TICKS, () -> {
                    context.assertTrue(macerator.processProgress() == 0f, "Lever off: macerator should not start");
                    context.assertTrue(energy.getAmount() == charged, "Lever off: no RF should be spent");
                })
                .thenExecute(() -> context.pullLever(lever))
                .thenWaitUntil(() -> context.assertTrue(
                        macerator.processProgress() > 0f, "Lever on: macerator should start grinding"))
                .thenExecute(() -> {
                    context.pullLever(lever);
                    frozenProgress[0] = macerator.processProgress();
                    frozenEnergy[0] = energy.getAmount();
                })
                .thenExecuteAfter(HOLD_TICKS, () -> {
                    context.assertTrue(macerator.processProgress() == frozenProgress[0],
                            "Lever off again: progress should hold at " + frozenProgress[0] + ", got: "
                                    + macerator.processProgress());
                    context.assertTrue(energy.getAmount() == frozenEnergy[0],
                            "Lever off again: no RF should be spent");
                    context.assertFalse(maceratorFinished(macerator), "Lever off again: run should not finish");
                })
                .thenExecute(() -> context.pullLever(lever))
                .thenWaitUntil(() -> context.assertTrue(
                        maceratorFinished(macerator), "Lever on again: macerator should finish"))
                .thenSucceed();
    }

    // ==================== Other processors ====================

    /** The Sequential Fabricator runs its own processor; HIGH holds it until a redstone block is placed. */
    public static void testFabricatorHighPausesUntilSignalled(GameTestHelper context) {
        BlockPos chestPos = MACHINE.below();
        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(MACHINE, LogisticsAutomation.BLOCK.SEQUENTIAL_FABRICATOR);
        SequentialFabricatorBlockEntity fabricator =
                (SequentialFabricatorBlockEntity) context.getBlockEntity(MACHINE);
        if (fabricator == null) {
            context.fail("Expected a Sequential Fabricator block entity");
            return;
        }
        fabricator.toggleSelection(ResourceId.in(LogisticsMod.MOD_ID, "fabricator/redstone_chipset"));
        fabricator.setItem(0, new ItemStack(Items.REDSTONE));
        chargeFully(fabricator.energyStorage(null));
        fabricator.setRedstoneMode(RedstoneMode.HIGH);

        heldUntilReleased(context, fabricator, () -> {
                    BaseContainerBlockEntity chest = (BaseContainerBlockEntity) context.getBlockEntity(chestPos);
                    return chest != null && chest.countItem(LogisticsCore.ITEM.REDSTONE_CHIPSET) > 0;
                },
                () -> context.setBlock(ABOVE, Blocks.REDSTONE_BLOCK));
    }

    /** The Transposer (a fluid recipe) holds a bucket fill under HIGH until a redstone block is placed. */
    public static void testTransposerHighPausesUntilSignalled(GameTestHelper context) {
        context.setBlock(MACHINE, LogisticsAutomation.BLOCK.TRANSPOSER);
        TransposerBlockEntity transposer = (TransposerBlockEntity) context.getBlockEntity(MACHINE);
        if (transposer == null) {
            context.fail("Expected a Transposer block entity");
            return;
        }
        transposer.tank().insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(1_000), false);
        transposer.setItem(INPUT_SLOT, new ItemStack(Items.BUCKET));
        chargeFully(transposer.energyStorage(null));
        transposer.setRedstoneMode(RedstoneMode.HIGH);

        heldUntilReleased(context, transposer, () -> transposer.getItem(OUTPUT_SLOT).is(Items.LAVA_BUCKET),
                () -> context.setBlock(ABOVE, Blocks.REDSTONE_BLOCK));
    }

    // ==================== Persistence ====================

    /** The saved {@code redstone} component's {@code Mode}, or {@code null} when nothing is saved. */
    private static String savedMode(CompoundTag saved) {
        CompoundTag data = NbtCompat.getCompoundOrEmpty(saved, "LogisticsData");
        CompoundTag components = NbtCompat.getCompoundOrEmpty(data, "components");
        CompoundTag redstone = NbtCompat.getCompoundOrEmpty(components, "redstone");
        String mode = NbtCompat.getString(redstone, "Mode", "");
        return mode.isEmpty() ? null : mode;
    }

    private static MaceratorBlockEntity rebuildMacerator(GameTestHelper context) {
        context.setBlock(MACHINE, Blocks.AIR);
        context.setBlock(MACHINE, LogisticsAutomation.BLOCK.MACERATOR);
        MaceratorBlockEntity rebuilt = (MaceratorBlockEntity) context.getBlockEntity(MACHINE);
        if (rebuilt == null) {
            throw new GameTestAssertException("Expected a fresh macerator block entity");
        }
        return rebuilt;
    }

    /**
     * A non-default mode survives save and reload; the default mode writes no entry, and loading such a
     * save resets a machine that had been set otherwise.
     */
    public static void testModePersistsAndDefaultIsNotSaved(GameTestHelper context) {
        HolderLookup.Provider registries = context.getLevel().registryAccess();
        context.setBlock(MACHINE, LogisticsAutomation.BLOCK.MACERATOR);
        MaceratorBlockEntity original = (MaceratorBlockEntity) context.getBlockEntity(MACHINE);
        if (original == null) {
            context.fail("Expected a macerator block entity");
            return;
        }
        original.setRedstoneMode(RedstoneMode.LOW);
        CompoundTag lowSave = original.saveCustomOnly(registries);
        if (!"low".equals(savedMode(lowSave))) {
            context.fail("LOW should be saved as the redstone Mode, got: " + savedMode(lowSave));
            return;
        }

        MaceratorBlockEntity rebuilt = rebuildMacerator(context);
        rebuilt.loadCustomOnly(lowSave, registries);
        if (rebuilt.redstoneControl().mode() != RedstoneMode.LOW) {
            context.fail("LOW should survive a reload, got: " + rebuilt.redstoneControl().mode());
            return;
        }

        rebuilt.setRedstoneMode(RedstoneMode.IGNORED);
        CompoundTag ignoredSave = rebuilt.saveCustomOnly(registries);
        if (savedMode(ignoredSave) != null) {
            context.fail("The default mode should write no redstone Mode, got: " + savedMode(ignoredSave));
            return;
        }

        MaceratorBlockEntity reloaded = rebuildMacerator(context);
        reloaded.setRedstoneMode(RedstoneMode.LOW);
        reloaded.loadCustomOnly(ignoredSave, registries);
        if (reloaded.redstoneControl().mode() != RedstoneMode.IGNORED) {
            context.fail("Loading a default-mode save should reset the mode to IGNORED, got: "
                    + reloaded.redstoneControl().mode());
            return;
        }
        context.succeed();
    }
}
