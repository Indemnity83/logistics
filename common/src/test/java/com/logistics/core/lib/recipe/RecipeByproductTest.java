package com.logistics.core.lib.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The shared chance-byproduct record round-trips and validates its chance. */
class RecipeByproductTest extends MinecraftTestEnvironment {

    private RegistryOps<Tag> ops;

    @BeforeEach
    void setUp() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ops = registries.createSerializationContext(NbtOps.INSTANCE);
    }

    @Test
    @DisplayName("round-trips a valid chance byproduct")
    void roundTripPreservesByproduct() {
        RecipeByproduct decoded = RecipeByproduct.CODEC.parse(ops, byproductTag(0.5f)).getOrThrow();

        assertThat(decoded.item()).isEqualTo(Items.GOLD_NUGGET);
        assertThat(decoded.chance()).isEqualTo(0.5f);
    }

    @Test
    @DisplayName("rejects a non-finite or negative byproduct chance on decode")
    void rejectsInvalidByproductChance() {
        assertThatThrownBy(() -> RecipeByproduct.CODEC.parse(ops, byproductTag(-0.1f)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RecipeByproduct.CODEC.parse(ops, byproductTag(Float.NaN)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Tag byproductTag(float chance) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:gold_nugget");
        tag.putFloat("chance", chance);
        return tag;
    }
}
