package com.logistics.core.config;

import com.indemnity83.configory.ConfigKey;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The server's config values, as received by a client connected to a <em>remote</em> server.
 *
 * <p>{@link com.logistics.LogisticsConfigHost#get} consults this first, so client-side renderers and
 * JEI read the values that actually drive gameplay without any call site knowing about the sync.
 *
 * <p>Nothing is installed in single-player: there the integrated server reads the very same config
 * files the client would, so the local read is already authoritative -- and leaving the store empty
 * keeps server-side logic in that JVM from ever resolving through a synced snapshot.
 */
public final class RemoteConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/config");

    /** Immutable snapshot; replaced wholesale so readers never observe a half-applied sync. */
    private static volatile Map<String, ConfigSyncPacket.Entry> values = Map.of();

    private RemoteConfig() {}

    /** Adopt the server's values. Called on join, client-side, for remote servers only. */
    public static void install(ConfigSyncPacket packet) {
        Map<String, ConfigSyncPacket.Entry> snapshot = new HashMap<>();
        for (ConfigSyncPacket.Entry entry : packet.entries()) {
            snapshot.put(id(entry.configId(), entry.path()), entry);
        }
        values = Map.copyOf(snapshot);
    }

    /** Drop the server's values on disconnect, so the next session starts from the local config. */
    public static void clear() {
        values = Map.of();
    }

    /** {@return whether a server's values are currently in effect} */
    public static boolean isActive() {
        return !values.isEmpty();
    }

    /**
     * {@return the server's value for {@code key}, or {@code null} when nothing has been synced for it}
     *
     * <p>A key the server did not send -- an unknown value, or a key added in a client-side mod version
     * the server does not have -- falls back to the local config rather than failing.
     */
    public static <T> T resolve(ConfigKey<T> key) {
        ConfigSyncPacket.Entry entry = values.get(id(key.configId(), key.path().fullPath()));
        if (entry == null) {
            return null;
        }
        try {
            return key.definition().valueClass().cast(decode(entry, key));
        } catch (RuntimeException e) {
            LOGGER.warn("Ignoring unreadable synced config value for {}; using the local value", key, e);
            return null;
        }
    }

    private static Object decode(ConfigSyncPacket.Entry entry, ConfigKey<?> key) {
        return switch (entry.type()) {
            case BOOLEAN -> Boolean.valueOf(entry.value());
            case INT -> Integer.valueOf(entry.value());
            case LONG -> Long.valueOf(entry.value());
            case FLOAT -> Float.valueOf(entry.value());
            case DOUBLE -> Double.valueOf(entry.value());
            case STRING -> entry.value();
            case ENUM -> enumValue(key.definition().valueClass(), entry.value());
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> valueClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) valueClass.asSubclass(Enum.class), name);
    }

    private static String id(String configId, String path) {
        return configId + ' ' + path;
    }
}
