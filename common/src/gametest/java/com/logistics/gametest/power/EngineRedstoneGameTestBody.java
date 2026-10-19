package com.logistics.gametest.power;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.power.engine.block.entity.FuelEngineBlockEntity;
import com.logistics.power.engine.block.entity.MagmaticEngineBlockEntity;
import com.logistics.power.engine.block.entity.ReactionEngineBlockEntity;
import com.logistics.power.engine.block.entity.SteamEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.power.engine.steam.SteamFireboxState;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Shared engine redstone-mode GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Fabric's {@code @GameTest}-annotated {@code
 * EngineRedstoneGameTest} delegates to these methods, and NeoForge's {@code
 * EngineRedstoneGameTestRegistration} references them directly as {@code Consumer<GameTestHelper>}
 * method references.
 *
 * <p>The default HIGH mode (run only while signalled) is covered by each engine's own tests; these
 * cover IGNORED and LOW. The signal is a real redstone block placed directly beside the engine.
 */
public class EngineRedstoneGameTestBody {

    private static final BlockPos ENGINE = new BlockPos(1, 1, 1);
    private static final BlockPos ABOVE = ENGINE.above();
    private static final int HOLD_TICKS = 20;

    private static <T extends EngineEntity> T placeUnpowered(GameTestHelper context, Block block, Class<T> type) {
        context.setBlock(ENGINE, block.defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, false));
        T engine = context.getBlockEntity(ENGINE, type);
        if (engine == null) {
            throw context.assertionException("Expected a " + type.getSimpleName() + " at " + ENGINE);
        }
        return engine;
    }

    /**
     * At IGNORED an unsignalled, fuelled engine runs. Switched to LOW, a redstone block placed beside it
     * stops it: {@code running} goes false and its fuel {@code reserve} is no longer spent.
     */
    private static <T extends EngineEntity> void runsIgnoredStopsLowWhenSignalled(
            GameTestHelper context,
            Block block,
            Class<T> type,
            Consumer<T> fuel,
            Predicate<T> running,
            ToLongFunction<T> reserve) {
        T engine = placeUnpowered(context, block, type);
        engine.setRedstoneMode(RedstoneMode.IGNORED);
        fuel.accept(engine);
        long[] heldReserve = new long[1];

        context.startSequence()
                .thenWaitUntil(() -> context.assertTrue(running.test(engine), "IGNORED engine should run unpowered"))
                .thenExecute(() -> {
                    context.assertFalse(engine.isPowered(), "No signal source should be present yet");
                    engine.setRedstoneMode(RedstoneMode.LOW);
                    context.assertTrue(engine.isEnabled(), "LOW with no signal should still be enabled");
                    context.setBlock(ABOVE, Blocks.REDSTONE_BLOCK);
                })
                .thenWaitUntil(() -> context.assertTrue(engine.isPowered(), "Redstone block should power the engine"))
                .thenWaitUntil(() -> context.assertFalse(running.test(engine), "Signalled LOW engine should stop"))
                .thenExecute(() -> heldReserve[0] = reserve.applyAsLong(engine))
                .thenExecuteAfter(HOLD_TICKS, () -> {
                    context.assertFalse(engine.isEnabled(), "Signalled LOW engine should be disabled");
                    context.assertFalse(engine.isRunning(), "Signalled LOW engine should not report running");
                    context.assertFalse(running.test(engine), "Signalled LOW engine should stay stopped");
                    context.assertTrue(reserve.applyAsLong(engine) == heldReserve[0],
                            "A stopped engine should not spend fuel, reserve " + heldReserve[0] + " -> "
                                    + reserve.applyAsLong(engine));
                })
                .thenSucceed();
    }

    private static Fluid modFluid(String name) {
        return BuiltInRegistries.FLUID.getValue(LogisticsCore.resource(name).toIdentifier());
    }

    public static void testStirlingEngineIgnoredAndLow(GameTestHelper context) {
        runsIgnoredStopsLowWhenSignalled(context, LogisticsPower.BLOCK.STIRLING_ENGINE, StirlingEngineBlockEntity.class,
                e -> e.setTheItem(new ItemStack(Items.COAL, 4)),
                e -> e.getBurnTime() > 0,
                e -> e.getTheItem().getCount());
    }

    public static void testFuelEngineIgnoredAndLow(GameTestHelper context) {
        runsIgnoredStopsLowWhenSignalled(context, LogisticsPower.BLOCK.FUEL_ENGINE, FuelEngineBlockEntity.class,
                e -> {
                    e.fuelTank().tank().setContents(SimpleFluidKey.of(modFluid("crude_oil")), FluidUnits.mb(500));
                    e.coolantTank().tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(500));
                },
                e -> e.simulation().lastGenerationRate() > 0,
                e -> e.simulation().committedFuelEnergy() + e.fuelTank().tank().getAmount());
    }

    public static void testMagmaticEngineIgnoredAndLow(GameTestHelper context) {
        runsIgnoredStopsLowWhenSignalled(context, LogisticsPower.BLOCK.MAGMATIC_ENGINE, MagmaticEngineBlockEntity.class,
                e -> e.fluidStorage(Direction.UP).insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(2000), false),
                e -> e.simulation().isRunning(),
                e -> e.simulation().remainingBurnTicks() + e.lavaTank().tank().getAmount());
    }

    public static void testSteamEngineIgnoredAndLow(GameTestHelper context) {
        runsIgnoredStopsLowWhenSignalled(context, LogisticsPower.BLOCK.STEAM_ENGINE, SteamEngineBlockEntity.class,
                e -> {
                    e.setTheItem(new ItemStack(Items.COAL, 16));
                    e.waterTank().tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(4000));
                },
                e -> e.simulation().fireboxState() == SteamFireboxState.FIRING,
                e -> e.simulation().committedBurnTicks() + e.getTheItem().getCount());
    }

    private static void fuelReaction(ReactionEngineBlockEntity engine) {
        engine.reactantTank().tank().setContents(SimpleFluidKey.of(modFluid("liquid_ender")), FluidUnits.mb(1_000));
        engine.setTheItem(new ItemStack(Items.ECHO_SHARD));
    }

    /** At IGNORED an unsignalled Reaction Engine ignites, consuming its catalyst. */
    public static void testReactionEngineIgnitesWhenIgnored(GameTestHelper context) {
        ReactionEngineBlockEntity engine =
                placeUnpowered(context, LogisticsPower.BLOCK.REACTION_ENGINE, ReactionEngineBlockEntity.class);
        engine.setRedstoneMode(RedstoneMode.IGNORED);
        fuelReaction(engine);

        context.succeedWhen(() -> {
            context.assertFalse(engine.isPowered(), "No signal source should be present");
            context.assertTrue(engine.simulation().isReacting(), "IGNORED Reaction Engine should ignite unpowered");
            context.assertTrue(engine.getTheItem().isEmpty(), "Ignition should consume the catalyst");
        });
    }

    /**
     * Redstone only gates a Reaction Engine's ignition, so LOW stops it by refusing to start one while a
     * redstone block sits beside it; removing the block lets it ignite.
     */
    public static void testReactionEngineLowHoldsIgnitionWhileSignalled(GameTestHelper context) {
        ReactionEngineBlockEntity engine =
                placeUnpowered(context, LogisticsPower.BLOCK.REACTION_ENGINE, ReactionEngineBlockEntity.class);
        engine.setRedstoneMode(RedstoneMode.LOW);
        context.setBlock(ABOVE, Blocks.REDSTONE_BLOCK);

        context.startSequence()
                .thenWaitUntil(() -> context.assertTrue(engine.isPowered(), "Redstone block should power the engine"))
                .thenExecute(() -> fuelReaction(engine))
                .thenExecuteAfter(HOLD_TICKS, () -> {
                    context.assertFalse(engine.simulation().isReacting(), "Signalled LOW engine should not ignite");
                    context.assertFalse(engine.getTheItem().isEmpty(), "The catalyst should be untouched");
                })
                .thenExecute(() -> context.setBlock(ABOVE, Blocks.AIR))
                .thenWaitUntil(() -> context.assertTrue(
                        engine.simulation().isReacting(), "Unsignalled LOW engine should ignite"))
                .thenSucceed();
    }

    /** A non-default engine mode survives save and reload; the default HIGH writes no entry. */
    public static void testEngineModePersists(GameTestHelper context) {
        HolderLookup.Provider registries = context.getLevel().registryAccess();
        StirlingEngineBlockEntity original =
                placeUnpowered(context, LogisticsPower.BLOCK.STIRLING_ENGINE, StirlingEngineBlockEntity.class);
        if (original.redstoneControl() == null || original.redstoneControl().mode() != RedstoneMode.HIGH) {
            context.fail("An engine should default to HIGH redstone mode");
            return;
        }
        original.setRedstoneMode(RedstoneMode.LOW);
        CompoundTag saved = original.saveCustomOnly(registries);
        CompoundTag redstone = NbtCompat.getCompoundOrEmpty(
                NbtCompat.getCompoundOrEmpty(NbtCompat.getCompoundOrEmpty(saved, "LogisticsData"), "components"),
                "redstone");
        if (!"low".equals(NbtCompat.getString(redstone, "Mode", ""))) {
            context.fail("LOW should be saved as the engine's redstone Mode, got: " + redstone);
            return;
        }

        context.setBlock(ENGINE, Blocks.AIR);
        StirlingEngineBlockEntity rebuilt =
                placeUnpowered(context, LogisticsPower.BLOCK.STIRLING_ENGINE, StirlingEngineBlockEntity.class);
        rebuilt.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));
        if (rebuilt.redstoneControl().mode() != RedstoneMode.LOW) {
            context.fail("LOW should survive an engine reload, got: " + rebuilt.redstoneControl().mode());
            return;
        }
        context.succeed();
    }
}
