package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.tier.MachineTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm work budget")
class WorkBudgetTest {

    private static int actionsThisTick(WorkBudget budget) {
        int actions = 0;
        while (budget.hasAction()) {
            budget.spendAction();
            actions++;
        }
        return actions;
    }

    @Test
    @DisplayName("each tier's ceilings: at most two ticks' worth, never less than one")
    void ceilings() {
        assertThat(WorkBudget.actionCeiling(4)).isEqualTo(1);
        assertThat(WorkBudget.actionCeiling(8)).isEqualTo(1);
        assertThat(WorkBudget.actionCeiling(16)).isEqualTo(2);
        assertThat(WorkBudget.actionCeiling(32)).isEqualTo(4);
        assertThat(WorkBudget.readCeiling(16, 64)).isEqualTo(128);
        assertThat(WorkBudget.readCeiling(128, 64)).isEqualTo(13 * 64);
        assertThat(WorkBudget.readCeiling(1, 64)).isEqualTo(64);
    }

    @Test
    @DisplayName("refills at the rate, exactly, and never holds more than the ceiling however long it idles")
    void refillsAtTheRateWithinTheCeiling() {
        for (MachineTier tier : MachineTier.values()) {
            FarmTierProfile profile = FarmTierProfile.defaults(tier);
            WorkBudget budget = new WorkBudget();
            for (int tick = 0; tick < 10_000; tick++) {
                budget.refill(profile.actionsPerSecond(), profile.checksPerSecond(), 64);
            }
            assertThat(actionsThisTick(budget)).as("%s after a long idle", tier)
                    .isLessThanOrEqualTo(WorkBudget.actionCeiling(profile.actionsPerSecond()));
            assertThat(budget.reads()).isLessThanOrEqualTo(WorkBudget.readCeiling(profile.checksPerSecond(), 64));

            WorkBudget busy = new WorkBudget();
            int actions = 0;
            long reads = 0;
            for (int tick = 0; tick < 20 * 60; tick++) {
                busy.refill(profile.actionsPerSecond(), profile.checksPerSecond(), 64);
                actions += actionsThisTick(busy);
                reads += busy.reads();
                busy.spendReads(busy.reads());
            }
            assertThat(actions).as("%s actions a minute", tier).isEqualTo(60 * profile.actionsPerSecond());
            assertThat(reads).as("%s reads a minute", tier).isEqualTo(60L * profile.checksPerSecond() * 64);
        }
    }

    @Test
    @DisplayName("a staggered start holds less than one action")
    void stagger() {
        WorkBudget budget = new WorkBudget();
        budget.stagger(-12_345L);
        assertThat(budget.hasAction()).isFalse();
    }

    @Test
    @DisplayName("a read debt is paid off by later refills")
    void readDebt() {
        WorkBudget budget = new WorkBudget();
        budget.refill(4, 16, 64);
        budget.spendReads(200);
        assertThat(budget.reads()).isZero();
        budget.refill(4, 16, 64);
        budget.refill(4, 16, 64);
        assertThat(budget.reads()).as("51.2 a tick: 153.6 refilled, 200 owed").isZero();
        budget.refill(4, 16, 64);
        assertThat(budget.reads()).isEqualTo(4);
    }
}
