package com.logistics.fabric.storage;

import com.logistics.core.lib.storage.IItemKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.logistics.fabric.storage.FabricStorageTestSupport.DIAMOND;
import static com.logistics.fabric.storage.FabricStorageTestSupport.GOLD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FabricItemKey} equality, which the staging map in {@code FabricItemStorage.asFabric()}
 * depends on: pending deltas are keyed by {@code IItemKey}, so a key that does not hash and
 * compare by its variant would stage every transfer under a fresh entry.
 */
@DisplayName("FabricItemKey")
class FabricItemKeyTest {

    @Test
    @DisplayName("two keys for the same variant are equal and hash alike")
    void sameVariant_isEqual() {
        FabricItemKey copy = new FabricItemKey(DIAMOND.variant());

        assertThat(copy).isEqualTo(DIAMOND);
        assertThat(DIAMOND).isEqualTo(copy);
        assertThat(copy).hasSameHashCodeAs(DIAMOND);
    }

    @Test
    @DisplayName("keys for different items are not equal")
    void differentVariant_isNotEqual() {
        assertThat(DIAMOND).isNotEqualTo(GOLD);
    }

    @Test
    @DisplayName("a separately built key finds the entry an equal key stored")
    void isUsableAsAMapKey() {
        // Exactly how the pending-delta map is used: keys are rebuilt from the incoming variant
        // on every call, never carried over from the previous one.
        Map<IItemKey, Long> deltas = new HashMap<>();
        deltas.put(DIAMOND, 7L);

        assertThat(deltas).containsEntry(new FabricItemKey(DIAMOND.variant()), 7L);
        assertThat(deltas).doesNotContainKey(GOLD);
    }

    @Test
    @DisplayName("a null variant is rejected at construction")
    void nullVariant_isRejected() {
        assertThatThrownBy(() -> new FabricItemKey(null)).isInstanceOf(NullPointerException.class);
    }
}
