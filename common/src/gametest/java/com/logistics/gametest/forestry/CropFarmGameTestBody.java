package com.logistics.gametest.forestry;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachine;
import com.logistics.forestry.farm.FarmMachineBlock;
import com.logistics.forestry.farm.FarmSettings;
import com.logistics.forestry.farm.FarmStatus;
import com.logistics.forestry.farm.FarmTierProfile;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Crop Farm GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code CropFarmGameTest} delegates to these methods and NeoForge's
 * {@code CropFarmGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Every test builds the same rig with real blocks: the farm, a Creative Engine held on by a
 * redstone block powering it from the west, and a field of crops on farmland two blocks out from
 * the farm, one level above it, inside its Reach. A hopper under the farm pulls its produce into a
 * chest. Two glowstone lamps above the Reach keep the field bright whatever the sky does.
 */
public class CropFarmGameTestBody {

    private static final BlockPos FARM = new BlockPos(3, 2, 3);
    private static final BlockPos ENGINE = FARM.west();
    private static final BlockPos ENGINE_SIGNAL = ENGINE.south();
    private static final BlockPos OUTPUT_HOPPER = FARM.below();
    private static final BlockPos CHEST = OUTPUT_HOPPER.below();
    /** Checks per second for the Plot test, so a pass over the Reach takes well under a second. */
    private static final int PLOT_CHECKS_PER_SECOND = 400;

    private static final List<BlockPos> LAMPS = List.of(new BlockPos(2, 5, 2), new BlockPos(4, 5, 4));

    /** The field: every position two blocks out from the farm, one above it, so its farmland is level with the farm. */
    private static final List<BlockPos> PLOTS = plots();

    private static List<BlockPos> plots() {
        List<BlockPos> plots = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                    plots.add(FARM.offset(dx, 1, dz));
                }
            }
        }
        return List.copyOf(plots);
    }

    /** Builds the rig, crops on {@code plots}; returns {@code null}, with the test failed, if a block entity is missing. */
    @Nullable
    private static FarmBlockEntity build(GameTestHelper context, List<BlockPos> plots, BlockState crop, boolean power) {
        for (BlockPos plot : plots) {
            context.setBlock(plot.below(), Blocks.FARMLAND);
            context.setBlock(plot, crop);
        }
        LAMPS.forEach(lamp -> context.setBlock(lamp, Blocks.GLOWSTONE));
        context.setBlock(FARM, LogisticsForestry.BLOCK.CROP_FARM.defaultBlockState()
                .setValue(FarmMachineBlock.FACING, Direction.EAST));
        FarmBlockEntity farm = context.getBlockEntity(FARM, FarmBlockEntity.class);
        if (farm == null) {
            context.fail("Expected the farm block entity");
            return null;
        }
        if (power) {
            CreativeEngineBlockEntity engine = placeEngine(context, ENGINE, ENGINE_SIGNAL, Direction.EAST);
            if (engine == null) {
                return null;
            }
            // 20 -> 40 -> 80 -> 160 RF/t, above the farm's input cap.
            engine.cycleOutputLevel();
            engine.cycleOutputLevel();
            engine.cycleOutputLevel();
        }
        return farm;
    }

    @Nullable
    private static CreativeEngineBlockEntity placeEngine(
            GameTestHelper context, BlockPos pos, BlockPos signal, Direction facing) {
        context.setBlock(signal, Blocks.REDSTONE_BLOCK);
        context.setBlock(pos, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, facing)
                .setValue(AbstractEngineBlock.POWERED, true));
        CreativeEngineBlockEntity engine = context.getBlockEntity(pos, CreativeEngineBlockEntity.class);
        if (engine == null) {
            context.fail("Expected a creative engine at " + pos);
        }
        return engine;
    }

    private static BlockState ripe(Block crop) {
        CropBlock block = (CropBlock) crop;
        return block.getStateForAge(block.getMaxAge());
    }

    private static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private static int count(@Nullable Container container, Item item) {
        if (container == null) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * A ripe wheat field is harvested with vanilla's loot and each plot replanted with wheat from its
     * own drops; the hopper under the farm pulls the wheat out into the chest. A harvest that rolls no
     * seed leaves its plot empty, so only most plots need be replanted.
     */
    public static void testHarvestsAndReplantsAndHopperPullsHarvest(GameTestHelper context) {
        context.setBlock(CHEST, Blocks.CHEST);
        context.setBlock(OUTPUT_HOPPER, Blocks.HOPPER);
        FarmBlockEntity farm = build(context, PLOTS, ripe(Blocks.WHEAT), true);
        if (farm == null) {
            return;
        }

        context.succeedWhen(() -> {
            int replanted = 0;
            for (BlockPos plot : PLOTS) {
                BlockState state = context.getBlockState(plot);
                if (isRipe(state)) {
                    throw context.assertionException(plot, "The ripe wheat should have been harvested");
                }
                if (state.is(Blocks.WHEAT)) {
                    replanted++;
                } else if (!state.isAir()) {
                    throw context.assertionException(plot, "A plot should hold wheat or nothing, holds " + state);
                }
            }
            if (replanted < PLOTS.size() / 2) {
                throw context.assertionException(FARM, "Most plots should be replanted, " + replanted + " of "
                        + PLOTS.size() + " are");
            }
            int pulled = count(context.getBlockEntity(CHEST, ChestBlockEntity.class), Items.WHEAT);
            if (pulled < PLOTS.size()) {
                throw context.assertionException(CHEST, "The hopper should pull all " + PLOTS.size()
                        + " wheat into the chest, " + pulled + " so far");
            }
        });
    }

    /**
     * A farm whose inventory is full keeps harvesting: the ripe carrots' drops land at their plot,
     * and the plot is replanted from them all the same.
     */
    public static void testFullFarmSpillsAtThePlot(GameTestHelper context) {
        BlockPos plot = PLOTS.get(0);
        FarmBlockEntity farm = build(context, List.of(plot), ripe(Blocks.CARROTS), true);
        if (farm == null) {
            return;
        }
        for (int slot = 0; slot < FarmMachine.SLOT_COUNT; slot++) {
            farm.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        Vec3 drop = Vec3.atCenterOf(context.absolutePos(plot));
        boolean[] spilled = {false};

        context.onEachTick(() -> {
            if (spilled[0]) {
                return;
            }
            AABB everywhere = new AABB(context.absolutePos(BlockPos.ZERO)).inflate(16.0);
            for (ItemEntity item : context.getLevel().getEntitiesOfClass(ItemEntity.class, everywhere)) {
                if (!item.getItem().is(Items.CARROT)) {
                    continue;
                }
                // Loose items drift, so they are checked the tick they first appear.
                if (item.position().distanceTo(drop) > 1.0) {
                    throw context.assertionException(plot, "The carrots should drop at their plot, landed at "
                            + item.position() + ", " + item.position().distanceTo(drop) + " away");
                }
                spilled[0] = true;
            }
        });
        context.succeedWhen(() -> {
            if (!spilled[0]) {
                throw context.assertionException(plot, "The full farm should drop the carrots at the plot");
            }
            BlockState state = context.getBlockState(plot);
            if (!state.is(Blocks.CARROTS) || isRipe(state)) {
                throw context.assertionException(plot, "The plot should be replanted with carrots, holds " + state);
            }
            if (farm.status() != FarmStatus.INVENTORY_FULL) {
                throw context.assertionException(FARM, "The farm should report Inventory full, reports " + farm.status());
            }
        });
    }

    /**
     * A harvested position stays a Plot: wheat taken off it is replanted from the inventory. Breaking
     * the farm and placing it again forgets its Plots, so the same bare farmland is then left alone.
     */
    public static void testBreakingTheFarmForgetsItsPlots(GameTestHelper context) {
        BlockPos plot = PLOTS.get(0);
        FarmBlockEntity farm = build(context, List.of(plot), ripe(Blocks.WHEAT), true);
        if (farm == null) {
            return;
        }
        context.setBlock(FARM.below(), Blocks.STONE);
        farm.setRateOverride(PLOT_CHECKS_PER_SECOND, 20);
        farm.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 16));
        FarmBlockEntity[] replaced = new FarmBlockEntity[1];

        context.startSequence()
                .thenWaitUntil(() -> assertGrowingWheat(context, plot, "The ripe wheat should be harvested and replanted"))
                .thenExecute(() -> context.setBlock(plot, Blocks.AIR))
                .thenWaitUntil(() -> assertGrowingWheat(context, plot, "The Plot should be replanted from the inventory"))
                .thenExecute(() -> {
                    replaced[0] = breakAndReplace(context);
                    replaced[0].setRateOverride(PLOT_CHECKS_PER_SECOND, 20);
                    replaced[0].setItem(0, new ItemStack(Items.WHEAT_SEEDS, 16));
                    context.setBlock(plot, Blocks.AIR);
                })
                .thenExecuteAfter(200, () -> {
                    if (!context.getBlockState(plot).isAir()) {
                        throw context.assertionException(plot, "A replaced farm should not remember the Plot, holds "
                                + context.getBlockState(plot));
                    }
                    if (count(replaced[0], Items.WHEAT_SEEDS) != 16) {
                        throw context.assertionException(FARM, "The replaced farm should plant none of its seeds");
                    }
                })
                .thenSucceed();
    }

    private static void assertGrowingWheat(GameTestHelper context, BlockPos plot, String message) {
        BlockState state = context.getBlockState(plot);
        if (!state.is(Blocks.WHEAT) || isRipe(state)) {
            throw context.assertionException(plot, message + ", holds " + state);
        }
    }

    /** Mines the farm and places the item it dropped back where it stood; returns the new farm. */
    private static FarmBlockEntity breakAndReplace(GameTestHelper context) {
        context.getLevel().destroyBlock(context.absolutePos(FARM), true);
        AABB around = new AABB(context.absolutePos(FARM)).inflate(2.0);
        ItemEntity dropped = context.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(item -> item.getItem().is(LogisticsForestry.BLOCK.CROP_FARM.asItem()))
                .findFirst()
                .orElseThrow(() -> context.assertionException(FARM, "Breaking the farm should drop it"));
        ItemStack item = dropped.getItem().copy();
        dropped.discard();
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        context.placeAt(player, item, FARM.below(2), Direction.UP);
        FarmBlockEntity replaced = context.getBlockEntity(FARM, FarmBlockEntity.class);
        if (replaced == null) {
            throw context.assertionException(FARM, "Placing the dropped item should put a farm back");
        }
        return replaced;
    }

    /** Without power the farm harvests nothing, and reports No power. */
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        FarmBlockEntity farm = build(context, PLOTS, ripe(Blocks.WHEAT), false);
        if (farm == null) {
            return;
        }

        context.runAfterDelay(300, () -> {
            for (BlockPos plot : PLOTS) {
                if (!isRipe(context.getBlockState(plot))) {
                    throw context.assertionException(plot, "Nothing should be harvested without power");
                }
            }
            if (farm.status() != FarmStatus.NO_POWER) {
                throw context.assertionException(FARM, "Farm should report No power, reports " + farm.status());
            }
            context.succeed();
        });
    }

    /** A farm set to run only without a redstone signal stops while one reaches it, and reports Paused. */
    public static void testRedstoneSignalPausesFarm(GameTestHelper context) {
        FarmBlockEntity farm = build(context, PLOTS, ripe(Blocks.WHEAT), true);
        if (farm == null) {
            return;
        }
        farm.setRedstoneMode(RedstoneMode.LOW);
        context.setBlock(FARM.north(), Blocks.REDSTONE_BLOCK);

        context.runAfterDelay(300, () -> {
            for (BlockPos plot : PLOTS) {
                if (!isRipe(context.getBlockState(plot))) {
                    throw context.assertionException(plot, "A paused farm should harvest nothing");
                }
            }
            if (farm.status() != FarmStatus.PAUSED) {
                throw context.assertionException(FARM, "Farm should report Paused, reports " + farm.status());
            }
            context.succeed();
        });
    }

    /**
     * An engine on top of the farm, switched on by a lever on the engine, powers the farm without
     * relaying the lever's signal into it: a farm set to run only without a signal is not paused,
     * and harvests its field.
     */
    public static void testLeverOnEngineDoesNotPauseFarm(GameTestHelper context) {
        FarmBlockEntity farm = build(context, PLOTS, ripe(Blocks.WHEAT), false);
        if (farm == null) {
            return;
        }
        farm.setRedstoneMode(RedstoneMode.LOW);
        BlockPos engineOnTop = FARM.above();
        BlockPos lever = engineOnTop.above();
        context.setBlock(engineOnTop, LogisticsPower.BLOCK.CREATIVE_ENGINE.defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.DOWN));
        CreativeEngineBlockEntity engine = context.getBlockEntity(engineOnTop, CreativeEngineBlockEntity.class);
        if (engine == null) {
            context.fail("Expected a creative engine on top of the farm");
            return;
        }
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        context.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
        context.pullLever(lever);

        context.succeedWhen(() -> {
            if (!context.getBlockState(engineOnTop).getValue(AbstractEngineBlock.POWERED)) {
                throw context.assertionException(engineOnTop, "The lever should switch the engine on");
            }
            if (farm.status() == FarmStatus.PAUSED) {
                throw context.assertionException(FARM, "A lever on the engine should not pause the farm");
            }
            for (BlockPos plot : PLOTS) {
                if (isRipe(context.getBlockState(plot))) {
                    throw context.assertionException(plot, "The ripe wheat should have been harvested");
                }
            }
        });
    }

    // ==================== Tiers ====================

    /**
     * A farm raised to Industrial and mined drops an item carrying its tier; placing that item again
     * gives an Industrial farm, with the Industrial buffer.
     */
    public static void testTierSurvivesBreakAndReplace(GameTestHelper context) {
        context.setBlock(FARM.below(), Blocks.STONE);
        context.setBlock(FARM, LogisticsForestry.BLOCK.CROP_FARM.defaultBlockState()
                .setValue(FarmMachineBlock.FACING, Direction.EAST));
        FarmBlockEntity placed = context.getBlockEntity(FARM, FarmBlockEntity.class);
        if (placed == null) {
            context.fail("Expected the farm block entity");
            return;
        }
        placed.tierComponent().setTier(MachineTier.INDUSTRIAL);

        context.getLevel().destroyBlock(context.absolutePos(FARM), true);
        AABB around = new AABB(context.absolutePos(FARM)).inflate(2.0);
        List<ItemEntity> dropped = context.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(item -> item.getItem().is(LogisticsForestry.BLOCK.CROP_FARM.asItem()))
                .toList();
        if (dropped.size() != 1) {
            context.fail("Breaking the farm should drop exactly one farm item, dropped " + dropped.size());
            return;
        }
        ItemStack item = dropped.get(0).getItem().copy();
        dropped.get(0).discard();
        if (item.get(LogisticsCore.DATA.MACHINE_TIER) != MachineTier.INDUSTRIAL) {
            context.fail("The dropped farm should carry its tier, carries " + item.get(LogisticsCore.DATA.MACHINE_TIER));
            return;
        }

        // Clicking the top of the stone below, item in hand, places the farm back where it was.
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        context.placeAt(player, item, FARM.below(2), Direction.UP);
        FarmBlockEntity replaced = context.getBlockEntity(FARM, FarmBlockEntity.class);
        if (replaced == null) {
            context.fail("Placing the dropped item should put a farm back");
            return;
        }
        FarmTierProfile industrial = FarmSettings.fromConfig().profile(MachineTier.INDUSTRIAL);
        if (replaced.tier() != MachineTier.INDUSTRIAL) {
            context.fail("The replaced farm should be Industrial, is " + replaced.tier());
            return;
        }
        if (replaced.energyStorage(null).getCapacity() != industrial.energyCapacity()) {
            context.fail("The replaced farm should have the Industrial buffer, has "
                    + replaced.energyStorage(null).getCapacity() + " RF");
            return;
        }
        context.succeed();
    }

    /**
     * Raising a powered farm's tier grows its energy buffer: the engine keeps filling it past the
     * Basic capacity, at the Industrial input rate.
     */
    public static void testRaisingTierGrowsEnergyBuffer(GameTestHelper context) {
        FarmBlockEntity farm = build(context, List.of(), ripe(Blocks.WHEAT), true);
        if (farm == null) {
            return;
        }
        FarmSettings settings = FarmSettings.fromConfig();
        FarmTierProfile basic = settings.profile(MachineTier.BASIC);
        FarmTierProfile industrial = settings.profile(MachineTier.INDUSTRIAL);
        long basicCapacity = farm.energyStorage(null).getCapacity();
        if (basicCapacity != basic.energyCapacity()) {
            context.fail("A new farm should have the Basic buffer, has " + basicCapacity + " RF");
            return;
        }
        farm.tierComponent().setTier(MachineTier.INDUSTRIAL);
        context.succeedWhen(() -> {
            long capacity = farm.energyStorage(null).getCapacity();
            if (capacity != industrial.energyCapacity()) {
                throw context.assertionException(FARM, "The buffer should hold " + industrial.energyCapacity()
                        + " RF, holds " + capacity);
            }
            long stored = farm.energyStorage(null).getAmount();
            if (stored <= basic.energyCapacity()) {
                throw context.assertionException(FARM, "The engine should fill the buffer past the Basic "
                        + basic.energyCapacity() + " RF, has " + stored);
            }
        });
    }
}
