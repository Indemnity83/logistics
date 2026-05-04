package com.logistics.pipe.network;

import com.logistics.core.DebugLog;

/**
 * Debug logger for the pipe network subsystem.
 * Enable via: /logistics debug network true
 */
public final class NetDbg {
    public static final String DOMAIN = "network";

    private NetDbg() {}

    public static boolean isEnabled() {
        return DebugLog.isEnabled(DOMAIN);
    }

    public static void out(String format, Object... args) {
        DebugLog.debug(DOMAIN, format, args);
    }
}
