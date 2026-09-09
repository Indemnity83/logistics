package com.logistics.automation.laserquarry.entity;

/** Quarry operation phases. */
public enum QuarryPhase {
    CLEARING,
    BUILDING_FRAME,
    MINING,
    /** Mining is paused while blocks that intruded into the frame band are cleared out. */
    MAINTAINING_CLEARANCE,
    /** Mining is paused while gaps punched in the standing frame are rebuilt. */
    REPAIRING_FRAME
}
