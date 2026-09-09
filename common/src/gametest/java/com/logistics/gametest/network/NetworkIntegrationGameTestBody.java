package com.logistics.gametest.network;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.block.entity.PowerJunctionBlockEntity;
import com.logistics.pipe.modules.ProviderModule;
import com.logistics.pipe.modules.RequesterModule;
import com.logistics.pipe.modules.SinkModule;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import com.logistics.pipe.ui.ChassisInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Shared network-integration GameTest bodies (see {@code common/build.gradle}). The original
 * Fabric test used Fabric's transfer API purely as test-setup plumbing (filling/counting a chest);
 * {@link #testProviderDeliversItemToRequester} below uses vanilla {@link ChestBlockEntity} access
 * instead, which behaves identically on both loaders.
 */
public class NetworkIntegrationGameTestBody {

    /** Place a filled Power Junction so the network can pay module energy costs. */
    private static void placeChargedPowerJunction(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = context.getBlockEntity(pos, PowerJunctionBlockEntity.class);
        IEnergyStorage es = junction.energyStorage(null);
        long remaining = PowerJunctionBlockEntity.CAPACITY;
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted <= 0) break;
            remaining -= inserted;
        }
    }

    /**
     * Verifies that placing connected logistics pipes causes a PipeNetwork to form.
     *
     * <p>Layout (y=1): [basic_logistics_pipe] [basic_logistics_pipe] [basic_logistics_pipe]
     * After a few ticks (pipes tick and register with NetworkRegistry), all three positions
     * should belong to the same network.
     */
    public static void testLogisticsNetworkForms(GameTestHelper context) {
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
     */
    public static void testInsertionPipeDeliversToChest(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos chestPos = new BlockPos(1, 1, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.ITEM_INSERTION_PIPE);

        PipeBlockEntity pipe = context.getBlockEntity(pipePos, PipeBlockEntity.class);
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
     */
    public static void testBasicSinkDeliveresToAdjacentChest(GameTestHelper context) {
        BlockPos sinkPos = new BlockPos(0, 1, 0);
        BlockPos chestPos = new BlockPos(1, 1, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(sinkPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        placeChargedPowerJunction(context, sinkPos.above()); // power the network so routing can pay its cost

        PipeBlockEntity pipeEntity = context.getBlockEntity(sinkPos, PipeBlockEntity.class);
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
            PipeBlockEntity pipe = context.getBlockEntity(sinkPos, PipeBlockEntity.class);
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
     */
    public static void testNetworkSplitsAndRejoins(GameTestHelper context) {
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
     */
    public static void testProviderDeliversItemToRequester(GameTestHelper context) {
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
        placeChargedPowerJunction(context, transportPos.above()); // power the network so modules can pay their costs

        // Pre-fill source chest with 4 diamonds
        ChestBlockEntity sourceChest = context.getBlockEntity(sourceChestPos, ChestBlockEntity.class);
        if (sourceChest == null) {
            context.fail("Expected source chest block entity");
            return;
        }
        sourceChest.setItem(0, new ItemStack(Items.DIAMOND, 4));

        // Configure requester to request 4 diamonds
        PipeBlockEntity requesterEntity = context.getBlockEntity(requesterPos, PipeBlockEntity.class);
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
            ChestBlockEntity destChest = context.getBlockEntity(destChestPos, ChestBlockEntity.class);
            long count = 0;
            if (destChest != null) {
                for (int slot = 0; slot < destChest.getContainerSize(); slot++) {
                    ItemStack stack = destChest.getItem(slot);
                    if (stack.is(Items.DIAMOND)) {
                        count += stack.getCount();
                    }
                }
            }
            if (count < 4) {
                throw context.assertionException("Expected >= 4 diamonds in dest chest, found: " + count);
            }
        });
    }


    /**
     * A Provider set to "Leave First Slot" must not drain the slot it promises to leave.
     *
     * <p>The same layout as {@link #testProviderDeliversItemToRequester}, but the source chest
     * holds diamonds in slots 0 and 1 and the Provider is in RESERVE mode. Only slot 1 is in
     * scope, so the requester's 4 diamonds must come out of slot 1 and slot 0 must be untouched.
     *
     * <p>This runs through a real chest, so it covers the loader storage adapter as well as the
     * module: a resource-scoped extract always drains from slot 0 regardless of the crop.
     *
     * <p>Layout (y=1):
     * [source_chest] &#8592; [provider_pipe] &#8594; [transport_pipe] &#8594; [requester_pipe] &#8594; [dest_chest]
     *   (0,1,0)            (1,1,0)           (2,1,0)             (3,1,0)          (4,1,0)
     */
    public static void testProviderReserveModeLeavesFirstSlot(GameTestHelper context) {
        BlockPos sourceChestPos = new BlockPos(0, 1, 0);
        BlockPos providerPos = new BlockPos(1, 1, 0);
        BlockPos transportPos = new BlockPos(2, 1, 0);
        BlockPos requesterPos = new BlockPos(3, 1, 0);
        BlockPos destChestPos = new BlockPos(4, 1, 0);

        context.setBlock(destChestPos, Blocks.CHEST);
        context.setBlock(requesterPos, LogisticsPipe.BLOCK.REQUESTER_LOGISTICS_PIPE);
        context.setBlock(transportPos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(providerPos, LogisticsPipe.BLOCK.PROVIDER_LOGISTICS_PIPE);
        context.setBlock(sourceChestPos, Blocks.CHEST);
        placeChargedPowerJunction(context, transportPos.above());

        ChestBlockEntity sourceChest = context.getBlockEntity(sourceChestPos, ChestBlockEntity.class);
        if (sourceChest == null) {
            context.fail("Expected source chest block entity");
            return;
        }
        sourceChest.setItem(0, new ItemStack(Items.DIAMOND, 4)); // the slot RESERVE protects
        sourceChest.setItem(1, new ItemStack(Items.DIAMOND, 4)); // the only slot in scope

        PipeBlockEntity providerEntity = context.getBlockEntity(providerPos, PipeBlockEntity.class);
        if (providerEntity == null) {
            context.fail("Provider pipe should have a block entity");
            return;
        }
        if (!(providerEntity.getBlockState().getBlock() instanceof PipeBlock providerBlock)
                || providerBlock.getPipe() == null) {
            throw new IllegalStateException("Pipe missing for block entity at providerPos");
        }
        ProviderModule provider = providerBlock.getPipe().getModule(ProviderModule.class, providerEntity);
        if (provider == null) {
            throw new IllegalStateException("ProviderModule missing from pipe at providerPos");
        }
        provider.setMode(providerEntity.createContext(), ProviderModule.ProviderMode.RESERVE);

        PipeBlockEntity requesterEntity = context.getBlockEntity(requesterPos, PipeBlockEntity.class);
        if (requesterEntity == null) {
            context.fail("Requester pipe should have a block entity");
            return;
        }
        if (!(requesterEntity.getBlockState().getBlock() instanceof PipeBlock requesterBlock)
                || requesterBlock.getPipe() == null) {
            throw new IllegalStateException("Pipe missing for block entity at requesterPos");
        }
        RequesterModule requester = requesterBlock.getPipe().getModule(RequesterModule.class, requesterEntity);
        if (requester == null) {
            throw new IllegalStateException("RequesterModule missing from pipe at requesterPos");
        }
        requester.setRequestConfig(requesterEntity.createContext(), 0, "minecraft:diamond", 4);

        context.succeedWhen(() -> {
            ChestBlockEntity chest = context.getBlockEntity(sourceChestPos, ChestBlockEntity.class);
            if (chest == null) {
                throw context.assertionException("Source chest disappeared");
            }
            ItemStack reserved = chest.getItem(0);
            if (!reserved.is(Items.DIAMOND) || reserved.getCount() != 4) {
                throw context.assertionException(
                        "Slot 0 is reserved by Leave First Slot but held: " + reserved);
            }
            ChestBlockEntity destChest = context.getBlockEntity(destChestPos, ChestBlockEntity.class);
            long delivered = 0;
            if (destChest != null) {
                for (int slot = 0; slot < destChest.getContainerSize(); slot++) {
                    ItemStack stack = destChest.getItem(slot);
                    if (stack.is(Items.DIAMOND)) {
                        delivered += stack.getCount();
                    }
                }
            }
            if (delivered < 4) {
                throw context.assertionException(
                        "Expected >= 4 diamonds delivered from slot 1, found: " + delivered);
            }
        });
    }

    /**
     * Build the shared split-recovery layout and return the position of the pipe the player breaks.
     *
     * <p>Layout (y=1):
     * <pre>
     *   z=0:  [injector] [sink pipe] [chest]      (0,1,0) (1,1,0) (2,1,0)
     *   z=1:  [bridge]                            (0,1,1)  &#8592; broken to force the split
     *   z=2:  [leaf]                              (0,1,2)
     * </pre>
     * Breaking the bridge strands the leaf, so the network splits and both components get
     * brand-new {@code PipeNetwork} instances with empty sink registries. The sink pipe's own
     * connections never change, so nothing about its physical state tells it to re-register.
     */
    private static BlockPos buildSplitRecoveryLayout(GameTestHelper context) {
        BlockPos injectorPos = new BlockPos(0, 1, 0);
        BlockPos bridgePos = new BlockPos(0, 1, 1);
        BlockPos leafPos = new BlockPos(0, 1, 2);

        context.setBlock(leafPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(bridgePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(injectorPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        placeChargedPowerJunction(context, injectorPos.above());

        return bridgePos;
    }

    /** Force-inject an item into the injector pipe from the west, as an upstream pipe would. */
    private static void injectFromWest(GameTestHelper context, ItemStack stack) {
        PipeBlockEntity injector = context.getBlockEntity(new BlockPos(0, 1, 0), PipeBlockEntity.class);
        if (injector == null) {
            context.fail("Injector pipe should have a block entity");
            return;
        }
        if (!injector.forceAddItem(new TravelingItem(stack, Direction.WEST, 0.5f), Direction.WEST)) {
            context.fail("Injector pipe should accept the force-injected " + stack.getItem());
        }
    }

    /**
     * An Enchantment Sink chassis must still be a routing target after the network splits.
     *
     * <p>Regression test: the module registers as a sink only from {@code onConnectionsChanged},
     * and a split elsewhere in the component leaves this pipe's own connections untouched. Before
     * the fix the rebuilt network had an empty sink registry, so the router found no destination
     * for the enchanted sword and dropped it on the floor instead of delivering it.
     */
    public static void testEnchantmentSinkStillReceivesAfterNetworkSplit(GameTestHelper context) {
        BlockPos chassisPos = new BlockPos(1, 1, 0);
        BlockPos chestPos = new BlockPos(2, 1, 0);
        BlockPos bridgePos = buildSplitRecoveryLayout(context);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(chassisPos, LogisticsPipe.BLOCK.CHASSIS_LOGISTICS_PIPE_MK1);

        PipeBlockEntity chassis = context.getBlockEntity(chassisPos, PipeBlockEntity.class);
        if (chassis == null) {
            context.fail("Chassis pipe should have a block entity");
            return;
        }
        // Insert the module the way a player does — through the chassis inventory.
        new ChassisInventory(chassis).setItem(0, new ItemStack(LogisticsPipe.ITEM.ENCHANTMENT_SINK_MODULE));

        ItemStack enchantedSword = new ItemStack(Items.DIAMOND_SWORD);
        enchantedSword.enchant(
                context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SHARPNESS),
                1);

        // Let the network form and the sink register, then break the bridge to split it.
        context.runAfterDelay(25, () -> context.setBlock(bridgePos, Blocks.AIR));

        // Inject well after the split: the Power Junction rescans every 20 ticks, so by now the
        // rebuilt network can pay the routing cost. Anything still missing is a sink-registry gap.
        context.runAfterDelay(50, () -> {
            injectFromWest(context, enchantedSword);
            context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIAMOND_SWORD));
        });
    }

    /**
     * The plain Sink module must keep recovering from a split exactly as it does today.
     *
     * <p>Guards the modules that already work against a regression from changing how split
     * recovery is driven. Same layout as
     * {@link #testEnchantmentSinkStillReceivesAfterNetworkSplit}, with a default-route Basic
     * Logistics Pipe in place of the Enchantment Sink chassis.
     */
    public static void testBasicSinkStillReceivesAfterNetworkSplit(GameTestHelper context) {
        BlockPos sinkPos = new BlockPos(1, 1, 0);
        BlockPos chestPos = new BlockPos(2, 1, 0);
        BlockPos bridgePos = buildSplitRecoveryLayout(context);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(sinkPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);

        PipeBlockEntity sinkEntity = context.getBlockEntity(sinkPos, PipeBlockEntity.class);
        if (sinkEntity == null) {
            context.fail("Sink pipe should have a block entity");
            return;
        }
        if (!(sinkEntity.getBlockState().getBlock() instanceof PipeBlock sinkBlock)
                || sinkBlock.getPipe() == null) {
            throw new IllegalStateException("Pipe missing for block entity at sinkPos");
        }
        SinkModule sink = sinkBlock.getPipe().getModule(SinkModule.class, sinkEntity);
        if (sink == null) {
            throw new IllegalStateException("SinkModule missing from pipe at sinkPos");
        }
        // Enable default route, as a player would through the wrench GUI.
        sink.setDefaultRoute(sinkEntity.createContext(), true);

        context.runAfterDelay(25, () -> context.setBlock(bridgePos, Blocks.AIR));

        context.runAfterDelay(50, () -> {
            injectFromWest(context, new ItemStack(Items.IRON_INGOT));
            context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.IRON_INGOT));
        });
    }

    // testSinkPriorityRoutesItemToHigherPrioritySink:
    //   Two basic logistics pipes connected to separate chests, one with higher priority.
    //   Items injected into the network should be routed to the higher-priority sink first.
}
