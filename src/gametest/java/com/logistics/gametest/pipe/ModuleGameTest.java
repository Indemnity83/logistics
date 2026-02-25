package com.logistics.gametest.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ItemFilterModule;
import com.logistics.pipe.modules.MergerModule;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Game tests for pipe modules.
 * Tests routing logic, filter matching, extraction timing, and energy calculations.
 */
public class ModuleGameTest {

    /**
     * Test that filter pipes can be placed and have block entities.
     */
    @FabricGameTest
    public void testFilterPipePlacement(GameTestHelper context) {
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
    @FabricGameTest
    public void testExtractorPipePlacement(GameTestHelper context) {
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
    @FabricGameTest
    public void testMergerPipePlacement(GameTestHelper context) {
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
    @FabricGameTest
    public void testInsertionPipePlacement(GameTestHelper context) {
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
    @FabricGameTest
    public void testVoidPipePlacement(GameTestHelper context) {
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
    @FabricGameTest
    public void testPassthroughPipePlacement(GameTestHelper context) {
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
    @FabricGameTest
    public void testGoldTransportPipePlacement(GameTestHelper context) {
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
    @FabricGameTest
    public void testFilterModuleRoutesMatchingItems(GameTestHelper context) {
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

        Pipe pipe = ctx.pipe();
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
    @FabricGameTest
    public void testFilterModulePassesThroughNonMatching(GameTestHelper context) {
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
    @FabricGameTest
    public void testMergerModuleRoutesToOutput(GameTestHelper context) {
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
    @FabricGameTest
    public void testFilterModuleMultipleSideFilters(GameTestHelper context) {
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
}
