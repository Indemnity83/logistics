package com.logistics.pipe.runtime;

import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("TravelingItem")
class TravelingItemTest extends MinecraftTestEnvironment {

    private static final float TOLERANCE = 0.0001f;

    private TravelingItem createItem(float speed) {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        return new TravelingItem(stack, Direction.NORTH, speed);
    }

    // ==================== Speed Control Tests ====================

    @Test
    @DisplayName("should move at constant speed when no forces applied")
    void constantSpeedMovement() {
        TravelingItem item = createItem(0.05f);

        float initialProgress = item.getProgress();
        item.tick(0f, 0f, 0.08f, TravelingItem.DEFAULT_MIN_SPEED);

        assertThat(item.getProgress()).isCloseTo(initialProgress + 0.05f, within(TOLERANCE));
        assertThat(item.getSpeed()).isCloseTo(0.05f, within(TOLERANCE));
    }

    @Test
    @DisplayName("should accelerate when acceleration rate is positive")
    void accelerationIncreasesSpeed() {
        TravelingItem item = createItem(0.05f);

        item.tick(0.01f, 0f, 0.08f, TravelingItem.DEFAULT_MIN_SPEED);

        assertThat(item.getSpeed()).isCloseTo(0.06f, within(TOLERANCE));
    }

    @Test
    @DisplayName("should decelerate when acceleration rate is negative")
    void negativeAccelerationDecreasesSpeed() {
        TravelingItem item = createItem(0.05f);

        item.tick(-0.01f, 0f, 0.08f, TravelingItem.DEFAULT_MIN_SPEED);

        assertThat(item.getSpeed()).isCloseTo(0.04f, within(TOLERANCE));
    }

    // ==================== Quadratic Deceleration Tests ====================

    @Test
    @DisplayName("should decelerate smoothly when above max speed")
    void decelerationToMax() {
        TravelingItem item = createItem(0.15f);
        float maxSpeed = 0.08f;

        item.tick(0f, 0f, maxSpeed, TravelingItem.DEFAULT_MIN_SPEED);

        // Should use quadratic deceleration
        assertThat(item.getSpeed()).isLessThan(0.15f);
        assertThat(item.getSpeed()).isGreaterThanOrEqualTo(maxSpeed);
    }

    @Test
    @DisplayName("should reach max speed at end of segment when decelerating")
    void decelerationReachesMaxAtSegmentEnd() {
        TravelingItem item = createItem(0.20f);
        float maxSpeed = 0.08f;

        boolean reachedEnd = false;
        int tickCount = 0;
        while (!reachedEnd && tickCount < 100) {
            reachedEnd = item.tick(0f, 0f, maxSpeed, TravelingItem.DEFAULT_MIN_SPEED);
            tickCount++;

            assertThat(item.getSpeed()).isGreaterThanOrEqualTo(maxSpeed);
        }

        assertThat(reachedEnd).isTrue();
        assertThat(item.getSpeed()).isCloseTo(maxSpeed, within(TOLERANCE));
    }

    @Test
    @DisplayName("should clamp speed to max when not decelerating")
    void maxSpeedClampingWhenNotDecelerating() {
        TravelingItem item = createItem(0.05f);

        item.tick(0.10f, 0f, 0.08f, TravelingItem.DEFAULT_MIN_SPEED);

        assertThat(item.getSpeed()).isCloseTo(0.08f, within(TOLERANCE));
    }

    // ==================== Drag Tests ====================

    @Test
    @DisplayName("should apply drag when drag coefficient is set")
    void dragApplication() {
        TravelingItem item = createItem(0.10f);
        float dragCoefficient = 0.1f;

        item.tick(0f, dragCoefficient, 0.15f, TravelingItem.DEFAULT_MIN_SPEED);

        assertThat(item.getSpeed()).isCloseTo(0.09f, within(TOLERANCE));
    }

    @Test
    @DisplayName("should not apply drag when accelerating")
    void noDragWhileAccelerating() {
        TravelingItem item = createItem(0.10f);

        item.tick(0.01f, 0.1f, 0.15f, TravelingItem.DEFAULT_MIN_SPEED);

        assertThat(item.getSpeed()).isCloseTo(0.11f, within(TOLERANCE));
    }

    // ==================== Min Speed Enforcement Tests ====================

    /** A floor deliberately unlike the 0.02 default, so a hardcoded one cannot satisfy these. */
    private static final float RAISED_MIN_SPEED = 0.05f;

    @Test
    @DisplayName("should enforce the minimum speed it is given, not a built-in one")
    void minSpeedEnforcement() {
        TravelingItem item = createItem(0.03f);
        float dragCoefficient = 0.5f;

        item.tick(0f, dragCoefficient, 0.15f, RAISED_MIN_SPEED);

        assertThat(item.getSpeed()).isCloseTo(RAISED_MIN_SPEED, within(TOLERANCE));
    }

    @Test
    @DisplayName("should not go below the given minimum with negative acceleration")
    void minSpeedWithNegativeAcceleration() {
        TravelingItem item = createItem(0.03f);

        item.tick(-0.02f, 0f, 0.15f, RAISED_MIN_SPEED);

        assertThat(item.getSpeed()).isCloseTo(RAISED_MIN_SPEED, within(TOLERANCE));
    }

