package com.logistics.gametest.core;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.LogisticsPower;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.automation.macerator.MaceratorScreenHandler;
import com.logistics.core.lib.block.behavior.MenuBehavior;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.control.MachineStatus;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.forestry.farm.FarmBlockEntity;
import com.logistics.forestry.farm.FarmMenu;
import com.logistics.power.engine.block.entity.MagmaticEngineBlockEntity;
import com.logistics.power.engine.ui.MagmaticEngineScreenHandler;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared machine-controls GameTest bodies: the menu's status block, the redstone-mode button, and the
 * energy telemetry it syncs. Fabric's {@code MachineControlsGameTest} delegates to these methods and
 * NeoForge's {@code MachineControlsGameTestRegistration} references them directly.
 *
 * <p>The {@code ...HandlerValidation} tests call {@code menu.clickMenuButton} directly, so they prove the
 * menu's own defense-in-depth checks, not vanilla's packet-level ones. The {@code ...PacketPath} test
 * dispatches real {@link ServerboundContainerButtonClickPacket}s through a connected player's
 * {@code handleContainerButtonClick}, covering vanilla's container-id, spectator and still-valid checks.
 */
public class MachineControlsGameTestBody {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static final List<Supplier<Block>> MENU_BLOCKS = List.of(
            () -> LogisticsAutomation.BLOCK.MACERATOR,
            () -> LogisticsAutomation.BLOCK.KILN,
            () -> LogisticsAutomation.BLOCK.SAWMILL,
            () -> LogisticsAutomation.BLOCK.ALLOY_SMELTER,
            () -> LogisticsAutomation.BLOCK.CRUCIBLE,
            () -> LogisticsAutomation.BLOCK.REFINERY,
            () -> LogisticsAutomation.BLOCK.TRANSPOSER,
            () -> LogisticsAutomation.BLOCK.SEQUENTIAL_FABRICATOR,
            () -> LogisticsPower.BLOCK.STIRLING_ENGINE,
            () -> LogisticsPower.BLOCK.FUEL_ENGINE,
            () -> LogisticsPower.BLOCK.MAGMATIC_ENGINE,
            () -> LogisticsPower.BLOCK.STEAM_ENGINE,
            () -> LogisticsPower.BLOCK.REACTION_ENGINE,
            () -> LogisticsForestry.BLOCK.CROP_FARM,
            () -> LogisticsForestry.BLOCK.ARBORETUM);

    // ==================== Helpers ====================

    private static <T extends BlockEntity> T place(GameTestHelper context, Block block, Class<T> type) {
        context.setBlock(POS, block);
        T entity = context.getBlockEntity(POS, type);
        if (entity == null) {
            throw context.assertionException("Expected a " + type.getSimpleName() + " at " + POS);
        }
        return entity;
    }

    private static AbstractContainerMenu openServerMenu(BlockEntity entity, Player player) {
        return ((MenuBehavior.HasMenu) entity).createMenuProvider().createMenu(1, player.getInventory(), player);
    }

    private static MachineControls controlsOf(GameTestHelper context, AbstractContainerMenu menu) {
        try {
            return (MachineControls) menu.getClass().getMethod("controls").invoke(menu);
        } catch (ReflectiveOperationException e) {
            throw context.assertionException(menu.getClass().getSimpleName() + " has no public controls(): " + e);
        }
    }

    private static RedstoneMode modeOf(MachineStatus status) {
        return status.redstoneControl().mode();
    }

