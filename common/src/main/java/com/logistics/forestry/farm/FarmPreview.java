package com.logistics.forestry.farm;

/**
 * The area preview a client draws for a farm, as its block entity's update tag carries it: the
 * farm's state, whether the toggle shows it, the game time an upgrade's flash shows it until, the
 * ring's inner and outer sides, the height kept clear above each plot, and the survey (positions
 * only while the preview is visible).
 */
public record FarmPreview(
        FarmState state, boolean shown, long until, int inner, int outer, int height, FarmSurvey survey) {

    public static final FarmPreview NONE = new FarmPreview(FarmState.ACTIVE, false, 0, 0, 0, 0, FarmSurvey.EMPTY);

    /** Ticks an upgrade flashes the preview of its new lanes for. */
    public static final int SHOW_TICKS = 600;

    /** Shown while toggled on, or until {@link #until}. */
    public boolean visible(long gameTime) {
        return outer > 0 && (shown || gameTime < until);
    }
}
