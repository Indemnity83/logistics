package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Usable and blocked plot counts, as the GUI and Jade report them. */
@DisplayName("Farm plot counts")
class FarmPlotCountsTest extends MinecraftTestEnvironment {

    /** Longer than any idle backoff in the test settings, so every plot gets visited again. */
    private static final int AFTER_A_PASS = 400;

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    // ==================== BlockedPlots ====================

    @Test
    @DisplayName("a plot counts as blocked until a later visit finds it workable")
    void laterVisitClearsAPlot() {
        BlockedPlots plots = new BlockedPlots();
        plots.resize(8);

        plots.record(1, FarmStatus.BLOCKED);
        plots.record(2, FarmStatus.TOO_DARK);
        plots.record(3, FarmStatus.WORKING);
        assertThat(plots.count()).isEqualTo(2);

        plots.record(1, FarmStatus.IDLE);
        assertThat(plots.count()).isEqualTo(1);
        plots.record(2, FarmStatus.WORKING);
        assertThat(plots.count()).isZero();
    }

    @Test
    @DisplayName("only Blocked and Too dark count; supply problems do not")
    void onlyPlotProblemsCount() {
        for (FarmStatus outcome : FarmStatus.values()) {
            boolean expected = outcome == FarmStatus.BLOCKED || outcome == FarmStatus.TOO_DARK;
            assertThat(BlockedPlots.isBlocked(outcome)).as("%s", outcome).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("a wider ring keeps what is known; a narrower one drops the plots past its end")
    void resizeFollowsTheRing() {
        BlockedPlots plots = new BlockedPlots();
        plots.resize(8);
        plots.record(0, FarmStatus.BLOCKED);
        plots.record(7, FarmStatus.BLOCKED);

        plots.resize(24);
        assertThat(plots.count()).isEqualTo(2);
        plots.record(20, FarmStatus.BLOCKED);
        assertThat(plots.count()).isEqualTo(3);

        plots.resize(8);
        assertThat(plots.count()).isEqualTo(2);
        plots.record(20, FarmStatus.BLOCKED);
        assertThat(plots.count()).as("an index outside the ring is ignored").isEqualTo(2);
    }

    // ==================== FarmComponent ====================

    @Test
    @DisplayName("blocked plots are counted per pass, and one that becomes workable drops out")
    void farmCountsBlockedPlots() {
        Set<BlockPos> dark = new HashSet<>();
        Set<BlockPos> obstructed = new HashSet<>();
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) -> {
            if (dark.contains(plot)) {
                return PlotAction.blocked(PlotAction.Reason.TOO_DARK);
            }
            return obstructed.contains(plot) ? PlotAction.blocked(PlotAction.Reason.OBSTRUCTED) : PlotAction.NONE;
        }));
        farm.power(10_000);
        dark.add(farm.plot(0));
        obstructed.add(farm.plot(7));

        assertThat(farm.farm().plotsTotal()).isEqualTo(8);
        assertThat(farm.farm().plotsBlocked()).as("nothing visited yet").isZero();

        farm.tick();
        assertThat(farm.farm().plotsBlocked()).as("what is known before the first pass").isEqualTo(1);

        farm.ticks(AFTER_A_PASS);
        assertThat(farm.farm().plotsBlocked()).isEqualTo(2);

        dark.clear();
        farm.ticks(AFTER_A_PASS);
        assertThat(farm.farm().plotsBlocked()).isEqualTo(1);
        assertThat(farm.farm().plotsTotal()).isEqualTo(8);
    }

    @Test
    @DisplayName("a plot claim protection refuses counts as blocked")
    void protectedPlotIsBlocked() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic()
                .script((world, plot) -> world.state(plot).isAir()
                        ? PlotAction.plant(plot, new ItemStack(Items.OAK_SAPLING))
                        : PlotAction.NONE));
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));
        farm.power(1_000);
        farm.world.claimed.add(farm.plot(0));

        farm.tick();

        assertThat(farm.farm().status()).isEqualTo(FarmStatus.BLOCKED);
        assertThat(farm.farm().plotsBlocked()).isEqualTo(1);
    }

    @Test
    @DisplayName("after a tier change the counts cover the wider ring and keep the plots already known")
    void tierChangeResizesTheCounts() {
        Set<BlockPos> obstructed = new HashSet<>();
        FarmHarness farm = new FarmHarness(
                new ScriptedFarmLogic().script((world, plot) ->
                        obstructed.contains(plot) ? PlotAction.blocked(PlotAction.Reason.OBSTRUCTED) : PlotAction.NONE),
                FarmHarness.SETTINGS.withTiers(FarmHarness.shippedTiersAtTestRate()));
        farm.areaOverride(0, 0);
        farm.power(10_000);
        obstructed.add(farm.plot(0));

        farm.ticks(AFTER_A_PASS);
        assertThat(farm.farm().plotsTotal()).isEqualTo(72);
        assertThat(farm.farm().plotsBlocked()).isEqualTo(1);

        farm.machine.tier().setTier(MachineTier.INDUSTRIAL);
        assertThat(farm.farm().plotsTotal()).isEqualTo(176);
        assertThat(farm.farm().plotsBlocked()).isEqualTo(1);

        obstructed.add(farm.plot(150));
        farm.ticks(AFTER_A_PASS);
        assertThat(farm.farm().plotsBlocked()).isEqualTo(2);
    }
}
