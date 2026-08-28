package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared glass tank bucket GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each loader's
 * own registration mechanism: Fabric's {@code @GameTest}-annotated {@code GlassTankBucketGameTest}
 * delegates to these methods, and NeoForge's {@code GlassTankBucketGameTestRegistration} references
 * them directly as {@code Consumer<GameTestHelper>} method references.
 *
 * <p>Right-clicking a Glass Tank with an empty bucket drains it into a filled bucket (see
 * {@code GlassTankBlock}).
 */
public class GlassTankBucketGameTestBody {

    /**
     * A {@link ServerPlayer} in the given game mode that is never placed into the player list.
     *
     * <p>MC 26.2 offers {@code GameTestHelper.makeMockServerPlayer(GameType)} for exactly this; 26.1
     * only has {@code makeMockServerPlayerInLevel()}, which runs a real {@code placeNewPlayer} login.
     * That login fires the loaders' join handlers, and NeoForge then refuses to send this mod's
     * payloads over the test's embedded channel ("may not be sent to the client!"), failing the test.
     * So mirror 26.2's construction directly. {@code setGameMode} is unusable here — it writes to the
     * absent connection — hence overriding {@code gameMode()} and applying abilities by hand.
     */
    private static ServerPlayer mockServerPlayer(GameTestHelper context, GameType gameType) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = new ServerPlayer(
                level.getServer(),
                level,
                new GameProfile(UUID.randomUUID(), "test-mock-player"),
                ClientInformation.createDefault()) {
            @Override
            public GameType gameMode() {
                return gameType;
            }
        };
        gameType.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    private static GlassTankBlockEntity fullTank(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.GLASS_TANK);
        GlassTankBlockEntity tank = context.getBlockEntity(pos, GlassTankBlockEntity.class);
        tank.tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(1000));
        return tank;
    }

    /** Survival: the held bucket itself becomes the filled bucket, and the tank empties. */
    public static void emptyBucketDrainsGlassTankInSurvival(GameTestHelper context) {
        BlockPos tankPos = new BlockPos(0, 1, 0);
        GlassTankBlockEntity tank = fullTank(context, tankPos);

        ServerPlayer player = mockServerPlayer(context, GameType.SURVIVAL);
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
     * Creative: the held container item is left alone and the tank still empties, regardless of
     * loader. Does NOT call {@code context.succeed()} and returns the mock player — the loader
     * wrappers finish the test themselves, because what happens to the drained fluid beyond that
     * point is genuinely loader-specific (see below).
     *
     * <p>Fabric's {@code FluidStorageUtil} additionally grants a copy of the filled container
     * elsewhere in the inventory (so testers don't burn through their held stack); NeoForge's own
     * {@code FluidUtil.interactWithFluidHandler} does not replicate that nicety. That's an
     * independently-maintained third-party helper on each loader, not this mod's own contract, so
     * only Fabric's wrapper asserts it.
     */
    public static Player emptyBucketDrainsGlassTankInCreative(GameTestHelper context) {
        BlockPos tankPos = new BlockPos(0, 1, 0);
        GlassTankBlockEntity tank = fullTank(context, tankPos);

        ServerPlayer player = mockServerPlayer(context, GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));

        context.useBlock(tankPos, player);

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.is(Items.BUCKET) || held.getCount() != 1) {
            throw context.assertionException("Expected the held item to remain a single empty bucket, got " + held);
        }
        if (tank.tank().getAmount() != 0) {
            throw context.assertionException("Expected tank to be drained, still has " + tank.tank().getAmount());
        }
        return player;
    }
}
