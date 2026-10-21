package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.forestry.farm.crop.CropTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm upkeep per Plot")
class FarmUpkeepTest extends MinecraftTestEnvironment {

    /** Long enough for one full pass over the Reach at one check a tick. */
    private static final int PASS = 160;

    private static final Set<Block> TENDED = Set.of(Blocks.WHEAT);
    private static final Set<Block> GROUND = Set.of(Blocks.FARMLAND);

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A Crop Farm paying {@code rate} RF/t per Plot, whose wheat drops one seed: every harvest replants. */
    private static FarmHarness cropFarm(double rate) {
        FarmHarness farm = FarmHarness.tending(
                new CropTendLogic(state -> TENDED.contains(state.getBlock()), state -> GROUND.contains(state.getBlock())),
                FarmHarness.SETTINGS.withUpkeep(FarmUpkeep.CROP_FARM, rate));
        farm.world.drops = state -> List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT_SEEDS));
        farm.power(10_000);
        return farm;
    }

    /** Ripe wheat on farmland at {@code count} positions of the Reach's middle layer. */
    private static void field(FarmHarness farm, int count) {
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        int placed = 0;
        for (int z = -3; z <= 3 && placed < count; z++) {
            for (int x = -3; x <= 3 && placed < count; x++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                BlockPos plot = new BlockPos(x, 0, z);
                farm.world.set(plot.below(), Blocks.FARMLAND.defaultBlockState());
                farm.world.set(plot, wheat.getStateForAge(wheat.getMaxAge()));
                placed++;
            }
        }
    }

    /** A farm that has harvested and replanted {@code count} Plots and has nothing ripe left. */
    private static FarmHarness withPlots(double rate, int count) {
        FarmHarness farm = cropFarm(rate);
        field(farm, count);
        farm.ticks(2 * PASS);
        assertThat(farm.engine().plotsTotal()).isEqualTo(count);
        return farm;
    }

    @Test
    void tenCropPlotsCostOneRfEachTick() {
        FarmHarness farm = withPlots(0.1, 10);

        for (int tick = 0; tick < 5; tick++) {
            long before = farm.energy();
            farm.tick();
            assertThat(before - farm.energy()).as("tick %d", tick).isEqualTo(1);
        }
    }

    @Test
    void fractionsBuildUpAndArePaidInWholeRf() {
        FarmHarness farm = withPlots(0.1, 3);
        long before = farm.energy();

        for (int tick = 0; tick < 10; tick++) {
            long start = farm.energy();
            farm.tick();
            assertThat(start - farm.energy()).as("tick %d", tick).isBetween(0L, 1L);
        }

        assertThat(before - farm.energy()).isEqualTo(3);
    }

    @Test
    void emptyPlotsPayUpkeepToo() {
        FarmHarness farm = cropFarm(0.1);
        farm.world.drops = state -> List.of(new ItemStack(Items.WHEAT));
        field(farm, 10);
        farm.ticks(2 * PASS);
        assertThat(farm.engine().emptyPlots()).isEqualTo(10);
        long before = farm.energy();

        farm.ticks(10);

        assertThat(before - farm.energy()).isEqualTo(10);
    }

    @Test
    void aFarmWithNoPlotsPaysNothing() {
        FarmHarness farm = cropFarm(5.0);

        farm.ticks(PASS);

        assertThat(farm.energy()).isEqualTo(10_000);
    }

    @Test
    void aPausedFarmPaysNoUpkeep() {
        FarmHarness farm = withPlots(0.1, 10);
        farm.host.workEnabled = false;
        long before = farm.energy();

        farm.ticks(PASS);

        assertThat(farm.energy()).isEqualTo(before);
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.PAUSED);
    }

    @Test
    void unpaidUpkeepStopsWorkKeepsThePlotsAndResumesOncePowered() {
        FarmHarness farm = cropFarm(50.0);
        field(farm, 10);
        for (int tick = 0; tick < 2 * PASS; tick++) {
            farm.power(10_000);
            farm.tick();
        }
        assertThat(farm.engine().plotsTotal()).isEqualTo(10);
        field(farm, 10);
        long harvestAndReplant = farm.cost(FarmCost.HARVEST_CROP) + farm.cost(FarmCost.PLANT);
        farm.power(harvestAndReplant + 1);
        int mutations = farm.world.mutations.size();

        farm.ticks(PASS);

        assertThat(farm.world.mutations).as("nothing harvested or replanted").hasSize(mutations);
        assertThat(farm.energy()).isEqualTo(harvestAndReplant + 1);
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.NO_POWER);
        assertThat(farm.engine().plotsTotal()).isEqualTo(10);

        for (int tick = 0; tick < 2 * PASS; tick++) {
            farm.power(10_000);
            farm.tick();
        }

        assertThat(farm.world.mutations.size()).as("harvesting again").isGreaterThan(mutations);
        assertThat(farm.engine().plotsTotal()).isEqualTo(10);
    }
}
