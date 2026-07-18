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
    public interface RunningGate {
        boolean isRunning(MachineContext ctx);
    }

    /** Supplies the current piston speed the renderer animates at. */
    public interface PistonState {
        float pistonSpeed();
    }

    /** Owns the engine's heat/overheat state and the {@code STAGE} block-state property. */
    public interface HeatState {
        double temperature();

        double maxTemperature();

        HeatStage stage();

        boolean canOverheat();

        boolean isOverheated();

        /** Clears an overheated engine (drains the buffer, resets to COLD); returns whether it was overheated. */
        boolean resetOverheat(MachineContext ctx);
    }

    /** Toggles the engine's LIT block state (e.g. the Stirling Engine while burning fuel). */
    @FunctionalInterface
    public interface LitController {
        void setLit(MachineContext ctx, boolean lit);
    }
}
