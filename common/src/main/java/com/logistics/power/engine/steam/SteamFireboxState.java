package com.logistics.power.engine.steam;

/**
 * The Steam Engine's firebox, derived each tick (never persisted). A committed fuel reserve does not
 * freeze when it can't boil — it damps to {@link #STOKED} and burns slowly to keep the boiler hot,
 * which prevents pressure decay. Only a truly-out ({@link #OFF}) firebox lets pressure cool away.
 */
public enum SteamFireboxState {
    /** No committed reserve (or redstone-disabled): cold boiler, pressure decays. */
    OFF,
    /** Committed reserve but no steam this tick (at target / dry): damped, burns at the stoked rate, holds pressure. */
    STOKED,
    /** Committed reserve actively producing steam this tick: full burn rate. */
    BOILING
}
