package com.logistics.core;

import java.util.function.Supplier;

/**
 * No-op on mc/1.21.1: the vanilla thread-local {@code Profiler.get()} accessor does not exist on
 * this version, so tick-section instrumentation is inert here. The public API matches
 * mc/26.x/1.21.11 (where it wraps {@code Profiler.get()}) so the call sites stay identical across
 * branches.
 */
public final class LogisticsProfiler {

    private LogisticsProfiler() {}

    public static void push(String name) {}

    public static void push(Supplier<String> name) {}

    public static void popPush(String name) {}

    public static void pop() {}
}
