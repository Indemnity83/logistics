package com.logistics.automation.sawmill;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class SawmillByproductTest extends MinecraftTestEnvironment {

    @Test
    void guaranteedCount_isIntegerPartOfChance() {
        assertThat(new SawmillByproduct(Items.STICK, 1.0f).guaranteedCount()).isEqualTo(1);
        assertThat(new SawmillByproduct(Items.STICK, 1.25f).guaranteedCount()).isEqualTo(1);
        assertThat(new SawmillByproduct(Items.STICK, 0.5f).guaranteedCount()).isZero();
        assertThat(new SawmillByproduct(Items.STICK, 2.0f).guaranteedCount()).isEqualTo(2);
    }

    @Test
    void roll_wholeChanceAlwaysYieldsThatCount() {
        RandomSource random = RandomSource.create(1234L);
        SawmillByproduct one = new SawmillByproduct(Items.STICK, 1.0f);
        SawmillByproduct two = new SawmillByproduct(Items.STICK, 2.0f);
        for (int i = 0; i < 20; i++) {
            assertThat(one.roll(random)).isEqualTo(1);
            assertThat(two.roll(random)).isEqualTo(2);
        }
    }

    @Test
    void roll_fractionalChanceStaysWithinGuaranteedPlusOne() {
        RandomSource random = RandomSource.create(42L);
        SawmillByproduct bonus = new SawmillByproduct(Items.STICK, 1.25f);
        for (int i = 0; i < 50; i++) {
            assertThat(bonus.roll(random)).isBetween(1, 2);
        }
    }
}
