package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.MachineHudModel;
import com.logistics.forestry.farm.crop.CropTendLogic;
import com.logistics.forestry.farm.orchard.OrchardFruit;
import com.logistics.forestry.farm.orchard.OrchardTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Orchard picking apples off trees in its Reach")
class OrchardTendTest extends MinecraftTestEnvironment {

    private static final BlockPos BASE = new BlockPos(2, 0, 1);

    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    /** Natural leaves one step from a log, as a grown tree's are. */
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
    private static final BlockState PLACED_LEAVES = LEAVES.setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState DETACHED_LEAVES =
            LEAVES.setValue(LeavesBlock.DISTANCE, LeavesBlock.DECAY_DISTANCE);

    /** Every random tick picks: a fruit that always comes up. */
    private static final double ALWAYS = 1.0;

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** Oak leaves bear apples; tags are not bound in unit tests. */
    private static OrchardFruit apple(double chance) {
        return fruit("apple", Items.APPLE, chance);
    }

    private static OrchardFruit fruit(String name, Item item, double chance) {
        return new OrchardFruit(
                ResourceId.in("logistics", name), "minecraft:oak_leaves", state -> state.is(Blocks.OAK_LEAVES), item, chance);
    }

    private static OrchardTendLogic logic(OrchardFruit... fruits) {
        return new OrchardTendLogic(() -> List.of(fruits));
    }

    /** An Orchard of {@code fruits}, scanning its Reach quickly, with no random ticks until a test asks. */
    private static FarmHarness farm(FarmSettings settings, OrchardFruit... fruits) {
        FarmHarness farm = FarmHarness.tending(logic(fruits), settings);
        farm.world.trees = FakeFarmWorld.OAK_TREES;
        farm.world.randomTickSpeed = 0;
        farm.engine().setRateOverride(2_000, 20);
        farm.power(10_000);
        return farm;
    }

    private static FarmHarness farm(OrchardFruit... fruits) {
        return farm(FarmHarness.SETTINGS, fruits);
    }

    /** Long enough for passes over the whole Reach, trees and all, at the test's scan rate. */
    private static void scan(FarmHarness farm) {
        farm.ticks(200);
    }