    private static void standBeside(Player player, GameTestHelper context) {
        BlockPos absolute = context.absolutePos(POS);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 1, absolute.getZ() + 0.5);
    }

    /**
     * A player logged in over an embedded connection, like {@code makeMockServerPlayerInLevel} but with a
     * real, changeable game mode (that helper pins the mode to creative, so it can never be a spectator).
     * Callers must {@link #disconnect} it in a {@code finally}.
     */
    private static ServerPlayer connectedPlayer(GameTestHelper context, GameType mode) {
        CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-controls-player"), false);
        ServerPlayer player = new ServerPlayer(
                context.getLevel().getServer(), context.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        context.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(mode);
        standBeside(player, context);
        return player;
    }

    /** Closes the player's menu and logs it out, so it cannot affect neighbouring tests. */
    private static void disconnect(ServerPlayer player) {
        player.closeContainer();
        player.level().getServer().getPlayerList().remove(player);
    }

    /** Mirrors every data slot the server menu sends into {@code client}, truncated to a short as the packet is. */
    private static ContainerSynchronizer mirrorInto(AbstractContainerMenu client) {
        return new ContainerSynchronizer() {
            @Override
            public void sendInitialData(
                    AbstractContainerMenu container, List<ItemStack> slotItems, ItemStack carried, int[] dataSlots) {
                for (int i = 0; i < dataSlots.length; i++) {
                    client.setData(i, (short) dataSlots[i]);
                }
            }

            @Override
            public void sendSlotChange(AbstractContainerMenu container, int slotIndex, ItemStack itemStack) {}

            @Override
            public void sendCarriedChange(AbstractContainerMenu container, ItemStack itemStack) {}

            @Override
            public void sendDataChange(AbstractContainerMenu container, int id, int value) {
                client.setData(id, (short) value);
            }

            @Override
            public RemoteSlot createSlot() {
                return RemoteSlot.PLACEHOLDER;
            }
        };
    }

    // ==================== Every server menu ====================

    /** Each of the 15 server menus, opened through its block entity's real menu provider, carries live controls. */
    public static void testEveryServerMenuCarriesControls(GameTestHelper context) {
        Player player = context.makeMockServerPlayer(GameType.SURVIVAL);
        for (Supplier<Block> block : MENU_BLOCKS) {
            context.setBlock(POS, Blocks.AIR);
            context.setBlock(POS, block.get());
            BlockEntity entity = context.getLevel().getBlockEntity(context.absolutePos(POS));
            String name = block.get().getDescriptionId();
            context.assertTrue(entity instanceof MachineStatus, name + " should expose a MachineStatus");
            context.assertTrue(entity instanceof MenuBehavior.HasMenu, name + " should have a menu");
            MachineStatus status = (MachineStatus) entity;

            AbstractContainerMenu menu = openServerMenu(entity, player);
            MachineControls controls = controlsOf(context, menu);
            context.assertTrue(controls.data().getCount() == MachineControls.COUNT,
                    name + " controls should have " + MachineControls.COUNT + " slots");
            context.assertTrue(controls.supported(), name + " should report a redstone control");
            context.assertTrue(controls.mode() == modeOf(status), name + " controls should read the live mode");
        }
        context.succeed();
    }

    // ==================== Menu handler (defense in depth) ====================

    /**
     * The server menu applies a redstone click itself, and the change reaches the client's copy of the
     * controls on the next {@code broadcastChanges}. Direct calls, so this proves our handler only.
     */
    private static void handlerAppliesModeAndRejectsBadClicks(
            GameTestHelper context,
            BlockEntity entity,
            Function<Inventory, AbstractContainerMenu> clientMenu) {
        MachineStatus status = (MachineStatus) entity;
        Player survivor = context.makeMockServerPlayer(GameType.SURVIVAL);
        Player spectator = context.makeMockServerPlayer(GameType.SPECTATOR);
        standBeside(survivor, context);

        AbstractContainerMenu menu = openServerMenu(entity, survivor);
        AbstractContainerMenu client = clientMenu.apply(survivor.getInventory());
        menu.setSynchronizer(mirrorInto(client));
        MachineControls clientControls = controlsOf(context, client);
        RedstoneMode initial = modeOf(status);
        RedstoneMode target = initial == RedstoneMode.HIGH ? RedstoneMode.LOW : RedstoneMode.HIGH;
        context.assertTrue(clientControls.supported() && clientControls.mode() == initial,
                "The client should start synced to " + initial + ", got " + clientControls.mode());

        context.assertFalse(menu.clickMenuButton(spectator, MachineControls.redstoneButton(target)),
                "A spectator's click should be rejected");
        context.assertFalse(menu.clickMenuButton(survivor, MachineControls.BUTTON_BASE - 1),
                "An id below the reserved range should be rejected");
        context.assertFalse(menu.clickMenuButton(survivor, MachineControls.BUTTON_BASE + MachineControls.BUTTON_RANGE),
                "An id above the reserved range should be rejected");
        context.assertFalse(menu.clickMenuButton(survivor, MachineControls.BUTTON_BASE + 3),
                "An unknown sync id should be rejected");
        context.assertTrue(modeOf(status) == initial, "Rejected clicks should leave the mode at " + initial);

        context.assertTrue(menu.clickMenuButton(survivor, MachineControls.redstoneButton(target)),
                "A survival player's click should be accepted");
        context.assertTrue(modeOf(status) == target, "The click should set the mode to " + target);
        context.assertTrue(clientControls.mode() == initial, "The client should not see the change before a broadcast");
        menu.broadcastChanges();
        context.assertTrue(clientControls.mode() == target,
                "After broadcastChanges the client should read " + target + ", got " + clientControls.mode());
        context.assertTrue(controlsOf(context, menu).mode() == target, "The server controls should read " + target);

        // A click on a menu whose machine is gone changes nothing.
        context.setBlock(POS, Blocks.AIR);
        context.assertTrue(entity.isRemoved(), "Breaking the block should remove its block entity");
        context.assertFalse(menu.clickMenuButton(survivor, MachineControls.redstoneButton(initial)),
                "A click on a removed machine should be rejected");
        context.assertTrue(modeOf(status) == target, "A removed machine's mode should be unchanged");
        context.succeed();
    }

    public static void testMaceratorMenuHandlerValidation(GameTestHelper context) {
        MaceratorBlockEntity macerator = place(context, LogisticsAutomation.BLOCK.MACERATOR, MaceratorBlockEntity.class);
        handlerAppliesModeAndRejectsBadClicks(context, macerator, inv -> new MaceratorScreenHandler(1, inv));
    }

    /** The farm menu hands redstone ids to its controls before its own Start and Show area buttons. */
    public static void testCropFarmMenuHandlerValidation(GameTestHelper context) {
        FarmBlockEntity farm = place(context, LogisticsForestry.BLOCK.CROP_FARM, FarmBlockEntity.class);
        handlerAppliesModeAndRejectsBadClicks(context, farm, inv -> new FarmMenu(1, inv));
    }

    public static void testMagmaticEngineMenuHandlerValidation(GameTestHelper context) {
        MagmaticEngineBlockEntity engine =
                place(context, LogisticsPower.BLOCK.MAGMATIC_ENGINE, MagmaticEngineBlockEntity.class);
        handlerAppliesModeAndRejectsBadClicks(context, engine, inv -> new MagmaticEngineScreenHandler(1, inv));
    }

    // ==================== Packet path ====================

    /**
     * Real button-click packets through a connected player's packet listener: an accepted click applies
     * the mode; a wrong container id, a spectator, and a menu that is no longer valid change nothing.
     */
    public static void testMaceratorRedstoneButtonPacketPath(GameTestHelper context) {
        MaceratorBlockEntity macerator = place(context, LogisticsAutomation.BLOCK.MACERATOR, MaceratorBlockEntity.class);
        PlayerList players = context.getLevel().getServer().getPlayerList();
        int playersBefore = players.getPlayerCount();
        ServerPlayer player = connectedPlayer(context, GameType.SURVIVAL);
        try {
            player.openMenu(macerator.createMenuProvider());
            AbstractContainerMenu menu = player.containerMenu;
            context.assertTrue(menu instanceof MaceratorScreenHandler, "openMenu should open the macerator menu");
            int containerId = menu.containerId;
            context.assertTrue(macerator.redstoneControl().mode() == RedstoneMode.IGNORED, "Macerators default to IGNORED");

            player.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(
                    containerId, MachineControls.redstoneButton(RedstoneMode.HIGH)));
            context.assertTrue(macerator.redstoneControl().mode() == RedstoneMode.HIGH,
                    "An accepted click packet should set HIGH, got " + macerator.redstoneControl().mode());

            player.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(
                    containerId + 1, MachineControls.redstoneButton(RedstoneMode.LOW)));
            context.assertTrue(macerator.redstoneControl().mode() == RedstoneMode.HIGH,
                    "A click for another container id should be ignored");

            player.setGameMode(GameType.SPECTATOR);
            context.assertTrue(player.isSpectator(), "The player should now be a spectator");
            player.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(
                    containerId, MachineControls.redstoneButton(RedstoneMode.LOW)));
            context.assertTrue(macerator.redstoneControl().mode() == RedstoneMode.HIGH,
                    "A spectator's click packet should be ignored");

            player.setGameMode(GameType.SURVIVAL);
            BlockPos absolute = context.absolutePos(POS);
            player.setPos(absolute.getX() + 12.5, absolute.getY() + 1, absolute.getZ() + 0.5);
            context.assertFalse(menu.stillValid(player), "A player out of reach should invalidate the menu");
            player.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(
                    containerId, MachineControls.redstoneButton(RedstoneMode.LOW)));
            context.assertTrue(macerator.redstoneControl().mode() == RedstoneMode.HIGH,
                    "A click on a menu that is no longer valid should be ignored");

            standBeside(player, context);
            player.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(
                    containerId, MachineControls.redstoneButton(RedstoneMode.LOW)));
            context.assertTrue(macerator.redstoneControl().mode() == RedstoneMode.LOW,
                    "Back in reach, the click should apply again");
        } finally {
            disconnect(player);
        }
        context.assertTrue(player.isRemoved(), "The test player should be removed from the level");
        context.assertTrue(players.getPlayerCount() == playersBefore,
                "The test player should be gone, players " + playersBefore + " -> " + players.getPlayerCount());
        context.succeed();
    }

    // ==================== Energy telemetry ====================

    /** A running macerator reports its per-tick spend as usage, and 0 within two ticks of finishing. */
    public static void testMaceratorUsageTracksProcessing(GameTestHelper context) {
        MaceratorBlockEntity macerator = place(context, LogisticsAutomation.BLOCK.MACERATOR, MaceratorBlockEntity.class);
        MachineControls controls = MachineControls.server(macerator);
        long rfPerTick = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.MACERATOR_ENERGY_PER_TICK);
        IEnergyStorage energy = macerator.energyStorage(null);
        for (int i = 0; i < 80; i++) {
            energy.insert(128, false);
        }
        macerator.setItem(0, new ItemStack(Items.IRON_ORE));

        context.startSequence()
                .thenWaitUntil(() -> context.assertTrue(macerator.isProcessing(), "The macerator should start"))
                .thenExecuteAfter(5, () -> {
                    context.assertTrue(macerator.isProcessing(), "The macerator should still be processing");
                    context.assertTrue(controls.rate() == rfPerTick,
                            "Usage should equal " + rfPerTick + " RF/t while processing, got " + controls.rate());
                    context.assertTrue(controls.maxRate() == 128, "Max input should be 128, got " + controls.maxRate());
                    context.assertTrue(controls.hasBuffer(), "A macerator has an energy buffer");
                    context.assertTrue(controls.stored() == energy.getAmount(), "Stored should match the buffer");
                })
                .thenWaitUntil(() -> context.assertTrue(
                        !macerator.isProcessing() && !macerator.getItem(1).isEmpty(), "The macerator should finish"))
                .thenExecuteAfter(2, () -> context.assertTrue(controls.rate() == 0,
                        "Usage should read 0 two ticks after finishing, got " + controls.rate()))
                .thenSucceed();
    }

    /** A generating magmatic engine reports a positive generation no higher than its maximum. */
    public static void testMagmaticGenerationWithinMax(GameTestHelper context) {
        context.setBlock(POS, LogisticsPower.BLOCK.MAGMATIC_ENGINE.defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, false));
        MagmaticEngineBlockEntity engine = context.getBlockEntity(POS, MagmaticEngineBlockEntity.class);
        if (engine == null) {
            context.fail("Expected a magmatic engine");
            return;
        }
        engine.setRedstoneMode(RedstoneMode.IGNORED);
        engine.fluidStorage(Direction.UP).insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(2000), false);
        MachineControls controls = MachineControls.server(engine);

        context.succeedWhen(() -> {
            long rate = controls.rate();
            long max = controls.maxRate();
            context.assertTrue(rate > 0, "A burning engine should report generation, got " + rate);
            context.assertTrue(rate <= max, "Generation " + rate + " should not exceed max " + max);
            context.assertTrue(max == engine.getCurrentOutputPower(), "Max should be the engine's output power");
            context.assertTrue(controls.hasBuffer(), "A magmatic engine has an output buffer");
        });
    }
}
