package com.logistics.forestry.farm;

/**
 * The area preview a client draws for a farm, as its block entity's update tag carries it: the
 * farm's state, the game time the preview shows until, the ring's inner and outer sides, the
 * height kept clear above each plot, and the survey (positions only while the preview is visible).
 */
public record FarmPreview(FarmState state, long until, int inner, int outer, int height, FarmSurvey survey) {

    public static final FarmPreview NONE = new FarmPreview(FarmState.ACTIVE, 0, 0, 0, 0, FarmSurvey.EMPTY);

    /** Ticks a preview shows for once asked: after Show area, or an upgrade. */
    public static final int SHOW_TICKS = 600;

    /** Shown while the farm surveys, or until {@link #until}. */
    public boolean visible(long gameTime) {
        return outer > 0 && (state == FarmState.SURVEY || gameTime < until);
    }
}
