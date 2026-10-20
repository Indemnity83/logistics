package com.logistics.forestry.farm;

/**
 * Whether a farm only surveys its ring or works it. A placed farm surveys until the player presses
 * Start. Saved by {@link #id()}, never by ordinal; a missing or unknown id loads as {@link #SURVEY}.
 * The menu syncs it by ordinal, so new values go at the end.
 */
public enum FarmState {
    /** Reads the ring and reports what working it would clear; changes nothing and spends nothing. */
    SURVEY("survey"),
    /** Works the ring. */
    ACTIVE("active");

    private final String id;

    FarmState(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** The state saved as {@code id}; {@link #SURVEY} for anything else. */
    public static FarmState fromId(String id) {
        return ACTIVE.id.equals(id) ? ACTIVE : SURVEY;
    }
}
