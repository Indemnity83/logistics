package com.logistics.automation.laserquarry.entity;

import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * When a quarry counts as running for chunk loading: while powered, and for
 * {@link QuarryChunkHold#GRACE_TICKS} ticks after it last was. That the quarry's chunk area follows
 * this is covered by {@code QuarryChunkAreaTest}, and the live tickets by
 * {@code QuarryGameTestBody#testQuarryChunkLoadingToggle}.
 *
 * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Chunk_loading">wiki/Laser Quarry.txt § Chunk loading</a>
 */
@DisplayName("Quarry chunk hold")
class QuarryChunkHoldTest {

    private static void tickUnpowered(QuarryChunkHold hold, int ticks) {
        for (int i = 0; i < ticks; i++) {
            hold.tick(false);
        }
    }

    @Test
    @DisplayName("a quarry that has never had power holds nothing")
    void neverPoweredHoldsNothing() {
        QuarryChunkHold hold = new QuarryChunkHold();

        assertThat(hold.holds()).isFalse();
        hold.tick(false);
        assertThat(hold.holds()).isFalse();
    }

    @Test
    @DisplayName("a powered tick holds")
    void poweredTickHolds() {
        QuarryChunkHold hold = new QuarryChunkHold();

        hold.tick(true);

        assertThat(hold.holds()).isTrue();
    }

    @Test
    @DisplayName("keeps holding until the grace window has passed since the last powered tick")
    void holdsThroughGraceWindowThenReleases() {
        QuarryChunkHold hold = new QuarryChunkHold();
        hold.tick(true);

        tickUnpowered(hold, QuarryChunkHold.GRACE_TICKS - 1);
        assertThat(hold.holds()).as("last tick inside the window").isTrue();

        hold.tick(false);
        assertThat(hold.holds()).as("first tick past the window").isFalse();

        tickUnpowered(hold, 10);
        assertThat(hold.holds()).as("stays released").isFalse();
    }

    @Test
    @DisplayName("power during the window starts it over, so a brown-out never drops the chunks")
    void powerDuringWindowRestartsIt() {
        QuarryChunkHold hold = new QuarryChunkHold();
        hold.tick(true);
        tickUnpowered(hold, QuarryChunkHold.GRACE_TICKS - 1);

        hold.tick(true);
        tickUnpowered(hold, QuarryChunkHold.GRACE_TICKS - 1);

        assertThat(hold.holds()).isTrue();
    }

    @Test
    @DisplayName("the remaining window survives a save and load")
    void remainingWindowSurvivesSaveAndLoad() {
        QuarryChunkHold hold = new QuarryChunkHold();
        hold.tick(true);
        tickUnpowered(hold, QuarryChunkHold.GRACE_TICKS - 2);
        CompoundTag tag = new CompoundTag();
        hold.save(tag);

        QuarryChunkHold loaded = new QuarryChunkHold();
        loaded.load(tag);

        assertThat(loaded.holds()).isTrue();
        loaded.tick(false);
        assertThat(loaded.holds()).as("one tick was left").isTrue();
        loaded.tick(false);
        assertThat(loaded.holds()).isFalse();
    }

    @Test
    @DisplayName("a save from before the hold existed loads as not holding")
    void missingKeyLoadsAsNotHolding() {
        QuarryChunkHold hold = new QuarryChunkHold();
        hold.tick(true);

        hold.load(new CompoundTag());

        assertThat(hold.holds()).isFalse();
    }
}
