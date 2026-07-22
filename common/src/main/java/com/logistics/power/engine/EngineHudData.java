package com.logistics.power.engine;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.SteamEngineBlockEntity;
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
    public static final String KEY_STEAM = "steam";
    public static final String KEY_PRESSURE = "pressure";
    public static final String KEY_MAX_PRESSURE = "maxPressure";
    public static final String KEY_GENERATION = "generation";
    public static final String KEY_WATER_FLUID = "waterFluid";
    public static final String KEY_WATER_AMOUNT = "waterAmount";
    public static final String KEY_BURN_RESERVE = "burnReserve";
    public static final String KEY_FIREBOX = "firebox";
    public static final String KEY_STATUS = "status";
    public static final String KEY_BOILER_HEAT = "boilerHeat";
    public static final String KEY_MAX_HEAT = "maxHeat";
    public static final String KEY_SAFETY_VALVE = "safetyValve";

    private EngineHudData() {}

    public static void write(CompoundTag data, EngineEntity engine) {
        // Energy is intentionally omitted — Jade's built-in energy bar already shows the buffer. We only
        // add what Jade doesn't surface on its own.
        data.putString(KEY_STAGE, engine.getHeatStage().name());
        // The Creative and Steam engines have no meaningful heat: the Creative stage is hardwired to COLD,
        // and the Steam engine cannot overheat (pressure, not temperature, is its state), so both hide it.
        data.putBoolean(
                KEY_HAS_HEAT,
                !(engine instanceof CreativeEngineBlockEntity) && !(engine instanceof SteamEngineBlockEntity));
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

        if (engine instanceof SteamEngineBlockEntity steam) {
            data.putBoolean(KEY_STEAM, true);
            data.putDouble(KEY_PRESSURE, steam.simulation().pressure());
            data.putDouble(KEY_MAX_PRESSURE, steam.simulation().maxPressure());
            data.putLong(KEY_GENERATION, steam.simulation().lastGenerationRate());
            data.putInt(KEY_BURN_RESERVE, steam.simulation().committedBurnTicks());
            data.putInt(KEY_FIREBOX, steam.simulation().fireboxState().ordinal());
            data.putInt(KEY_STATUS, steam.simulation().status().ordinal());
            data.putDouble(KEY_BOILER_HEAT, steam.simulation().boilerHeat());
            data.putDouble(KEY_MAX_HEAT, steam.simulation().maxBoilerHeat());
            data.putBoolean(KEY_SAFETY_VALVE, steam.simulation().isSafetyValveActive());
            if (!steam.waterTank().tank().isEmpty()) {
                data.putString(KEY_WATER_FLUID,
                        BuiltInRegistries.FLUID.getKey(steam.waterTank().tank().getFluidKey().getFluid()).toString());
                data.putInt(KEY_WATER_AMOUNT,
                        (int) Math.min(FluidUnits.toMillibuckets(steam.waterTank().tank().getAmount()), Integer.MAX_VALUE));
            }
        }
    }
}
