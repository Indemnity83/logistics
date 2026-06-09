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

    /** NBT key whose presence marks that the tag carries engine diagnostics. */
    public static final String KEY_STAGE = "stage";

    private EngineHudData() {}

    public static void write(CompoundTag data, AbstractEngineBlockEntity engine) {
        // Energy is intentionally omitted — Jade's built-in energy bar already shows the buffer. We only
        // add what Jade doesn't surface on its own.
        data.putString(KEY_STAGE, engine.getHeatStage().name());
        // The Creative engine is the odd man out: its stage is hardwired to COLD and its temperature is
        // meaningless, so the readout hides both for it.
        data.putBoolean("hasHeat", !(engine instanceof CreativeEngineBlockEntity));
        data.putDouble("temp", engine.getTemperature());
        data.putDouble("maxTemp", engine.getMaxTemperature());
        data.putLong("output", engine.getCurrentOutputPower());
        data.putBoolean("running", engine.isRunning());
        data.putBoolean("overheated", engine.isOverheated());

        if (engine instanceof StirlingEngineBlockEntity stirling) {
            data.putBoolean("stirling", true);
            data.putInt("burnTime", stirling.getBurnTime());
            data.putInt("fuelTime", stirling.getFuelTime());
        }
    }
}
