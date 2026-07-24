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
 * The Steam Engine simulation, built as a physical thermal-mass model rather than a bag of gameplay
 * rules. <b>Boiler heat is the engine's truth</b>; everything else is a one-way consequence of it:
 *
 * <pre>{@code
 * fuel  ->  boiler heat  ->  steam  ->  pressure  ->  RF
 * }</pre>
 *
 * <p>Once a fuel item is lit it burns continuously to completion (adding heat); the firebox controls
 * temperature only by deciding <em>when to commit the next item</em> — at/below {@code refuelHeat}.
 * Heat makes steam above the boiling point (consuming latent heat), steam is stored as
 * {@link PressureVessel} pressure, and the turbine spends pressure directly into RF on the output face
 * (no RF buffer). Passive loss bleeds heat every tick; below boiling, pressure condenses away. Heat is
 * clamped at {@code maxBoilerHeat} and any overflow is simply discarded — the engine never overheats or
 * fails; the only penalty for under-utilization is wasted fuel. The one feedback is indirect: load
 * spends pressure, which lets more steam form, which removes more heat, which eventually commits fuel.
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
    private double boilerHeat; // the primary state; heat past maxBoilerHeat is discarded
    private int committedBurnTicks;
    private int totalFuelTicks;
    private double waterDebt;

    private long lastGenerationRate; // accepted RF/t; see save() for why it is (client-sync) persisted
    private SteamFireboxState firebox = SteamFireboxState.OFF; // derived
    private boolean safetyValveActive; // derived, cosmetic: heat was discarded this tick
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
        double h0 = boilerHeat;
        int b0 = committedBurnTicks;
        double w0 = waterDebt;

        tickSimulation(ctx);
        syncStage(ctx);

        lit.setLit(ctx, firebox == SteamFireboxState.FIRING);

        if (vessel.pressure() != p0 || boilerHeat != h0 || committedBurnTicks != b0 || waterDebt != w0) {
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
            // Redstone-disabled: preserve the committed reserve unconsumed, firebox OFF; passive loss and
            // condensation still run (heat is lost first, then pressure once below boiling).
            firebox = SteamFireboxState.OFF;
            safetyValveActive = false;
            lastGenerationRate = 0;
            boilerHeat = Math.max(0, boilerHeat - profile.passiveHeatLoss());
            condense();
            vessel.clampTo(profile.maxPressure());
            status = SteamEngineStatus.REDSTONE_DISABLED;
            return;
        }

        // 1-2. Turbine: offer RF directly from pressure to the output-face neighbor (no buffer); spend
        // pressure only for what is accepted. The clamp keeps a misbehaving receiver from over-drawing.
        long offered = computeOutput();
        long accepted = Math.clamp(output.offer(ctx, offered), 0, offered);
        if (accepted > 0) {
            vessel.draw(accepted * profile.pressurePerRf());
        }
        lastGenerationRate = accepted;

        // 3. Passive heat loss (every state).
        boilerHeat = Math.max(0, boilerHeat - profile.passiveHeatLoss());

        // 4-5. Steam (only above boiling, with headroom + water): adds pressure, removes latent heat + water.
        boolean waterAvailable = !waterStore.tank().isEmpty();
        double steam = produceSteam();

        // 6. Fuel: a lit reserve burns continuously to completion; only when it is spent AND heat has
        // fallen to/below refuelHeat do we commit the next item (dry-firing is allowed — no water gate).
        boolean noFuelToCommit = advanceFuel(ctx);

        // 7. Derive the firebox purely from committed fuel + heat.
        if (committedBurnTicks > 0) {
            firebox = SteamFireboxState.FIRING;
        } else if (boilerHeat > profile.refuelHeat()) {
            firebox = SteamFireboxState.STOKED;
        } else {
            firebox = SteamFireboxState.OFF;
        }

        // 8-9. Clamp heat; the overflow is discarded (cosmetic safety valve, never a failure).
        safetyValveActive = boilerHeat > profile.maxBoilerHeat() + EPSILON;
        boilerHeat = Math.clamp(boilerHeat, 0.0, profile.maxBoilerHeat());

        // 10. Pressure condensation while cold; then the safety pressure clamp.
        condense();
        vessel.clampTo(profile.maxPressure());

        // 11. Status (GUI/tint/sync happen in serverTick around this).
        status = deriveStatus(offered, accepted, noFuelToCommit, waterAvailable, steam);
    }

    /**
     * Burns the committed reserve toward completion, or — once it is spent and the boiler has cooled to/below
     * refuelHeat — commits the next fuel item. Returns whether nothing could be committed (empty fuel slot).
     */
    private boolean advanceFuel(MachineContext ctx) {
        if (committedBurnTicks > 0) {
            int burned = Math.min(profile.firingRate(), committedBurnTicks);
            committedBurnTicks -= burned;
            boilerHeat += burned * profile.heatPerBurnTick();
            if (committedBurnTicks <= 0) {
                totalFuelTicks = 0;
            }
            return false;
        }
        if (boilerHeat <= profile.refuelHeat()) {
            int ticks = fuel.ignite(ctx);
            if (ticks > 0) {
                committedBurnTicks = ticks;
                totalFuelTicks = ticks;
                return false;
            }
            return true;
        }
        return false;
    }

    /** Steam condenses back to water only once the boiler has cooled below the boiling point. */
    private void condense() {
        if (boilerHeat < profile.boilingHeat()) {
            vessel.leak(profile.condensationRate());
        }
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
     * Produce steam this tick, limited by the heat-quality rate, target headroom, available water, and
     * the latent heat physically available. Consumes latent heat and (transactionally, via a fractional
     * accumulator that never promises absent water) the water it boils.
     *
     * @return the pressure of steam actually produced.
     */
    private double produceSteam() {
        if (boilerHeat < profile.boilingHeat()) {
            return 0; // below boiling: heating up, no steam yet
        }
        double targetHeadroom = Math.max(0, profile.targetPressure() - vessel.pressure());
        if (targetHeadroom <= EPSILON || profile.steamPerWaterMb() <= 0 || profile.latentHeat() <= 0) {
            return 0;
        }
        double rateLimited = profile.steamRate() * profile.heatFactor(boilerHeat);
        long unitsPerMb = FluidUnits.mb(1);
        double availableMb = waterStore.tank().getAmount() / (double) unitsPerMb;
        double usableWaterMb = Math.max(0, availableMb - waterDebt); // honor water already owed
        double waterLimited = usableWaterMb * profile.steamPerWaterMb();
        double heatLimited = boilerHeat / profile.latentHeat();
        double steam = Math.min(Math.min(rateLimited, targetHeadroom), Math.min(waterLimited, heatLimited));
        if (steam <= EPSILON) {
            return 0;
        }
        vessel.add(steam);
        boilerHeat -= steam * profile.latentHeat();
        waterDebt += steam / profile.steamPerWaterMb();
        long wholeMb = (long) Math.floor(waterDebt);
        if (wholeMb > 0) {
            waterStore.tank().extract(waterStore.tank().getFluidKey(), FluidUnits.mb(wholeMb), false);
            waterDebt -= wholeMb;
        }
        return steam;
    }

    private SteamEngineStatus deriveStatus(
            long offered, long accepted, boolean noFuelToCommit, boolean waterAvailable, double steam) {
        double pressure = vessel.pressure();
        boolean hasReserve = committedBurnTicks > 0;

        if (!hasReserve && noFuelToCommit && boilerHeat <= profile.refuelHeat() && pressure <= EPSILON) {
            return SteamEngineStatus.NO_FUEL;
        }
        if (hasReserve && boilerHeat < profile.boilingHeat()) {
            return SteamEngineStatus.HEATING; // fire lit, warming up, no steam yet
        }
        if (boilerHeat >= profile.boilingHeat() && !waterAvailable && pressure < profile.targetPressure()) {
            return SteamEngineStatus.NO_WATER; // hot enough to boil but dry
        }
        if (steam > EPSILON && pressure < profile.operatingPressure()) {
            return SteamEngineStatus.BUILDING_PRESSURE;
        }
        if (offered > 0 && accepted == 0) {
            return SteamEngineStatus.OUTPUT_BLOCKED; // pressure available but the neighbor took nothing
        }
        if (accepted > 0) {
            // Steaming while generating = fresh steam; generating without steam = coasting on pressure.
            return steam > EPSILON ? SteamEngineStatus.GENERATING : SteamEngineStatus.COASTING;
        }
        if (pressure > EPSILON) {
            return SteamEngineStatus.COASTING;
        }
        return SteamEngineStatus.EMPTY;
    }

    /**
     * Boiler heat mapped onto the shared engine heat-stage property so the static shaft tints by real
     * temperature (blue cold → red hot), keyed to the thermal regime: cold below boiling, cool up to the
     * refuel point, warm up to the steam-quality target, hot at/above it.
     */
    private HeatStage heatStage() {
        if (boilerHeat < profile.boilingHeat()) {
            return HeatStage.COLD;
        }
        if (boilerHeat < profile.refuelHeat()) {
            return HeatStage.COOL;
        }
        if (boilerHeat < profile.targetHeat()) {
            return HeatStage.WARM;
        }
        return HeatStage.HOT;
    }

    /** Writes the heat stage onto the block's {@code STAGE} property (drives the shaft tint) on change. */
    private void syncStage(MachineContext ctx) {
        BlockState state = ctx.blockState();
        if (!state.hasProperty(HeatStage.STAGE)) {
            return;
        }
        HeatStage stage = heatStage();
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

    public double boilerHeat() {
        return boilerHeat;
    }

    public double maxBoilerHeat() {
        return profile.maxBoilerHeat();
    }

    public double boilingHeat() {
        return profile.boilingHeat();
    }

    public double heatFraction() {
        double max = profile.maxBoilerHeat();
        return max <= 0 ? 0 : Math.clamp(boilerHeat / max, 0.0, 1.0);
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

    /** Cosmetic: heat was discarded this tick (fuel burning into a saturated boiler). Never affects the sim. */
    public boolean isSafetyValveActive() {
        return safetyValveActive;
    }

    public SteamEngineStatus status() {
        return status;
    }

    // ==================== Persistence ====================

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        vessel.save(tag, "Pressure");
        tag.putDouble("BoilerHeat", boilerHeat);
        tag.putInt("CommittedBurnTicks", committedBurnTicks);
        tag.putInt("TotalFuelTicks", totalFuelTicks);
        tag.putDouble("WaterDebt", waterDebt);
        // lastGenerationRate is CLIENT-ANIMATION SYNC state, not authoritative simulation state: the
        // update tag == save() is the only channel to the client, and the piston must reflect accepted RF
        // (not client-derivable). The next server tick unconditionally overwrites it, so the on-disk copy
        // is never trusted. (firebox/status/safetyValve are derived and not persisted.)
        tag.putLong("LastGenerationRate", lastGenerationRate);
        fuel.save(tag, registries);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        vessel.load(tag, "Pressure");
        boilerHeat = Math.max(0, NbtCompat.getDouble(tag, "BoilerHeat", 0)); // defaults cold; obsolete tags ignored
        committedBurnTicks = NbtCompat.getInt(tag, "CommittedBurnTicks", 0);
        totalFuelTicks = NbtCompat.getInt(tag, "TotalFuelTicks", 0);
        waterDebt = Math.max(0, NbtCompat.getDouble(tag, "WaterDebt", 0));
        lastGenerationRate = NbtCompat.getLong(tag, "LastGenerationRate", 0); // client piston; server recomputes
        if (committedBurnTicks <= 0) {
            committedBurnTicks = 0;
            totalFuelTicks = 0;
        }
        fuel.load(tag, registries);
    }
}
