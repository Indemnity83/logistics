package com.logistics.forestry.fertilizer;

import static org.assertj.core.api.Assertions.assertThat;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.DomainRegistrations;
import com.logistics.LogisticsForestry;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FertilizerCharges")
class FertilizerChargesTest extends MinecraftTestEnvironment {

    // The registry is shared with every other class in the fork, so put back what was there.
    private Config fertilizer;
    private int originalFertilizerCharges;
    private int originalBoneMealCharges;

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    @BeforeEach
    void captureConfig() {
        fertilizer = ConfigRegistry.config(LogisticsForestry.CONFIG.FERTILIZER_CHARGES.configId());
        originalFertilizerCharges = fertilizer.get(LogisticsForestry.CONFIG.FERTILIZER_CHARGES);
        originalBoneMealCharges = fertilizer.get(LogisticsForestry.CONFIG.BONE_MEAL_CHARGES);
    }

    @AfterEach
    void restoreConfig() {
        fertilizer.set(LogisticsForestry.CONFIG.FERTILIZER_CHARGES, originalFertilizerCharges);
        fertilizer.set(LogisticsForestry.CONFIG.BONE_MEAL_CHARGES, originalBoneMealCharges);
    }

    @Test
    @DisplayName("defaults: Fertilizer is worth 8 charges, bone meal 1")
    void defaults() {
        assertThat(LogisticsForestry.CONFIG.FERTILIZER_CHARGES.definition().defaultValue()).isEqualTo(8);
        assertThat(LogisticsForestry.CONFIG.BONE_MEAL_CHARGES.definition().defaultValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fertilizer is clearly better than bone meal per item at the defaults")
    void fertilizerBeatsBoneMeal() {
        fertilizer.set(LogisticsForestry.CONFIG.FERTILIZER_CHARGES, 8);
        fertilizer.set(LogisticsForestry.CONFIG.BONE_MEAL_CHARGES, 1);

        assertThat(FertilizerCharges.charges(new ItemStack(LogisticsForestry.ITEM.FERTILIZER))).isEqualTo(8);
        assertThat(FertilizerCharges.charges(new ItemStack(Items.BONE_MEAL))).isEqualTo(1);
    }

    @Test
    @DisplayName("reads the configured values")
    void followsConfig() {
        fertilizer.set(LogisticsForestry.CONFIG.FERTILIZER_CHARGES, 12);
        fertilizer.set(LogisticsForestry.CONFIG.BONE_MEAL_CHARGES, 3);

        assertThat(FertilizerCharges.charges(new ItemStack(LogisticsForestry.ITEM.FERTILIZER))).isEqualTo(12);
        assertThat(FertilizerCharges.charges(new ItemStack(Items.BONE_MEAL))).isEqualTo(3);
    }

    @Test
    @DisplayName("counts one item, not the whole stack")
    void ignoresStackCount() {
        fertilizer.set(LogisticsForestry.CONFIG.FERTILIZER_CHARGES, 8);

        assertThat(FertilizerCharges.charges(new ItemStack(LogisticsForestry.ITEM.FERTILIZER, 64))).isEqualTo(8);
    }

    @Test
    @DisplayName("anything else, and an empty stack, is worth nothing")
    void othersAreWorthNothing() {
        assertThat(FertilizerCharges.charges(ItemStack.EMPTY)).isZero();
        assertThat(FertilizerCharges.charges(new ItemStack(Items.DIRT))).isZero();
        assertThat(FertilizerCharges.charges(new ItemStack(Items.WHEAT_SEEDS))).isZero();
    }
}
