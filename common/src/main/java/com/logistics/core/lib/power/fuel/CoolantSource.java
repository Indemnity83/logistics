package com.logistics.core.lib.power.fuel;

import com.logistics.core.machine.MachineContext;

/**
 * Optional coolant an engine consumes to keep burning safely. The burn component checks
 * {@link #available} before igniting and {@link #consume}s a unit per burn cycle; engines with a
 * coolant (steam, combustion) cannot overheat while coolant is present, while coolant-less engines
 * (stirling) use {@link #NONE}.
 */
public interface CoolantSource {

    /** Whether coolant is available to permit a new burn cycle. */
    boolean available(MachineContext ctx);

    /** Consume coolant for one burn cycle. */
    void consume(MachineContext ctx);

    /** No coolant required — always available, consumes nothing. */
    CoolantSource NONE = new CoolantSource() {
        @Override
        public boolean available(MachineContext ctx) {
            return true;
        }

        @Override
        public void consume(MachineContext ctx) {}
    };
}
