package com.logistics.pipe.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("TravelingItemPhysics")
class TravelingItemPhysicsTest {
    private static final float MIN_SPEED = 0.01f;
    private static final float EPSILON = 0.0001f;

    private TravelingItemPhysics physics;

    @BeforeEach
    void setUp() {
        physics = new TravelingItemPhysics(MIN_SPEED);
    }

    @Nested
    @DisplayName("Acceleration mode")
    class AccelerationMode {
        @Test
        @DisplayName("should increase speed when accelerating")
        void shouldAccelerate() {
            float result = physics.updateSpeed(
                    0.05f,  // currentSpeed
                    0.25f,  // currentProgress
                    0.02f,  // accelerationRate
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            assertThat(result).isCloseTo(0.07f, within(EPSILON));
        }

        @Test
        @DisplayName("should clamp to maxSpeed when accelerating above max")
        void shouldClampToMax() {
            float result = physics.updateSpeed(
                    0.09f,  // currentSpeed
                    0.25f,  // currentProgress
                    0.05f,  // accelerationRate (would go to 0.14)
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            assertThat(result).isCloseTo(0.1f, within(EPSILON));
        }

        @Test
        @DisplayName("should not accelerate when already at max speed")
        void shouldNotAccelerateAtMax() {
            float result = physics.updateSpeed(
                    0.1f,   // currentSpeed (at max)
                    0.25f,  // currentProgress
                    0.02f,  // accelerationRate
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            assertThat(result).isCloseTo(0.1f, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Drag mode")
    class DragMode {
        @Test
        @DisplayName("should reduce speed when drag is applied")
        void shouldApplyDrag() {
            float result = physics.updateSpeed(
                    0.1f,   // currentSpeed
                    0.25f,  // currentProgress
                    0f,     // accelerationRate
                    0.2f,   // dragCoefficient (20% reduction)
                    0.1f    // maxSpeed
            );

            assertThat(result).isCloseTo(0.08f, within(EPSILON));
        }

        @Test
        @DisplayName("should clamp to minSpeed when drag reduces below minimum")
        void shouldClampToMin() {
            float result = physics.updateSpeed(
                    0.02f,  // currentSpeed
                    0.25f,  // currentProgress
                    0f,     // accelerationRate
                    0.9f,   // dragCoefficient (90% reduction)
                    0.1f    // maxSpeed
            );

            assertThat(result).isCloseTo(MIN_SPEED, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Deceleration mode")
    class DecelerationMode {
        @Test
        @DisplayName("should decelerate when above max speed")
        void shouldDecelerate() {
            float result = physics.updateSpeed(
                    0.15f,  // currentSpeed (above max)
                    0.5f,   // currentProgress (halfway)
                    0f,     // accelerationRate
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            // Should decelerate toward maxSpeed
            assertThat(result).isLessThan(0.15f);
            assertThat(result).isGreaterThan(0.1f);
        }

        @Test
        @DisplayName("should reach maxSpeed at segment exit")
        void shouldReachMaxAtExit() {
            // Start with overspeed, near exit
            float result = physics.updateSpeed(
                    0.15f,  // currentSpeed
                    0.99f,  // currentProgress (very close to exit)
                    0f,     // accelerationRate
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            // Should be very close to maxSpeed
            assertThat(result).isCloseTo(0.1f, within(0.01f));
        }

        @Test
        @DisplayName("should not go below target speed when decelerating")
        void shouldNotGoBelowTarget() {
            float result = physics.updateSpeed(
                    0.11f,  // currentSpeed (slightly above max)
                    0.99f,  // currentProgress (near exit)
                    0f,     // accelerationRate
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            // Should clamp to maxSpeed, not go below
            assertThat(result).isGreaterThanOrEqualTo(0.1f);
        }
    }

    @Nested
    @DisplayName("Physics mode priority")
    class PhysicsPriority {
        @Test
        @DisplayName("deceleration should override acceleration")
        void decelerationOverridesAcceleration() {
            float result = physics.updateSpeed(
                    0.15f,  // currentSpeed (above max)
                    0.5f,   // currentProgress
                    0.05f,  // accelerationRate (would increase speed)
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            // Should decelerate despite acceleration being present
            assertThat(result).isLessThan(0.15f);
        }

        @Test
        @DisplayName("deceleration should override drag")
        void decelerationOverridesDrag() {
            float result = physics.updateSpeed(
                    0.15f,  // currentSpeed (above max)
                    0.5f,   // currentProgress
                    0f,     // accelerationRate
                    0.5f,   // dragCoefficient (would reduce to 0.075)
                    0.1f    // maxSpeed
            );

            // Should decelerate toward maxSpeed, not apply drag
            assertThat(result).isGreaterThan(0.075f);
            assertThat(result).isLessThan(0.15f);
        }

        @Test
        @DisplayName("acceleration should override drag")
        void accelerationOverridesDrag() {
            float result = physics.updateSpeed(
                    0.05f,  // currentSpeed
                    0.25f,  // currentProgress
                    0.02f,  // accelerationRate
                    0.5f,   // dragCoefficient (would reduce to 0.025)
                    0.1f    // maxSpeed
            );

            // Should accelerate, not apply drag
            assertThat(result).isCloseTo(0.07f, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {
        @Test
        @DisplayName("should maintain speed with no forces")
        void shouldMaintainSpeed() {
            float result = physics.updateSpeed(
                    0.05f,  // currentSpeed
                    0.25f,  // currentProgress
                    0f,     // accelerationRate
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            assertThat(result).isCloseTo(0.05f, within(EPSILON));
        }

        @Test
        @DisplayName("should handle zero current speed")
        void shouldHandleZeroSpeed() {
            float result = physics.updateSpeed(
                    0f,     // currentSpeed
                    0.25f,  // currentProgress
                    0.01f,  // accelerationRate
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            assertThat(result).isCloseTo(0.01f, within(EPSILON));
        }

        @Test
        @DisplayName("should handle progress at start")
        void shouldHandleStartProgress() {
            float result = physics.updateSpeed(
                    0.05f,  // currentSpeed
                    0f,     // currentProgress (at start)
                    0.01f,  // accelerationRate
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            assertThat(result).isCloseTo(0.06f, within(EPSILON));
        }

        @Test
        @DisplayName("should handle progress near exit")
        void shouldHandleNearExitProgress() {
            float result = physics.updateSpeed(
                    0.05f,  // currentSpeed
                    0.99f,  // currentProgress (near exit)
                    0.01f,  // accelerationRate
                    0f,     // dragCoefficient
                    0.1f    // maxSpeed
            );

            assertThat(result).isCloseTo(0.06f, within(EPSILON));
        }
    }
}
