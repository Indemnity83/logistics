package com.logistics.gametest.core;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMachine;
import com.logistics.forestry.farm.FarmMachineBlock;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Upgrade Kit GameTest bodies, compiled into both loaders' {@code gametest} source sets.
 * Fabric's {@code UpgradeKitGameTest} delegates to these methods and NeoForge's
 * {@code UpgradeKitGameTestRegistration} references them as {@code Consumer<GameTestHelper>}.
 *
 * <p>Each test right-clicks a Crop Farm with a kit through the normal block-use path, so the farm
 * block has to let the kit through instead of opening its menu.
 */
public class UpgradeKitGameTestBody {

    private static final BlockPos FARM = new BlockPos(1, 2, 1);
    private static final long STORED_RF = 50;

    @Nullable
    private static FarmBlockEntity placeFarm(GameTestHelper context) {
        context.setBlock(FARM.below(), Blocks.STONE);
        context.setBlock(FARM, LogisticsForestry.BLOCK.CROP_FARM.defaultBlockState()
                .setValue(FarmMachineBlock.FACING, Direction.EAST));
        FarmBlockEntity farm = context.getBlockEntity(FARM, FarmBlockEntity.class);
        if (farm == null) {
            context.fail("Expected the farm block entity");
        }
        return farm;
    }

    private static ServerPlayer playerHolding(GameTestHelper context, Item kit, int count) {
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(kit, count));
        return player;
    }

    private static List<ItemStack> contents(FarmBlockEntity farm) {
        return IntStream.range(0, FarmMachine.SLOT_COUNT)
                .mapToObj(slot -> farm.getItem(slot).copy())
                .toList();
    }

    private static boolean sameContents(List<ItemStack> a, List<ItemStack> b) {
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return a.size() == b.size();
    }

    /** An Industrial kit raises a Basic farm, keeping its items and energy, and one kit is used. */
    public static void testIndustrialKitUpgradesBasicFarm(GameTestHelper context) {
        FarmBlockEntity farm = placeFarm(context);
        if (farm == null) {
            return;
        }
        farm.setItem(FarmMachine.GERMLING_START, new ItemStack(Items.WHEAT_SEEDS, 5));
        farm.setItem(FarmMachine.SOIL_START, new ItemStack(Items.DIRT, 7));
        farm.setItem(FarmMachine.OUTPUT_START, new ItemStack(Items.WHEAT, 3));
        farm.energyStorage(null).insert(STORED_RF, false);
        long energy = farm.energyStorage(null).getAmount();
        List<ItemStack> before = contents(farm);
        if (energy != STORED_RF) {
            context.fail("The farm should hold " + STORED_RF + " RF before the upgrade, holds " + energy);
            return;
        }

        ServerPlayer player = playerHolding(context, LogisticsCore.ITEM.INDUSTRIAL_UPGRADE_KIT, 2);
        context.useBlock(FARM, player);

        if (farm.tier() != MachineTier.INDUSTRIAL) {
            context.fail("The farm should be Industrial, is " + farm.tier());
            return;
        }
        if (farm.energyStorage(null).getAmount() != energy) {
            context.fail("The upgrade should keep " + energy + " RF, the farm holds "
                    + farm.energyStorage(null).getAmount());
            return;
        }
        if (!sameContents(before, contents(farm))) {
            context.fail("The upgrade should keep the farm's inventory, it holds " + contents(farm));
            return;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.is(LogisticsCore.ITEM.INDUSTRIAL_UPGRADE_KIT) || held.getCount() != 1) {
            context.fail("One kit should be used, the player holds " + held);
            return;
        }
        context.succeed();
    }

    /** A Precision kit does not fit a Basic farm: the farm stays Basic and the kit is kept. */
    public static void testPrecisionKitRefusedOnBasicFarm(GameTestHelper context) {
        FarmBlockEntity farm = placeFarm(context);
        if (farm == null) {
            return;
        }
        ServerPlayer player = playerHolding(context, LogisticsCore.ITEM.PRECISION_UPGRADE_KIT, 1);
        context.useBlock(FARM, player);

        if (farm.tier() != MachineTier.BASIC) {
            context.fail("The farm should stay Basic, is " + farm.tier());
            return;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.is(LogisticsCore.ITEM.PRECISION_UPGRADE_KIT) || held.getCount() != 1) {
            context.fail("The refused kit should be kept, the player holds " + held);
            return;
        }
        context.succeed();
    }

    /** A farm upgraded with a kit and then mined drops an item carrying its new tier. */
    public static void testUpgradedFarmDropsItsTier(GameTestHelper context) {
        FarmBlockEntity farm = placeFarm(context);
        if (farm == null) {
            return;
        }
        context.useBlock(FARM, playerHolding(context, LogisticsCore.ITEM.INDUSTRIAL_UPGRADE_KIT, 1));
        if (farm.tier() != MachineTier.INDUSTRIAL) {
            context.fail("The kit should make the farm Industrial, is " + farm.tier());
            return;
        }

        context.getLevel().destroyBlock(context.absolutePos(FARM), true);
        AABB around = new AABB(context.absolutePos(FARM)).inflate(2.0);
        List<ItemEntity> dropped = context.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(item -> item.getItem().is(LogisticsForestry.BLOCK.CROP_FARM.asItem()))
                .toList();
        if (dropped.size() != 1) {
            context.fail("Breaking the farm should drop exactly one farm item, dropped " + dropped.size());
            return;
        }
        MachineTier carried = dropped.get(0).getItem().get(LogisticsCore.DATA.MACHINE_TIER);
        dropped.forEach(ItemEntity::discard);
        if (carried != MachineTier.INDUSTRIAL) {
            context.fail("The dropped farm should carry the Industrial tier, carries " + carried);
            return;
        }
        context.succeed();
    }
}
