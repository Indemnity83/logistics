package com.logistics.power.cable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CableTierTest {
    @Test
    void cableTiersUseExpectedTransferRates() {
        assertThat(CableTier.BASIC.transferRate()).isEqualTo(30);
        assertThat(CableTier.CONDUCTIVE.transferRate()).isEqualTo(60);
        assertThat(CableTier.RESONANT.transferRate()).isEqualTo(120);
        assertThat(CableTier.DEEP.transferRate()).isEqualTo(240);
    }

    @Test
    void cableTierModelNameRejectsNonCablePrefixes() {
        assertThatThrownBy(() -> CableTier.BASIC.modelName("pipe_core"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
