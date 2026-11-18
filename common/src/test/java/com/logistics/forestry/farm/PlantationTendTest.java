package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.forestry.farm.plantation.PlantationTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Plantation tending stalks above their base in its Reach")
class PlantationTendTest extends MinecraftTestEnvironment {

    /** Long enough for a pass over the Reach and a tall stalk's breaks at one check and one action a tick. */
    private static final int PASS = 400;

    private static final BlockPos BASE = new BlockPos(2, 0, 1);

    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState CANE = Blocks.SUGAR_CANE.defaultBlockState();
    private static final BlockState BAMBOO = Blocks.BAMBOO.defaultBlockState();
    private static final BlockState SHOOT = Blocks.BAMBOO_SAPLING.defaultBlockState();
    private static final BlockState CACTUS = Blocks.CACTUS.defaultBlockState();
    /** Stands in for the cactus flower, which only 26.x has. */
    private static final BlockState TOPPING = Blocks.DANDELION.defaultBlockState();

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A Plantation whose tags are stood in for: tags are not bound in unit tests. */
    private static FarmHarness farm() {
        return farm(FarmHarness.SETTINGS);
    }

    private static FarmHarness farm(FarmSettings settings) {
        FarmHarness farm = FarmHarness.tending(
                new PlantationTendLogic(
                        state -> state.is(Blocks.SUGAR_CANE)
                                || state.is(Blocks.BAMBOO)
                                || state.is(Blocks.BAMBOO_SAPLING)
                                || state.is(Blocks.CACTUS),
                        state -> state.is(Blocks.DANDELION),
                        state -> state.is(Blocks.SAND) || state.is(Blocks.DIRT)),
                settings);
        farm.power(10_000);
        return farm;
    }

    /** A stalk of {@code height} blocks of {@code stalk} on {@code ground} from {@code base} up; returns the blocks above the base, top first. */
    private static List<BlockPos> stalk(FarmHarness farm, BlockPos base, BlockState ground, BlockState stalk, int height) {
        farm.world.set(base.below(), ground);
        List<BlockPos> above = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            farm.world.set(base.above(y), stalk);
            if (y > 0) {
                above.add(0, base.above(y));
            }
        }
        return above;
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

    private static boolean placed(FarmHarness farm) {
        return farm.world.mutations.stream().anyMatch(mutation -> mutation.after() != null);
    }

    @Test
    void harvestsSugarCaneTopDownAndLeavesItsBaseAsAPlantedPlot() {
        FarmHarness farm = farm();
        List<BlockPos> above = stalk(farm, BASE, SAND, CANE, 3);

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactlyElementsOf(above);
        assertThat(farm.world.blocks.get(BASE)).isEqualTo(CANE);
        assertThat(count(farm.items(), Items.SUGAR_CANE)).isEqualTo(2);
        assertThat(placed(farm)).as("nothing is replanted").isFalse();
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
        assertThat(farm.engine().emptyPlots()).isZero();
    }

    @Test
    void harvestsBambooFarAboveTheReachDownToItsBase() {
        FarmHarness farm = farm();
        BlockPos base = new BlockPos(-2, -1, -2);
        List<BlockPos> above = stalk(farm, base, DIRT, BAMBOO, 14);

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactlyElementsOf(above);
        assertThat(farm.world.blocks.get(base)).isEqualTo(BAMBOO);
        assertThat(count(farm.items(), Items.BAMBOO)).isEqualTo(13);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
    }

    @Test
    void picksTheToppingFirstAndKeepsTheCactusBase() {
        FarmHarness farm = farm();
        List<BlockPos> above = stalk(farm, BASE, SAND, CACTUS, 3);
        BlockPos top = BASE.above(3);
        farm.world.set(top, TOPPING);

        farm.ticks(PASS);

        List<BlockPos> expected = new ArrayList<>(above);
        expected.add(0, top);
        assertThat(farm.world.broken).containsExactlyElementsOf(expected);
        assertThat(farm.world.blocks.get(BASE)).isEqualTo(CACTUS);
        assertThat(count(farm.items(), Items.CACTUS)).isEqualTo(2);
        assertThat(count(farm.items(), Blocks.DANDELION.asItem())).isEqualTo(1);
    }

