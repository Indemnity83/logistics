package com.logistics.core.machine;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class RecipeInputFilterTest extends MinecraftTestEnvironment {

    private static RecipeInputFilter<Object> filter(MachineContext host) {
        return RecipeInputFilter.of(host, Object.class, (recipe, stack) -> true);
    }

    @Test
    void emptyStackIsNeverInsertable() {
        assertThat(filter(new FakeMachineContext()).test(ItemStack.EMPTY)).isFalse();
    }

    @Test
    void withoutARecipeManagerTheGateIsPermissive() {
        // No recipe manager off-server / mid-load: refusing everything there would strand items the
        // machine can in fact use. The server tick re-validates.
        assertThat(filter(new FakeMachineContext()).test(new ItemStack(Items.DIRT))).isTrue();
    }
}
