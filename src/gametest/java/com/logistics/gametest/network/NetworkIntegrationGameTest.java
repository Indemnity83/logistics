package com.logistics.gametest.network;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.RequesterModule;
import com.logistics.pipe.modules.SinkModule;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Tick-based game tests for multi-block logistics network behaviours.
 *
 * <p>These tests verify that the pipe network layer (NetworkRegistry, SinkModule, routing)
 * functions correctly in a live Minecraft environment where block entities tick normally.
 *
 * <p>Run all in-game: /test runall
 * Run one test:       /test run logistics-gametest.networkintegrationgametest.&lt;methodname&gt;
 */
public class NetworkIntegrationGameTest {

    /**
     * Verifies that placing connected logistics pipes causes a PipeNetwork to form.
     *
     * <p>Layout (y=1): [basic_logistics_pipe] [basic_logistics_pipe] [basic_logistics_pipe]
     * After a few ticks (pipes tick and register with NetworkRegistry), all three positions
     * should belong to the same network.
     *
     * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testlogisticsnetworkforms
     */
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testLogisticsNetworkForms(GameTestHelper context) {
        BlockPos pos1 = new BlockPos(0, 1, 0);
        BlockPos pos2 = new BlockPos(1, 1, 0);
        BlockPos pos3 = new BlockPos(2, 1, 0);

        context.setBlock(pos1, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(pos2, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(pos3, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);

        // Wait for pipes to tick and form their network (SinkModule re-syncs every 20 ticks)
        context.runAfterDelay(25, () -> {
            PipeNetwork net1 = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pos1));
            PipeNetwork net2 = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pos2));
            PipeNetwork net3 = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pos3));

            if (net1 == null) {
                context.fail("No network found at pipe position 1 after 25 ticks");
                return;
            }
            if (net2 == null) {
                context.fail("No network found at pipe position 2 after 25 ticks");
                return;
            }
            if (net3 == null) {
                context.fail("No network found at pipe position 3 after 25 ticks");
                return;
            }
            if (net1 != net2 || net1 != net3) {
                context.fail("Connected pipes should all belong to the same network object, but got distinct instances");
                return;
            }
            context.succeed();
        });
    }

    /**
     * Verifies that an insertion pipe delivers an injected item to an adjacent chest.
     *
     * <p>Layout (y=1): [item_insertion_pipe] → [chest]
     * The InsertionModule prefers to route items toward adjacent inventories, so a diamond
     * injected from the west should arrive in the chest without any network or energy setup.
     *
     * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testinsertionpipedeliverstochest
     */
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testInsertionPipeDeliversToChest(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos chestPos = new BlockPos(1, 1, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.ITEM_INSERTION_PIPE);

        PipeBlockEntity pipe = (PipeBlockEntity) context.getBlockEntity(pipePos);
        if (pipe == null) {
            context.fail("Insertion pipe should have a block entity");
            return;
        }

        // Inject from the west using forceAddItem (bypasses ingress check).
        // InsertionModule should route east into the chest.
        TravelingItem diamond = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.WEST, 0.5f);
        boolean accepted = pipe.forceAddItem(diamond, Direction.WEST);
        if (!accepted) {
            context.fail("Insertion pipe should accept the injected diamond");
            return;
        }

        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIAMOND));
    }

    /**
     * Verifies that a basic logistics pipe (default-route sink) routes an incoming item to an
     * adjacent inventory.
     *
     * <p>Layout (y=1): [basic_logistics_pipe] → [chest]
     * The default-route flag must be enabled explicitly (it is off by default, matching the
     * in-game behaviour where a player must toggle it via the wrench GUI). Once enabled,
     * any item arriving at the pipe will be deposited into the adjacent chest.
     *
     * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testbasicsinkdeliverstoadjacentchest
     */
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testBasicSinkDeliveresToAdjacentChest(GameTestHelper context) {
        BlockPos sinkPos = new BlockPos(0, 1, 0);
        BlockPos chestPos = new BlockPos(1, 1, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(sinkPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);

        PipeBlockEntity pipeEntity = (PipeBlockEntity) context.getBlockEntity(sinkPos);
        if (pipeEntity == null) {
            context.fail("Basic logistics pipe should have a block entity");
            return;
        }

        // Enable default route (off by default — must be toggled like a player would via wrench GUI).
        // This registers the pipe as a generic-interest sink so the NetworkRouterModule can route
        // items to it without a specific filter match.
        if (!(pipeEntity.getBlockState().getBlock() instanceof PipeBlock pipeBlock)) {
            throw new IllegalStateException("Block at sinkPos is not a PipeBlock");
        }
        if (pipeBlock.getPipe() == null) {
            throw new IllegalStateException("Pipe missing for block entity at sinkPos");
        }
        SinkModule sink = pipeBlock.getPipe().getModule(SinkModule.class, pipeEntity);
        if (sink == null) {
            throw new IllegalStateException("SinkModule missing from pipe at sinkPos");
        }
        PipeContext pipeCtx = pipeEntity.createContext();
        sink.setDefaultRoute(pipeCtx, true);

        // Wait 2 ticks for the connection cache to update so the SinkModule sets sinkDirection=EAST.
        context.runAfterDelay(2, () -> {
            PipeBlockEntity pipe = (PipeBlockEntity) context.getBlockEntity(sinkPos);
            if (pipe == null) {
                context.fail("Basic logistics pipe should have a block entity");
                return;
            }

            // Inject from the west using forceAddItem (bypasses ingress check).
            // NetworkRouterModule routes to this pipe (default-route sink), then SinkModule
            // deposits the item into the adjacent chest.
            TravelingItem ingot = new TravelingItem(new ItemStack(Items.IRON_INGOT), Direction.WEST, 0.5f);
            boolean accepted = pipe.forceAddItem(ingot, Direction.WEST);
            if (!accepted) {
                context.fail("Basic logistics pipe should accept the force-injected iron ingot");
                return;
            }

            context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.IRON_INGOT));
        });
    }

    /**
     * Verifies that the logistics network can be split and re-formed.
     *
     * <p>Layout (y=1): [basic_logistics_pipe] [basic_logistics_pipe] [basic_logistics_pipe]
     * After the initial network forms, the middle pipe is removed (network splits into two),
     * then replaced (network merges back into one). The final state should be a single network.
     *
     * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testnetworksplitsandrejoins
     */
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testNetworkSplitsAndRejoins(GameTestHelper context) {
        BlockPos pos1 = new BlockPos(0, 1, 0);
        BlockPos pos2 = new BlockPos(1, 1, 0);
        BlockPos pos3 = new BlockPos(2, 1, 0);

        context.setBlock(pos1, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(pos2, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(pos3, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);

        // Let the initial network form, then remove the middle pipe
        context.runAfterDelay(25, () -> {
            PipeNetwork initialNet = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pos1));
            if (initialNet == null) {
                context.fail("Initial network should have formed by tick 25");
                return;
            }
            // Remove the middle pipe to split the network into two isolated segments
            context.setBlock(pos2, Blocks.AIR);
        });

        // Re-place the middle pipe to trigger a network merge
        context.runAfterDelay(50, () ->
                context.setBlock(pos2, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE));

        // After enough time for re-sync (SinkModule syncs every 20 ticks), the three pipes
        // should all belong to the same network again
        context.runAfterDelay(80, () -> {
            PipeNetwork net1 = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pos1));
            PipeNetwork net3 = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pos3));

            if (net1 == null || net3 == null) {
                context.fail("Pipes at pos1 and pos3 should both have a network after rejoining");
                return;
            }
            if (net1 != net3) {
                context.fail("pos1 and pos3 should be in the same network after the middle pipe was restored, but got distinct network instances");
                return;
            }
            context.succeed();
        });
    }

    /**
     * Verifies the end-to-end provider→requester delivery flow.
     *
     * <p>A provider pipe scans a source chest and registers supply; a requester pipe is
     * configured to request 4 diamonds; the network dispatches the order; the provider
     * extracts the items and injects them into the pipe; they travel through the transport
     * pipe and arrive in the destination chest.
     *
     * <p>Timing: provider scans at tick 6, requester fires at tick 20, provider drains
     * dispatch queue at tick 24, items arrive in dest chest around tick 40.
     *
     * <p>Layout (y=1):
     * [source_chest] ← [provider_pipe] → [transport_pipe] → [requester_pipe] → [dest_chest]
     *   (0,1,0)            (1,1,0)           (2,1,0)             (3,1,0)          (4,1,0)
     *
     * <p>Run in-game: /test run logistics-gametest.networkintegrationgametest.testproviderdeliversitemtorequester
     */
    @net.minecraft.gametest.framework.GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testProviderDeliversItemToRequester(GameTestHelper context) {
        BlockPos sourceChestPos = new BlockPos(0, 1, 0);
        BlockPos providerPos = new BlockPos(1, 1, 0);
        BlockPos transportPos = new BlockPos(2, 1, 0);
        BlockPos requesterPos = new BlockPos(3, 1, 0);
        BlockPos destChestPos = new BlockPos(4, 1, 0);

        // Place dest chest first so the requester auto-selects EAST toward it on placement
        context.setBlock(destChestPos, Blocks.CHEST);
        context.setBlock(requesterPos, LogisticsPipe.BLOCK.REQUESTER_LOGISTICS_PIPE);
        context.setBlock(transportPos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(providerPos, LogisticsPipe.BLOCK.PROVIDER_LOGISTICS_PIPE);
        context.setBlock(sourceChestPos, Blocks.CHEST);

        // Pre-fill source chest with 4 diamonds
        Storage<ItemVariant> sourceStorage = ItemStorage.SIDED.find(
                context.getLevel(),
                context.absolutePos(sourceChestPos),
                Direction.EAST);
        if (sourceStorage == null) {
            context.fail("Source chest should expose ItemStorage on its east face");
            return;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = sourceStorage.insert(ItemVariant.of(Items.DIAMOND), 4, transaction);
            if (inserted != 4) {
                context.fail("Expected to insert 4 diamonds into source chest, got: " + inserted);
            }
            transaction.commit();
        }

        // Configure requester to request 4 diamonds
        PipeBlockEntity requesterEntity = (PipeBlockEntity) context.getBlockEntity(requesterPos);
        if (requesterEntity == null) {
            context.fail("Requester pipe should have a block entity");
            return;
        }
        if (!(requesterEntity.getBlockState().getBlock() instanceof PipeBlock requesterBlock)) {
            throw new IllegalStateException("Block at requesterPos is not a PipeBlock");
        }
        if (requesterBlock.getPipe() == null) {
            throw new IllegalStateException("Pipe missing for block entity at requesterPos");
        }
        RequesterModule requester = requesterBlock.getPipe().getModule(RequesterModule.class, requesterEntity);
        if (requester == null) {
            throw new IllegalStateException("RequesterModule missing from pipe at requesterPos");
        }
        PipeContext ctx = requesterEntity.createContext();
        requester.setRequestConfig(ctx, 0, "minecraft:diamond", 4);

        context.succeedWhen(() -> {
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(
                    context.getLevel(), context.absolutePos(destChestPos), Direction.WEST);
            long count = 0;
            if (storage != null) {
                for (var view : storage) {
                    if (view.getResource().isOf(Items.DIAMOND)) {
                        count += view.getAmount();
                    }
                }
            }
            if (count < 4) {
                context.assertTrue(false, "Expected >= 4 diamonds in dest chest, found: " + count);
            }
        });
    }

    // testSinkPriorityRoutesItemToHigherPrioritySink:
    //   Two basic logistics pipes connected to separate chests, one with higher priority.
    //   Items injected into the network should be routed to the higher-priority sink first.
}
