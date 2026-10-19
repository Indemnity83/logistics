package com.logistics.core.lib.client.screen.tab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.core.lib.client.screen.tab.TabFixtures.FakeClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TabAnimation")
class TabAnimationTest {

    private final FakeClock clock = new FakeClock();
    private final TabAnimation animation = new TabAnimation(clock);

    @Test
    @DisplayName("starts closed and at rest")
    void startsClosed() {
        assertThat(animation.progress()).isZero();
        assertThat(animation.eased()).isZero();
        assertThat(animation.targetOpen()).isFalse();
        assertThat(animation.isAnimating()).isFalse();
        assertThat(animation.isFullyOpen()).isFalse();
    }

    @Test
    @DisplayName("easing maps 0 to 0, 1 to 1, and is monotonic ease-out")
    void easingEndpointsAndMonotonic() {
        assertThat(TabAnimation.ease(0.0)).isZero();
        assertThat(TabAnimation.ease(1.0)).isEqualTo(1.0);
        double previous = 0.0;
        for (int i = 1; i <= 100; i++) {
            double p = i / 100.0;
            double eased = TabAnimation.ease(p);
            assertThat(eased).isGreaterThan(previous).isGreaterThanOrEqualTo(p);
            previous = eased;
        }
        assertThat(TabAnimation.ease(0.5)).isCloseTo(0.875, within(1e-9));
    }

    @Test
    @DisplayName("a full sweep takes exactly the duration and lands exactly on 1")
    void fullSweepDuration() {
        animation.setTarget(true);
        assertThat(animation.isAnimating()).isTrue();

        clock.advance(TabAnimation.DURATION_MS / 2);
        assertThat(animation.progress()).isCloseTo(0.5, within(1e-9));
        assertThat(animation.isFullyOpen()).isFalse();

        clock.advance(TabAnimation.DURATION_MS / 2 - 1);
        assertThat(animation.progress()).isLessThan(1.0);

        clock.advance(1);
        assertThat(animation.progress()).isEqualTo(1.0);
        assertThat(animation.eased()).isEqualTo(1.0);
        assertThat(animation.isFullyOpen()).isTrue();
        assertThat(animation.isAnimating()).isFalse();

        clock.advance(10_000);
        assertThat(animation.progress()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("progress and eased value rise monotonically while opening")
    void monotonicWhileOpening() {
        animation.setTarget(true);
        double previous = -1.0;
        for (int t = 0; t <= TabAnimation.DURATION_MS; t += 5) {
            double eased = animation.eased();
            assertThat(eased).isGreaterThanOrEqualTo(previous);
            previous = eased;
            clock.advance(5);
        }
    }

    @Test
    @DisplayName("retargeting mid-flight continues from the current value without a jump")
    void retargetContinuity() {
        animation.setTarget(true);
        clock.advance(72);
        double before = animation.progress();
        assertThat(before).isCloseTo(0.4, within(1e-9));

        animation.setTarget(false);
        assertThat(animation.progress()).isEqualTo(before);
        assertThat(animation.targetOpen()).isFalse();

        clock.advance(36);
        assertThat(animation.progress()).isCloseTo(0.2, within(1e-9));

        clock.advance(36);
        assertThat(animation.progress()).isZero();
        assertThat(animation.isAnimating()).isFalse();
    }

    @Test
    @DisplayName("time to reach the new target is proportional to the remaining distance")
    void retargetTimeProportional() {
        animation.setTarget(true);
        clock.advance(TabAnimation.DURATION_MS);
        animation.setTarget(false);
        clock.advance(TabAnimation.DURATION_MS / 4);
        animation.setTarget(true);

        clock.advance(TabAnimation.DURATION_MS / 4 - 1);
        assertThat(animation.progress()).isLessThan(1.0);
        clock.advance(1);
        assertThat(animation.progress()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("setting the current target again does not restart the animation")
    void sameTargetIsNoOp() {
        animation.setTarget(true);
        clock.advance(90);
        animation.setTarget(true);
        clock.advance(90);
        assertThat(animation.progress()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("snap jumps instantly in either direction, mid-flight included")
    void snap() {
        animation.snap(true);
        assertThat(animation.progress()).isEqualTo(1.0);
        assertThat(animation.isFullyOpen()).isTrue();
        assertThat(animation.isAnimating()).isFalse();

        animation.setTarget(false);
        clock.advance(40);
        animation.snap(false);
        assertThat(animation.progress()).isZero();
        assertThat(animation.isAnimating()).isFalse();
    }

    @Test
    @DisplayName("a clock running backwards never moves progress past its start")
    void backwardsClock() {
        animation.setTarget(true);
        clock.advance(-50);
        assertThat(animation.progress()).isZero();
    }
}
