package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.pipe.block.entity.PowerJunctionBlockEntity;
import com.logistics.pipe.Pipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ItemFilterModule;
import com.logistics.pipe.modules.MergerModule;
import com.logistics.pipe.modules.SinkModule;
import com.logistics.pipe.ui.ChassisInventory;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Shared pipe module GameTest bodies, compiled directly into both loaders' {@code gametest} source
 * sets (see {@code common/build.gradle}). Loader-specific glue wires these into each loader's own
 * registration mechanism: Fabric's {@code @GameTest}-annotated {@code ModuleGameTest} delegates to
 * these methods, and NeoForge's {@code ModuleGameTestRegistration} references them directly as
 * {@code Consumer<GameTestHelper>} method references.
 *
 * <p>Tests routing logic, filter matching, extraction timing, and energy calculations.
 */
public class ModuleGameTestBody {

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
     * Test that filter pipes can be placed and have block entities.
     */
    public static void testFilterPipePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_FILTER_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Filter pipe should have block entity");
        }

        // Verify the pipe block has the correct Pipe instance
        if (!(context.getBlockState(pos).getBlock() instanceof PipeBlock)) {
            context.fail("Block should be a PipeBlock");
        }

        context.succeed();
    }

    /**
     * Test that extractor pipes can be placed and have block entities.
     */
    public static void testExtractorPipePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_EXTRACTOR_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Extractor pipe should have block entity");
        }

        context.succeed();
    }

    /**
     * Test that merger pipes can be placed and have block entities.
     */
    public static void testMergerPipePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_MERGER_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Merger pipe should have block entity");
        }

        context.succeed();
    }

    /**
     * Test that insertion pipes can be placed and have block entities.
     */
    public static void testInsertionPipePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_INSERTION_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Insertion pipe should have block entity");
        }

        context.succeed();
    }

    /**
     * Test that void pipes can be placed and have block entities.
     */
    public static void testVoidPipePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_VOID_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Void pipe should have block entity");
        }

        context.succeed();
    }

    /**
     * Test that passthrough pipes can be placed and have block entities.
     */
    public static void testPassthroughPipePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_PASSTHROUGH_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Passthrough pipe should have block entity");
        }

        context.succeed();
    }

    /**
     * Test that gold transport pipes can be placed and have block entities.
     */
    public static void testGoldTransportPipePlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.GOLD_TRANSPORT_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Gold transport pipe should have block entity");
        }

        context.succeed();
    }

    // ==================== Module Functionality Tests ====================

    /**
     * Test that filter module routes items based on configured filters.
     */
    public static void testFilterModuleRoutesMatchingItems(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_FILTER_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Filter pipe should have block entity");
        }

        // Create PipeContext
        PipeContext ctx = new PipeContext(
            context.getLevel(),
            pos,
            context.getBlockState(pos),
            pipeEntity
        );

        Pipe pipe = context.getBlockState(pos).getBlock() instanceof PipeBlock pb ? pb.getPipe() : null;
        if (pipe == null) {
            context.fail("PipeContext should have pipe");
        }

        // Configure filter for NORTH to accept diamonds
        ItemFilterModule filterModule = new ItemFilterModule();
        String diamondId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND).toString();

        CompoundTag filters = ctx.getCompoundTag(filterModule, "filters");
        ListTag northFilters = new ListTag();
        northFilters.add(StringTag.valueOf(diamondId));
        filters.put("north", northFilters);
        ctx.putCompoundTag(filterModule, "filters", filters);

        // Create traveling item with diamond
        TravelingItem diamondItem = new TravelingItem(
            new ItemStack(Items.DIAMOND),
            Direction.SOUTH,
            0.05f
        );

        // Test routing decision with NORTH as an option
        List<Direction> options = List.of(Direction.NORTH, Direction.EAST);
        RoutePlan plan = filterModule.route(ctx, diamondItem, options);

        // Diamond should be routed to NORTH (matches filter)
        if (plan.getType() != RoutePlan.Type.REROUTE) {
            context.fail("Diamond should be rerouted, got: " + plan.getType());
        }
        if (!plan.getDirections().contains(Direction.NORTH)) {
            context.fail("Diamond should route to NORTH, got: " + plan.getDirections());
        }

        context.succeed();
    }

    /**
     * Test that filter module passes through non-matching items.
     */
    public static void testFilterModulePassesThroughNonMatching(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_FILTER_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Filter pipe should have block entity");
        }

        PipeContext ctx = new PipeContext(
            context.getLevel(),
            pos,
            context.getBlockState(pos),
            pipeEntity
        );

        // Configure filter for NORTH to accept diamonds only
        ItemFilterModule filterModule = new ItemFilterModule();
        String diamondId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND).toString();

        CompoundTag filters = ctx.getCompoundTag(filterModule, "filters");
        ListTag northFilters = new ListTag();
        northFilters.add(StringTag.valueOf(diamondId));
        filters.put("north", northFilters);
        ctx.putCompoundTag(filterModule, "filters", filters);

        // Create traveling item with dirt (not in filter)
        TravelingItem dirtItem = new TravelingItem(
            new ItemStack(Items.DIRT),
            Direction.SOUTH,
            0.05f
        );

        // Test routing with NORTH (filtered) and EAST (no filter)
        List<Direction> options = List.of(Direction.NORTH, Direction.EAST);
        RoutePlan plan = filterModule.route(ctx, dirtItem, options);

        // Dirt should be rerouted to EAST (fallback to unfiltered side)
        if (plan.getType() != RoutePlan.Type.REROUTE) {
            context.fail("Dirt should be rerouted to fallback, got: " + plan.getType());
        }
        if (!plan.getDirections().contains(Direction.EAST)) {
            context.fail("Dirt should route to EAST (unfiltered), got: " + plan.getDirections());
        }

        context.succeed();
    }

    /**
     * Test that merger module routes items to configured output direction.
     */
    public static void testMergerModuleRoutesToOutput(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_MERGER_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Merger pipe should have block entity");
        }

        PipeContext ctx = new PipeContext(
            context.getLevel(),
            pos,
            context.getBlockState(pos),
            pipeEntity
        );

        // Configure merger to output to NORTH (via NBT state)
        MergerModule mergerModule = new MergerModule();
        ctx.saveString(mergerModule, "output_direction", String.valueOf(Direction.NORTH.get3DDataValue()));

        // Create traveling items from different directions
        TravelingItem fromSouth = new TravelingItem(
            new ItemStack(Items.DIAMOND),
            Direction.SOUTH,
            0.05f
        );
        TravelingItem fromEast = new TravelingItem(
            new ItemStack(Items.DIRT),
            Direction.EAST,
            0.05f
        );

        // Test routing decisions
        List<Direction> options = List.of(Direction.NORTH, Direction.EAST, Direction.WEST);
        RoutePlan southPlan = mergerModule.route(ctx, fromSouth, options);
        RoutePlan eastPlan = mergerModule.route(ctx, fromEast, options);

        // Both should be routed to NORTH (the configured output)
        if (!southPlan.getDirections().contains(Direction.NORTH)) {
            context.fail("Item from SOUTH should route to NORTH, got: " + southPlan.getDirections());
        }
        if (!eastPlan.getDirections().contains(Direction.NORTH)) {
            context.fail("Item from EAST should route to NORTH, got: " + eastPlan.getDirections());
        }

        context.succeed();
    }

    /**
     * Test that filter module handles multiple filters on different sides.
     */
    public static void testFilterModuleMultipleSideFilters(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.ITEM_FILTER_PIPE);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Filter pipe should have block entity");
        }

        PipeContext ctx = new PipeContext(
            context.getLevel(),
            pos,
            context.getBlockState(pos),
            pipeEntity
        );

        // Configure filters: NORTH for diamonds, EAST for gold
        ItemFilterModule filterModule = new ItemFilterModule();
        String diamondId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND).toString();
        String goldId = BuiltInRegistries.ITEM.getKey(Items.GOLD_INGOT).toString();

        CompoundTag filters = ctx.getCompoundTag(filterModule, "filters");

        ListTag northFilters = new ListTag();
        northFilters.add(StringTag.valueOf(diamondId));
        filters.put("north", northFilters);

        ListTag eastFilters = new ListTag();
        eastFilters.add(StringTag.valueOf(goldId));
        filters.put("east", eastFilters);

        ctx.putCompoundTag(filterModule, "filters", filters);

        // Create test items
        TravelingItem diamondItem = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.SOUTH, 0.05f);
        TravelingItem goldItem = new TravelingItem(new ItemStack(Items.GOLD_INGOT), Direction.SOUTH, 0.05f);
        TravelingItem ironItem = new TravelingItem(new ItemStack(Items.IRON_INGOT), Direction.SOUTH, 0.05f);

        // Test routing with all sides available
        List<Direction> options = List.of(Direction.NORTH, Direction.EAST, Direction.WEST);
        RoutePlan diamondPlan = filterModule.route(ctx, diamondItem, options);
        RoutePlan goldPlan = filterModule.route(ctx, goldItem, options);
        RoutePlan ironPlan = filterModule.route(ctx, ironItem, options);

        // Diamond should route to NORTH
        if (!diamondPlan.getDirections().contains(Direction.NORTH)) {
            context.fail("Diamond should route to NORTH, got: " + diamondPlan.getDirections());
        }

        // Gold should route to EAST
        if (!goldPlan.getDirections().contains(Direction.EAST)) {
            context.fail("Gold should route to EAST, got: " + goldPlan.getDirections());
        }

        // Iron should route to WEST (unfiltered fallback)
        if (!ironPlan.getDirections().contains(Direction.WEST)) {
            context.fail("Iron should route to WEST (unfiltered), got: " + ironPlan.getDirections());
        }

        context.succeed();
    }

    // ==================== InsertionModule ====================

    /**
     * Verifies that InsertionModule routes an item into an adjacent chest when space is available.
     *
     * <p>InsertionModule.route() uses ItemStorage.SIDED.find() to probe real inventory capacity —
     * this path requires a live world and cannot be exercised in unit tests.
     *
     * <p>Layout (y=1): [chest at (0,1,0)] ← [insertion_pipe at (1,1,0)]
     * Diamond is injected from EAST (travels WEST toward the chest).
     */
    public static void testInsertionModuleDeliversToAdjacentChest(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 1, 0);
        BlockPos chestPos = new BlockPos(0, 1, 0); // WEST of pipe

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.ITEM_INSERTION_PIPE);

        PipeBlockEntity pipe = context.getBlockEntity(pipePos, PipeBlockEntity.class);
        if (pipe == null) {
            context.fail("Item insertion pipe should have a block entity");
            return;
        }

        // Diamond comes from EAST, travels WEST toward the chest
        TravelingItem diamond = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.EAST, 0.1f);
        if (!pipe.forceAddItem(diamond, Direction.EAST)) {
            context.fail("Pipe should accept the force-injected diamond");
            return;
        }

        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIAMOND));
    }

    // ==================== SinkModule ====================

    /**
     * Verifies that SinkModule routes a filter-matched item into the adjacent inventory.
     *
     * <p>SinkModule.matchesFilter() calls BuiltInRegistries.ITEM.get() and then route() uses
     * the live connection cache (set by onConnectionsChanged) — both require a running game.
     *
     * <p>Layout (y=1): [chest at (0,1,0)] ← [basic_logistics_pipe at (1,1,0)]
     * Diamond filter on SinkModule; diamond injected from EAST routes WEST into the chest.
     *
     * <p>BASIC_LOGISTICS_PIPE includes NetworkRouterModule, which drops items when no sink is
     * registered in the network. SinkModule.onTick registers after SYNC_INTERVAL = 20 ticks,
     * so we inject the diamond at tick 22 to ensure the sink is already registered.
     */
    public static void testSinkModuleFilterMatchRoutesToInventory(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 1, 0);
        BlockPos chestPos = new BlockPos(0, 1, 0); // WEST of pipe

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        placeChargedPowerJunction(context, pipePos.above()); // power the network so routing can pay its cost

        PipeBlockEntity pipe = context.getBlockEntity(pipePos, PipeBlockEntity.class);
        if (pipe == null) {
            context.fail("Basic logistics pipe should have a block entity");
            return;
        }

        // Write diamond filter into SinkModule NBT state (slot 0 = "0" key per FilterSlots)
        PipeContext ctx = new PipeContext(
                context.getLevel(), pipePos, context.getBlockState(pipePos), pipe);
        String diamondId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND).toString();
        CompoundTag filterTag = new CompoundTag();
        filterTag.putString("0", diamondId);
        ctx.putCompoundTag(new SinkModule(5), SinkModule.FILTERS, filterTag);

        // Wait for SinkModule.onTick to register the diamond sink interest with the network.
        // NetworkRouterModule (which runs before SinkModule) calls network.findSinkFor() and
        // drops the item if the sink is not yet registered. Registration happens after 20 ticks.
        context.runAfterDelay(22, () -> {
            TravelingItem diamond = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.EAST, 0.1f);
            if (!pipe.forceAddItem(diamond, Direction.EAST)) {
                context.fail("Pipe should accept the force-injected diamond");
            }
        });

        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIAMOND));
    }

    /**
     * Verifies that SinkModule with default route accepts any item that has no network destination.
     *
     * <p>With only the chest at WEST connected, validDirections = [WEST]. Since the sink direction
     * is the only option, hasOtherOptions = false and the default route fires.
     *
     * <p>Layout (y=1): [chest at (0,1,0)] ← [basic_logistics_pipe at (1,1,0)]
     * Default route enabled; dirt (no destination) injected from EAST routes WEST to chest.
     *
     * <p>BASIC_LOGISTICS_PIPE includes NetworkRouterModule, which drops items when no sink is
     * registered in the network. SinkModule.onTick registers the generic sink interest after
     * SYNC_INTERVAL = 20 ticks, so we inject the dirt at tick 22.
     */
    public static void testSinkModuleDefaultRouteAcceptsItems(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(1, 1, 0);
        BlockPos chestPos = new BlockPos(0, 1, 0); // WEST of pipe — only connection

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        placeChargedPowerJunction(context, pipePos.above()); // power the network so routing can pay its cost

        PipeBlockEntity pipe = context.getBlockEntity(pipePos, PipeBlockEntity.class);
        if (pipe == null) {
            context.fail("Basic logistics pipe should have a block entity");
            return;
        }

        // Enable default route directly via NBT (avoids markDirtyAndSync with relative pos)
        PipeContext ctx = new PipeContext(
                context.getLevel(), pipePos, context.getBlockState(pipePos), pipe);
        ctx.saveInt(new SinkModule(5), SinkModule.DEFAULT_ROUTE, 1);

        // Wait for SinkModule.onTick to register the generic sink interest with the network.
        // NetworkRouterModule (which runs before SinkModule) calls network.findSinkFor() and
        // drops the item if the sink is not yet registered. Registration happens after 20 ticks.
        context.runAfterDelay(22, () -> {
            TravelingItem dirt = new TravelingItem(new ItemStack(Items.DIRT), Direction.EAST, 0.1f);
            if (!pipe.forceAddItem(dirt, Direction.EAST)) {
                context.fail("Pipe should accept the force-injected dirt");
            }
        });

        context.succeedWhen(() -> context.assertContainerContains(chestPos, Items.DIRT));
    }

    /**
     * A chassis pipe destroyed by a non-player removal (explosion, /setblock) must still drop its
     * installed modules with configuration intact. The drop runs in
     * {@code PipeBlockEntity.preRemoveSideEffects()}, which fires on every removal path -- not only
     * the player-break path -- so modules are no longer voided by explosions.
     */
    public static void testChassisDropsModulesOnNonPlayerBreak(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.CHASSIS_LOGISTICS_PIPE_MK1);

        PipeBlockEntity pipeEntity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (pipeEntity == null) {
            context.fail("Chassis pipe should have a block entity");
        }

        // Insert a module carrying a marker so we can confirm its configuration survives the break.
        ItemStack module = new ItemStack(LogisticsPipe.ITEM.ITEM_SINK_MODULE);
        CompoundTag marker = new CompoundTag();
        marker.putString("qa_marker", "kept");
        module.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));

        ChassisInventory inventory = new ChassisInventory(pipeEntity);
        inventory.setItem(0, module);

        // Remove the block via a non-player path (the same removal path explosions use).
        BlockPos absPos = context.absolutePos(pos);
        ServerLevel level = context.getLevel();
        level.destroyBlock(absPos, false);

        List<ItemEntity> drops = level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absPos).inflate(2.0),
                e -> e.getItem().is(LogisticsPipe.ITEM.ITEM_SINK_MODULE));

        if (drops.size() != 1) {
            context.fail("Expected exactly one module to drop on break, found " + drops.size());
        }

        ItemStack dropped = drops.get(0).getItem();
        CustomData data = dropped.get(DataComponents.CUSTOM_DATA);
        if (data == null || !"kept".equals(NbtCompat.getString(data.copyTag(), "qa_marker", ""))) {
            context.fail("Dropped module lost its configuration on break");
        }

        context.succeed();
    }
}
