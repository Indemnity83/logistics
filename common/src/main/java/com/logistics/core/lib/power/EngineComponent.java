package com.logistics.core.lib.power;

import com.logistics.core.machine.MachineContext;

/**
 * Engine-specific capability SPIs, the source-side counterpart to
 * {@link com.logistics.core.machine.MachineComponent}'s nested access interfaces.
 *
 * <p>Engine components implement {@code MachineComponent} for tick/save/load and additionally
 * implement whichever of these interfaces they provide; the {@code EngineEntity} host queries them
 * generically (via {@code find}/{@code forEach}) to answer the block- and renderer-facing getters.
 */
public final class EngineComponent {

    private EngineComponent() {}

    /** A gate that must be satisfied for the engine to be considered running (e.g. fuel burning). */
    interface RunningGate {
        boolean isRunning(MachineContext ctx);
    }

    /** Supplies the current piston speed the renderer animates at. */
    interface PistonState {
        float pistonSpeed();
    }

    /** Owns the engine's heat/overheat state and the {@code STAGE} block-state property. */
    interface HeatState {
        double temperature();

        HeatStage stage();

        boolean isOverheated();

        void resetOverheat();
    }

    /** Toggles the engine's LIT block state (e.g. the Stirling Engine while burning fuel). */
    @FunctionalInterface
    interface LitController {
        void setLit(MachineContext ctx, boolean lit);
    }
}
