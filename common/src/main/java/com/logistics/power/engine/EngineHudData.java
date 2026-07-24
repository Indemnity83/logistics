package com.logistics.power.engine;

import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.engine.block.entity.FuelEngineBlockEntity;
import com.logistics.power.engine.block.entity.MagmaticEngineBlockEntity;
import com.logistics.power.engine.block.entity.ReactionEngineBlockEntity;
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
    public static final String KEY_GENERATION = "generation";
    public static final String KEY_MAGMATIC_ENGINE = "magmaticEngine";
    public static final String KEY_MAGMATIC_TEMP = "magmaTemp";
    public static final String KEY_LAVA_FLUID = "lavaFluid";
    public static final String KEY_LAVA_AMOUNT = "lavaAmount";
    public static final String KEY_STEAM = "steam";
    public static final String KEY_PRESSURE = "pressure";
    public static final String KEY_MAX_PRESSURE = "maxPressure";
    public static final String KEY_WATER_FLUID = "waterFluid";
    public static final String KEY_WATER_AMOUNT = "waterAmount";
    public static final String KEY_BURN_RESERVE = "burnReserve";
    public static final String KEY_FIREBOX = "firebox";
    public static final String KEY_STATUS = "status";
    public static final String KEY_BOILER_HEAT = "boilerHeat";
    public static final String KEY_MAX_HEAT = "maxHeat";
    public static final String KEY_SAFETY_VALVE = "safetyValve";
    public static final String KEY_FUEL_ENGINE = "fuelEngine";
    public static final String KEY_COMMITTED_FUEL = "committedFuel";
    public static final String KEY_FUEL_FLUID = "fuelFluid";
    public static final String KEY_FUEL_AMOUNT = "fuelAmount";
    public static final String KEY_COOLANT_FLUID = "coolantFluid";
    public static final String KEY_COOLANT_AMOUNT = "coolantAmount";

    // Reaction engine (bufferless): attempted vs accepted output, reaction progress, and the reactant.
    public static final String KEY_REACTION = "reaction";
    public static final String KEY_ATTEMPTED = "attempted";
    public static final String KEY_ACCEPTED = "accepted";
    public static final String KEY_PROGRESS_REMAINING = "progressRemaining";
    public static final String KEY_PROGRESS_TOTAL = "progressTotal";
    public static final String KEY_REACTANT_ID = "reactantId";

    private EngineHudData() {}

    public static void write(CompoundTag data, EngineEntity engine) {
        // Energy is intentionally omitted — Jade's built-in energy bar already shows the buffer. We only
        // add what Jade doesn't surface on its own.
        data.putString(KEY_STAGE, engine.getHeatStage().name());
        // Creative, Magmatic, Steam, and Reaction engines omit the generic heat readout.
        data.putBoolean(
                KEY_HAS_HEAT,
                !(engine instanceof CreativeEngineBlockEntity)
                        && !(engine instanceof MagmaticEngineBlockEntity)
                        && !(engine instanceof SteamEngineBlockEntity)
                        && !(engine instanceof ReactionEngineBlockEntity));
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

        if (engine instanceof ReactionEngineBlockEntity reaction) {
            data.putBoolean(KEY_REACTION, true);
            data.putLong(KEY_ATTEMPTED, reaction.simulation().lastAttempted());
            data.putLong(KEY_ACCEPTED, reaction.simulation().lastAccepted());
            data.putInt(KEY_PROGRESS_REMAINING, reaction.simulation().remainingReactionTicks());
            data.putInt(KEY_PROGRESS_TOTAL, reaction.simulation().reactionDurationTicks());
            var tank = reaction.reactantTank().tank();
            data.putInt(KEY_REACTANT_ID, tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(tank.getFluidKey().getFluid()));
        }

        if (engine instanceof MagmaticEngineBlockEntity magma) {
            data.putBoolean(KEY_MAGMATIC_ENGINE, true);
            data.putLong(KEY_GENERATION, magma.simulation().lastAccepted());
            data.putInt(KEY_MAGMATIC_TEMP, magma.simulation().temperatureCelsius());
            writeTank(data, KEY_LAVA_FLUID, KEY_LAVA_AMOUNT, magma.lavaTank());
        }

        if (engine instanceof SteamEngineBlockEntity steam) {
            data.putBoolean(KEY_STEAM, true);
            data.putDouble(KEY_PRESSURE, steam.simulation().pressure());
            data.putDouble(KEY_MAX_PRESSURE, steam.simulation().maxPressure());
            data.putLong(KEY_GENERATION, steam.simulation().lastGenerationRate());
            data.putInt(KEY_BURN_RESERVE, steam.simulation().committedBurnTicks());
            data.putString(KEY_FIREBOX, steam.simulation().fireboxState().name());
            data.putString(KEY_STATUS, steam.simulation().status().name());
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

        if (engine instanceof FuelEngineBlockEntity fuel) {
            data.putBoolean(KEY_FUEL_ENGINE, true);
            data.putLong(KEY_GENERATION, fuel.simulation().lastGenerationRate());
            data.putLong(KEY_COMMITTED_FUEL, fuel.simulation().committedFuelEnergy());
            writeTank(data, KEY_FUEL_FLUID, KEY_FUEL_AMOUNT, fuel.fuelTank());
            writeTank(data, KEY_COOLANT_FLUID, KEY_COOLANT_AMOUNT, fuel.coolantTank());
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
