package com.logistics.automation.render;

import com.logistics.automation.jei.ClientMachineRecipes;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

/**
 * When the automation domain's client-side caches are evicted, in one place.
 *
 * <p>These caches are keyed by {@link net.minecraft.core.BlockPos} alone — no dimension, no level —
 * so an entry outlives the quarry that made it and is inherited by the next quarry at those
 * coordinates, in any dimension or any save, for the rest of the client session.
 *
 * <p>The policy lives here rather than in each loader's client setup because it is loader-agnostic:
 * only the <em>events</em> that trigger it differ. Listing it per loader is what let NeoForge miss
 * every eviction path while Fabric had them all.
 */
public final class AutomationClientHooks {

    private AutomationClientHooks() {}

    /** Wire the eviction callbacks the block entities fire. Call once during client setup. */
    public static void install() {
        ClientRenderCacheHooks.setQuarryInterpolationClearer(LaserQuarryRenderState::clearInterpolationCache);
        ClientRenderCacheHooks.setClearAllInterpolationCaches(LaserQuarryRenderState::clearAllInterpolationCaches);
    }

    /** Drop entries for quarries that are no longer loaded. Call from the loader's client tick. */
    public static void onClientTick(@Nullable ClientLevel level) {
        if (level != null) {
            LaserQuarryRenderState.pruneInterpolationCache(level);
        }
    }

    /** Drop everything. Call on disconnect and on client shutdown. */
    public static void clearAll() {
        ClientRenderCacheHooks.clearAllInterpolationCaches();
        ClientMachineRecipes.clear();
    }
}
