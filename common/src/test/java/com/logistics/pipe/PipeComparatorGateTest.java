package com.logistics.pipe;

import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.MinecraftTestEnvironment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code hasComparatorOutput()} is the gate a block consults before asking a pipe for a comparator
 * reading, and it is answered from a {@code BlockState} alone — no level, no position. A chassis
 * pipe therefore cannot inspect its runtime-installed modules to answer it, so it must advertise
 * the capability unconditionally or its {@code getComparatorOutput} override is unreachable.
 */
@DisplayName("Pipe comparator gate")
class PipeComparatorGateTest extends MinecraftTestEnvironment {

    /** A module that reports a comparator reading, standing in for any future comparator module. */
    private static final class ComparatorModule implements Module {
        @Override
        public boolean hasComparatorOutput() {
            return true;
        }

        @Override
        public int comparatorOutput(PipeContext ctx) {
            return 7;
        }
    }

    @Test
    @DisplayName("A chassis pipe advertises comparator output even with no static comparator module")
    void chassisPipe_advertisesComparatorOutput() {
        assertTrue(new ChassisPipe(4).hasComparatorOutput());
    }

    @Test
    @DisplayName("A plain pipe advertises comparator output only when a module provides one")
    void plainPipe_advertisesComparatorOutputOnlyWithAModule() {
        assertFalse(new ItemPipe().hasComparatorOutput());
        assertTrue(new ItemPipe(new ComparatorModule()).hasComparatorOutput());
    }
}
