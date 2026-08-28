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
     * <p>{@code GameTestHelper.makeMockServerPlayerInLevel()} runs a real {@code placeNewPlayer}
     * login, which fires the loaders' join handlers; NeoForge then refuses to send this mod's
     * payloads over the test's embedded channel ("may not be sent to the client!") and the test
     * fails. Constructing the player directly skips the login entirely.
     *
     * <p>1.21.1 delta: newer versions expose a {@code gameMode()} accessor that can simply be
     * overridden. Here {@code gameMode} is a {@link net.minecraft.server.level.ServerPlayerGameMode}
     * field whose setter is protected, and both {@code ServerPlayer.setGameMode} and
     * {@code ServerPlayerGameMode.changeGameModeForPlayer} route through
     * {@code onUpdateAbilities()}, which writes to the absent connection. So set the abilities by
     * hand and override the two predicates that would otherwise read the unset game mode.
     */
    private static ServerPlayer mockServerPlayer(GameTestHelper context, GameType gameType) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = new ServerPlayer(
                level.getServer(),
                level,
                new GameProfile(UUID.randomUUID(), "test-mock-player"),
                ClientInformation.createDefault()) {
            @Override
            public boolean isCreative() {
                return gameType == GameType.CREATIVE;
            }

            @Override
            public boolean isSpectator() {
                return gameType == GameType.SPECTATOR;
            }
        };
        gameType.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    private static GlassTankBlockEntity fullTank(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.GLASS_TANK);
        GlassTankBlockEntity tank = (GlassTankBlockEntity) context.getBlockEntity(pos);
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
        context.assertTrue(held.is(Items.WATER_BUCKET),
                "Expected a water bucket in hand after clicking the tank, got " + held);
        context.assertTrue(tank.tank().getAmount() == 0,
                "Expected tank to be drained, still has " + tank.tank().getAmount());
        context.succeed();
    }

    /**
     * Creative: the held container item is left alone and the tank still empties, regardless of
     * loader. Does NOT call {@code context.succeed()} and returns the mock player — the loader
     * wrappers finish the test themselves, because what happens to the drained fluid beyond that
     * point is genuinely loader-specific.
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
        context.assertTrue(held.is(Items.BUCKET) && held.getCount() == 1,
                "Expected the held item to remain a single empty bucket, got " + held);
        context.assertTrue(tank.tank().getAmount() == 0,
                "Expected tank to be drained, still has " + tank.tank().getAmount());
        return player;
    }
}
