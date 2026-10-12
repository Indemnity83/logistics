package com.logistics.pipe.modules;

import com.logistics.core.lib.network.IngredientChecker;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CraftingModule ingredient check")
class CraftingModuleIngredientCheckTest extends MinecraftTestEnvironment {

    private record Call(String itemId, long amount) {}

    private CraftingModule module;
    private PipeContext ctx;
    private final List<Call> calls = new ArrayList<>();
    private final IngredientChecker recordingChecker = (stack, amount) -> {
        calls.add(new Call(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), amount));
        return List.<IItemKey>of();
    };

    @BeforeEach
    void setUp() {
        module = new CraftingModule(64, 4);
        ctx = new PipeContext(null, BlockPos.ZERO, null, new FakePipeAccess());
        calls.clear();
    }

    private void configureCraftingTable() {
        for (int slot : new int[] {0, 1, 3, 4}) {
            module.setIngredient(ctx, slot, "minecraft:oak_planks", 1);
        }
        module.setResult(ctx, "minecraft:crafting_table", 1);
    }

    @Test
    @DisplayName("ingredientTotals sums one ingredient spread over several slots")
    void ingredientTotals_sumsRepeatedIngredient() {
        configureCraftingTable();
        module.setIngredient(ctx, 8, "minecraft:stick", 2);

        assertThat(module.ingredientTotals(ctx, 3))
                .containsExactly(Map.entry("minecraft:oak_planks", 12L), Map.entry("minecraft:stick", 6L));
    }

    @Test
    @DisplayName("checks a multi-slot ingredient once with the aggregated amount")
    void checkIngredients_multiSlotIngredient_checkedOnce() {
        configureCraftingTable();

        module.checkIngredients(ctx, 1, recordingChecker);

        assertThat(calls).containsExactly(new Call("minecraft:oak_planks", 4));
    }

    @Test
    @DisplayName("rounds the requested amount up to whole batches")
    void checkIngredients_roundsUpToWholeBatches() {
        module.setIngredient(ctx, 0, "minecraft:oak_log", 1);
        module.setIngredient(ctx, 1, "minecraft:oak_log", 1);
        module.setResult(ctx, "minecraft:oak_planks", 4);

        module.checkIngredients(ctx, 5, recordingChecker);

        assertThat(calls).containsExactly(new Call("minecraft:oak_log", 4));
    }

    @Test
    @DisplayName("an unknown ingredient id is checked once as an air placeholder")
    void checkIngredients_unknownId_checkedOnceAsPlaceholder() {
        module.setIngredient(ctx, 0, "logistics:no_such_item", 1);
        module.setIngredient(ctx, 1, "logistics:no_such_item", 1);
        module.setResult(ctx, "minecraft:crafting_table", 1);
        IItemKey missingKey = ItemStorageLookup.of(new ItemStack(Items.STONE));

        List<IItemKey> missing = module.checkIngredients(ctx, 1, (stack, amount) -> {
            calls.add(new Call(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), amount));
            return List.of(missingKey);
        });

        assertThat(calls).containsExactly(new Call("minecraft:air", 2));
        assertThat(missing).containsExactly(missingKey);
    }
}
