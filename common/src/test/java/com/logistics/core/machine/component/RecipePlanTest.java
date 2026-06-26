package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class RecipePlanTest extends MinecraftTestEnvironment {

    @Test
    void singleInputConvenienceConstructorDefaultsCountAndByproducts() {
        RecipePlan plan = new RecipePlan(2_400, new ItemStack(Items.IRON_INGOT), 0.7f);

        assertThat(plan.energyRequired()).isEqualTo(2_400);
        assertThat(plan.inputCount()).isEqualTo(1);
        assertThat(plan.result().getItem()).isEqualTo(Items.IRON_INGOT);
        assertThat(plan.byproducts()).isEmpty();
        assertThat(plan.experience()).isEqualTo(0.7f);
    }
}
