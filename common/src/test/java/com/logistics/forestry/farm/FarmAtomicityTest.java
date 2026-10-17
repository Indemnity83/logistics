package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.TreeFeller;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every world-changing action checks first, changes the world, and charges only once the change
 * has landed: a refused or failed change costs nothing and loses nothing.
 */
@DisplayName("Farm actions: all or nothing")
class FarmAtomicityTest extends MinecraftTestEnvironment {

    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
    private static final BlockState SAPLING = Blocks.OAK_SAPLING.defaultBlockState();
    private static final BlockState WET_FARMLAND = Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7);
    private static final int RF = 10_000;

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static long cost(FarmCost cost) {
        return FarmHarness.SETTINGS.cost(cost);
    }

    private static ScriptedFarmLogic felling() {
        ScriptedFarmLogic logic = new ScriptedFarmLogic().script((world, plot) -> {
            FellPlan tree = world.fellPlan(plot, TreeFeller.Limits.around(plot, 4));
            return tree.isFound() ? new PlotAction.Harvest(tree.blocks()) : PlotAction.NONE;
        });
        logic.ownGrowth = state -> state.is(Blocks.OAK_LOG) || state.is(Blocks.OAK_LEAVES);
        return logic;
    }

    /** A trunk of {@code logs} on {@code base} under a single leaf. */
    private static List<BlockPos> tree(FarmHarness farm, BlockPos base, int logs) {
        farm.world.trees = FakeFarmWorld.OAK_TREES;
        List<BlockPos> blocks = new ArrayList<>();
        blocks.add(base.above(logs));
        farm.world.set(base.above(logs), LEAVES);
        for (int dy = logs - 1; dy >= 0; dy--) {
            farm.world.set(base.above(dy), LOG);
            blocks.add(base.above(dy));
        }
        return blocks;
    }

    private static List<ItemStack> outputAndOverflow(FarmHarness farm) {
        List<ItemStack> stacks = new ArrayList<>(farm.farm().overflow());
        for (int slot = FarmMachine.OUTPUT_START; slot < FarmMachine.OUTPUT_END; slot++) {
            stacks.add(farm.items().getItem(slot));
        }
        return stacks;
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    // ==================== Harvests ====================

    @Test
    @DisplayName("a felling that runs out of energy stops cleanly, leaves the rest standing, and resumes with energy")
    void energyRunsOutMidTree() {
        FarmHarness farm = new FarmHarness(felling());
        BlockPos base = farm.plot(0);
        List<BlockPos> blocks = tree(farm, base, 6);
        farm.power(cost(FarmCost.HARVEST_LEAVES) + 2 * cost(FarmCost.HARVEST_LOG) + 1);

        farm.ticks(40);

        assertThat(farm.world.broken).containsExactlyElementsOf(blocks.subList(0, 3));
        blocks.subList(3, blocks.size()).forEach(pos -> assertThat(farm.world.blocks).containsEntry(pos, LOG));
        assertThat(farm.energy()).isEqualTo(1);
        assertThat(farm.farm().isHarvesting()).isTrue();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_POWER);

        farm.power(RF);
        farm.ticks(40);

        assertThat(farm.world.broken).containsExactlyElementsOf(blocks);
        assertThat(farm.energy()).isEqualTo(RF - 4 * cost(FarmCost.HARVEST_LOG));
    }

    @Test
    @DisplayName("when the output fills mid-felling the last drops wait in the overflow and the felling stalls; nothing is voided")
    void outputFillsMidHarvest() {
        FarmHarness farm = new FarmHarness(felling());
        farm.world.drops = state -> List.of(new ItemStack(Items.STICK, 40));
        BlockPos base = farm.plot(0);
        List<BlockPos> blocks = tree(farm, base, 6);
        for (int slot = FarmMachine.OUTPUT_START; slot < FarmMachine.OUTPUT_END - 1; slot++) {
            farm.items().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        farm.power(RF);

        farm.ticks(40);

        assertThat(farm.world.broken).hasSize(2);
        assertThat(count(outputAndOverflow(farm), Items.STICK)).isEqualTo(80);
        assertThat(farm.farm().overflow()).isNotEmpty();
        assertThat(farm.farm().isHarvesting()).isTrue();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.OUTPUT_FULL);
        blocks.subList(2, blocks.size()).forEach(pos -> assertThat(farm.world.blocks).containsKey(pos));

        int taken = 0;
        for (int round = 0; round < 20 && farm.farm().isHarvesting(); round++) {
            ItemStack last = farm.items().getItem(FarmMachine.OUTPUT_END - 1);
            taken += last.is(Items.STICK) ? last.getCount() : 0;
            farm.items().setItem(FarmMachine.OUTPUT_END - 1, ItemStack.EMPTY);
            farm.ticks(10);
        }

        assertThat(farm.world.broken).containsExactlyElementsOf(blocks);
        assertThat(taken + count(outputAndOverflow(farm), Items.STICK)).isEqualTo(40 * blocks.size());
    }

    @Test
    @DisplayName("blocks claim protection refuses are left standing and cost nothing; the rest of the tree is felled")
    void refusedTargetsAreNotCharged() {
        FarmHarness farm = new FarmHarness(felling());
        BlockPos base = farm.plot(0);
        List<BlockPos> blocks = tree(farm, base, 6);
        farm.world.claimed.add(blocks.get(2));
        farm.world.refuseBreak = blocks.get(4)::equals;
        farm.power(RF);

        farm.ticks(40);

        assertThat(farm.world.broken).doesNotContain(blocks.get(2), blocks.get(4)).hasSize(blocks.size() - 2);
        assertThat(farm.world.blocks).containsEntry(blocks.get(2), LOG).containsEntry(blocks.get(4), LOG);
        assertThat(farm.energy())
                .isEqualTo(RF - cost(FarmCost.HARVEST_LEAVES) - (blocks.size() - 3) * cost(FarmCost.HARVEST_LOG));
        assertThat(farm.farm().isHarvesting()).isFalse();
    }

    @Test
    @DisplayName("a crop whose break is refused yields nothing and costs nothing")
    void refusedCropBreak() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic()
                .script((world, plot) -> world.state(plot).is(Blocks.WHEAT) ? PlotAction.harvest(plot) : PlotAction.NONE));
        farm.world.set(farm.plot(0), Blocks.WHEAT.defaultBlockState());
        farm.world.refuseBreak = farm.plot(0)::equals;
        farm.power(RF);

        farm.ticks(3);

        assertThat(farm.world.blocks).containsEntry(farm.plot(0), Blocks.WHEAT.defaultBlockState());
        assertThat(farm.energy()).isEqualTo(RF);
        assertThat(outputAndOverflow(farm)).allMatch(ItemStack::isEmpty);
        assertThat(farm.farm().isHarvesting()).isFalse();
    }

    // ==================== Planting ====================

    @Test
    @DisplayName("a 2x2 group whose fourth cell refuses its sapling plants nothing and spends nothing")
    void groupIsAllOrNothing() {
        FarmHarness probe = new FarmHarness(new ScriptedFarmLogic());
        List<BlockPos> group = List.of(probe.plot(0), probe.plot(1), probe.plot(0).north(), probe.plot(1).north());
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) -> plot.equals(group.get(0))
                ? new PlotAction.Plant(group, new ItemStack(Items.OAK_SAPLING))
                : PlotAction.NONE));
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 4));
        farm.world.refuseSet = (pos, state) -> pos.equals(group.get(3)) && state.is(Blocks.OAK_SAPLING);
        farm.power(RF);

        farm.tick();

        group.forEach(pos -> assertThat(farm.world.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()).isAir())
                .as("%s", pos)
                .isTrue());
        assertThat(farm.items().getItem(0).getCount()).isEqualTo(4);
        assertThat(farm.energy()).isEqualTo(RF);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.BLOCKED);

        farm.world.refuseSet = (pos, state) -> false;
        farm.ticks(40); // round the ring again, past the idle back-off
        group.forEach(pos -> assertThat(farm.world.blocks).containsEntry(pos, SAPLING));
        assertThat(farm.items().getItem(0).isEmpty()).isTrue();
        assertThat(farm.energy()).isEqualTo(RF - 4 * cost(FarmCost.PLANT));
    }

    // ==================== Soil, tilling, clearing ====================

    @Test
    @DisplayName("soil refused after the floor was broken: the break is charged and its drops kept, no soil or soil RF spent")
    void soilPlaceFailsAfterBreak() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(false, true).script((world, plot) ->
                world.state(plot.below()).is(Blocks.PODZOL) ? PlotAction.NONE : new PlotAction.PlaceSoil(plot.below())));
        BlockPos floor = farm.plot(0).below();
        for (int i = 1; i < 8; i++) {
            farm.world.set(farm.plot(i).below(), Blocks.PODZOL.defaultBlockState());
        }
        farm.world.set(floor, Blocks.STONE.defaultBlockState());
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
        farm.world.refuseSet = (pos, state) -> pos.equals(floor) && state.is(Blocks.PODZOL);
        farm.power(RF);

        farm.tick();

        assertThat(farm.world.broken).containsExactly(floor);
        assertThat(farm.world.blocks).doesNotContainKey(floor);
        assertThat(count(outputAndOverflow(farm), Items.STONE)).isEqualTo(1);
        assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).isEqualTo(2);
        assertThat(farm.energy()).isEqualTo(RF - cost(FarmCost.CLEAR));

        farm.world.refuseSet = (pos, state) -> false;
        farm.ticks(8);
        assertThat(farm.world.blocks).as("laid on a later pass").containsEntry(floor, Blocks.PODZOL.defaultBlockState());
        assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).isEqualTo(1);
        assertThat(farm.energy()).isEqualTo(RF - cost(FarmCost.CLEAR) - cost(FarmCost.SOIL));
    }

    @Test
    @DisplayName("soil that cannot pay for the break and the soil both breaks nothing")
    void soilNeedsEnergyForBothSteps() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(false, true).script((world, plot) ->
                world.state(plot.below()).is(Blocks.PODZOL) ? PlotAction.NONE : new PlotAction.PlaceSoil(plot.below())));
        BlockPos floor = farm.plot(0).below();
        farm.world.set(floor, Blocks.STONE.defaultBlockState());
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
        farm.power(cost(FarmCost.SOIL));

        farm.tick();

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.energy()).isEqualTo(cost(FarmCost.SOIL));
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_POWER);
    }

    private static ScriptedFarmLogic tillingDirt() {
        return new ScriptedFarmLogic(true, false).script((world, plot) ->
                world.state(plot.below()).is(Blocks.DIRT) ? new PlotAction.Till(plot.below(), WET_FARMLAND) : PlotAction.NONE);
    }

    private static void fillWater(FarmHarness farm, long mb) {
        farm.machine.water().tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(mb), false);
    }

    private static long waterMb(FarmHarness farm) {
        return FluidUnits.toMillibuckets(farm.machine.water().tank().getAmount());
    }

    @Test
    @DisplayName("a till or a drain the world refuses spends no RF and no water")
    void refusedWritesCostNothing() {
        FarmHarness farm = new FarmHarness(tillingDirt());
        farm.world.set(farm.plot(0).below(), Blocks.DIRT.defaultBlockState());
        fillWater(farm, 1_000);
        farm.world.refuseSet = (pos, state) -> true;
        farm.power(RF);
        farm.tick();
        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), Blocks.DIRT.defaultBlockState());
        assertThat(waterMb(farm)).isEqualTo(1_000);
        assertThat(farm.energy()).isEqualTo(RF);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.BLOCKED);

        FarmHarness drain = new FarmHarness(new ScriptedFarmLogic());
        drain.world.set(drain.plot(0), Blocks.WATER.defaultBlockState());
        drain.world.refuseSet = (pos, state) -> true;
        drain.power(RF);
        drain.tick();
        assertThat(drain.world.blocks).containsEntry(drain.plot(0), Blocks.WATER.defaultBlockState());
        assertThat(drain.energy()).isEqualTo(RF);

        FarmHarness clear = new FarmHarness(new ScriptedFarmLogic());
        clear.world.set(clear.plot(0), Blocks.COBBLESTONE.defaultBlockState());
        clear.world.refuseBreak = pos -> true;
        clear.power(RF);
        clear.tick();
        assertThat(clear.world.blocks).containsEntry(clear.plot(0), Blocks.COBBLESTONE.defaultBlockState());
        assertThat(clear.energy()).isEqualTo(RF);
        assertThat(outputAndOverflow(clear)).allMatch(ItemStack::isEmpty);
    }

    // ==================== Conservation ====================

    /**
     * Over a seeded random run of every action against a world that refuses writes and breaks at
     * random, the RF, water and items the farm spends and gains balance exactly against what the
     * world reports it changed.
     */
    @Test
    @DisplayName("conservation: RF, water and items in and out balance exactly against the world's changes")
    void conservation() {
        long rollbacks = 0;
        for (long seed = 1; seed <= 12; seed++) {
            rollbacks += conservationRun(seed);
        }
        assertThat(rollbacks).as("refused groups rolled back").isPositive();
    }

    private static final Item[] GERMLINGS = {Items.OAK_SAPLING, Items.WHEAT_SEEDS};

    /** One seeded run; returns how many planted cells were rolled back. */
    private static long conservationRun(long seed) {
        Random random = new Random(seed);
        ScriptedFarmLogic logic = new ScriptedFarmLogic(true, true).script((world, plot) -> {
            BlockState state = world.state(plot);
            BlockState soil = world.state(plot.below());
            if (state.is(Blocks.WHEAT) || state.is(Blocks.OAK_LOG)) {
                List<BlockPos> blocks = new ArrayList<>();
                for (int dy = 2; dy >= 0; dy--) {
                    if (world.state(plot.above(dy)).is(Blocks.OAK_LOG) || dy == 0) {
                        blocks.add(plot.above(dy));
                    }
                }
                return new PlotAction.Harvest(blocks);
            }
            if (soil.is(Blocks.STONE) || soil.isAir()) {
                return new PlotAction.PlaceSoil(plot.below());
            }
            if (soil.is(Blocks.DIRT)) {
                return new PlotAction.Till(plot.below(), WET_FARMLAND);
            }
            if (soil.is(Blocks.FARMLAND) && state.isAir()) {
                boolean pair = (plot.getX() + plot.getZ()) % 2 == 0;
                List<BlockPos> cells = pair ? List.of(plot, plot.above(4)) : List.of(plot);
                return new PlotAction.Plant(cells, new ItemStack((plot.getX() & 1) == 0 ? Items.OAK_SAPLING : Items.WHEAT_SEEDS));
            }
            return PlotAction.NONE;
        });
        logic.clearanceHeight = 2;
        logic.afterHarvest = soil -> soil.is(Blocks.FARMLAND) ? Blocks.DIRT.defaultBlockState() : null;
        FarmHarness farm = new FarmHarness(logic);
        farm.areaOverride(1, 5);
        FakeFarmWorld world = farm.world;
        List<BlockPos> plots = new ArrayList<>();
        for (int i = 0; i < farm.area().plotCount(); i++) {
            plots.add(farm.plot(i));
        }
        BlockState[] soils = {Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), WET_FARMLAND,
            Blocks.FARMLAND.defaultBlockState(), Blocks.AIR.defaultBlockState()};
        BlockState[] tops = {Blocks.AIR.defaultBlockState(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7),
            LOG, Blocks.COBBLESTONE.defaultBlockState(), Blocks.WATER.defaultBlockState()};

        long rollbacks = 0;
        for (int tick = 0; tick < 600; tick++) {
            // The world moves on its own between ticks, never through the farm's writes.
            for (int n = random.nextInt(3); n > 0; n--) {
                BlockPos plot = plots.get(random.nextInt(plots.size()));
                world.set(plot.below(), soils[random.nextInt(soils.length)]);
                world.set(plot, tops[random.nextInt(tops.length)]);
                if (random.nextInt(4) == 0) {
                    world.set(plot.above(), random.nextBoolean() ? LOG : Blocks.COBBLESTONE.defaultBlockState());
                }
            }
            Set<BlockPos> refusedSets = new HashSet<>();
            Set<BlockPos> refusedBreaks = new HashSet<>();
            for (BlockPos plot : plots) {
                for (int dy = -1; dy <= 4; dy++) {
                    if (random.nextInt(6) == 0) {
                        refusedSets.add(plot.above(dy));
                    }
                    if (random.nextInt(8) == 0) {
                        refusedBreaks.add(plot.above(dy));
                    }
                }
            }
            // Clearing a cell just written always lands, as it does in a live level within a tick.
            world.refuseSet = (pos, state) -> refusedSets.contains(pos) && !state.isAir();
            world.refuseBreak = refusedBreaks::contains;
            world.claimed.clear();
            if (random.nextInt(5) == 0) {
                world.claimed.add(plots.get(random.nextInt(plots.size())));
            }
            if (random.nextInt(10) == 0) {
                farm.power(random.nextInt(200));
            } else if (random.nextInt(10) == 0) {
                farm.power(RF);
            }
            if (random.nextInt(15) == 0) {
                fillWater(farm, random.nextInt(600));
            }
            if (random.nextInt(10) == 0) {
                farm.items().setItem(random.nextInt(4), new ItemStack(GERMLINGS[random.nextInt(2)], random.nextInt(5)));
                farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, random.nextInt(4)));
            }
            if (random.nextInt(20) == 0) {
                for (int slot = FarmMachine.OUTPUT_START; slot < FarmMachine.OUTPUT_END; slot++) {
                    farm.items().setItem(slot, ItemStack.EMPTY);
                }
            }

            long energy = farm.energy();
            long water = waterMb(farm);
            Map<Item, Integer> items = inventory(farm);
            int mutations = world.mutations.size();

            farm.tick();

            List<FakeFarmWorld.Mutation> changes = world.mutations.subList(mutations, world.mutations.size());
            rollbacks += changes.stream().filter(change -> unplacedItem(change) != null).count();
            long expectedRf = 0;
            Map<Item, Integer> expectedItems = new HashMap<>(items);
            for (FakeFarmWorld.Mutation change : changes) {
                expectedRf += rf(change);
                change.drops().forEach(drop -> expectedItems.merge(drop.getItem(), drop.getCount(), Integer::sum));
                Item placed = placedItem(change);
                if (placed != null) {
                    expectedItems.merge(placed, -1, Integer::sum);
                }
                Item unplaced = unplacedItem(change);
                if (unplaced != null) {
                    expectedItems.merge(unplaced, 1, Integer::sum);
                }
            }
            String context = "seed " + seed + ", tick " + tick + ", changes " + changes;
            assertThat(energy - farm.energy()).as("RF, %s", context).isEqualTo(expectedRf);
            assertThat(waterMb(farm)).as("working never spends water, %s", context).isEqualTo(water);
            expectedItems.values().removeIf(count -> count == 0);
            assertThat(inventory(farm)).as("items, %s", context).isEqualTo(expectedItems);
        }
        // Not vacuous: every kind of change happened.
        List<FakeFarmWorld.Mutation> all = world.mutations;
        assertThat(all).as("seed %d", seed)
                .anyMatch(m -> m.after() == null && m.before().is(Blocks.WHEAT))
                .anyMatch(m -> m.after() == null && m.before().is(Blocks.OAK_LOG))
                .anyMatch(m -> m.after() == null && m.before().is(Blocks.COBBLESTONE))
                .anyMatch(m -> m.after() == null && m.before().is(Blocks.STONE))
                .anyMatch(m -> m.after() != null && m.after().is(Blocks.PODZOL))
                .anyMatch(m -> m.after() != null && m.after().is(Blocks.FARMLAND) && m.before().is(Blocks.DIRT))
                .anyMatch(m -> m.after() != null && m.after().is(Blocks.OAK_SAPLING))
                .anyMatch(m -> m.after() != null && m.after().is(Blocks.WHEAT));
        return rollbacks;
    }

    /** Every item in the farm's slots and overflow. */
    private static Map<Item, Integer> inventory(FarmHarness farm) {
        Map<Item, Integer> items = new HashMap<>();
        for (int slot = 0; slot < farm.items().getContainerSize(); slot++) {
            ItemStack stack = farm.items().getItem(slot);
            if (!stack.isEmpty()) {
                items.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        for (ItemStack stack : farm.farm().overflow()) {
            items.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        items.values().removeIf(count -> count == 0);
        return items;
    }

    /**
     * What the farm owes for one change, read from the change alone. A planted cell cleared again
     * is a rolled-back group: its PLANT comes back. Farmland back to dirt is a harvest's free soil
     * restoration.
     */
    private static long rf(FakeFarmWorld.Mutation change) {
        BlockState before = change.before();
        BlockState after = change.after();
        if (after == null) {
            if (before.is(Blocks.WHEAT)) {
                return cost(FarmCost.HARVEST_CROP);
            }
            return before.is(Blocks.OAK_LOG) && change.pos().getY() >= 1 && !before.is(Blocks.COBBLESTONE)
                    ? harvestOrClear(change)
                    : cost(FarmCost.CLEAR);
        }
        if (after.is(Blocks.OAK_SAPLING) || after.is(Blocks.WHEAT)) {
            return cost(FarmCost.PLANT);
        }
        if (after.isAir()) {
            if (before.is(Blocks.OAK_SAPLING) || before.is(Blocks.WHEAT)) {
                return -cost(FarmCost.PLANT);
            }
            return cost(FarmCost.CLEAR); // drained
        }
        if (after.is(Blocks.PODZOL)) {
            return cost(FarmCost.SOIL);
        }
        if (after.is(Blocks.FARMLAND)) {
            return cost(FarmCost.TILL);
        }
        if (after.is(Blocks.DIRT) && before.is(Blocks.FARMLAND)) {
            return 0;
        }
        throw new AssertionError("unexpected change " + change);
    }

    /** Logs are the farm's own growth: broken by a harvest, never cleared. */
    private static long harvestOrClear(FakeFarmWorld.Mutation change) {
        return cost(FarmCost.HARVEST_LOG);
    }

    private static Item placedItem(FakeFarmWorld.Mutation change) {
        if (change.after() == null) {
            return null;
        }
        if (change.after().is(Blocks.OAK_SAPLING)) {
            return Items.OAK_SAPLING;
        }
        if (change.after().is(Blocks.WHEAT)) {
            return Items.WHEAT_SEEDS;
        }
        if (change.after().is(Blocks.PODZOL)) {
            return Items.PODZOL;
        }
        return null;
    }

    private static Item unplacedItem(FakeFarmWorld.Mutation change) {
        if (change.after() == null || !change.after().isAir()) {
            return null;
        }
        if (change.before().is(Blocks.OAK_SAPLING)) {
            return Items.OAK_SAPLING;
        }
        if (change.before().is(Blocks.WHEAT)) {
            return Items.WHEAT_SEEDS;
        }
        return null;
    }
}
