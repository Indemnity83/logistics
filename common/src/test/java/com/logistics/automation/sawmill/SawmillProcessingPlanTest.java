package com.logistics.automation.sawmill;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class SawmillProcessingPlanTest extends MinecraftTestEnvironment {

    @Test
    void advance_pausesWhenEnergyIsInsufficient() {
        SawmillProcessingPlan.Result result = SawmillProcessingPlan.advance(3, 10, 0, 2, true);

        assertThat(result.progress()).isEqualTo(3);
        assertThat(result.consumedEnergy()).isFalse();
        assertThat(result.complete()).isFalse();
    }

    @Test
    void advance_pausesWhenOutputCannotAccept() {
        SawmillProcessingPlan.Result result = SawmillProcessingPlan.advance(3, 10, 10, 2, false);

        assertThat(result.progress()).isEqualTo(3);
        assertThat(result.consumedEnergy()).isFalse();
    }

    @Test
    void advance_consumesEnergyAndCompletesAtTotal() {
        SawmillProcessingPlan.Result result = SawmillProcessingPlan.advance(9, 10, 10, 2, true);

        assertThat(result.progress()).isEqualTo(10);
        assertThat(result.consumedEnergy()).isTrue();
        assertThat(result.complete()).isTrue();
    }

    @Test
    void canInsert_trueIntoEmptyOutputs() {
        SimpleContainer container = new SimpleContainer(2);

        assertThat(SawmillProcessingPlan.canInsert(container, new int[] {0, 1}, new ItemStack(Items.OAK_PLANKS, 6)))
                .isTrue();
    }

    @Test
    void canInsert_falseWhenSlotFullOfAnotherItem() {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.STICK, 64));

        assertThat(SawmillProcessingPlan.canInsert(container, new int[] {0}, new ItemStack(Items.OAK_PLANKS, 6)))
                .isFalse();
    }

    @Test
    void insert_mergesIntoMatchThenFillsEmpties() {
        SimpleContainer container = new SimpleContainer(2);
        container.setItem(0, new ItemStack(Items.OAK_PLANKS, 62));

        int leftover = SawmillProcessingPlan.insert(container, new int[] {0, 1}, new ItemStack(Items.OAK_PLANKS, 6));

        assertThat(leftover).isZero();
        assertThat(container.getItem(0).getCount()).isEqualTo(64);
        assertThat(container.getItem(1).is(Items.OAK_PLANKS)).isTrue();
        assertThat(container.getItem(1).getCount()).isEqualTo(4);
    }

    @Test
    void insert_returnsLeftoverWhenNoRoom() {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        int leftover = SawmillProcessingPlan.insert(container, new int[] {0}, new ItemStack(Items.OAK_PLANKS, 6));

        assertThat(leftover).isEqualTo(6);
    }
}
