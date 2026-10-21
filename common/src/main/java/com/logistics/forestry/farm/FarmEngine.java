package com.logistics.forestry.farm;

import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.UUID;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * The farm's work loop as its block entity, menu and HUD see it, whichever engine runs: the ring
 * engine ({@link FarmComponent}) or the tend engine ({@link TendFarmComponent}).
 *
 * <p>The survey, start, area preview and ring overrides only mean something on the ring engine;
 * the tend engine always works, has nothing to preview and ignores them.
 */
public interface FarmEngine extends MachineComponent, MachineComponent.HudContributor {

    FarmStatus status();

    @Nullable
    UUID owner();

    void setOwner(@Nullable UUID owner);

    /** Side of the square the farm works out to. */
    int areaSize();

    /** Plots the farm counts. */
    int plotsTotal();

    /** Of {@link #plotsTotal()}, those the farm cannot work right now. */
    int plotsBlocked();

    /**
     * Test seam: inspects {@code checksPerSecond} positions and performs {@code actionsPerSecond}
     * actions a second instead of the tier's; 0 or less for both restores them.
     */
    void setRateOverride(int checksPerSecond, int actionsPerSecond);

    FarmState state();

    FarmSurvey survey();

    boolean start();

    boolean toggleArea(MachineContext ctx, long now);

    FarmSurvey expanded(MachineContext ctx, long now);

    boolean previewShown(long now);

    FarmPreview syncedPreview();

    <T> T withPreview(MachineContext ctx, long now, Supplier<T> save);

    void setAreaOverride(int inner, int outer);

    void setClearanceHeightOverride(int height);
}
