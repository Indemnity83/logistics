package com.logistics.pipe.modules;

import com.logistics.pipe.modules.FluidTransportModule.FlowRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FluidTransportModule flow-rate scaling")
class FluidTransportModuleTest {

    @ParameterizedTest(name = "{0} multiplier is {1}x")
    @DisplayName("each tier multiplier defines the pipe throughput ladder")
    @CsvSource({"SLOW, 1", "NORMAL, 2", "FAST, 4"})
    void multiplier_perTier(FlowRate rate, int expected) {
        assertThat(rate.multiplier()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} scales base {1} to {2}")
    @DisplayName("modifyTransferRate scales the incoming base rate by the tier multiplier")
    @CsvSource({
        "SLOW, 100, 100",
        "NORMAL, 100, 200",
        "FAST, 100, 400",
        "NORMAL, 0, 0",
        "FAST, 250, 1000",
    })
    void modifyTransferRate_scalesBaseRate(FlowRate rate, long base, long expected) {
        assertThat(new FluidTransportModule(rate).modifyTransferRate(base)).isEqualTo(expected);
    }

    @Test
    @DisplayName("the tier ladder keeps NORMAL at 2x and FAST at 4x the slow baseline")
    void tierLadder_ratiosHold() {
        long base = 60;
        long slow = new FluidTransportModule(FlowRate.SLOW).modifyTransferRate(base);
        long normal = new FluidTransportModule(FlowRate.NORMAL).modifyTransferRate(base);
        long fast = new FluidTransportModule(FlowRate.FAST).modifyTransferRate(base);

        assertThat(normal).isEqualTo(2 * slow);
        assertThat(fast).isEqualTo(4 * slow);
    }
}
