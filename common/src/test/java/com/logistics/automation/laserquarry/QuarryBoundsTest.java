package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.laserquarry.entity.QuarryBounds;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuarryBounds")
class QuarryBoundsTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("defaults to non-custom with zeroed values")
    void defaultsToNonCustom() {
        QuarryBounds bounds = new QuarryBounds();

        assertThat(bounds.isCustom()).isFalse();
        assertThat(bounds.getMinX()).isZero();
        assertThat(bounds.getMinZ()).isZero();
        assertThat(bounds.getMaxX()).isZero();
        assertThat(bounds.getMaxZ()).isZero();
    }

    @Test
    @DisplayName("setCustom stores the supplied bounds and flips isCustom to true")
    void setCustomStoresValues() {
        QuarryBounds bounds = new QuarryBounds();
        bounds.setCustom(-5, 10, 15, 30);

        assertThat(bounds.isCustom()).isTrue();
        assertThat(bounds.getMinX()).isEqualTo(-5);
        assertThat(bounds.getMinZ()).isEqualTo(10);
        assertThat(bounds.getMaxX()).isEqualTo(15);
        assertThat(bounds.getMaxZ()).isEqualTo(30);
    }

    @Test
    @DisplayName("clear resets values and disables custom mode")
    void clearResets() {
        QuarryBounds bounds = new QuarryBounds();
        bounds.setCustom(-5, 10, 15, 30);

        bounds.clear();

        assertThat(bounds.isCustom()).isFalse();
        assertThat(bounds.getMinX()).isZero();
        assertThat(bounds.getMinZ()).isZero();
        assertThat(bounds.getMaxX()).isZero();
        assertThat(bounds.getMaxZ()).isZero();
    }

    @Test
    @DisplayName("round-trips custom bounds through NBT")
    void roundTripsCustomBounds() {
        QuarryBounds original = new QuarryBounds();
        original.setCustom(-12, 5, 12, 33);
        CompoundTag tag = new CompoundTag();
        original.save(tag);

        QuarryBounds loaded = new QuarryBounds();
        loaded.load(tag);

        assertThat(loaded.isCustom()).isTrue();
        assertThat(loaded.getMinX()).isEqualTo(-12);
        assertThat(loaded.getMinZ()).isEqualTo(5);
        assertThat(loaded.getMaxX()).isEqualTo(12);
        assertThat(loaded.getMaxZ()).isEqualTo(33);
    }

    @Test
    @DisplayName("uses the existing CustomBounds* NBT keys for compatibility")
    void usesExistingNbtKeys() {
        QuarryBounds bounds = new QuarryBounds();
        bounds.setCustom(1, 2, 3, 4);
        CompoundTag tag = new CompoundTag();
        bounds.save(tag);

        assertThat(tag.getBoolean("UseCustomBounds")).hasValue(true);
        assertThat(tag.getInt("CustomBoundsMinX")).hasValue(1);
        assertThat(tag.getInt("CustomBoundsMinZ")).hasValue(2);
        assertThat(tag.getInt("CustomBoundsMaxX")).hasValue(3);
        assertThat(tag.getInt("CustomBoundsMaxZ")).hasValue(4);
    }

    @Test
    @DisplayName("does not write bound values when not custom")
    void omitsBoundValuesWhenNotCustom() {
        QuarryBounds bounds = new QuarryBounds();
        CompoundTag tag = new CompoundTag();
        bounds.save(tag);

        assertThat(tag.getBoolean("UseCustomBounds")).hasValue(false);
        assertThat(tag.contains("CustomBoundsMinX")).isFalse();
        assertThat(tag.contains("CustomBoundsMaxX")).isFalse();
    }

    @Test
    @DisplayName("load with missing flag yields default non-custom bounds")
    void loadFromEmptyTag() {
        QuarryBounds bounds = new QuarryBounds();
        bounds.setCustom(7, 8, 9, 10);

        bounds.load(new CompoundTag());

        assertThat(bounds.isCustom()).isFalse();
        assertThat(bounds.getMinX()).isZero();
        assertThat(bounds.getMaxZ()).isZero();
    }

    @Test
    @DisplayName("loadLegacy promotes raw values to a custom configuration")
    void loadLegacyApplies() {
        QuarryBounds bounds = new QuarryBounds();
        bounds.loadLegacy(-1, -2, 5, 6);

        assertThat(bounds.isCustom()).isTrue();
        assertThat(bounds.getMinX()).isEqualTo(-1);
        assertThat(bounds.getMinZ()).isEqualTo(-2);
        assertThat(bounds.getMaxX()).isEqualTo(5);
        assertThat(bounds.getMaxZ()).isEqualTo(6);
    }
}
