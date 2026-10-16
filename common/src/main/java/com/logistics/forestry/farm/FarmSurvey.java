package com.logistics.forestry.farm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * What working a farm's ring would clear, as one survey pass found it: counts of natural and
 * flagged blocks it would clear and of protected blocks it would skip, and where the flagged and
 * protected ones are, up to {@link #MAX_LISTED} of each. The farm's own growth is not counted.
 */
public final class FarmSurvey {

    /** Positions listed per kind; the counts go on past it. */
    public static final int MAX_LISTED = 256;

    public static final FarmSurvey EMPTY = new FarmSurvey();

    private int natural;
    private int flagged;
    private int protectedCount;
    private final List<BlockPos> flaggedAt = new ArrayList<>();
    private final List<BlockPos> protectedAt = new ArrayList<>();

    FarmSurvey() {}

    /** A survey with these counts and positions, as synced to a client; lists past {@link #MAX_LISTED} are cut. */
    public static FarmSurvey of(int natural, int flagged, int protectedCount, List<BlockPos> flaggedAt, List<BlockPos> protectedAt) {
        FarmSurvey survey = new FarmSurvey();
        survey.natural = Math.max(0, natural);
        survey.flagged = Math.max(0, flagged);
        survey.protectedCount = Math.max(0, protectedCount);
        flaggedAt.stream().limit(MAX_LISTED).map(BlockPos::immutable).forEach(survey.flaggedAt::add);
        protectedAt.stream().limit(MAX_LISTED).map(BlockPos::immutable).forEach(survey.protectedAt::add);
        return survey;
    }

    /** Records a block the farm would clear. */
    void cleared(BlockPos pos, ClearClassifier.Kind kind) {
        if (kind == ClearClassifier.Kind.FLAGGED) {
            flagged++;
            if (flaggedAt.size() < MAX_LISTED) {
                flaggedAt.add(pos.immutable());
            }
        } else {
            natural++;
        }
    }

    /** Records a block the farm would skip, its plot blocked. */
    void skipped(BlockPos pos) {
        protectedCount++;
        if (protectedAt.size() < MAX_LISTED) {
            protectedAt.add(pos.immutable());
        }
    }

    /** Natural blocks it would clear. */
    public int natural() {
        return natural;
    }

    /** Blocks it would clear that look player-built. */
    public int flagged() {
        return flagged;
    }

    /** Every block it would clear. */
    public int toClear() {
        return natural + flagged;
    }

    /** Blocks it would skip. */
    public int protectedCount() {
        return protectedCount;
    }

    public List<BlockPos> flaggedAt() {
        return Collections.unmodifiableList(flaggedAt);
    }

    public List<BlockPos> protectedAt() {
        return Collections.unmodifiableList(protectedAt);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FarmSurvey other
                && natural == other.natural
                && flagged == other.flagged
                && protectedCount == other.protectedCount
                && flaggedAt.equals(other.flaggedAt)
                && protectedAt.equals(other.protectedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(natural, flagged, protectedCount, flaggedAt, protectedAt);
    }

    @Override
    public String toString() {
        return "FarmSurvey[natural=" + natural + ", flagged=" + flagged + " " + flaggedAt
                + ", protected=" + protectedCount + " " + protectedAt + "]";
    }
}
