package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

/** Right-clicking a Glass Tank with an empty bucket drains it into a filled bucket (see {@code GlassTankBlock}). */
public class GlassTankBucketGameTest {

    private static GlassTankBlockEntity fullTank(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.GLASS_TANK);
        GlassTankBlockEntity tank = context.getBlockEntity(pos, GlassTankBlockEntity.class);
        tank.tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(1000));
        return tank;
    }

    /** Survival: the held bucket itself becomes the filled bucket, and the tank empties. */
    @GameTest
    public void emptyBucketDrainsGlassTankInSurvival(GameTestHelper context) {
        BlockPos tankPos = new BlockPos(0, 1, 0);
        GlassTankBlockEntity tank = fullTank(context, tankPos);

        Player player = context.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));

        context.useBlock(tankPos, player);

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.is(Items.WATER_BUCKET)) {
            throw context.assertionException("Expected a water bucket in hand after clicking the tank, got " + held);
        }
        if (tank.tank().getAmount() != 0) {
            throw context.assertionException("Expected tank to be drained, still has " + tank.tank().getAmount());
        }
        context.succeed();
    }

    /**
     * Creative: Fabric's {@code FluidStorageUtil} deliberately leaves the held container item alone and
     * grants a copy elsewhere in the inventory instead (so testers don't burn through their held stack).
     * The tank still empties either way.
     */
    @GameTest
    public void emptyBucketDrainsGlassTankInCreative(GameTestHelper context) {
        BlockPos tankPos = new BlockPos(0, 1, 0);
        GlassTankBlockEntity tank = fullTank(context, tankPos);

        Player player = context.makeMockServerPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));

        context.useBlock(tankPos, player);

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.is(Items.BUCKET) || held.getCount() != 1) {
            throw context.assertionException("Expected the held item to remain a single empty bucket, got " + held);
        }
        if (tank.tank().getAmount() != 0) {
            throw context.assertionException("Expected tank to be drained, still has " + tank.tank().getAmount());
        }
        if (!player.getInventory().contains(new ItemStack(Items.WATER_BUCKET))) {
            throw context.assertionException("Expected a water bucket to land in the creative player's inventory");
        }
        context.succeed();
    }
}
