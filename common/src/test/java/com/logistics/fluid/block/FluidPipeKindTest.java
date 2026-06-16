package com.logistics.fluid.block;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.LogisticsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FluidPipeKind")
class FluidPipeKindTest {

    private final LogisticsConfig.FluidPipeConfig cfg = new LogisticsConfig.FluidPipeConfig();

    @Test
    @DisplayName("transfer rate is the base rate scaled by a per-tier multiplier")
    void transferRatePerKind() {
        long base = cfg.baseTransferRate;
        // stone/extractor/void 1×, copper/bypass 2×, merger 3×, gold 4×
        assertThat(FluidPipeKind.STONE.transferRate(cfg)).isEqualTo(base);
        assertThat(FluidPipeKind.EXTRACTOR.transferRate(cfg)).isEqualTo(base);
        assertThat(FluidPipeKind.VOID.transferRate(cfg)).isEqualTo(base);
        assertThat(FluidPipeKind.COPPER.transferRate(cfg)).isEqualTo(2 * base);
        assertThat(FluidPipeKind.BYPASS.transferRate(cfg)).isEqualTo(2 * base);
        assertThat(FluidPipeKind.MERGER.transferRate(cfg)).isEqualTo(3 * base);
        assertThat(FluidPipeKind.GOLD.transferRate(cfg)).isEqualTo(4 * base);
        assertThat(FluidPipeKind.INSERTION.transferRate(cfg)).isEqualTo(2 * base); // matches copper
    }

    @Test
    @DisplayName("only the insertion pipe prioritizes filling adjacent handlers")
    void prioritizesHandlers() {
        assertThat(FluidPipeKind.INSERTION.prioritizesHandlers()).isTrue();
        for (FluidPipeKind kind : new FluidPipeKind[] {
            FluidPipeKind.COPPER, FluidPipeKind.STONE, FluidPipeKind.GOLD, FluidPipeKind.BYPASS,
            FluidPipeKind.MERGER, FluidPipeKind.VOID, FluidPipeKind.EXTRACTOR
        }) {
            assertThat(kind.prioritizesHandlers()).as("%s prioritizes handlers", kind).isFalse();
        }
    }

    @Test
    @DisplayName("only void and bypass refuse to connect to external handlers")
    void connectsToHandlers() {
        assertThat(FluidPipeKind.VOID.connectsToHandlers()).isFalse();
        assertThat(FluidPipeKind.BYPASS.connectsToHandlers()).isFalse();
        for (FluidPipeKind kind : new FluidPipeKind[] {
            FluidPipeKind.COPPER, FluidPipeKind.STONE, FluidPipeKind.GOLD,
            FluidPipeKind.INSERTION, FluidPipeKind.MERGER, FluidPipeKind.EXTRACTOR
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
        assertThat(FluidPipeKind.INSERTION.isPassiveMover()).isTrue();
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
        assertThat(FluidPipeKind.INSERTION.modelBase()).isEqualTo("insertion_fluid_pipe");
        assertThat(FluidPipeKind.MERGER.modelBase()).isEqualTo("merger_fluid_pipe");
        assertThat(FluidPipeKind.EXTRACTOR.modelBase()).isEqualTo("fluid_extractor_pipe");
        assertThat(FluidPipeKind.VOID.modelBase()).isEqualTo("void_fluid_pipe");
        assertThat(FluidPipeKind.BYPASS.modelBase()).isEqualTo("bypass_fluid_pipe");
    }
}
