package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsForestry;
import com.logistics.forestry.farm.arboretum.ArboretumTendLogic;
import com.logistics.forestry.logic.TreeView;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Arboretum tending Humus trees in its Reach")
class ArboretumTendTest extends MinecraftTestEnvironment {

    /** Long enough for a pass over the Reach and a whole tree's breaks at one check and one action a tick. */
    private static final int FELL = 400;

    /** Stand in for the {@code #saplings} item tag; Humus for {@code logistics:forestry/arboretum_ground}. Tags are not bound in unit tests. */
    private static final Set<Item> SAPLINGS = Set.of(Items.OAK_SAPLING, Items.BIRCH_SAPLING);

    private static final BlockPos BASE = new BlockPos(2, 0, 1);

    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState();
    private static final BlockState PLACED_LEAVES = LEAVES.setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState SAPLING = Blocks.OAK_SAPLING.defaultBlockState();

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static BlockState humus() {
        return LogisticsForestry.BLOCK.HUMUS.defaultBlockState();
    }

    /** An Arboretum whose leaves drop one oak sapling each and whose logs drop themselves. */
    private static FarmHarness farm() {
        FarmHarness farm = FarmHarness.tending(new ArboretumTendLogic(
                state -> state.is(LogisticsForestry.BLOCK.HUMUS), stack -> SAPLINGS.contains(stack.getItem())));
        farm.world.trees = FakeFarmWorld.OAK_TREES;
        farm.world.drops = state -> state.is(Blocks.OAK_LEAVES)
                ? List.of(new ItemStack(Items.OAK_SAPLING))
                : List.of(new ItemStack(state.getBlock().asItem()));
        farm.power(10_000);
        return farm;
    }