    /**
     * A small oak: a straight trunk of {@code height} logs from {@code base} up on grass, under a
     * canopy of radius 2 on its top two logs and radius 1 above them. Returns its leaves.
     */
    private static List<BlockPos> tree(FakeFarmWorld world, BlockPos base, int height, BlockState leaves) {
        world.set(base.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        for (int y = 0; y < height; y++) {
            world.set(base.above(y), LOG);
        }
        List<BlockPos> canopy = new ArrayList<>();
        for (int y = height - 2; y <= height; y++) {
            int radius = y < height ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = base.offset(dx, y, dz);
                    if (!world.blocks.containsKey(pos)) {
                        world.set(pos, leaves);
                        canopy.add(pos);
                    }
                }
            }
        }
        return canopy;
    }

    private static int count(Container items, Item item) {
        int total = 0;
        for (int slot = 0; slot < items.getContainerSize(); slot++) {
            ItemStack stack = items.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // ==================== Trees ====================

    @Test
    void takesOnATreeWhoseTrunkBaseIsInTheReachWithoutTouchingIt() {
        FarmHarness farm = farm(apple(ALWAYS));
        tree(farm.world, BASE, 5, LEAVES);

        scan(farm);

        assertThat(farm.engine().pickPlotsTotal()).isEqualTo(1);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
        assertThat(farm.world.mutations).isEmpty();
    }

    @Test
    void findsATreeFromAnyLogOfItsTrunkAsItsBase() {
        FakeFarmWorld world = new FakeFarmWorld();
        world.trees = FakeFarmWorld.OAK_TREES;
        tree(world, BASE, 5, LEAVES);

        assertThat(logic(apple(ALWAYS)).pickPlot(world, BASE.above(4))).isEqualTo(BASE);
        assertThat(logic(apple(ALWAYS)).pickPlot(world, BASE.above(6))).as("leaves are no trunk").isNull();
    }

    @Test
    void keepsSideBySideTrunksAsOneTree() {
        FakeFarmWorld world = new FakeFarmWorld();
        world.trees = FakeFarmWorld.OAK_TREES;
        tree(world, BASE, 5, LEAVES);
        for (int y = 0; y < 5; y++) {
            world.set(BASE.east().above(y), LOG);
        }

        OrchardTendLogic logic = logic(apple(ALWAYS));
        assertThat(logic.pickPlot(world, BASE.east())).isEqualTo(BASE);
        assertThat(logic.bearing(world, BASE.east())).as("only the westmost base is the Plot").isNull();
        assertThat(logic.bearing(world, BASE)).isNotNull();
    }

    @Test
    void ignoresATreeWhoseTrunkBaseIsAboveTheReachLowestLevels() {
        FarmHarness farm = farm(apple(ALWAYS));
        // In the Reach, which runs 7 above the farm, but rooted 2 above it.
        tree(farm.world, BASE.above(2), 4, LEAVES);

        scan(farm);

        assertThat(farm.engine().pickPlotsTotal()).isZero();
    }

    @Test
    void ignoresCanopyOfATreeRootedOutsideTheReach() {
        FarmHarness farm = farm(apple(ALWAYS));
        // Trunk 4 out, its canopy reaching 2 into the Reach.
        tree(farm.world, new BlockPos(5, 0, 0), 5, LEAVES);

        scan(farm);

        assertThat(farm.engine().pickPlotsTotal()).isZero();
    }

    @Test
    void countsAllOfTheTreesCanopyEvenBeyondTheReach() {
        FakeFarmWorld world = new FakeFarmWorld();
        world.trees = FakeFarmWorld.OAK_TREES;
        // Leaves up to y 10, and out to x 4, past the Reach's 7 up and 3 out.
        List<BlockPos> canopy = tree(world, BASE, 9, LEAVES);

        TendLogic.Bearing bearing = logic(apple(ALWAYS)).bearing(world, BASE);

        assertThat(bearing).isNotNull();
        assertThat(bearing.sources()).containsExactlyInAnyOrderElementsOf(canopy);
        assertThat(bearing.sources()).anyMatch(pos -> pos.getY() > OrchardTendLogic.REACH_ABOVE);
        assertThat(bearing.sources()).anyMatch(pos -> pos.getX() > TendFarmComponent.REACH);
    }

    @Test
    void leavesLeavesNearerAnotherTreesWoodToThatTree() {
        FakeFarmWorld world = new FakeFarmWorld();
        world.trees = FakeFarmWorld.OAK_TREES;
        tree(world, BASE, 5, LEAVES);
        // A neighbour three east: the leaves between them at x 4 are a step from its trunk, two from ours.
        BlockPos neighbour = BASE.east(3);
        tree(world, neighbour, 5, LEAVES);

        TendLogic.Bearing bearing = logic(apple(ALWAYS)).bearing(world, BASE);

        assertThat(bearing).isNotNull();
        assertThat(bearing.sources()).noneMatch(pos -> pos.getX() >= neighbour.getX() - 1);
    }

    @Test
    void ignoresTreesWithPlacedOrDetachedLeaves() {
        OrchardTendLogic logic = logic(apple(ALWAYS));
        for (BlockState leaves : List.of(PLACED_LEAVES, DETACHED_LEAVES)) {
            FakeFarmWorld world = new FakeFarmWorld();
            world.trees = FakeFarmWorld.OAK_TREES;
            tree(world, BASE, 5, leaves);

            assertThat(logic.pickPlot(world, BASE)).as("%s", leaves).isNull();
            assertThat(logic.bearing(world, BASE)).as("%s", leaves).isNull();
        }
    }

    @Test
    void ignoresTreesWhoseLeavesBearNoFruit() {
        FakeFarmWorld world = new FakeFarmWorld();
        world.trees = FakeFarmWorld.OAK_TREES;
        tree(world, BASE, 5, LEAVES);
        OrchardFruit birchOnly = new OrchardFruit(
                ResourceId.in("logistics", "pear"), "minecraft:birch_leaves", state -> state.is(Blocks.BIRCH_LEAVES),
                Items.APPLE, ALWAYS);

        assertThat(logic(birchOnly).pickPlot(world, BASE)).isNull();
        assertThat(logic().pickPlot(world, BASE)).as("no fruit loaded").isNull();
    }

    @Test
    void firstFruitInLoadOrderWins() {
        FakeFarmWorld world = new FakeFarmWorld();
        world.trees = FakeFarmWorld.OAK_TREES;
        tree(world, BASE, 5, LEAVES);

        TendLogic.Bearing bearing =
                logic(fruit("golden", Items.GOLDEN_APPLE, 0.5), apple(ALWAYS)).bearing(world, BASE);

        assertThat(bearing).isNotNull();
        assertThat(bearing.produce().getItem()).isEqualTo(Items.GOLDEN_APPLE);
        assertThat(bearing.chance()).isEqualTo(0.5);
        assertThat(bearing.cost()).isEqualTo(FarmCost.HARVEST_CROP);
    }

    @Test
    void forgetsATreeOnceItIsFelledOrBare() {
        FarmHarness farm = farm(apple(ALWAYS));
        List<BlockPos> canopy = tree(farm.world, BASE, 5, LEAVES);
        scan(farm);
        assertThat(farm.engine().pickPlotsTotal()).isEqualTo(1);

        canopy.forEach(pos -> farm.world.set(pos, Blocks.AIR.defaultBlockState()));
        farm.world.randomTickSpeed = TendFarmComponent.RANDOM_TICK_BLOCKS;
        farm.tick();

        assertThat(farm.engine().pickPlotsTotal()).isZero();
        assertThat(count(farm.items(), Items.APPLE)).isZero();
    }

    @Test
    void forgetsATreeTheScanFindsGone() {
        FarmHarness farm = farm(apple(ALWAYS));
        tree(farm.world, BASE, 5, LEAVES);
        scan(farm);

        for (int y = 0; y < 5; y++) {
            farm.world.set(BASE.above(y), Blocks.AIR.defaultBlockState());
        }
        scan(farm);

        assertThat(farm.engine().pickPlotsTotal()).isZero();
    }

    // ==================== Ripening ====================

    @Test
    void picksOneAppleForEachSuccessfulRollAndChargesForIt() {
        FarmHarness farm = farm(apple(ALWAYS));
        List<BlockPos> canopy = tree(farm.world, BASE, 5, LEAVES);
        scan(farm);
        long before = farm.energy();

        farm.world.randomTickSpeed = TendFarmComponent.RANDOM_TICK_BLOCKS;
        farm.ticks(10);

        assertThat(count(farm.items(), Items.APPLE)).isEqualTo(10);
        assertThat(before - farm.energy()).isEqualTo(10 * farm.cost(FarmCost.HARVEST_CROP));
        assertThat(farm.world.rustled).hasSize(10).allMatch(canopy::contains);
        assertThat(farm.world.mutations).as("nothing is ever broken").isEmpty();
    }

    @Test
    void ripensNothingAtARandomTickSpeedOfZero() {
        FarmHarness farm = farm(apple(ALWAYS));
        tree(farm.world, BASE, 5, LEAVES);
        scan(farm);

        farm.ticks(5_000);

        assertThat(farm.engine().pickPlotsTotal()).isEqualTo(1);
        assertThat(count(farm.items(), Items.APPLE)).isZero();
    }

    @Test
    void rollsARandomTickAtTheTickSpeedThenTheFruitsChance() {
        FarmHarness farm = farm(apple(0.02));
        tree(farm.world, BASE, 5, LEAVES);
        scan(farm);
        ScriptedRandom random = new ScriptedRandom();
        farm.host.random = random;
        farm.world.randomTickSpeed = 3;

        // A random tick (2 < 3) whose roll comes up (0.019 < 0.02): one apple.
        random.ints.add(2);
        random.doubles.add(0.019);
        farm.tick();
        assertThat(count(farm.items(), Items.APPLE)).isEqualTo(1);

        // A random tick whose roll misses.
        random.ints.add(2);
        random.doubles.add(0.021);
        farm.tick();
        // No random tick (3 is not under 3): the fruit is not even rolled.
        random.ints.add(3);
        random.doubles.add(0.0);
        farm.tick();

        assertThat(count(farm.items(), Items.APPLE)).isEqualTo(1);
        assertThat(random.doubles).as("no roll without a random tick").containsExactly(0.0);
    }

    @Test
    void picksNothingItCannotPayFor() {
        FarmHarness farm = farm(apple(ALWAYS));
        tree(farm.world, BASE, 5, LEAVES);
        scan(farm);
        farm.power(farm.cost(FarmCost.HARVEST_CROP) - 1);

        farm.world.randomTickSpeed = TendFarmComponent.RANDOM_TICK_BLOCKS;
        farm.ticks(5);

        assertThat(count(farm.items(), Items.APPLE)).isZero();
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.NO_POWER);
    }

    @Test
    void picksNothingWhilePaused() {
        FarmHarness farm = farm(apple(ALWAYS));
        tree(farm.world, BASE, 5, LEAVES);
        scan(farm);
        farm.host.workEnabled = false;

        farm.world.randomTickSpeed = TendFarmComponent.RANDOM_TICK_BLOCKS;
        farm.ticks(5);

        assertThat(count(farm.items(), Items.APPLE)).isZero();
    }

    @Test
    void spillsAtTheTrunkBaseWhenTheInventoryIsFull() {
        FarmHarness farm = farm(apple(ALWAYS));
        tree(farm.world, BASE, 5, LEAVES);
        scan(farm);
        for (int slot = 0; slot < farm.items().getContainerSize(); slot++) {
            farm.items().setItem(slot, new ItemStack(Items.DIRT, 64));
        }

        farm.world.randomTickSpeed = TendFarmComponent.RANDOM_TICK_BLOCKS;
        farm.tick();

        assertThat(farm.world.spilled).hasSize(1);
        assertThat(farm.world.spilled.get(0).pos()).isEqualTo(BASE);
        assertThat(farm.world.spilled.get(0).stack().is(Items.APPLE)).isTrue();
    }

    @Test
    void picksForTheTestOverrideEveryTick() {
        FarmHarness farm = farm(apple(0.02));
        tree(farm.world, BASE, 5, LEAVES);
        scan(farm);

        farm.engine().setPickOverride(1.0);
        farm.ticks(3);

        assertThat(count(farm.items(), Items.APPLE)).isEqualTo(3);
    }

    // ==================== Upkeep, saves and HUD ====================

    @Test
    void paysItsOwnUpkeepForEachTree() {
        FarmHarness farm = farm(
                FarmHarness.SETTINGS.withUpkeep(FarmUpkeep.ORCHARD, 1.5).withUpkeep(FarmUpkeep.ARBORETUM, 5.0),
                apple(ALWAYS));
        tree(farm.world, BASE, 5, LEAVES);
        tree(farm.world, new BlockPos(-2, 0, -2), 5, LEAVES);
        scan(farm);
        assertThat(farm.engine().pickPlotsTotal()).isEqualTo(2);
        long before = farm.energy();

        farm.ticks(10);

        assertThat(before - farm.energy()).isEqualTo(30);
    }

    @Test
    void keepsItsTreesThroughASaveButNotWhenCopiedElsewhere() {
        FarmHarness farm = farm(apple(ALWAYS));
        tree(farm.world, BASE, 5, LEAVES);
        scan(farm);
        CompoundTag saved = farm.save();

        FarmHarness same = farm(apple(ALWAYS));
        tree(same.world, BASE, 5, LEAVES);
        same.load(saved);
        same.tick();
        assertThat(same.engine().pickPlotsTotal()).isEqualTo(1);

        FarmHarness copy = farm(apple(ALWAYS));
        copy.load(saved);
        copy.host.pos = new BlockPos(40, 0, 40);
        copy.tick();
        assertThat(copy.engine().pickPlotsTotal()).isZero();
    }

    @Test
    void showsTheTreesItTracksOnItsHud() {
        FarmHarness farm = farm(apple(ALWAYS));
        tree(farm.world, BASE, 5, LEAVES);
        scan(farm);

        assertThat(hud(farm.engine())).contains(new MachineHudModel.LineEntry(
                TendFarmComponent.HUD_TREES, TendFarmComponent.HUD_TREES_COUNT, List.of("1")));
        assertThat(hud(FarmHarness.tending(new CropTendLogic()).engine()))
                .noneMatch(entry -> entry instanceof MachineHudModel.LineEntry line
                        && line.labelKey().equals(TendFarmComponent.HUD_TREES));
    }

    private static List<MachineHudModel.Entry> hud(TendFarmComponent farm) {
        MachineHudModel model = new MachineHudModel();
        farm.contributeHud(model);
        CompoundTag tag = new CompoundTag();
        model.save(tag);
        return MachineHudModel.entries(tag);
    }

    // ==================== The shared Reach ====================

    @Test
    void reachesSevenAboveItAndOneBelow() {
        List<BlockPos> reach = TendFarmComponent.reachOffsets(OrchardTendLogic.REACH_ABOVE);

        assertThat(reach).hasSize(9 * 49 - 1);
        assertThat(reach.get(0).getY()).isEqualTo(7);
        assertThat(reach.get(reach.size() - 1).getY()).isEqualTo(-1);
    }

    @Test
    void leavesTheSharedReachOfOtherFarmsAsItWas() {
        TendLogic crops = new CropTendLogic();

        assertThat(crops.reachAbove()).isEqualTo(TendFarmComponent.REACH_HEIGHT);
        assertThat(crops.picks()).isFalse();
        assertThat(TendFarmComponent.REACH_OFFSETS).hasSize(3 * 49 - 1);
        assertThat(TendFarmComponent.REACH_OFFSETS).isEqualTo(TendFarmComponent.reachOffsets(1));
        assertThat(TendFarmComponent.REACH_OFFSETS.get(0)).isEqualTo(new BlockPos(-3, 1, -3));
    }

    /** Hands out queued values: {@code nextInt(4096)} from {@link #ints}, {@code nextDouble} from {@link #doubles}. */
    private static final class ScriptedRandom extends SingleThreadedRandomSource {
        final Deque<Integer> ints = new ArrayDeque<>();
        final Deque<Double> doubles = new ArrayDeque<>();

        ScriptedRandom() {
            super(0L);
        }

        @Override
        public int nextInt(int bound) {
            return bound == TendFarmComponent.RANDOM_TICK_BLOCKS ? ints.removeFirst() : 0;
        }

        @Override
        public double nextDouble() {
            return doubles.removeFirst();
        }
    }
}
