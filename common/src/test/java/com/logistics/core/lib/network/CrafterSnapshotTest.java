package com.logistics.core.lib.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.test.MinecraftTestEnvironment;
import com.logistics.test.TestItemKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CrafterSnapshot")
class CrafterSnapshotTest extends MinecraftTestEnvironment {

    private static IItemKey key() {
        return new TestItemKey(Items.COBBLESTONE);
    }

    private static RecipeIngredient ingredient() {
        return new RecipeIngredient(key(), 1);
    }

    private static CrafterSnapshot snapshot(List<RecipeIngredient> ingredients) {
        return new CrafterSnapshot(BlockPos.ZERO, key(), 4, ingredients, new CrafterBufferState(3, 10));
    }

    @Test
    @DisplayName("availableBatchCapacity delegates to the buffer state")
    void delegatesCapacity() {
        assertThat(snapshot(List.of(ingredient())).availableBatchCapacity()).isEqualTo(3);
    }

    @Nested
    @DisplayName("ingredients are captured defensively")
    class DefensiveCopy {

        @Test
        @DisplayName("mutating the source list after capture does not affect the snapshot")
        void sourceMutationIsolated() {
            List<RecipeIngredient> source = new ArrayList<>();
            source.add(ingredient());

            CrafterSnapshot snapshot = snapshot(source);
            source.add(ingredient());

            assertThat(snapshot.ingredients()).hasSize(1);
        }

        @Test
        @DisplayName("the exposed ingredient list is immutable")
        void exposedListImmutable() {
            CrafterSnapshot snapshot = snapshot(List.of(ingredient()));
            assertThatThrownBy(() -> snapshot.ingredients().add(ingredient()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("rejects a null position")
        void rejectsNullPos() {
            assertThatThrownBy(() ->
                            new CrafterSnapshot(null, key(), 4, List.of(), new CrafterBufferState(1, 1)))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a null output key")
        void rejectsNullOutput() {
            assertThatThrownBy(() ->
                            new CrafterSnapshot(BlockPos.ZERO, null, 4, List.of(), new CrafterBufferState(1, 1)))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a non-positive output count")
        void rejectsNonPositiveOutputCount() {
            assertThatThrownBy(() ->
                            new CrafterSnapshot(BlockPos.ZERO, key(), 0, List.of(), new CrafterBufferState(1, 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a null buffer state")
        void rejectsNullBuffer() {
            assertThatThrownBy(() -> new CrafterSnapshot(BlockPos.ZERO, key(), 4, List.of(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
