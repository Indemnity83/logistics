package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.farm.crop.CropTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm pace, status lamp and tier buffers")
class FarmScheduleTest extends MinecraftTestEnvironment {

    /** Positions in the Reach: one pass at one check a tick. */
    private static final int PASS = TendFarmComponent.REACH_OFFSETS.size();

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A Crop Farm logic that counts the positions it is asked about. */
    private static final class CountingLogic implements TendLogic {
        private final TendLogic crops = new CropTendLogic(state -> state.is(Blocks.WHEAT), state -> state.is(Blocks.FARMLAND));
        int visits;

        @Override
        @Nullable
        public Harvest ripe(FarmWorld world, BlockPos pos) {
            visits++;
            return crops.ripe(world, pos);
        }

        @Override
        public FarmUpkeep upkeep() {
            return crops.upkeep();
        }

        @Override
        public FarmCost harvestCost(BlockState state) {
            return crops.harvestCost(state);
        }

        @Override
        @Nullable
        public BlockState plotGround(BlockState ground) {
            return crops.plotGround(ground);
        }
    }

    @Test
    void backsOffAfterAPassWithNothingRipe() {
        CountingLogic logic = new CountingLogic();
        FarmHarness farm = FarmHarness.tending(logic);
        farm.power(10_000);

        farm.ticks(PASS);
        assertThat(logic.visits).as("one check a tick").isEqualTo(PASS);
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.IDLE);

        farm.ticks(FarmHarness.SETTINGS.idleBackoffBaseTicks() - 1);
        assertThat(logic.visits).as("backing off").isEqualTo(PASS);

        farm.ticks(2);
        assertThat(logic.visits).as("scanning again").isGreaterThan(PASS);
    }

    @Test
    void theRateOverrideChecksFasterAndRestoresTheTiers() {
        CountingLogic tierRate = new CountingLogic();
        CountingLogic raised = new CountingLogic();
        FarmHarness slow = FarmHarness.tending(tierRate);
        FarmHarness fast = FarmHarness.tending(raised);
        fast.engine().setRateOverride(80, 80);
        slow.power(10_000);
        fast.power(10_000);

        slow.ticks(6);
        fast.ticks(6);

        assertThat(tierRate.visits).as("one check a tick").isEqualTo(6);
        assertThat(raised.visits).as("four checks a tick").isEqualTo(24);

        fast.engine().setRateOverride(0, 0);
        fast.ticks(3);
        int afterRestore = raised.visits;
        fast.ticks(3);
        assertThat(raised.visits - afterRestore).as("back to one check a tick").isEqualTo(3);
    }

    @Test
    void lampIsOffWithAnEmptyBufferElseItsStatusColourAndOnlyWritesOnAChange() {
        FarmHarness farm = FarmHarness.tending(new CountingLogic());
        farm.tick();
        assertThat(farm.host.lamp()).as("empty buffer").isEqualTo(FarmLamp.OFF);
        assertThat(farm.host.stateWrites).as("already off").isZero();

        farm.power(1);
        farm.tick();
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.NO_POWER);
        assertThat(farm.host.lamp()).as("one RF, not enough for anything").isEqualTo(FarmLamp.RED);
        assertThat(farm.host.stateWrites).isEqualTo(1);

        farm.ticks(3);
        assertThat(farm.host.stateWrites).as("no write while nothing changes").isEqualTo(1);

        farm.power(10_000);
        farm.tick();
        assertThat(farm.engine().status()).isIn(FarmStatus.WORKING, FarmStatus.IDLE);
        assertThat(farm.host.lamp()).isEqualTo(FarmLamp.GREEN);
        assertThat(farm.host.stateWrites).isEqualTo(2);

        farm.host.workEnabled = false;
        farm.tick();
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.PAUSED);
        assertThat(farm.host.lamp()).isEqualTo(FarmLamp.AMBER);
        assertThat(farm.host.stateWrites).isEqualTo(3);

        farm.power(0);
        farm.tick();
        assertThat(farm.host.lamp()).as("empty buffer, whatever the status").isEqualTo(FarmLamp.OFF);
        assertThat(farm.host.stateWrites).isEqualTo(4);
    }

    @Test
    void raisingTheTierGrowsTheBufferKeepingItsCharge() {
        FarmHarness farm = shippedTiers();
        assertThat(farm.machine.energy().capacity()).isEqualTo(20_000);
        assertThat(farm.machine.energy().maxInsert()).isEqualTo(64);
        farm.power(15_000);

        assertThat(farm.machine.tier().setTier(MachineTier.INDUSTRIAL)).isTrue();

        assertThat(farm.machine.energy().capacity()).isEqualTo(40_000);
        assertThat(farm.machine.energy().maxInsert()).isEqualTo(128);
        assertThat(farm.energy()).isEqualTo(15_000);
        assertThat(farm.engine().reachSize()).as("the Reach does not grow with the tier").isEqualTo(7);
    }

    @Test
    void aReloadedFarmKeepsItsTierAndAChargeAboveTheBasicCapacity() {
        FarmHarness farm = shippedTiers();
        farm.machine.tier().setTier(MachineTier.INDUSTRIAL);
        farm.power(30_000);
        CompoundTag saved = farm.save();

        FarmHarness reloaded = shippedTiers();
        reloaded.load(saved);

        assertThat(reloaded.machine.tier().tier()).isEqualTo(MachineTier.INDUSTRIAL);
        assertThat(reloaded.machine.energy().capacity()).isEqualTo(40_000);
        assertThat(reloaded.energy()).as("not clamped to Basic's 20 000").isEqualTo(30_000);
    }

    private static FarmHarness shippedTiers() {
        return FarmHarness.tending(
                new CountingLogic(), FarmHarness.SETTINGS.withTiers(FarmHarness.shippedTiersAtTestRate()));
    }
}
