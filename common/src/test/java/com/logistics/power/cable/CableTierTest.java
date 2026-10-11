package com.logistics.power.cable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CableTierTest {
    /**
     * Wiki claim (Usage): "Each grade moves more RF per tick — Basic 30, Conductive 60, Resonant
     * 120, Deep 240".
     *
     * @see <a href="https://logistics.fandom.com/wiki/Cables#Usage">wiki/Cables.txt § Usage</a>
     */
    @Test
    void cableTiersUseExpectedTransferRates() {
        assertThat(CableTier.BASIC.transferRate()).isEqualTo(30);
        assertThat(CableTier.CONDUCTIVE.transferRate()).isEqualTo(60);
        assertThat(CableTier.RESONANT.transferRate()).isEqualTo(120);
        assertThat(CableTier.DEEP.transferRate()).isEqualTo(240);
    }

    /**
     * Wiki claim (Variants): "Each grade carries twice the RF of the one below it."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Cables#Variants">wiki/Cables.txt § Variants</a>
     */
    @Test
    void eachGradeCarriesTwiceTheOneBelowIt() {
        CableTier[] ladder = CableTier.values();
        for (int i = 1; i < ladder.length; i++) {
            assertThat(ladder[i].transferRate())
                    .as("%s carries twice %s", ladder[i], ladder[i - 1])
                    .isEqualTo(ladder[i - 1].transferRate() * 2);
        }
    }

    @Test
    void cableTierModelNameRejectsNonCablePrefixes() {
        assertThatThrownBy(() -> CableTier.BASIC.modelName("pipe_core"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
