package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.forestry.farm.gourd.GourdTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Gourd Farm tending pumpkin and melon stems in its Reach")
class GourdTendTest extends MinecraftTestEnvironment {

    /** Long enough for one full pass over the Reach at one check a tick. */
    private static final int PASS = 160;

    /** Stands in for {@code logistics:forestry/gourd_farm_ground}; tags are not bound in unit tests. */
    private static final Set<Block> GROUND = Set.of(Blocks.FARMLAND, Blocks.DIRT, Blocks.GRASS_BLOCK);

    private static final BlockPos STEM = new BlockPos(2, 0, 1);

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static FarmHarness farm() {
        return farm(FarmHarness.SETTINGS.withUpkeep(FarmUpkeep.GOURD_FARM, 0.0));
    }

    private static FarmHarness farm(FarmSettings settings) {
        FarmHarness farm = FarmHarness.tending(
                new GourdTendLogic(state -> GROUND.contains(state.getBlock())), settings);
        farm.world.drops = state -> state.is(Blocks.MELON)
                ? List.of(new ItemStack(Items.MELON_SLICE, 5))
                : List.of(new ItemStack(state.getBlock().asItem()));
        farm.power(10_000);
        return farm;
    }

    /** A stem on farmland at {@code stem}, attached to {@code fruit} on dirt one block {@code facing}. */
    private static BlockPos attached(FarmHarness farm, BlockPos stem, Block attachedStem, Block fruit, Direction facing) {
        farm.world.set(stem.below(), Blocks.FARMLAND.defaultBlockState());
        farm.world.set(stem, attachedStem.defaultBlockState().setValue(AttachedStemBlock.FACING, facing));
        BlockPos fruitPos = stem.relative(facing);
        farm.world.set(fruitPos.below(), Blocks.DIRT.defaultBlockState());
        farm.world.set(fruitPos, fruit.defaultBlockState());
        return fruitPos;
    }

