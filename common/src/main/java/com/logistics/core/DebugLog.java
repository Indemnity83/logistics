package com.logistics.core;

import com.logistics.LogisticsMod;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime-toggleable debug logging by named subsystem.
 * Domain names are arbitrary strings — no registration required.
 * Enable via: /logistics debug <domain> true
 * Output is emitted at INFO level — no log4j configuration needed.
 */
public final class DebugLog {
    private static final Set<String> registered = ConcurrentHashMap.newKeySet();
    private static final Set<String> enabled = ConcurrentHashMap.newKeySet();

    private DebugLog() {}

    public static void register(String domain) {
        registered.add(domain);
    }

    public static Set<String> getRegisteredDomains() {
        return Collections.unmodifiableSet(registered);
    }

    public static boolean isEnabled(String domain) {
        return enabled.contains(domain);
    }

    public static void setEnabled(String domain, boolean on) {
        if (on) enabled.add(domain);
        else enabled.remove(domain);
    }

    public static Set<String> getEnabledDomains() {
        return Collections.unmodifiableSet(enabled);
    }

    public static void debug(String domain, String format, Object... args) {
        if (enabled.contains(domain)) {
            LogisticsMod.LOGGER.info("[" + domain + "] " + format, args);
        }
    }
}