    @Test
    void picksAToppingOnABareBase() {
        FarmHarness farm = farm();
        stalk(farm, BASE, SAND, CACTUS, 1);
        farm.world.set(BASE.above(), TOPPING);

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactly(BASE.above());
        assertThat(farm.world.blocks.get(BASE)).isEqualTo(CACTUS);
    }

    @Test
    void takesABareBaseOnAsAPlotAndPaysItsUpkeep() {
        FarmHarness farm = farm(FarmHarness.SETTINGS
                .withUpkeep(FarmUpkeep.PLANTATION, 0.5)
                .withUpkeep(FarmUpkeep.CROP_FARM, 5.0));
        stalk(farm, BASE, SAND, CANE, 1);

        farm.ticks(PASS);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
        assertThat(farm.engine().emptyPlots()).isZero();
        long before = farm.energy();

        farm.ticks(10);

        assertThat(before - farm.energy()).isEqualTo(5);
        assertThat(farm.world.mutations).isEmpty();
    }

    @Test
    void countsABambooShootAsPlantedBamboo() {
        FarmHarness farm = farm();
        stalk(farm, BASE, DIRT, SHOOT, 1);

        farm.ticks(PASS);

        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
        assertThat(farm.engine().emptyPlots()).isZero();
        assertThat(farm.world.mutations).isEmpty();
    }

    @Test
    void harvestsBambooStandingOnItsShoot() {
        FarmHarness farm = farm();
        stalk(farm, BASE, DIRT, SHOOT, 1);
        farm.world.set(BASE.above(), BAMBOO);

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactly(BASE.above());
        assertThat(farm.world.blocks.get(BASE)).isEqualTo(SHOOT);
    }

    @Test
    void takesEveryStalkInAStackAsOnePlot() {
        FarmHarness farm = farm();
        // The base sits below the farm, so two of its stalk blocks are inside the Reach too.
        BlockPos base = new BlockPos(1, -1, -1);
        stalk(farm, base, SAND, CANE, 1);
        farm.world.set(base.above(), CANE);
        farm.world.set(base.above(2), CANE);
        farm.world.set(base.above(3), CANE);

        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(base)).isEqualTo(CANE);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
    }

    @Test
    void leavesAStalkOnOtherGroundStanding() {
        FarmHarness farm = farm();
        stalk(farm, BASE, Blocks.STONE.defaultBlockState(), CANE, 3);

        farm.ticks(PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.engine().plotsTotal()).isZero();
    }

    @Test
    void replantsAMissingBaseFromTheInventory() {
        FarmHarness farm = farm();
        stalk(farm, BASE, DIRT, SHOOT, 1);
        farm.ticks(PASS);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
        farm.world.blocks.remove(BASE);
        farm.items().setItem(0, new ItemStack(Items.BAMBOO, 2));

        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(BASE)).isEqualTo(SHOOT);
        assertThat(count(farm.items(), Items.BAMBOO)).isEqualTo(1);
        assertThat(farm.engine().emptyPlots()).isZero();
    }

    @Test
    void startsAHarvestWithOnlyItsBreakInTheBuffer() {
        FarmHarness farm = farm();
        stalk(farm, BASE, SAND, CANE, 2);
        farm.power(farm.cost(FarmCost.HARVEST_CROP));

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactly(BASE.above());
        assertThat(farm.energy()).isZero();
    }

    @Test
    void forgetsThePlotOnceItsGroundIsGone() {
        FarmHarness farm = farm();
        stalk(farm, BASE, SAND, CANE, 1);
        farm.ticks(PASS);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
        farm.world.set(BASE.below(), Blocks.STONE.defaultBlockState());
        farm.world.blocks.remove(BASE);

        farm.ticks(PASS);

        assertThat(farm.engine().plotsTotal()).isZero();
    }
}
