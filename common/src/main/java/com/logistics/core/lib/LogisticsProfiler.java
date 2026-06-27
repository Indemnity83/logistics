package com.logistics.core.lib;

import java.util.function.Supplier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Thin wrapper over the active Minecraft {@link ProfilerFiller} that prefixes every section with
 * {@code logistics:} so our work groups together in the vanilla {@code /debug} profiler and in
 * sampling profilers like Spark.
 *
 * <p>{@link Profiler#get()} returns an inactive no-op filler when no profiler is running (i.e.
 * outside a server tick), so these calls are safe and effectively free to leave in hot paths.
 * Sections must be balanced — pair every {@link #push} with a {@link #pop}, preferably in a
 * {@code try/finally}.
 */
public final class LogisticsProfiler {
    private static final String PREFIX = "logistics:";

    private LogisticsProfiler() {}

    public static void push(String name) {
        Profiler.get().push(PREFIX + name);
    }

    /** Lazy variant; the label is only built when the profiler is active. */
    public static void push(Supplier<String> name) {
        Profiler.get().push(() -> PREFIX + name.get());
    }

    public static void popPush(String name) {
        Profiler.get().popPush(PREFIX + name);
    }

    public static void pop() {
        Profiler.get().pop();
    }
}