    private static BlockState growingStem(Block stem) {
        return stem.defaultBlockState().setValue(StemBlock.AGE, StemBlock.MAX_AGE);
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

    /** Whether the farm ever broke or wrote the block at {@code pos}. */
    private static boolean touched(FarmHarness farm, BlockPos pos) {
        return farm.world.mutations.stream().anyMatch(change -> change.pos().equals(pos));
    }

    @Test
    void harvestsThePumpkinAndLeavesItsStemAsAPlantedPlot() {
        FarmHarness farm = farm();
        farm.items().setItem(0, new ItemStack(Items.PUMPKIN_SEEDS, 4));
        BlockPos fruit = attached(farm, STEM, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.PUMPKIN, Direction.EAST);

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactly(fruit);
        assertThat(touched(farm, STEM)).as("the stem is never broken or replanted").isFalse();
        assertThat(farm.world.blocks.get(STEM).is(Blocks.ATTACHED_PUMPKIN_STEM)).isTrue();
        assertThat(count(farm.items(), Items.PUMPKIN)).isEqualTo(1);
        assertThat(count(farm.items(), Items.PUMPKIN_SEEDS)).as("no seed spent").isEqualTo(4);
        assertThat(farm.energy()).as("the harvest only, no plant").isEqualTo(10_000 - farm.cost(FarmCost.HARVEST_CROP));
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
        assertThat(farm.engine().emptyPlots()).isZero();
    }

    @Test
    void harvestsTheMelonIntoSlices() {
        FarmHarness farm = farm();
        BlockPos fruit = attached(farm, STEM, Blocks.ATTACHED_MELON_STEM, Blocks.MELON, Direction.NORTH);

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactly(fruit);
        assertThat(count(farm.items(), Items.MELON_SLICE)).isEqualTo(5);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
    }

    @Test
    void theStemGrowsTheNextFruitAndThePlotIsHarvestedAgain() {
        FarmHarness farm = farm();
        BlockPos fruit = attached(farm, STEM, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.PUMPKIN, Direction.EAST);
        farm.ticks(PASS);
        // Vanilla turns the stem back into a grown, unattached stem once its fruit is gone.
        farm.world.set(STEM, growingStem(Blocks.PUMPKIN_STEM));
        farm.ticks(PASS);
        assertThat(farm.engine().emptyPlots()).as("an unattached stem is still the Plot's own plant").isZero();

        attached(farm, STEM, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.PUMPKIN, Direction.EAST);
        farm.ticks(2 * PASS);

        assertThat(farm.world.broken).containsExactly(fruit, fruit);
        assertThat(count(farm.items(), Items.PUMPKIN)).isEqualTo(2);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
    }

    @Test
    void aFruitOneBlockOutsideTheReachIsHarvestedFromItsStemInside() {
        FarmHarness farm = farm();
        BlockPos stem = new BlockPos(TendFarmComponent.REACH, 0, 0);
        BlockPos fruit = attached(farm, stem, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.PUMPKIN, Direction.EAST);

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactly(fruit);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
    }

    @Test
    void aFruitInsideTheReachWhoseStemIsOutsideIsLeftAlone() {
        FarmHarness farm = farm();
        BlockPos stem = new BlockPos(TendFarmComponent.REACH + 1, 0, 0);
        attached(farm, stem, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.PUMPKIN, Direction.WEST);

        farm.ticks(2 * PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.engine().plotsTotal()).isZero();
    }

    @Test
    void aPumpkinBesideAnUnattachedStemIsLeftAlone() {
        FarmHarness farm = farm();
        farm.world.set(STEM.below(), Blocks.FARMLAND.defaultBlockState());
        farm.world.set(STEM, growingStem(Blocks.PUMPKIN_STEM));
        farm.world.set(STEM.east(), Blocks.PUMPKIN.defaultBlockState());
        // An attached pumpkin stem pointing at a melon is no pumpkin's stem.
        BlockPos other = new BlockPos(-2, 0, -2);
        attached(farm, other, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.MELON, Direction.SOUTH);

        farm.ticks(2 * PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.engine().plotsTotal()).as("both stems are still Plots").isEqualTo(2);
    }

    @Test
    void aStemOnOtherGroundIsNotTended() {
        FarmHarness farm = farm();
        attached(farm, STEM, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.PUMPKIN, Direction.EAST);
        farm.world.set(STEM.below(), Blocks.STONE.defaultBlockState());

        farm.ticks(2 * PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.engine().plotsTotal()).isZero();
    }

    @Test
    void aGrowingStemIsTakenOnAsAPlotWithoutTouchingIt() {
        FarmHarness farm = farm();
        farm.world.set(STEM.below(), Blocks.FARMLAND.defaultBlockState());
        farm.world.set(STEM, Blocks.MELON_STEM.defaultBlockState());

        farm.ticks(PASS);

        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
        assertThat(farm.world.mutations).isEmpty();
    }

    @Test
    void aBrokenStemWaitsForSeedsThenIsReTilledAndReplanted() {
        FarmHarness farm = farm();
        farm.world.set(STEM.below(), Blocks.FARMLAND.defaultBlockState());
        farm.world.set(STEM, growingStem(Blocks.PUMPKIN_STEM));
        farm.ticks(PASS);

        // Trampled: the farmland turns to dirt and the stem pops off.
        farm.world.blocks.remove(STEM);
        farm.world.set(STEM.below(), Blocks.DIRT.defaultBlockState());
        farm.ticks(PASS);
        assertThat(farm.engine().emptyPlots()).isEqualTo(1);
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.WAITING_FOR_SEEDS);

        farm.items().setItem(0, new ItemStack(Items.PUMPKIN_SEEDS, 3));
        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(STEM.below())).isEqualTo(Blocks.FARMLAND.defaultBlockState());
        assertThat(farm.world.blocks.get(STEM)).isEqualTo(Blocks.PUMPKIN_STEM.defaultBlockState());
        assertThat(count(farm.items(), Items.PUMPKIN_SEEDS)).isEqualTo(2);
        assertThat(farm.engine().emptyPlots()).isZero();
    }

    @Test
    void paysItsUpkeepForEachStem() {
        FarmHarness farm = farm(FarmHarness.SETTINGS.withUpkeep(FarmUpkeep.GOURD_FARM, 0.75));
        for (int dx = -2; dx <= 1; dx++) {
            BlockPos stem = new BlockPos(dx, 0, 2);
            farm.world.set(stem.below(), Blocks.FARMLAND.defaultBlockState());
            farm.world.set(stem, Blocks.PUMPKIN_STEM.defaultBlockState());
        }
        farm.ticks(PASS);
        assertThat(farm.engine().plotsTotal()).isEqualTo(4);

        long before = farm.energy();
        farm.ticks(100);

        assertThat(before - farm.energy()).as("4 stems at 0.75 RF/t for 100 ticks").isEqualTo(300);
    }

    @Test
    void theShippedUpkeepIsThreeQuartersOfAnRfPerStem() {
        assertThat(FarmUpkeep.GOURD_FARM.defaultRfPerTick()).isEqualTo(0.75);
        assertThat(new GourdTendLogic().upkeep()).isEqualTo(FarmUpkeep.GOURD_FARM);
    }
}
