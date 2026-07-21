package com.logistics.power.engine.steam;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.power.EngineComponent;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.core.lib.power.fuel.FuelSource;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.FluidStoreComponent;
import java.util.function.BooleanSupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Steam Engine simulation: a pressure-vessel engine. Solid fuel is a committed burn reserve, lit
 * only when pressure falls below the relight threshold and water is present. While a reserve burns it
 * boils water into stored {@link PressureVessel} pressure (up to the target, not the safety max). The
 * turbine offers RF <em>directly</em> from pressure to the output-face neighbor each tick and spends
 * pressure only for what the neighbor accepts — pressure is the engine's only stored-energy reserve
 * (no RF buffer). Below the operating threshold a committed fire is "force-fired": it burns its reserve
 * {@code startupBurnMultiplier}× faster (an efficiency penalty, not more steam). The firebox damps to
 * {@link SteamFireboxState#STOKED} when it can't boil — burning slowly to keep the boiler hot — so only
 * a truly-out firebox lets pressure decay. It has no heat model and cannot overheat.
 *
 * <p>Plugs into {@code EngineEntity} through {@link EngineComponent.RunningGate} +
 * {@link EngineComponent.PistonState}; the fuel-ignition ({@link FuelSource}) and energy-output
 * ({@link TurbineOutput}) seams are injected so unit tests can drive it without a live level.
 */
public final class SteamEngineComponent
        implements MachineComponent, EngineComponent.RunningGate, EngineComponent.PistonState {

    private static final double EPSILON = 1e-6;

    private final String id;
    private final TurbineOutput output;
    private final FluidStoreComponent waterStore;
    private final FuelSource fuel;
    private final SteamEngineProfile profile;
    private final BooleanSupplier powered;
    private final EngineComponent.LitController lit;
    private final Runnable onChanged;

    private final PressureVessel vessel = new PressureVessel();
    private int committedBurnTicks;
    private int totalFuelTicks;
    private int stokedTickAccumulator;
    private double waterDebt;

    private long lastGenerationRate; // accepted RF/t; see save() for why it is (client-sync) persisted
    private SteamFireboxState firebox = SteamFireboxState.OFF; // derived
    private boolean forcedFiring; // derived: boiling below operating pressure
    private SteamEngineStatus status = SteamEngineStatus.EMPTY; // derived
    private long syncedSpeedBucket;

    public SteamEngineComponent(
            String id,
            TurbineOutput output,
            FluidStoreComponent waterStore,
            FuelSource fuel,
            SteamEngineProfile profile,
            BooleanSupplier powered,
            EngineComponent.LitController lit,
            Runnable onChanged) {
        this.id = id;
        this.output = output;
        this.waterStore = waterStore;
        this.fuel = fuel;
        this.profile = profile;
        this.powered = powered;
        this.lit = lit;
        this.onChanged = onChanged;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        double p0 = vessel.pressure();
        int b0 = committedBurnTicks;
        int s0 = stokedTickAccumulator;
        double w0 = waterDebt;

        tickSimulation(ctx);
        syncStage(ctx);

        lit.setLit(ctx, firebox != SteamFireboxState.OFF);

        if (vessel.pressure() != p0 || committedBurnTicks != b0 || stokedTickAccumulator != s0 || waterDebt != w0) {
            onChanged.run();
        }

        // Push a client sync when the piston's speed bucket changes (including start/stop). The piston
        // reflects accepted RF/t (lastGenerationRate), which is not client-derivable, so this refreshes it.
        long bucket = Math.round(pistonSpeed() * 1000);
        if (bucket != syncedSpeedBucket) {
            syncedSpeedBucket = bucket;
            ctx.sync();
        }
    }

    private void tickSimulation(MachineContext ctx) {
        if (!powered.getAsBoolean()) {
            // Redstone shutdown: preserve the reserve without consuming it, firebox OFF, cooling decay.
            firebox = SteamFireboxState.OFF;
            forcedFiring = false;
            stokedTickAccumulator = 0;
            lastGenerationRate = 0;
            vessel.leak(profile.coolingDecayPerTick());
            vessel.clampTo(profile.maxPressure());
            status = SteamEngineStatus.REDSTONE_DISABLED;
            return;
        }

        // Turbine: offer RF directly from pressure to the output-face neighbor (no buffer); spend pressure
        // only for what is accepted. The clamp keeps a misbehaving receiver from over-drawing pressure.
        long offered = computeOutput();
        long accepted = Math.clamp(output.offer(ctx, offered), 0, offered);
        if (accepted > 0) {
            vessel.draw(accepted * profile.pressurePerRf());
        }
        lastGenerationRate = accepted;

        // Relight when the reserve is out, pressure is low, and water is present (never waste fuel dry).
        boolean waterAvailable = !waterStore.tank().isEmpty();
        boolean noFuel = false;
        if (committedBurnTicks <= 0 && vessel.pressure() < profile.relightPressure() && waterAvailable) {
            int ticks = fuel.ignite(ctx);
            if (ticks > 0) {
                committedBurnTicks = ticks;
                totalFuelTicks = ticks;
            } else {
                noFuel = true;
            }
        }

        // Pressure the boiler sees this tick (post-turbine, pre-boil) — classifies the whole boiler tick,
        // so steam added below can't flip the burn cost mid-tick.
        double pressureBeforeBoiling = vessel.pressure();

        // Boil: produce steam if a reserve exists and pressure is below the target.
        double steam = 0;
        if (committedBurnTicks > 0 && pressureBeforeBoiling < profile.targetPressure()) {
            steam = produceSteam();
        }

        // Derive the firebox from the reserve and whether steam was produced this tick.
        if (committedBurnTicks <= 0) {
            firebox = SteamFireboxState.OFF;
        } else if (steam > EPSILON) {
            firebox = SteamFireboxState.BOILING;
        } else {
            firebox = SteamFireboxState.STOKED;
        }
        forcedFiring = firebox == SteamFireboxState.BOILING && pressureBeforeBoiling < profile.operatingPressure();

        // Spend the reserve: forced-firing (boiling below operating) burns faster, then normal boiling,
        // then slow stoking, then nothing while off.
        switch (firebox) {
            case BOILING -> {
                int cost = forcedFiring ? profile.startupBurnMultiplier() : 1;
                committedBurnTicks = Math.max(0, committedBurnTicks - cost);
                stokedTickAccumulator = 0;
            }
            case STOKED -> {
                if (++stokedTickAccumulator >= profile.stokedBurnInterval()) {
                    committedBurnTicks--;
                    stokedTickAccumulator = 0;
                }
            }
            case OFF -> stokedTickAccumulator = 0;
        }
        if (committedBurnTicks <= 0) {
            committedBurnTicks = 0;
            totalFuelTicks = 0;
            stokedTickAccumulator = 0;
        }

        // Pressure decay only while the firebox is out (a stoked/boiling fire maintains boiler heat).
        if (firebox == SteamFireboxState.OFF) {
            vessel.leak(profile.coolingDecayPerTick());
        }

        vessel.clampTo(profile.maxPressure());
        status = deriveStatus(offered, accepted, noFuel, waterAvailable);
    }

    /** Flat-then-ramp turbine output, limited only by the pressure physically available (no buffer). */
    private long computeOutput() {
        long desired = profile.desiredOutput(vessel.pressure());
        long pressureLimited = profile.pressurePerRf() > 0
                ? (long) Math.floor(vessel.pressure() / profile.pressurePerRf())
                : Long.MAX_VALUE;
        return Math.max(0, Math.min(desired, pressureLimited));
    }

    /**
     * Produce steam this tick, limited by the burn rate, target headroom, and available water; consume
     * the water transactionally via a fractional accumulator that never promises absent water.
     *
     * @return the pressure of steam actually produced.
     */
    private double produceSteam() {
        double targetHeadroom = Math.max(0, profile.targetPressure() - vessel.pressure());
        if (targetHeadroom <= EPSILON || profile.steamPerWaterMb() <= 0) {
            return 0;
        }
        long unitsPerMb = FluidUnits.mb(1);
        double availableMb = waterStore.tank().getAmount() / (double) unitsPerMb;
        double usableWaterMb = Math.max(0, availableMb - waterDebt); // honor water already owed
        double waterLimited = usableWaterMb * profile.steamPerWaterMb();
        double steam = Math.min(profile.steamPerBurnTick(), Math.min(targetHeadroom, waterLimited));
        if (steam <= EPSILON) {
            return 0;
        }
        vessel.add(steam);
        waterDebt += steam / profile.steamPerWaterMb();
        long wholeMb = (long) Math.floor(waterDebt);
        if (wholeMb > 0) {
            waterStore.tank().extract(waterStore.tank().getFluidKey(), FluidUnits.mb(wholeMb), false);
            waterDebt -= wholeMb;
        }
        return steam;
    }

    private SteamEngineStatus deriveStatus(long offered, long accepted, boolean noFuel, boolean waterAvailable) {
        double pressure = vessel.pressure();
        boolean hasReserve = committedBurnTicks > 0;

        if (firebox == SteamFireboxState.STOKED && !waterAvailable) {
            return SteamEngineStatus.NO_WATER; // lit but can't boil for lack of water
        }
        if (!hasReserve && noFuel && pressure < profile.relightPressure()) {
            return SteamEngineStatus.NO_FUEL;
        }
        if (pressure <= EPSILON && !hasReserve) {
            return SteamEngineStatus.EMPTY;
        }
        if (firebox == SteamFireboxState.BOILING && pressure < profile.operatingPressure()) {
            return SteamEngineStatus.BUILDING_PRESSURE; // (forced firing while below operating)
        }
        if (offered > 0 && accepted == 0) {
            return SteamEngineStatus.OUTPUT_BLOCKED; // pressure available but the neighbor took nothing
        }
        if (accepted > 0) {
            // Boiling while generating = fresh steam; generating without boiling = coasting on the reserve.
            return firebox == SteamFireboxState.BOILING ? SteamEngineStatus.GENERATING : SteamEngineStatus.COASTING;
        }
        return SteamEngineStatus.COASTING;
    }

    /**
     * Pressure mapped onto the shared engine heat-stage property so the static shaft tints the same way
     * Stirling's does, but by stored pressure instead of temperature: blue building, green/yellow through
     * the operating band, red when over the target.
     */
    private HeatStage pressureStage() {
        double p = vessel.pressure();
        if (p < profile.operatingPressure()) {
            return HeatStage.COLD;
        }
        if (p < profile.relightPressure()) {
            return HeatStage.COOL;
        }
        if (p < profile.targetPressure()) {
            return HeatStage.WARM;
        }
        return HeatStage.HOT;
    }

    /** Writes the pressure stage onto the block's {@code STAGE} property (drives the shaft tint) on change. */
    private void syncStage(MachineContext ctx) {
        BlockState state = ctx.blockState();
        if (!state.hasProperty(HeatStage.STAGE)) {
            return;
        }
        HeatStage stage = pressureStage();
        if (state.getValue(HeatStage.STAGE) != stage) {
            ctx.setBlockState(state.setValue(HeatStage.STAGE, stage), Block.UPDATE_ALL);
        }
    }

    // ==================== RunningGate / PistonState ====================

    @Override
    public boolean isRunning(MachineContext ctx) {
        return lastGenerationRate > 0; // host ANDs isPowered(); reflects actual accepted RF, synced to the client
    }

    /** Piston speed tracks the accepted RF/t (0.02..0.08 over 0..maxOutput), synced via lastGenerationRate. */
    @Override
    public float pistonSpeed() {
        if (lastGenerationRate <= 0) {
            return 0f;
        }
        long max = Math.max(1, profile.maxOutput());
        float t = Math.clamp(lastGenerationRate / (float) max, 0f, 1f);
        return 0.02f + t * 0.06f;
    }

    // ==================== GUI/HUD getters (derived, live) ====================

    public double pressure() {
        return vessel.pressure();
    }

    public double pressureFraction() {
        return vessel.fraction(profile.maxPressure());
    }

    public double maxPressure() {
        return profile.maxPressure();
    }

    public long lastGenerationRate() {
        return lastGenerationRate;
    }

    public int committedBurnTicks() {
        return committedBurnTicks;
    }

    public int totalFuelTicks() {
        return totalFuelTicks;
    }

    public double burnFraction() {
        return totalFuelTicks > 0 ? Math.clamp((double) committedBurnTicks / totalFuelTicks, 0.0, 1.0) : 0.0;
    }

    public SteamFireboxState fireboxState() {
        return firebox;
    }

    /** Whether the fire is being force-fired (boiling below operating pressure, burning fuel faster). */
    public boolean isForcedFiring() {
        return forcedFiring;
    }

    public SteamEngineStatus status() {
        return status;
    }

    // ==================== Persistence ====================

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        vessel.save(tag, "Pressure");
        tag.putInt("CommittedBurnTicks", committedBurnTicks);
        tag.putInt("TotalFuelTicks", totalFuelTicks);
        tag.putInt("StokedTickAccumulator", stokedTickAccumulator);
        tag.putDouble("WaterDebt", waterDebt);
        // lastGenerationRate is CLIENT-ANIMATION SYNC state, not authoritative simulation state: the
        // update tag == save() is the only channel to the client, and the piston must reflect accepted RF
        // (not client-derivable). The next server tick unconditionally overwrites it, so the on-disk copy
        // is never trusted. (firebox/status are derived and not persisted.)
        tag.putLong("LastGenerationRate", lastGenerationRate);
        fuel.save(tag, registries);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        vessel.load(tag, "Pressure");
        committedBurnTicks = NbtCompat.getInt(tag, "CommittedBurnTicks", 0);
        totalFuelTicks = NbtCompat.getInt(tag, "TotalFuelTicks", 0);
        stokedTickAccumulator = NbtCompat.getInt(tag, "StokedTickAccumulator", 0);
        waterDebt = Math.max(0, NbtCompat.getDouble(tag, "WaterDebt", 0));
        lastGenerationRate = NbtCompat.getLong(tag, "LastGenerationRate", 0); // client piston; server recomputes
        if (committedBurnTicks <= 0) {
            committedBurnTicks = 0;
            totalFuelTicks = 0;
            stokedTickAccumulator = 0;
        }
        fuel.load(tag, registries);
    }
}
