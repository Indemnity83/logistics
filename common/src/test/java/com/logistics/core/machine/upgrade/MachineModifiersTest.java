package com.logistics.core.machine.upgrade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MachineModifiersTest {

    private static final OperationKey OP = () -> "test.op";

    @Test
    void identityLeavesEveryBehaviorUnchanged() {
        MachineModifiers identity = MachineModifiers.identity();

        assertThat(identity.cost(OP)).isEqualTo(1.0);
        assertThat(identity.speed(OP)).isEqualTo(1.0);
        assertThat(identity.range(OP)).isZero();
        assertThat(identity.capacity(50_000)).isEqualTo(50_000);
        assertThat(identity.receiveRate(128)).isEqualTo(128);
    }

    @Test
    void identityIsTheSharedSingleton() {
        assertThat(MachineModifiers.identity()).isSameAs(MachineModifiers.IDENTITY);
    }

    @Test
    void upgradeComponentReportsIdentityUntilRealUpgradesExist() {
        UpgradeComponent component = new UpgradeComponent("upgrades");

        assertThat(component.id()).isEqualTo("upgrades");
        assertThat(component.modifiers()).isSameAs(MachineModifiers.identity());
    }
}
