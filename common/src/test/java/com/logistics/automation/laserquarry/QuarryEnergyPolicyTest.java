package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.laserquarry.entity.QuarryEnergyPolicy;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.upgrade.MachineModifiers;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("QuarryEnergyPolicy")
class QuarryEnergyPolicyTest extends MinecraftTestEnvironment {

    private final AtomicInteger consumeCalls = new AtomicInteger();

    private EnergyStorageComponent chargedBuffer(long amount) {
        EnergyStorageComponent energy = new EnergyStorageComponent("energy", 100_000, 100_000, 100_000, () -> {});
        energy.setAmount(amount);
        return energy;
    }

    private QuarryEnergyPolicy policy(EnergyStorageComponent energy) {
        return new QuarryEnergyPolicy(energy, MachineModifiers.identity(), consumeCalls::incrementAndGet);
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("spends RF, flags work this tick, and notifies when affordable")
        void spendsWhenAffordable() {
            EnergyStorageComponent energy = chargedBuffer(500);
            QuarryEnergyPolicy policy = policy(energy);

            policy.consume(200);

            assertThat(energy.amount()).isEqualTo(300);
            assertThat(policy.consumedThisTick()).isTrue();
            assertThat(consumeCalls.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("does nothing when the buffer can't afford the cost")
        void skipsWhenUnaffordable() {
            EnergyStorageComponent energy = chargedBuffer(150);
            QuarryEnergyPolicy policy = policy(energy);

            policy.consume(200);

            assertThat(energy.amount()).isEqualTo(150);
            assertThat(policy.consumedThisTick()).isFalse();
            assertThat(consumeCalls.get()).isZero();
        }

        @Test
        @DisplayName("spends when the cost exactly equals the buffer")
        void spendsExactlyAllBuffer() {
            EnergyStorageComponent energy = chargedBuffer(200);
            QuarryEnergyPolicy policy = policy(energy);

            policy.consume(200);

            assertThat(energy.amount()).isZero();
            assertThat(policy.consumedThisTick()).isTrue();
        }
    }

    @Nested
    @DisplayName("consumedThisTick lifecycle")
    class ConsumedFlag {

        @Test
        @DisplayName("resets back to false")
        void resets() {
            QuarryEnergyPolicy policy = policy(chargedBuffer(500));
            policy.consume(100);
            assertThat(policy.consumedThisTick()).isTrue();

            policy.resetConsumedThisTick();

            assertThat(policy.consumedThisTick()).isFalse();
        }
    }

    @Nested
    @DisplayName("has")
    class Has {

        @Test
        @DisplayName("true when the buffer holds at least the amount")
        void trueWhenEnough() {
            QuarryEnergyPolicy policy = policy(chargedBuffer(100));
            assertThat(policy.has(100)).isTrue();
            assertThat(policy.has(99)).isTrue();
        }

        @Test
        @DisplayName("false when the buffer holds less")
        void falseWhenShort() {
            QuarryEnergyPolicy policy = policy(chargedBuffer(100));
            assertThat(policy.has(101)).isFalse();
        }
    }

    @Nested
    @DisplayName("drainIdle")
    class DrainIdle {

        @Test
        @DisplayName("bleeds RF and notifies but does not flag work this tick")
        void bleedsWithoutFlaggingWork() {
            EnergyStorageComponent energy = chargedBuffer(500);
            QuarryEnergyPolicy policy = policy(energy);

            policy.drainIdle(200);

            assertThat(energy.amount()).isEqualTo(300);
            assertThat(policy.consumedThisTick()).isFalse();
            assertThat(consumeCalls.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("floors the buffer at zero when draining more than it holds")
        void floorsAtZero() {
            EnergyStorageComponent energy = chargedBuffer(150);
            QuarryEnergyPolicy policy = policy(energy);

            policy.drainIdle(1_000);

            assertThat(energy.amount()).isZero();
        }

        @Test
        @DisplayName("does nothing on an empty buffer")
        void noopWhenEmpty() {
            EnergyStorageComponent energy = chargedBuffer(0);
            QuarryEnergyPolicy policy = policy(energy);

            policy.drainIdle(100);

            assertThat(energy.amount()).isZero();
            assertThat(consumeCalls.get()).isZero();
        }
    }

    @Test
    @DisplayName("stored reflects the buffer amount")
    void storedReflectsBuffer() {
        assertThat(policy(chargedBuffer(742)).stored()).isEqualTo(742);
    }
}
