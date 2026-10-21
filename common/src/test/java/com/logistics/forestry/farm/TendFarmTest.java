package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.forestry.farm.crop.CropTendLogic;
import com.logistics.DomainRegistrations;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Crop Farm tending its Reach")
class TendFarmTest extends MinecraftTestEnvironment {

    /** Long enough for one full pass over the Reach at one check a tick. */
    private static final int PASS = 160;

    /** Stands in for {@code logistics:forestry/crop_farm_tends}, which is not bound in unit tests. */
    private static final Set<Block> TENDED = Set.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);

    /** Stands in for {@code logistics:forestry/crop_farm_ground}. */
    private static final Set<Block> GROUND = Set.of(Blocks.FARMLAND, Blocks.DIRT, Blocks.GRASS_BLOCK);

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static FarmHarness farm() {
        FarmHarness farm = FarmHarness.tending(new CropTendLogic(
                state -> TENDED.contains(state.getBlock()), state -> GROUND.contains(state.getBlock())));
        farm.world.drops = TendFarmTest::drops;
        farm.power(10_000);
        return farm;
    }

    /** A ripe crop's drops as vanilla's loot rolls them at their most common. */
    private static List<ItemStack> drops(BlockState state) {
        if (state.is(Blocks.WHEAT)) {
            return List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT_SEEDS, 2));
        }
        if (state.is(Blocks.BEETROOTS)) {
            return List.of(new ItemStack(Items.BEETROOT), new ItemStack(Items.BEETROOT_SEEDS, 2));
        }
        if (state.is(Blocks.CARROTS)) {
            return List.of(new ItemStack(Items.CARROT, 3));
        }
        if (state.is(Blocks.POTATOES)) {
            return List.of(new ItemStack(Items.POTATO, 3));
        }
        return List.of(new ItemStack(state.getBlock().asItem()));
    }

    private static BlockState ripe(Block crop) {
        CropBlock block = (CropBlock) crop;
        return block.getStateForAge(block.getMaxAge());
    }

    private static void field(FarmHarness farm, BlockPos plot, BlockState crop) {
        farm.world.set(plot.below(), Blocks.FARMLAND.defaultBlockState());
        farm.world.set(plot, crop);
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
    void harvestsRipeWheatAndReplantsFromItsDrops() {
        FarmHarness farm = farm();
        BlockPos plot = new BlockPos(2, 0, 1);
        field(farm, plot, ripe(Blocks.WHEAT));

        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(plot)).isEqualTo(Blocks.WHEAT.defaultBlockState());
        assertThat(count(farm.items(), Items.WHEAT)).isEqualTo(1);
        // Two seeds dropped, one went back in the ground.
        assertThat(count(farm.items(), Items.WHEAT_SEEDS)).isEqualTo(1);
    }

    @Test
    void tendsTheWholeReachCornersAndOneLevelUpAndDown() {
        FarmHarness farm = farm();
        List<BlockPos> edges = List.of(
                new BlockPos(3, 1, 3), new BlockPos(-3, 1, -3), new BlockPos(3, -1, -3), new BlockPos(-3, -1, 3),
                new BlockPos(0, 1, 0), new BlockPos(0, -1, 0), new BlockPos(3, 0, 0), new BlockPos(0, 0, -3));
        edges.forEach(plot -> field(farm, plot, ripe(Blocks.WHEAT)));

        farm.ticks(PASS);

        for (BlockPos plot : edges) {
            assertThat(farm.world.broken).as("harvested at %s", plot).contains(plot);
            assertThat(farm.world.blocks.get(plot)).as("replanted at %s", plot).isEqualTo(Blocks.WHEAT.defaultBlockState());
        }
    }

    @Test
    void leavesCropsOneStepBeyondTheReachAlone() {
        FarmHarness farm = farm();
        List<BlockPos> outside = List.of(
                new BlockPos(4, 0, 0), new BlockPos(0, 0, -4), new BlockPos(4, 1, 4),
                new BlockPos(0, 2, 0), new BlockPos(0, -2, 0), new BlockPos(3, 2, 3), new BlockPos(-3, -2, -3));
        outside.forEach(plot -> field(farm, plot, ripe(Blocks.WHEAT)));

        farm.ticks(PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.reads).doesNotContainAnyElementsOf(outside);
    }

    @Test
    void neverWorksItsOwnPosition() {
        FarmHarness farm = farm();
        farm.world.set(BlockPos.ZERO, ripe(Blocks.WHEAT));

        farm.ticks(PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.reads).doesNotContain(BlockPos.ZERO);
    }

    @Test
    void harvestsEachTendedCropOnlyWhenRipe() {
        FarmHarness farm = farm();
        List<Block> crops = List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);
        for (int i = 0; i < crops.size(); i++) {
            CropBlock crop = (CropBlock) crops.get(i);
            field(farm, new BlockPos(i - 2, 0, 2), ripe(crop));
            field(farm, new BlockPos(i - 2, 0, -2), crop.getStateForAge(crop.getMaxAge() - 1));
        }

        farm.ticks(PASS);

        for (int i = 0; i < crops.size(); i++) {
            CropBlock crop = (CropBlock) crops.get(i);
            BlockPos ripePlot = new BlockPos(i - 2, 0, 2);
            BlockPos youngPlot = new BlockPos(i - 2, 0, -2);
            assertThat(farm.world.broken).as("%s harvested", crop).contains(ripePlot);
            assertThat(farm.world.blocks.get(ripePlot)).isEqualTo(crop.defaultBlockState());
            assertThat(farm.world.broken).as("young %s left", crop).doesNotContain(youngPlot);
        }
        assertThat(count(farm.items(), Items.WHEAT)).isEqualTo(1);
        assertThat(count(farm.items(), Items.CARROT)).isEqualTo(2);
        assertThat(count(farm.items(), Items.POTATO)).isEqualTo(2);
        assertThat(count(farm.items(), Items.BEETROOT)).isEqualTo(1);
    }

    @Test
    void tendsAnExtraCropAddedToBothTagsAndNothingUntagged() {
        FarmHarness farm = FarmHarness.tending(new CropTendLogic(
                state -> state.is(Blocks.SWEET_BERRY_BUSH),
                state -> state.is(Blocks.FARMLAND) || state.is(Blocks.PODZOL)));
        farm.power(10_000);
        BlockPos bush = new BlockPos(1, 0, 1);
        BlockPos wheat = new BlockPos(-1, 0, -1);
        farm.world.set(bush.below(), Blocks.PODZOL.defaultBlockState());
        farm.world.set(bush, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        field(farm, wheat, ripe(Blocks.WHEAT));
        farm.world.drops = state -> List.of(new ItemStack(Items.SWEET_BERRIES, 3));

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactly(bush);
        assertThat(farm.world.blocks.get(bush.below())).isEqualTo(Blocks.FARMLAND.defaultBlockState());
        assertThat(farm.world.blocks.get(bush)).isEqualTo(Blocks.SWEET_BERRY_BUSH.defaultBlockState());
        assertThat(count(farm.items(), Items.SWEET_BERRIES)).isEqualTo(2);
    }

    @Test
    void leavesATaggedCropOnUntaggedGroundAloneAndNeverRemembersIt() {
        FarmHarness farm = farm();
        BlockPos plot = new BlockPos(1, 0, 1);
        farm.world.set(plot.below(), Blocks.SOUL_SAND.defaultBlockState());
        farm.world.set(plot, ripe(Blocks.WHEAT));

        farm.ticks(PASS);

        assertThat(farm.world.mutations).isEmpty();
        assertThat(farm.world.blocks.get(plot)).isEqualTo(ripe(Blocks.WHEAT));

        // Cleared by hand onto good ground: a remembered Plot would be replanted now.
        farm.world.set(plot, Blocks.AIR.defaultBlockState());
        farm.world.set(plot.below(), Blocks.FARMLAND.defaultBlockState());
        farm.items().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 4));
        farm.ticks(2 * PASS);

        assertThat(farm.world.mutations).isEmpty();
        assertThat(count(farm.items(), Items.WHEAT_SEEDS)).isEqualTo(4);
    }

    @Test
    void neverPlantsGroundItDidNotHarvest() {
        FarmHarness farm = farm();
        BlockPos bare = new BlockPos(1, 0, 1);
        farm.world.set(bare.below(), Blocks.FARMLAND.defaultBlockState());
        farm.items().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 16));

        farm.ticks(PASS);

        assertThat(farm.world.mutations).isEmpty();
        assertThat(count(farm.items(), Items.WHEAT_SEEDS)).isEqualTo(16);
    }

    private static void fill(FarmHarness farm) {
        for (int slot = 0; slot < farm.items().getContainerSize(); slot++) {
            farm.items().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private static int spilled(FarmHarness farm, BlockPos at, Item item) {
        return farm.world.spilled.stream()
                .filter(loose -> loose.pos().equals(at) && loose.stack().is(item))
                .mapToInt(loose -> loose.stack().getCount())
                .sum();
    }

    @Test
    void aFullInventorySpillsAtThePlotAndStillReplants() {
        FarmHarness farm = farm();
        fill(farm);
        BlockPos plot = new BlockPos(-2, 1, 3);
        field(farm, plot, ripe(Blocks.WHEAT));

        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(plot)).isEqualTo(Blocks.WHEAT.defaultBlockState());
        assertThat(spilled(farm, plot, Items.WHEAT)).isEqualTo(1);
        assertThat(spilled(farm, plot, Items.WHEAT_SEEDS)).isEqualTo(1);
        assertThat(farm.world.spilled).allMatch(loose -> loose.pos().equals(plot));
    }

    @Test
    void keepsHarvestingWhileFull() {
        FarmHarness farm = farm();
        fill(farm);
        BlockPos first = new BlockPos(-1, 0, 1);
        BlockPos second = new BlockPos(2, 0, -2);
        field(farm, first, ripe(Blocks.CARROTS));
        field(farm, second, ripe(Blocks.POTATOES));

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactlyInAnyOrder(first, second);
        assertThat(spilled(farm, first, Items.CARROT)).isEqualTo(2);
        assertThat(spilled(farm, second, Items.POTATO)).isEqualTo(2);
    }

    @Test
    void chargesTheHarvestAndThePlant() {
        FarmHarness farm = farm();
        field(farm, new BlockPos(1, 0, 2), ripe(Blocks.WHEAT));

        farm.ticks(PASS);

        assertThat(farm.energy()).isEqualTo(10_000 - farm.cost(FarmCost.HARVEST_CROP) - farm.cost(FarmCost.PLANT));
    }

    @Test
    void chargesNothingForAHarvestTheWorldRefuses() {
        FarmHarness farm = farm();
        BlockPos plot = new BlockPos(1, 0, 2);
        field(farm, plot, ripe(Blocks.WHEAT));
        farm.world.refuseBreak = plot::equals;

        farm.ticks(PASS);

        assertThat(farm.energy()).isEqualTo(10_000);
        assertThat(farm.world.blocks.get(plot)).isEqualTo(ripe(Blocks.WHEAT));
    }

    @Test
    void doesNothingWithoutPowerAndSaysSo() {
        FarmHarness farm = farm();
        farm.power(0);
        BlockPos plot = new BlockPos(1, 0, 2);
        field(farm, plot, ripe(Blocks.WHEAT));

        farm.ticks(PASS);

        assertThat(farm.world.mutations).isEmpty();
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.NO_POWER);
    }

    @Test
    void anUnpoweredFarmWithNothingToDoSaysNoPowerNotIdle() {
        FarmHarness farm = farm();
        farm.power(0);

        farm.ticks(PASS);

        assertThat(farm.engine().status()).isEqualTo(FarmStatus.NO_POWER);
    }

    @Test
    void aFarmShortOfItsCheapestActionSaysNoPowerUntilCharged() {
        FarmHarness farm = farm();
        long cheapest = farm.cost(FarmCost.HARVEST_LEAVES);
        farm.power(cheapest - 1);

        farm.ticks(PASS);
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.NO_POWER);

        farm.power(cheapest);
        farm.tick();
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.IDLE);
    }

    @Test
    void waitsUntilItCanPayForTheReplantToo() {
        FarmHarness farm = farm();
        long harvestOnly = farm.cost(FarmCost.HARVEST_CROP);
        farm.power(harvestOnly);
        BlockPos plot = new BlockPos(1, 0, 2);
        field(farm, plot, ripe(Blocks.WHEAT));

        farm.ticks(PASS);
        assertThat(farm.world.mutations).isEmpty();

        farm.power(harvestOnly + farm.cost(FarmCost.PLANT));
        farm.ticks(2);
        assertThat(farm.world.blocks.get(plot)).isEqualTo(Blocks.WHEAT.defaultBlockState());
        assertThat(farm.energy()).isZero();
    }

    @Test
    void reportsWorkingThenIdleOnceNothingIsRipe() {
        FarmHarness farm = farm();
        BlockPos plot = new BlockPos(1, 0, 2);
        field(farm, plot, ripe(Blocks.WHEAT));

        for (int tick = 0; tick < PASS && !farm.world.broken.contains(plot); tick++) {
            farm.tick();
        }
        assertThat(farm.world.broken).contains(plot);
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.WORKING);

        farm.ticks(2 * PASS);
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.IDLE);
    }

    @Test
    void reportsTheFullInventoryWhileItSpills() {
        FarmHarness farm = farm();
        fill(farm);
        field(farm, new BlockPos(1, 0, 2), ripe(Blocks.WHEAT));

        farm.ticks(PASS);

        assertThat(farm.engine().status()).isEqualTo(FarmStatus.INVENTORY_FULL);
    }

    @Test
    void pausesOnItsRedstoneControl() {
        FarmHarness farm = farm();
        farm.host.workEnabled = false;
        field(farm, new BlockPos(1, 0, 2), ripe(Blocks.WHEAT));

        farm.ticks(PASS);

        assertThat(farm.world.mutations).isEmpty();
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.PAUSED);
    }

    @Test
    void acceptsAnyItemAndGivesUpAnySlotOnEveryFace() {
        FarmHarness farm = farm();
        ItemStoreComponent store = farm.machine.items();
        assertThat(farm.items().getContainerSize()).isEqualTo(15);
        for (Direction face : Direction.values()) {
            assertThat(store.slotsForFace(face)).hasSize(15);
            for (int slot = 0; slot < 15; slot++) {
                assertThat(store.canPlace(slot, new ItemStack(Items.DIRT), face)).isTrue();
                assertThat(store.canPlace(slot, new ItemStack(Items.WHEAT_SEEDS), face)).isTrue();
                assertThat(store.canTake(slot, new ItemStack(Items.WHEAT_SEEDS), face)).isTrue();
            }
        }
    }

    @Test
    void leavesClaimedCropsAlone() {
        FarmHarness farm = farm();
        BlockPos claimed = new BlockPos(1, 0, 2);
        field(farm, claimed, ripe(Blocks.WHEAT));
        farm.world.claimed.add(claimed);

        farm.ticks(PASS);

        assertThat(farm.world.mutations).isEmpty();
        assertThat(farm.energy()).isEqualTo(10_000);
    }

    @Test
    void neverReadsAnUnloadedPosition() {
        FarmHarness farm = farm();
        BlockPos far = new BlockPos(3, 0, 3);
        field(farm, far, ripe(Blocks.WHEAT));
        farm.world.unloaded.add(far);

        farm.ticks(PASS);

        assertThat(farm.world.reads).doesNotContain(far);
        assertThat(farm.world.mutations).isEmpty();
    }

    @Test
    void keepsItsInventoryAndOwnerAcrossASave() {
        FarmHarness farm = farm();
        UUID owner = UUID.randomUUID();
        farm.engine().setOwner(owner);
        farm.items().setItem(14, new ItemStack(Items.WHEAT_SEEDS, 7));

        FarmHarness loaded = farm();
        loaded.load(farm.save());

        assertThat(loaded.engine().owner()).isEqualTo(owner);
        assertThat(loaded.items().getItem(14).is(Items.WHEAT_SEEDS)).isTrue();
        assertThat(loaded.items().getItem(14).getCount()).isEqualTo(7);
    }

    @Test
    void loadsARingFarmSaveAndStartsTending() {
        FarmHarness farm = farm();
        farm.load(FarmHarness.ringFarmSave(slot -> new ItemStack(Items.WHEAT_SEEDS, slot + 1)));
        farm.power(10_000);
        BlockPos plot = new BlockPos(1, 0, 2);
        field(farm, plot, ripe(Blocks.WHEAT));
        farm.ticks(PASS);

        for (int slot = 0; slot < FarmMachine.SLOT_COUNT; slot++) {
            assertThat(farm.items().getItem(slot).getCount()).as("slot %s", slot).isGreaterThanOrEqualTo(slot + 1);
        }
        assertThat(farm.world.blocks.get(plot)).isEqualTo(Blocks.WHEAT.defaultBlockState());
    }
}
