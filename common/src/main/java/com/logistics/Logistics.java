package com.logistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common constants and utilities for the Logistics mod.
 * Platform-specific entry points (LogisticsMod) should delegate to this.
 */
public final class Logistics {

    public static final String MOD_ID = "logistics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Logistics() {}
}
