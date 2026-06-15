package com.logistics.fluid.block;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.LogisticsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FluidPipeKind")
class FluidPipeKindTest {

    private final LogisticsConfig.FluidPipeConfig cfg = new LogisticsConfig.FluidPipeConfig();

    @Test
    @DisplayName("transfer rate resolves per kind, with stone slower and gold faster than copper")
    void transferRatePerKind() {
        long copper = FluidPipeKind.COPPER.transferRate(cfg);
        assertThat(FluidPipeKind.STONE.transferRate(cfg)).isEqualTo(cfg.stoneTransferRate).isLessThan(copper);
        assertThat(FluidPipeKind.GOLD.transferRate(cfg)).isEqualTo(cfg.goldTransferRate).isGreaterThan(copper);
        assertThat(FluidPipeKind.MERGER.transferRate(cfg)).isEqualTo(cfg.mergerTransferRate);
        assertThat(FluidPipeKind.VOID.transferRate(cfg)).isEqualTo(cfg.voidRate);
        assertThat(FluidPipeKind.BYPASS.transferRate(cfg)).isEqualTo(copper);
        assertThat(FluidPipeKind.EXTRACTOR.transferRate(cfg)).isEqualTo(copper);
    }

    @Test
    @DisplayName("only void and bypass refuse to connect to external handlers")
    void connectsToHandlers() {
        assertThat(FluidPipeKind.VOID.connectsToHandlers()).isFalse();
        assertThat(FluidPipeKind.BYPASS.connectsToHandlers()).isFalse();
        for (FluidPipeKind kind : new FluidPipeKind[] {
            FluidPipeKind.COPPER, FluidPipeKind.STONE, FluidPipeKind.GOLD,
            FluidPipeKind.MERGER, FluidPipeKind.EXTRACTOR
        }) {
            assertThat(kind.connectsToHandlers()).as("%s connects to handlers", kind).isTrue();
        }
    }

    @Test
    @DisplayName("roles are mutually exclusive and assigned to the right kinds")
    void roles() {
        assertThat(FluidPipeKind.EXTRACTOR.isExtractor()).isTrue();
        assertThat(FluidPipeKind.MERGER.isDirectional()).isTrue();
        assertThat(FluidPipeKind.VOID.isVoid()).isTrue();

        // Passive movers take part in normal equilibrium + omnidirectional handler push; the others don't.
        assertThat(FluidPipeKind.COPPER.isPassiveMover()).isTrue();
        assertThat(FluidPipeKind.STONE.isPassiveMover()).isTrue();
        assertThat(FluidPipeKind.GOLD.isPassiveMover()).isTrue();
        assertThat(FluidPipeKind.BYPASS.isPassiveMover()).isTrue();
        assertThat(FluidPipeKind.EXTRACTOR.isPassiveMover()).isFalse();
        assertThat(FluidPipeKind.MERGER.isPassiveMover()).isFalse();
        assertThat(FluidPipeKind.VOID.isPassiveMover()).isFalse();
    }

    @Test
    @DisplayName("model base names match the registered asset names")
    void modelBase() {
        assertThat(FluidPipeKind.COPPER.modelBase()).isEqualTo("copper_fluid_pipe");
        assertThat(FluidPipeKind.STONE.modelBase()).isEqualTo("stone_fluid_pipe");
        assertThat(FluidPipeKind.GOLD.modelBase()).isEqualTo("gold_fluid_pipe");
        assertThat(FluidPipeKind.MERGER.modelBase()).isEqualTo("merger_fluid_pipe");
        assertThat(FluidPipeKind.EXTRACTOR.modelBase()).isEqualTo("fluid_extractor_pipe");
        assertThat(FluidPipeKind.VOID.modelBase()).isEqualTo("void_fluid_pipe");
        assertThat(FluidPipeKind.BYPASS.modelBase()).isEqualTo("bypass_fluid_pipe");
    }
}