    /**
     * The small Humus tree: a straight trunk of {@code height} logs from {@code base} up, under a
     * canopy of radius 2 on its top two logs and radius 1 above them. Returns every block of it.
     */
    private static List<BlockPos> tree(FarmHarness farm, BlockPos base, BlockState ground, int height, BlockState leaves) {
        List<BlockPos> blocks = new ArrayList<>();
        farm.world.set(base.below(), ground);
        for (int y = 0; y < height; y++) {
            farm.world.set(base.above(y), LOG);
            blocks.add(base.above(y));
        }
        for (int y = height - 2; y <= height; y++) {
            int radius = y < height ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = base.offset(dx, y, dz);
                    if (!farm.world.blocks.containsKey(pos)) {
                        farm.world.set(pos, leaves);
                        blocks.add(pos);
                    }
                }
            }
        }
        return blocks;
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

    @Test
    void fellsAWholeTreeOnHumusIncludingLogsAboveTheReachAndReplantsIt() {
        FarmHarness farm = farm();
        List<BlockPos> tree = tree(farm, BASE, humus(), 5, LEAVES);

        farm.ticks(FELL);

        assertThat(farm.world.broken).containsExactlyInAnyOrderElementsOf(tree);
        assertThat(farm.world.blocks.get(BASE)).isEqualTo(SAPLING);
        assertThat(farm.world.blocks.get(BASE.below())).isEqualTo(humus());
        assertThat(count(farm.items(), Items.OAK_LOG)).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"grass_block", "dirt", "podzol"})
    void leavesATreeOnOtherGroundStanding(String ground) {
        FarmHarness farm = farm();
        tree(farm, BASE, BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(ground)).defaultBlockState(), 5, LEAVES);

        farm.ticks(FELL);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.mutations).isEmpty();
    }

    @Test
    void leavesALogPillarOnHumusStanding() {
        FarmHarness farm = farm();
        farm.world.set(BASE.below(), humus());
        for (int y = 0; y < 4; y++) {
            farm.world.set(BASE.above(y), LOG);
        }

        farm.ticks(FELL);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.mutations).isEmpty();
    }

    @Test
    void leavesALogPillarOnHumusHungWithVinesAndCocoaStanding() {
        FarmHarness farm = farm();
        // Vines and cocoa are canopy to the feller, as the tags would have them, but not leaves.
        farm.world.trees = state -> state.is(Blocks.VINE) || state.is(Blocks.COCOA)
                ? TreeView.Kind.CANOPY
                : FakeFarmWorld.OAK_TREES.apply(state);
        farm.world.set(BASE.below(), humus());
        for (int y = 0; y < 4; y++) {
            farm.world.set(BASE.above(y), LOG);
        }
        farm.world.set(BASE.offset(1, 1, 0), Blocks.VINE.defaultBlockState());
        farm.world.set(BASE.offset(-1, 2, 0), Blocks.COCOA.defaultBlockState());

        farm.ticks(FELL);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.mutations).isEmpty();
    }

    @Test
    void leavesAHandBuiltTreeWithPlacedLeavesStanding() {
        FarmHarness farm = farm();
        tree(farm, BASE, humus(), 5, PLACED_LEAVES);

        farm.ticks(FELL);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.mutations).isEmpty();
    }

    /** Leaves drop only sticks, so a fell yields no sapling. */
    private static FarmHarness saplinglessFarm() {
        FarmHarness farm = farm();
        farm.world.drops = state -> state.getBlock() instanceof LeavesBlock
                ? List.of(new ItemStack(Items.STICK))
                : List.of(new ItemStack(state.getBlock().asItem()));
        return farm;
    }

    /** Ticks until the farm has broken {@code breaks} blocks. */
    private static void fellUntil(FarmHarness farm, int breaks) {
        for (int tick = 0; tick < FELL && farm.world.broken.size() < breaks; tick++) {
            farm.tick();
        }
        assertThat(farm.world.broken).hasSize(breaks);
    }

    @Test
    void replantsFromTheInventoryWhenTheTreeDropsNoSapling() {
        FarmHarness farm = saplinglessFarm();
        farm.items().setItem(4, new ItemStack(Items.OAK_SAPLING, 3));
        tree(farm, BASE, humus(), 5, LEAVES);

        farm.ticks(FELL);

        assertThat(farm.world.blocks.get(BASE)).isEqualTo(SAPLING);
        assertThat(count(farm.items(), Items.OAK_SAPLING)).isEqualTo(2);
    }

    @Test
    void replantsTheTreesOwnKind() {
        FarmHarness farm = saplinglessFarm();
        farm.world.trees = state -> state.is(Blocks.BIRCH_LOG)
                ? TreeView.Kind.WOOD
                : state.is(Blocks.BIRCH_LEAVES) ? TreeView.Kind.CANOPY : FakeFarmWorld.NO_TREES.apply(state);
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 3));
        farm.items().setItem(1, new ItemStack(Items.BIRCH_SAPLING, 3));
        farm.world.set(BASE.below(), humus());
        farm.world.set(BASE, Blocks.BIRCH_LOG.defaultBlockState());
        farm.world.set(BASE.above(), Blocks.BIRCH_LOG.defaultBlockState());
        farm.world.set(BASE.above(2), Blocks.BIRCH_LEAVES.defaultBlockState());

        farm.ticks(FELL);

        assertThat(farm.world.blocks.get(BASE)).isEqualTo(Blocks.BIRCH_SAPLING.defaultBlockState());
        assertThat(count(farm.items(), Items.BIRCH_SAPLING)).isEqualTo(2);
        assertThat(count(farm.items(), Items.OAK_SAPLING)).isEqualTo(3);
    }

    @Test
    void leavesAnEmptyPlotAndReplantsItOnceASaplingArrives() {
        FarmHarness farm = saplinglessFarm();
        tree(farm, BASE, humus(), 5, LEAVES);
        farm.ticks(FELL);
        assertThat(farm.world.blocks.get(BASE)).as("left empty").isNull();
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.WAITING_FOR_SEEDS);

        farm.items().setItem(7, new ItemStack(Items.OAK_SAPLING, 2));
        farm.ticks(FELL);

        assertThat(farm.world.blocks.get(BASE)).isEqualTo(SAPLING);
        assertThat(count(farm.items(), Items.OAK_SAPLING)).isEqualTo(1);
    }

    @Test
    void forgetsAPlotWhoseHumusIsRemoved() {
        FarmHarness farm = saplinglessFarm();
        tree(farm, BASE, humus(), 5, LEAVES);
        farm.ticks(FELL);
        assertThat(farm.world.blocks.get(BASE)).as("left empty").isNull();

        farm.world.set(BASE.below(), Blocks.DIRT.defaultBlockState());
        farm.ticks(FELL);
        farm.world.set(BASE.below(), humus());
        farm.items().setItem(7, new ItemStack(Items.OAK_SAPLING, 2));
        farm.ticks(FELL);

        assertThat(farm.world.blocks.get(BASE)).isNull();
        assertThat(count(farm.items(), Items.OAK_SAPLING)).isEqualTo(2);
    }

    @Test
    void aFullInventorySpillsAtTheTrunkBaseAndStillReplants() {
        FarmHarness farm = farm();
        for (int slot = 0; slot < FarmMachine.SLOT_COUNT; slot++) {
            farm.items().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        tree(farm, BASE, humus(), 5, LEAVES);

        farm.ticks(FELL);

        assertThat(farm.world.blocks.get(BASE)).isEqualTo(SAPLING);
        assertThat(farm.world.spilled).isNotEmpty();
        assertThat(farm.world.spilled).allSatisfy(item -> assertThat(item.pos()).isEqualTo(BASE));
        assertThat(farm.world.spilled.stream()
                        .filter(item -> item.stack().is(Items.OAK_LOG))
                        .mapToInt(item -> item.stack().getCount())
                        .sum())
                .isEqualTo(5);
    }

    @Test
    void chargesEachLogAndLeafFelledAndTheReplant() {
        FarmHarness farm = farm();
        List<BlockPos> tree = tree(farm, BASE, humus(), 5, LEAVES);
        long leaves = tree.size() - 5;

        farm.ticks(FELL);

        assertThat(10_000 - farm.energy())
                .isEqualTo(5 * farm.cost(FarmCost.HARVEST_LOG)
                        + leaves * farm.cost(FarmCost.HARVEST_LEAVES)
                        + farm.cost(FarmCost.PLANT));
    }

    @Test
    void stopsAFellThatRunsOutOfPowerAndFinishesItOncePowered() {
        FarmHarness farm = farm();
        List<BlockPos> tree = tree(farm, BASE, humus(), 5, LEAVES);
        farm.power(300);

        farm.ticks(FELL);

        assertThat(farm.world.broken).isNotEmpty().hasSizeLessThan(tree.size());
        assertThat(farm.world.blocks.get(BASE)).isEqualTo(LOG);
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.NO_POWER);
        assertThat(farm.energy()).isLessThan(farm.cost(FarmCost.HARVEST_LOG));

        farm.power(10_000);
        farm.ticks(FELL);

        assertThat(farm.world.broken).containsExactlyInAnyOrderElementsOf(tree);
        assertThat(farm.world.blocks.get(BASE)).isEqualTo(SAPLING);
    }

    @Test
    void reTakesWhatIsLeftOfATreeThatChangedWhileFelled() {
        FarmHarness farm = farm();
        tree(farm, BASE, humus(), 5, LEAVES);
        fellUntil(farm, 3);
        BlockPos changed = BASE.offset(2, 3, 2);
        farm.world.set(changed, Blocks.GLASS.defaultBlockState());

        farm.ticks(FELL);

        assertThat(farm.world.blocks.get(changed)).isEqualTo(Blocks.GLASS.defaultBlockState());
        for (int y = 0; y < 5; y++) {
            assertThat(farm.world.broken).as("log %s", y).contains(BASE.above(y));
        }
        assertThat(farm.world.blocks.get(BASE)).isEqualTo(SAPLING);
    }

    @Test
    void finishesAFellUnderWayAcrossASaveWithItsDrops() {
        FarmHarness farm = saplinglessFarm();
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 1));
        tree(farm, BASE, humus(), 5, LEAVES);
        fellUntil(farm, 50);
        assertThat(count(farm.items(), Items.OAK_LOG)).as("nothing stored before the fell is done").isZero();

        FarmHarness loaded = saplinglessFarm();
        loaded.world.blocks.putAll(farm.world.blocks);
        loaded.load(farm.save());
        loaded.power(10_000);
        loaded.ticks(FELL);

        assertThat(loaded.world.blocks.values()).doesNotContain(LOG, LEAVES);
        assertThat(count(loaded.items(), Items.OAK_LOG)).isEqualTo(5);
        assertThat(count(loaded.items(), Items.STICK)).isEqualTo(57);
        assertThat(loaded.world.blocks.get(BASE)).isEqualTo(SAPLING);
    }

    @Test
    void loadsARingArboretumSaveAndStartsTending() {
        FarmHarness farm = farm();
        farm.load(FarmHarness.ringFarmSave(slot -> new ItemStack(Items.OAK_SAPLING, slot + 1)));
        farm.power(10_000);
        tree(farm, BASE, humus(), 5, LEAVES);
        farm.ticks(FELL);

        assertThat(farm.world.blocks.get(BASE)).isEqualTo(SAPLING);
        for (int y = 0; y < 5; y++) {
            assertThat(farm.world.broken).as("log %s", y).contains(BASE.above(y));
        }
    }
}
