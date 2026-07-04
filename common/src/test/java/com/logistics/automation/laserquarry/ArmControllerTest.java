package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.logistics.automation.laserquarry.entity.ArmController;
import com.logistics.automation.laserquarry.entity.QuarryArmState;
import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ArmController")
class ArmControllerTest extends MinecraftTestEnvironment {

    private static final float TOL = 1e-4f;

    private static ArmController armAt(float x, float y, float z) {
        ArmController arm = new ArmController();
        arm.initializeAt(x, y, z);
        return arm;
    }

    @Nested
    @DisplayName("moveTowards")
    class MoveTowards {

        @Test
        @DisplayName("advances one speed-step along a single axis without snapping")
        void partialAxisMove() {
            ArmController arm = armAt(0f, 0f, 0f);

            boolean snapped = arm.moveTowards(10f, 0f, 0f, 2f);

            assertThat(snapped).isFalse();
            assertThat(arm.getX()).isCloseTo(2f, within(TOL));
            assertThat(arm.getY()).isCloseTo(0f, within(TOL));
            assertThat(arm.getZ()).isCloseTo(0f, within(TOL));
        }

        @Test
        @DisplayName("advances proportionally along a diagonal (normalized by distance)")
        void partialDiagonalMove() {
            ArmController arm = armAt(0f, 0f, 0f);

            // distance to (3,4,0) is 5; a speed of 2.5 covers half → (1.5, 2.0, 0)
            boolean snapped = arm.moveTowards(3f, 4f, 0f, 2.5f);

            assertThat(snapped).isFalse();
            assertThat(arm.getX()).isCloseTo(1.5f, within(TOL));
            assertThat(arm.getY()).isCloseTo(2.0f, within(TOL));
        }

        @Test
        @DisplayName("snaps exactly to target and returns true when within one step")
        void snapsWhenWithinSpeed() {
            ArmController arm = armAt(0f, 0f, 0f);

            // distance to (3,4,0) is exactly 5; speed 5 reaches it
            boolean snapped = arm.moveTowards(3f, 4f, 0f, 5f);

            assertThat(snapped).isTrue();
            assertThat(arm.getX()).isEqualTo(3f);
            assertThat(arm.getY()).isEqualTo(4f);
            assertThat(arm.getZ()).isEqualTo(0f);
        }
    }

    @Nested
    @DisplayName("isAt")
    class IsAt {

        @Test
        @DisplayName("true when the target is within one speed-step")
        void withinRange() {
            ArmController arm = armAt(0f, 0f, 0f);
            assertThat(arm.isAt(1f, 0f, 0f, 2f)).isTrue();
        }

        @Test
        @DisplayName("false when the target is farther than one speed-step")
        void outOfRange() {
            ArmController arm = armAt(0f, 0f, 0f);
            assertThat(arm.isAt(5f, 0f, 0f, 2f)).isFalse();
        }

        @Test
        @DisplayName("does not move the arm")
        void isPure() {
            ArmController arm = armAt(1f, 2f, 3f);
            arm.isAt(9f, 9f, 9f, 1f);
            assertThat(arm.getX()).isEqualTo(1f);
            assertThat(arm.getY()).isEqualTo(2f);
            assertThat(arm.getZ()).isEqualTo(3f);
        }
    }

    @Nested
    @DisplayName("travelTicksFor")
    class TravelTicksFor {

        @Test
        @DisplayName("rounds the tick count up for a non-exact division")
        void roundsUp() {
            ArmController arm = armAt(0f, 0f, 0f);
            assertThat(arm.travelTicksFor(10f, 0f, 0f, 3f)).isEqualTo(4);
        }

        @Test
        @DisplayName("returns the exact quotient when it divides evenly")
        void exactDivision() {
            ArmController arm = armAt(0f, 0f, 0f);
            assertThat(arm.travelTicksFor(9f, 0f, 0f, 3f)).isEqualTo(3);
        }

        @Test
        @DisplayName("measures Euclidean distance across all axes")
        void diagonalDistance() {
            ArmController arm = armAt(0f, 0f, 0f);
            assertThat(arm.travelTicksFor(3f, 4f, 0f, 5f)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("warpTo")
    class WarpTo {

        @Test
        @DisplayName("jumps straight to the target and records the matching travel ticks")
        void jumpsAndRecordsTicks() {
            ArmController arm = armAt(1f, 1f, 1f);

            // distance from (1,1,1) to (4,5,1) is 5; at speed 2 → ceil(2.5) = 3 ticks
            arm.warpTo(4f, 5f, 1f, 2f);

            assertThat(arm.getX()).isEqualTo(4f);
            assertThat(arm.getY()).isEqualTo(5f);
            assertThat(arm.getZ()).isEqualTo(1f);
            assertThat(arm.getExpectedTravelTicks()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("settling timer")
    class Settling {

        @Test
        @DisplayName("counts down and only reports done on the final tick")
        void countsDown() {
            ArmController arm = new ArmController();
            arm.enterSettling(3);

            assertThat(arm.getState()).isEqualTo(QuarryArmState.SETTLING);
            assertThat(arm.tickSettling()).isFalse();
            assertThat(arm.tickSettling()).isFalse();
            assertThat(arm.tickSettling()).isTrue();
        }

        @Test
        @DisplayName("floors the settling duration to at least one tick")
        void flooredToOneTick() {
            ArmController arm = new ArmController();
            arm.enterSettling(0);

            assertThat(arm.tickSettling()).isTrue();
        }
    }

    @Nested
    @DisplayName("state transitions")
    class StateTransitions {

        @Test
        @DisplayName("initializeAt anchors the position, marks initialized, and resets to MOVING")
        void initializeAt() {
            ArmController arm = new ArmController();
            arm.enterSettling(5);

            arm.initializeAt(2f, 3f, 4f);

            assertThat(arm.getX()).isEqualTo(2f);
            assertThat(arm.getY()).isEqualTo(3f);
            assertThat(arm.getZ()).isEqualTo(4f);
            assertThat(arm.isInitialized()).isTrue();
            assertThat(arm.getState()).isEqualTo(QuarryArmState.MOVING);
            assertThat(arm.getExpectedTravelTicks()).isZero();
        }

        @Test
        @DisplayName("markUninitialized clears the initialized flag so the next tick re-anchors")
        void markUninitialized() {
            ArmController arm = armAt(0f, 0f, 0f);
            assertThat(arm.isInitialized()).isTrue();

            arm.markUninitialized();

            assertThat(arm.isInitialized()).isFalse();
        }

        @Test
        @DisplayName("enterBreaking and enterMoving set the arm state")
        void breakingAndMoving() {
            ArmController arm = new ArmController();

            arm.enterBreaking();
            assertThat(arm.getState()).isEqualTo(QuarryArmState.BREAKING);

            arm.enterMoving();
            assertThat(arm.getState()).isEqualTo(QuarryArmState.MOVING);
        }
    }
}
