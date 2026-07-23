package com.logistics.power.engine;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.MagmaticEngineBlockEntity;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
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
    public static final String KEY_GENERATION = "generation";
    public static final String KEY_MAGMATIC_ENGINE = "magmaticEngine";
    public static final String KEY_MAGMATIC_ATTEMPTED = "magmaAttempted";
    public static final String KEY_MAGMATIC_WASTED = "magmaWasted";
    public static final String KEY_MAGMATIC_TEMP = "magmaTemp";
    public static final String KEY_LAVA_FLUID = "lavaFluid";
    public static final String KEY_LAVA_AMOUNT = "lavaAmount";

    private EngineHudData() {}

    public static void write(CompoundTag data, EngineEntity engine) {
        // Energy is intentionally omitted — Jade's built-in energy bar already shows the buffer. We only
        // add what Jade doesn't surface on its own.
        data.putString(KEY_STAGE, engine.getHeatStage().name());
        // Creative has no meaningful heat; the Magmatic Engine shows its own temperature line, not the
        // generic buffer-derived °C, so both hide the generic heat readout.
        data.putBoolean(KEY_HAS_HEAT,
                !(engine instanceof CreativeEngineBlockEntity) && !(engine instanceof MagmaticEngineBlockEntity));
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

        if (engine instanceof MagmaticEngineBlockEntity magma) {
            data.putBoolean(KEY_MAGMATIC_ENGINE, true);
            data.putLong(KEY_GENERATION, magma.simulation().lastAccepted());
            data.putLong(KEY_MAGMATIC_ATTEMPTED, magma.simulation().lastAttempted());
            data.putLong(KEY_MAGMATIC_WASTED, magma.simulation().lastWasted());
            data.putInt(KEY_MAGMATIC_TEMP, magma.simulation().temperatureCelsius());
            writeTank(data, KEY_LAVA_FLUID, KEY_LAVA_AMOUNT, magma.lavaTank());
        }
    }

    private static void writeTank(CompoundTag data, String fluidKey, String amountKey, FluidStoreComponent store) {
        if (store.tank().isEmpty()) {
            return;
        }
        data.putString(fluidKey, BuiltInRegistries.FLUID.getKey(store.tank().getFluidKey().getFluid()).toString());
        data.putInt(amountKey, (int) Math.min(FluidUnits.toMillibuckets(store.tank().getAmount()), Integer.MAX_VALUE));
    }
}
