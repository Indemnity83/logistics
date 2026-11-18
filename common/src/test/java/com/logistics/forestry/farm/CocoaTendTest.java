package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.forestry.farm.cocoa.CocoaTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cocoa Plantation tending pods on jungle logs in its Reach")
class CocoaTendTest extends MinecraftTestEnvironment {

    /** Long enough for one full pass over the Reach at one check a tick. */
    private static final int PASS = 160;

    private static final BlockPos LOG = new BlockPos(2, 0, 1);

    /** North of {@link #LOG}, so its pod faces south. */
    private static final BlockPos POD = LOG.north();

    private static final BlockState JUNGLE_LOG = Blocks.JUNGLE_LOG.defaultBlockState();

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A Cocoa Plantation on jungle logs, standing in for its tag, whose ripe pods drop {@code beans} beans. */
    private static FarmHarness farm(int beans) {
        FarmHarness farm = FarmHarness.tending(new CocoaTendLogic(state -> state.is(Blocks.JUNGLE_LOG)));
        farm.world.drops = state -> state.is(Blocks.COCOA)
                ? beans > 0 ? List.of(new ItemStack(Items.COCOA_BEANS, beans)) : List.of()
                : List.of(new ItemStack(state.getBlock().asItem()));
        farm.power(10_000);
        return farm;
    }

    private static BlockState pod(Direction facing, int age) {
        return Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, facing).setValue(CocoaBlock.AGE, age);
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

    /** Picks the ripe pod at {@link #POD}, which drops no beans, so the plot is left empty. */
    private static FarmHarness emptyPodPlot() {
        FarmHarness farm = farm(0);
        farm.world.set(LOG, JUNGLE_LOG);
        farm.world.set(POD, pod(Direction.SOUTH, CocoaBlock.MAX_AGE));
        farm.ticks(PASS);
        assertThat(farm.world.blocks.get(POD)).as("left empty").isNull();
        return farm;
    }

    @Test
    void picksARipePodAndReplantsAFreshOneFacingTheSameLog() {
        FarmHarness farm = farm(3);
        farm.world.set(LOG, JUNGLE_LOG);
        farm.world.set(POD, pod(Direction.SOUTH, CocoaBlock.MAX_AGE));
        long before = farm.energy();

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactly(POD);
        assertThat(farm.world.blocks.get(POD)).isEqualTo(pod(Direction.SOUTH, 0));
        assertThat(farm.world.blocks.get(LOG)).isEqualTo(JUNGLE_LOG);
        assertThat(count(farm.items(), Items.COCOA_BEANS)).isEqualTo(2);
        assertThat(farm.energy()).isEqualTo(before - farm.cost(FarmCost.HARVEST_CROP) - farm.cost(FarmCost.PLANT));
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
    }

    @Test
    void facesTheReplantTowardsALogOnAnySide() {
        FarmHarness farm = farm(2);
        BlockPos east = LOG.east();
        farm.world.set(LOG, JUNGLE_LOG);
        farm.world.set(east, pod(Direction.WEST, CocoaBlock.MAX_AGE));

        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(east)).isEqualTo(pod(Direction.WEST, 0));
    }

    @Test
    void leavesAnUnripePodStandingButTakesItOnAsAPlot() {
        FarmHarness farm = farm(3);
        farm.world.set(LOG, JUNGLE_LOG);
        farm.world.set(POD, pod(Direction.SOUTH, 1));

        farm.ticks(PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);
        assertThat(farm.engine().emptyPlots()).isZero();
    }

    @Test
    void leavesPodsOnOtherLogsAlone() {
        FarmHarness farm = farm(3);
        farm.world.set(LOG, Blocks.OAK_LOG.defaultBlockState());
        farm.world.set(POD, pod(Direction.SOUTH, CocoaBlock.MAX_AGE));

        farm.ticks(2 * PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.engine().plotsTotal()).isZero();
    }

    @Test
    void neverTouchesTheLogs() {
        FarmHarness farm = farm(3);
        farm.world.set(LOG, JUNGLE_LOG);
        farm.world.set(POD, pod(Direction.SOUTH, CocoaBlock.MAX_AGE));
        farm.world.set(LOG.above(), JUNGLE_LOG);

        farm.ticks(2 * PASS);

        assertThat(farm.world.mutations)
                .extracting(FakeFarmWorld.Mutation::pos)
                .containsOnly(POD);
    }

    @Test
    void replantsAnEmptyPlotFromTheInventoryOnceBeansArrive() {
        FarmHarness farm = emptyPodPlot();
        assertThat(farm.engine().emptyPlots()).isEqualTo(1);

        farm.items().setItem(4, new ItemStack(Items.COCOA_BEANS, 2));
        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(POD)).isEqualTo(pod(Direction.SOUTH, 0));
        assertThat(count(farm.items(), Items.COCOA_BEANS)).isEqualTo(1);
        assertThat(farm.engine().emptyPlots()).isZero();
    }

    @Test
    void forgetsAPlotWhoseLogIsGone() {
        FarmHarness farm = emptyPodPlot();
        farm.world.blocks.remove(LOG);
        farm.ticks(PASS);

        farm.world.set(LOG, JUNGLE_LOG);
        farm.items().setItem(0, new ItemStack(Items.COCOA_BEANS, 4));
        farm.ticks(2 * PASS);

        assertThat(farm.world.blocks.get(POD)).as("not replanted").isNull();
        assertThat(count(farm.items(), Items.COCOA_BEANS)).isEqualTo(4);
        assertThat(farm.engine().plotsTotal()).isZero();
    }

    @Test
    void paysItsOwnUpkeepRateForEachPlot() {
        FarmHarness farm = FarmHarness.tending(
                new CocoaTendLogic(state -> state.is(Blocks.JUNGLE_LOG)),
                FarmHarness.SETTINGS
                        .withUpkeep(FarmUpkeep.COCOA_PLANTATION, 0.5)
                        .withUpkeep(FarmUpkeep.CROP_FARM, 5.0));
        farm.power(10_000);
        farm.world.set(LOG, JUNGLE_LOG);
        farm.world.set(POD, pod(Direction.SOUTH, 0));
        farm.world.set(LOG.south(), pod(Direction.NORTH, 0));
        farm.ticks(PASS);
        assertThat(farm.engine().plotsTotal()).isEqualTo(2);
        long before = farm.energy();

        farm.ticks(10);

        assertThat(before - farm.energy()).isEqualTo(10);
    }
}