    @Test
    @DisplayName("the configured floor is what a pipe actually applies")
    void configuredFloorIsUsed() {
        float configured = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED);
        TravelingItem item = createItem(0.03f);

        item.tick(0f, 0.5f, 0.15f, configured);

        assertThat(item.getSpeed()).isCloseTo(configured, within(TOLERANCE));
    }

    // ==================== Progress Tracking Tests ====================

    @Test
    @DisplayName("should return true when reaching end of segment")
    void segmentCompletion() {
        TravelingItem item = createItem(0.5f);

        boolean firstTick = item.tick(0f, 0f, 0.8f, TravelingItem.DEFAULT_MIN_SPEED);
        assertThat(firstTick).isFalse();
        assertThat(item.getProgress()).isCloseTo(0.5f, within(TOLERANCE));

        boolean secondTick = item.tick(0f, 0f, 0.8f, TravelingItem.DEFAULT_MIN_SPEED);
        assertThat(secondTick).isTrue();
        assertThat(item.getProgress()).isGreaterThanOrEqualTo(1.0f);
    }

    @Test
    @DisplayName("should track progress accurately over multiple ticks")
    void progressAccuracy() {
        TravelingItem item = createItem(0.1f);

        for (int i = 0; i < 5; i++) {
            item.tick(0f, 0f, 0.15f, TravelingItem.DEFAULT_MIN_SPEED);
        }

        assertThat(item.getProgress()).isCloseTo(0.5f, within(TOLERANCE));
    }

    // ==================== Direction and Routing Tests ====================

    @Test
    @DisplayName("should maintain direction through ticks")
    void directionPersistence() {
        TravelingItem item = createItem(0.05f);

        item.tick(0f, 0f, 0.08f, TravelingItem.DEFAULT_MIN_SPEED);
        item.tick(0f, 0f, 0.08f, TravelingItem.DEFAULT_MIN_SPEED);

        assertThat(item.getDirection()).isEqualTo(Direction.NORTH);
    }

    @Test
    @DisplayName("should update direction and mark as routed")
    void directionChange() {
        TravelingItem item = createItem(0.05f);

        assertThat(item.isRouted()).isFalse();

        item.setDirection(Direction.SOUTH);

        assertThat(item.getDirection()).isEqualTo(Direction.SOUTH);
        assertThat(item.isRouted()).isTrue();
    }

    @Test
    @DisplayName("should allow manual routing state control")
    void manualRoutingControl() {
        TravelingItem item = createItem(0.05f);

        item.setRouted(true);
        assertThat(item.isRouted()).isTrue();

        item.setRouted(false);
        assertThat(item.isRouted()).isFalse();
    }

    // ==================== Speed Modification Tests ====================

    @Test
    @DisplayName("should allow speed to be changed mid-flight")
    void speedModification() {
        TravelingItem item = createItem(0.05f);

        item.setSpeed(0.10f);

        assertThat(item.getSpeed()).isCloseTo(0.10f, within(TOLERANCE));

        item.tick(0f, 0f, 0.15f, TravelingItem.DEFAULT_MIN_SPEED);
        assertThat(item.getProgress()).isCloseTo(0.10f, within(TOLERANCE));
    }

    @Test
    @DisplayName("should allow progress to be set manually")
    void progressModification() {
        TravelingItem item = createItem(0.05f);

        item.setProgress(0.75f);

        assertThat(item.getProgress()).isCloseTo(0.75f, within(TOLERANCE));
    }

    // ==================== ItemStack Tests ====================

    @Test
    @DisplayName("should copy itemstack on construction")
    void itemStackCopying() {
        ItemStack original = new ItemStack(Items.DIAMOND, 5);
        TravelingItem item = new TravelingItem(original, Direction.NORTH, 0.05f);

        original.setCount(10);

        assertThat(item.getStack().getCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("should preserve itemstack through ticks")
    void itemStackPersistence() {
        TravelingItem item = createItem(0.05f);

        item.tick(0f, 0f, 0.08f, TravelingItem.DEFAULT_MIN_SPEED);
        item.tick(0f, 0f, 0.08f, TravelingItem.DEFAULT_MIN_SPEED);

        assertThat(item.getStack().getItem()).isEqualTo(Items.DIAMOND);
        assertThat(item.getStack().getCount()).isEqualTo(1);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("should handle very small remaining distance during deceleration")
    void smallRemainingDistance() {
        TravelingItem item = createItem(0.15f);
        item.setProgress(0.9999f);
        float maxSpeed = 0.08f;

        item.tick(0f, 0f, maxSpeed, TravelingItem.DEFAULT_MIN_SPEED);

        assertThat(item.getSpeed()).isFinite();
        assertThat(item.getSpeed()).isGreaterThanOrEqualTo(maxSpeed);
    }

    @Test
    @DisplayName("should handle zero max speed gracefully")
    void zeroMaxSpeed() {
        TravelingItem item = createItem(0.05f);

        item.tick(0f, 0f, 0.0f, TravelingItem.DEFAULT_MIN_SPEED);

        // When maxSpeed is 0 and current speed > 0, uses deceleration mode
        // Speed will decelerate toward 0 but get clamped to LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED)
        assertThat(item.getSpeed()).isGreaterThanOrEqualTo(LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED));
    }
}
