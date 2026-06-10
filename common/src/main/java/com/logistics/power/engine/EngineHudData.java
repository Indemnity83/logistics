package com.logistics.power.engine;

import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import net.minecraft.nbt.CompoundTag;

/**
 * Server-side capture of an engine's live diagnostics into the tag a look-at HUD (Jade) syncs to the
 * client. Loader-agnostic and free of any HUD-mod API, so the logic lives once in common; the thin
 * per-loader Jade provider just calls this from its server-data hook. Read back by the client-side
 * {@code EngineHudLines}.
 */
public final class EngineHudData {

    // NBT keys shared with the client reader (EngineHudLines) so the two can't silently drift.
    /** NBT key whose presence marks that the tag carries engine diagnostics. */
    public static final String KEY_STAGE = "stage";

    public static final String KEY_HAS_HEAT = "hasHeat";
    public static final String KEY_TEMP = "temp";
    public static final String KEY_MAX_TEMP = "maxTemp";
    public static final String KEY_OUTPUT = "output";
    public static final String KEY_RUNNING = "running";
    public static final String KEY_OVERHEATED = "overheated";
    public static final String KEY_STIRLING = "stirling";
    public static final String KEY_BURN_TIME = "burnTime";
    public static final String KEY_FUEL_TIME = "fuelTime";

    private EngineHudData() {}

    public static void write(CompoundTag data, AbstractEngineBlockEntity engine) {
        // Energy is intentionally omitted — Jade's built-in energy bar already shows the buffer. We only
        // add what Jade doesn't surface on its own.
        data.putString(KEY_STAGE, engine.getHeatStage().name());
        // The Creative engine is the odd man out: its stage is hardwired to COLD and its temperature is
        // meaningless, so the readout hides both for it.
        data.putBoolean(KEY_HAS_HEAT, !(engine instanceof CreativeEngineBlockEntity));
        data.putDouble(KEY_TEMP, engine.getTemperature());
        data.putDouble(KEY_MAX_TEMP, engine.getMaxTemperature());
        data.putLong(KEY_OUTPUT, engine.getCurrentOutputPower());
        data.putBoolean(KEY_RUNNING, engine.isRunning());
        data.putBoolean(KEY_OVERHEATED, engine.isOverheated());

        if (engine instanceof StirlingEngineBlockEntity stirling) {
            data.putBoolean(KEY_STIRLING, true);
            data.putInt(KEY_BURN_TIME, stirling.getBurnTime());
            data.putInt(KEY_FUEL_TIME, stirling.getFuelTime());
        }
    }
}
