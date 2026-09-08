package com.logistics.power.render;

import com.logistics.core.lib.power.EngineEntity;
import com.logistics.power.engine.reaction.jei.ReactionJeiSyncAdapter;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;

/**
 * When the power domain's client-side caches are evicted, in one place — the counterpart to
 * {@code AutomationClientHooks}.
 *
 * <p>The engine animation cache is keyed by {@link BlockPos} alone, so a stale entry makes a new
 * engine resume the previous one's piston stroke instead of starting at rest.
 *
 * <p>Unlike the automation caches, the engine cache itself is per loader — each has its own renderer
 * with its own static map — so the loader supplies the two clear methods. Everything about
 * <em>when</em> to evict stays here.
 */
public final class PowerClientHooks {

    private static volatile Runnable clearAllEngineCaches = () -> {};

    private PowerClientHooks() {}

    /**
     * Wire the eviction callbacks. Call once during client setup.
     *
     * @param clearEngineAt     drops the cache entry for one position, fired when an engine is removed
     * @param clearAllEngines   drops every entry
     */
    public static void install(Consumer<BlockPos> clearEngineAt, Runnable clearAllEngines) {
        EngineEntity.setOnRemovedCallback(clearEngineAt);
        clearAllEngineCaches = clearAllEngines == null ? () -> {} : clearAllEngines;
    }

    /** Drop everything. Call on disconnect and on client shutdown. */
    public static void clearAll() {
        clearAllEngineCaches.run();
        ReactionJeiSyncAdapter.INSTANCE.clear();
    }
}
