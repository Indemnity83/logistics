package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.machine.area.RingArea;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm tiers")
class FarmTierTest extends MinecraftTestEnvironment {

    /** One plot per tick, with the shipped tier profiles. */
    private static final FarmSettings TIERED = new FarmSettings(
            1, 40, 30, 100, 60, 60, 20, 250, 4, 20, 200, FarmSettings.defaultTiers());

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A water-using farm working its tier's ring, recording every plot it decides on. */
    private static FarmHarness farm(List<BlockPos> visited) {
        ScriptedFarmLogic logic = new ScriptedFarmLogic(true, false).script((world, plot) -> {
            visited.add(plot.immutable());
            return PlotAction.NONE;
        });
        FarmHarness farm = new FarmHarness(logic, TIERED);
        farm.areaOverride(0, 0);
        return farm;
    }

    @Test
    @DisplayName("each tier's ring around the 7×7 courtyard has 72, 176, 312 and 480 plots")
    void plotCountPerTier() {
        Map<MachineTier, Integer> expected = Map.of(
                MachineTier.BASIC, 72,
                MachineTier.INDUSTRIAL, 176,
                MachineTier.PRECISION, 312,
                MachineTier.INFERNAL, 480);
        for (MachineTier tier : MachineTier.values()) {
            RingArea ring = FarmGeometry.ring(BlockPos.ZERO, FarmTierProfile.defaults(tier).outerSize());
            assertThat(ring.plotCount()).as("%s", tier).isEqualTo(expected.get(tier));
            assertThat(ring.inner()).isEqualTo(FarmGeometry.COURTYARD);
        }
    }

    @Test
    @DisplayName("a wider tier only adds lanes: every plot of the narrower ring keeps its index, lane and step")
    void lanesAreStable() {
        RingArea basic = FarmGeometry.ring(BlockPos.ZERO, 11);
        for (MachineTier tier : MachineTier.values()) {
            RingArea wider = FarmGeometry.ring(BlockPos.ZERO, FarmTierProfile.defaults(tier).outerSize());
            for (int i = 0; i < basic.plotCount(); i++) {
                assertThat(wider.plot(i)).as("%s plot %d", tier, i).isEqualTo(basic.plot(i));
                assertThat(wider.lane(i)).isEqualTo(basic.lane(i));
                assertThat(wider.step(i)).isEqualTo(basic.step(i));
            }
        }
    }

    @Test
    @DisplayName("a new farm works the Basic ring with the Basic buffers")
    void startsAtBasic() {
        FarmHarness farm = farm(new ArrayList<>());

        assertThat(farm.machine.tier().tier()).isEqualTo(MachineTier.BASIC);
        assertThat(farm.farm().areaSize()).isEqualTo(11);
        assertThat(farm.machine.energy().capacity()).isEqualTo(20_000);
        assertThat(farm.machine.energy().maxInsert()).isEqualTo(64);
        assertThat(farm.machine.water().tank().getCapacity()).isEqualTo(FluidUnits.mb(8_000));
    }

    @Test
    @DisplayName("raising the tier grows the ring and the buffers, keeping what they hold")
    void tierChangeGrowsRingAndBuffers() {
        FarmHarness farm = farm(new ArrayList<>());
        farm.power(15_000);
        farm.machine.water().tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(6_000), false);

        assertThat(farm.machine.tier().setTier(MachineTier.INDUSTRIAL)).isTrue();

        assertThat(farm.farm().areaSize()).isEqualTo(15);
        assertThat(farm.machine.energy().capacity()).isEqualTo(40_000);
        assertThat(farm.machine.energy().maxInsert()).isEqualTo(128);
        assertThat(farm.machine.water().tank().getCapacity()).isEqualTo(FluidUnits.mb(16_000));
        assertThat(farm.energy()).isEqualTo(15_000);
        assertThat(farm.machine.water().tank().getAmount()).isEqualTo(FluidUnits.mb(6_000));

        farm.machine.tier().setTier(MachineTier.INFERNAL);
        assertThat(farm.farm().areaSize()).isEqualTo(23);
        assertThat(farm.machine.energy().capacity()).isEqualTo(160_000);
        assertThat(farm.machine.water().tank().getCapacity()).isEqualTo(FluidUnits.mb(64_000));
    }

    @Test
    @DisplayName("after an upgrade the scan carries on where it was and then covers the wider ring, never the courtyard")
    void upgradeKeepsTheScanAndWorksTheNewLanes() {
        List<BlockPos> visited = new ArrayList<>();
        FarmHarness farm = farm(visited);
        farm.power(10_000);
        RingArea basic = farm.area();

        farm.ticks(5);
        farm.machine.tier().setTier(MachineTier.INDUSTRIAL);
        RingArea industrial = farm.area();
        farm.ticks(industrial.plotCount() - 5);

        assertThat(visited.subList(0, 6)).containsExactly(
                basic.plot(0), basic.plot(1), basic.plot(2), basic.plot(3), basic.plot(4), basic.plot(5));
        assertThat(visited).doesNotHaveDuplicates().hasSize(industrial.plotCount());
        for (int i = 0; i < industrial.plotCount(); i++) {
            assertThat(visited).contains(industrial.plot(i));
        }
        assertThat(visited).noneMatch(industrial.keepOut()::contains);
    }

    @Test
    @DisplayName("a reloaded farm keeps its tier, ring and a charge above the Basic capacity")
    void tierSurvivesReload() {
        FarmHarness farm = farm(new ArrayList<>());
        farm.machine.tier().setTier(MachineTier.INDUSTRIAL);
        farm.power(30_000);
        farm.machine.water().tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(12_000), false);
        CompoundTag saved = farm.save();

        FarmHarness reloaded = farm(new ArrayList<>());
        reloaded.load(saved);

        assertThat(reloaded.machine.tier().tier()).isEqualTo(MachineTier.INDUSTRIAL);
        assertThat(reloaded.farm().areaSize()).isEqualTo(15);
        assertThat(reloaded.machine.energy().capacity()).isEqualTo(40_000);
        assertThat(reloaded.energy()).as("not clamped to Basic's 20 000").isEqualTo(30_000);
        assertThat(reloaded.machine.water().tank().getAmount()).isEqualTo(FluidUnits.mb(12_000));
    }

    @Test
    @DisplayName("the area override still wins over the tier")
    void overrideWinsOverTier() {
        FarmHarness farm = farm(new ArrayList<>());
        farm.areaOverride(1, 5);

        farm.machine.tier().setTier(MachineTier.PRECISION);

        assertThat(farm.farm().areaSize()).isEqualTo(5);
        assertThat(farm.machine.energy().capacity()).isEqualTo(80_000);
    }
}
