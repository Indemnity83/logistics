package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

/** Right-clicking a Glass Tank with an empty bucket drains it into a filled bucket (see {@code GlassTankBlock}). */
public class GlassTankBucketGameTest {

    private static GlassTankBlockEntity fullTank(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.GLASS_TANK);
        GlassTankBlockEntity tank = (GlassTankBlockEntity) context.getBlockEntity(pos);
        tank.tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(1000));
        return tank;
    }

    /** Survival: the held bucket itself becomes the filled bucket, and the tank empties. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void emptyBucketDrainsGlassTankInSurvival(GameTestHelper context) {
        BlockPos tankPos = new BlockPos(0, 1, 0);
        GlassTankBlockEntity tank = fullTank(context, tankPos);

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));

        context.useBlock(tankPos, player);

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        context.assertTrue(held.is(Items.WATER_BUCKET),
                "Expected a water bucket in hand after clicking the tank, got " + held);
        context.assertTrue(tank.tank().getAmount() == 0,
                "Expected tank to be drained, still has " + tank.tank().getAmount());
        context.succeed();
    }

    /**
     * Creative: Fabric's {@code FluidStorageUtil} deliberately leaves the held container item alone and
     * grants a copy elsewhere in the inventory instead (so testers don't burn through their held stack).
     * The tank still empties either way.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void emptyBucketDrainsGlassTankInCreative(GameTestHelper context) {
        BlockPos tankPos = new BlockPos(0, 1, 0);
        GlassTankBlockEntity tank = fullTank(context, tankPos);

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));

        context.useBlock(tankPos, player);

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        context.assertTrue(held.is(Items.BUCKET) && held.getCount() == 1,
                "Expected the held item to remain a single empty bucket, got " + held);
        context.assertTrue(tank.tank().getAmount() == 0,
                "Expected tank to be drained, still has " + tank.tank().getAmount());
        context.assertTrue(player.getInventory().contains(new ItemStack(Items.WATER_BUCKET)),
                "Expected a water bucket to land in the creative player's inventory");
        context.succeed();
    }
}
