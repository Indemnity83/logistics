package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class ChanceOutputTest extends MinecraftTestEnvironment {

    private static ChanceOutput of(float chance) {
        return new ChanceOutput(new ItemStack(Items.GUNPOWDER), chance);
    }

    @Test
    void guaranteedCountIsTheIntegerPart() {
        assertThat(of(0.25f).guaranteedCount()).isZero();
        assertThat(of(1.0f).guaranteedCount()).isEqualTo(1);
        assertThat(of(1.25f).guaranteedCount()).isEqualTo(1);
        assertThat(of(2.0f).guaranteedCount()).isEqualTo(2);
    }

    @Test
    void maxCountIncludesTheFractionalBonus() {
        assertThat(of(0.0f).maxCount()).isZero();
        assertThat(of(0.25f).maxCount()).isEqualTo(1); // a purely fractional bonus can still yield one
        assertThat(of(1.0f).maxCount()).isEqualTo(1);
        assertThat(of(1.25f).maxCount()).isEqualTo(2);
        assertThat(of(2.0f).maxCount()).isEqualTo(2);
    }

    @Test
    void wholeChanceAlwaysRollsExactly() {
        RandomSource random = RandomSource.create(1L);
        for (int i = 0; i < 100; i++) {
            assertThat(of(1.0f).roll(random)).isEqualTo(1);
            assertThat(of(0.0f).roll(random)).isZero();
        }
    }

    @Test
    void fractionalChanceRollsGuaranteedPlusBonus() {
        RandomSource random = RandomSource.create(7L);
        int twos = 0;
        for (int i = 0; i < 1000; i++) {
            int rolled = of(1.25f).roll(random);
            assertThat(rolled).isBetween(1, 2);
            if (rolled == 2) {
                twos++;
            }
        }
        // ~25% of rolls should produce the bonus item (loose bounds, deterministic seed).
        assertThat(twos).isBetween(150, 350);
    }

    @Test
    void rejectsNegativeNanAndInfiniteChance() {
        assertThatThrownBy(() -> of(-1.0f)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> of(Float.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> of(Float.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void displayCountShowsTheGuaranteedYieldFlooredAtOne() {
        // What a recipe listing renders in the byproduct slot: the guaranteed count, never zero.
        assertThat(of(0.25f).displayCount()).isEqualTo(1);
        assertThat(of(1.0f).displayCount()).isEqualTo(1);
        assertThat(of(1.25f).displayCount()).isEqualTo(1);
        assertThat(of(2.0f).displayCount()).isEqualTo(2);
        assertThat(of(4.0f).displayCount()).isEqualTo(4);
        assertThat(of(0.25f).displayStack().getCount()).isEqualTo(1);
        assertThat(of(2.0f).displayStack().getCount()).isEqualTo(2);
    }

    @Test
    void fractionalBonusIsTheRemainderNotTheWholeChance() {
        // The advertised chance is the remainder beyond the guaranteed count: 1.25 is 25%, not 125%.
        assertThat(of(0.25f).fractionalBonus()).isEqualTo(0.25f);
        assertThat(of(1.0f).fractionalBonus()).isZero();
        assertThat(of(1.25f).fractionalBonus()).isEqualTo(0.25f);
        assertThat(of(2.0f).fractionalBonus()).isZero();
    }

    @Test
    void aWholeChanceAdvertisesNoBonusAndDisplaysExactlyWhatItPaysOut() {
        // With nothing left over to advertise, the displayed count is the whole story — so it has to
        // equal every roll, not sit at one with the surplus hidden in a percentage.
        RandomSource random = RandomSource.create(3L);
        for (float chance : new float[] {1.0f, 2.0f, 4.0f}) {
            ChanceOutput output = of(chance);
            assertThat(output.fractionalBonus()).isZero();
            for (int i = 0; i < 100; i++) {
                assertThat(output.roll(random)).isEqualTo(output.displayCount());
            }
        }
    }

    @Test
    void stackCopiesTemplateWithCount() {
        assertThat(of(1.25f).stack(3).getCount()).isEqualTo(3);
        assertThat(of(1.25f).guaranteedStack().getCount()).isEqualTo(1);
    }
}
