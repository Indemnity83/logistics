package com.logistics.core.lib.farmland;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Every server level's {@link HydrationSource}s: farmland they hold counts as near water.
 *
 * <p>Each loader answers vanilla's farmland water check from here once it found no real water:
 *
 * <ul>
 *   <li>Fabric: a mixin on {@code FarmlandBlock.isNearWater} asks {@link #hydrate(ServerLevel, BlockPos)}.
 *   <li>NeoForge: its {@link Hook} gives each source a {@code FarmlandWaterManager} ticket.
 * </ul>
 *
 * <p>A source is registered while its owner is loaded and wants it, and unregistered once it stops
 * wanting it, is removed or unloads; {@link #clearLevel} ends every source of a level as it unloads.
 * Server thread only.
 */
public final class FarmlandHydration {

    /** A source's place in the registry. */
    @FunctionalInterface
    public interface Registration {
        /** Stops the source wetting anything; safe to call more than once. */
        void unregister();
    }

    /** The loader's part of a registration, for a loader that answers its check without {@link #hydrate(ServerLevel, BlockPos)}. */
    @FunctionalInterface
    public interface Hook {
        Registration attach(ServerLevel level, HydrationSource source);
    }

    private static final Registration NOTHING = () -> {};

    private static final Map<ServerLevel, LevelSources> LEVELS = new IdentityHashMap<>();
    private static Hook hook = (level, source) -> NOTHING;

    private FarmlandHydration() {}

    /** Installs the loader's hook; called once during loader initialization. */
    public static void installHook(Hook installed) {
        hook = installed;
    }

    /** Lets {@code source} wet farmland in {@code level} until the returned registration ends. */
    public static Registration register(ServerLevel level, HydrationSource source) {
        LevelSources sources = LEVELS.computeIfAbsent(level, key -> new LevelSources());
        Handle handle = new Handle(level, sources, source, hook.attach(level, source));
        sources.index.add(source);
        sources.handles.add(handle);
        return handle;
    }

    /** Whether a registered source wets the farmland at {@code pos}; the source pays if it does. */
    public static boolean hydrate(ServerLevel level, BlockPos pos) {
        LevelSources sources = LEVELS.get(level);
        return sources != null && sources.index.hydrate(level, pos);
    }

    /** Whether {@code source} wets {@code pos}: never outside its bounds. */
    public static boolean hydrate(HydrationSource source, ServerLevel level, BlockPos pos) {
        return source.bounds().isInside(pos) && source.hydrate(level, pos);
    }

    /** Ends every source of {@code level}, which is unloading. */
    public static void clearLevel(ServerLevel level) {
        LevelSources sources = LEVELS.get(level);
        if (sources != null) {
            new ArrayList<>(sources.handles).forEach(Handle::unregister);
            LEVELS.remove(level);
        }
    }

    /** Sources registered in {@code level}. */
    public static int sourceCount(ServerLevel level) {
        LevelSources sources = LEVELS.get(level);
        return sources != null ? sources.index.size() : 0;
    }

    /** Levels with at least one source. */
    static int levelCount() {
        return LEVELS.size();
    }

    private static final class LevelSources {
        final HydrationIndex index = new HydrationIndex();
        final Set<Handle> handles = new LinkedHashSet<>();
    }

    private static final class Handle implements Registration {
        private final ServerLevel level;
        private final LevelSources sources;
        private final HydrationSource source;
        private final Registration loader;
        private boolean active = true;

        Handle(ServerLevel level, LevelSources sources, HydrationSource source, Registration loader) {
            this.level = level;
            this.sources = sources;
            this.source = source;
            this.loader = loader;
        }

        @Override
        public void unregister() {
            if (!active) {
                return;
            }
            active = false;
            loader.unregister();
            sources.handles.remove(this);
            sources.index.remove(source);
            if (sources.index.isEmpty() && LEVELS.get(level) == sources) {
                LEVELS.remove(level);
            }
        }
    }
}
