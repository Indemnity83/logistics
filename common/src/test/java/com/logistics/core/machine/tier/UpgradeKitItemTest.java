package com.logistics.core.machine.tier;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.machine.tier.UpgradeKitItem.Outcome;
import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UpgradeKitItemTest extends MinecraftTestEnvironment {

    @ParameterizedTest(name = "{0} machine + {1} kit -> {2}")
    @CsvSource({
        "BASIC,      INDUSTRIAL, APPLY",
        "INDUSTRIAL, PRECISION,  APPLY",
        "PRECISION,  INFERNAL,   APPLY",
        "BASIC,      PRECISION,  NEEDS_PREVIOUS",
        "BASIC,      INFERNAL,   NEEDS_PREVIOUS",
        "INDUSTRIAL, INFERNAL,   NEEDS_PREVIOUS",
        "INDUSTRIAL, INDUSTRIAL, ALREADY",
        "PRECISION,  INDUSTRIAL, ALREADY",
        "PRECISION,  PRECISION,  ALREADY",
        "INFERNAL,   INDUSTRIAL, ALREADY",
        "INFERNAL,   PRECISION,  ALREADY",
        "INFERNAL,   INFERNAL,   ALREADY",
    })
    void decidesByTheMachineTierAndTheKitTarget(MachineTier current, MachineTier target, Outcome expected) {
        assertThat(UpgradeKitItem.decide(current, target)).isEqualTo(expected);
    }
}
