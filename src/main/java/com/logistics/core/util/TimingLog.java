package com.logistics.core.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public final class TimingLog {
    private static final Map<String, Long> STARTS = new ConcurrentHashMap<>();
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("logistics.timing", "false"));

    private TimingLog() {}

    public static void time(Logger logger, String label, Runnable runnable) {
        if (!ENABLED) {
            runnable.run();
            return;
        }
        long start = System.nanoTime();
        runnable.run();
        log(logger, label, start);
    }

    public static void start(String key) {
        if (!ENABLED) {
            return;
        }
        STARTS.put(key, System.nanoTime());
    }

    public static long getStart(String key) {
        if (!ENABLED) {
            return -1L;
        }
        Long start = STARTS.get(key);
        return start == null ? -1L : start;
    }

    public static void logSince(Logger logger, String key, String label) {
        if (!ENABLED) {
            return;
        }
        Long start = STARTS.remove(key);
        if (start != null) {
            log(logger, label, start);
        }
    }

    public static void log(Logger logger, String label, long startNanos) {
        if (!ENABLED) {
            return;
        }
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        logger.info("[timing] {} took {} ms", label, durationMs);
    }
}
