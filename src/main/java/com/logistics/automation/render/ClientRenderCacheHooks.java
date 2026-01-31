package com.logistics.automation.render;

import java.util.function.Consumer;
import net.minecraft.util.math.BlockPos;

public final class ClientRenderCacheHooks {
    private static final Consumer<BlockPos> NOOP_CLEARER = pos -> {};
    private static final Runnable NOOP_CLEAR_ALL = () -> {};

    private static volatile Consumer<BlockPos> quarryInterpolationClearer = NOOP_CLEARER;
    private static volatile Runnable clearAllInterpolationCaches = NOOP_CLEAR_ALL;

    private ClientRenderCacheHooks() {}

    public static void setQuarryInterpolationClearer(Consumer<BlockPos> clearer) {
        quarryInterpolationClearer = clearer == null ? NOOP_CLEARER : clearer;
    }

    public static void setClearAllInterpolationCaches(Runnable clearAll) {
        clearAllInterpolationCaches = clearAll == null ? NOOP_CLEAR_ALL : clearAll;
    }

    public static void clearQuarryInterpolationCache(BlockPos pos) {
        quarryInterpolationClearer.accept(pos);
    }

    public static void clearAllInterpolationCaches() {
        clearAllInterpolationCaches.run();
    }
}
