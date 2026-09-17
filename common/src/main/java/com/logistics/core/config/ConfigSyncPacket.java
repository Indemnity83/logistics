package com.logistics.core.config;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigDefinition;
import com.indemnity83.configory.ConfigType;
import com.indemnity83.configory.ConfigValue;
import com.logistics.LogisticsConfigHost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client synchronization of the server's config values.
 *
 * <p>Config is server-authoritative, but client-side renderers and JEI read it too. Without this a
 * client reads <em>its own</em> config file, which on a server with non-default settings is simply a
 * different number — a Laser Quarry frame drawn at the wrong size, travelling items interpolated at
 * the wrong speed, wrong figures in a JEI tooltip.
 *
 * <p>Every exposed value of every domain config is sent, rather than a curated subset, so a new
 * client-side read needs no wire-format change and cannot be forgotten.
 */
public record ConfigSyncPacket(List<Entry> entries) implements CustomPacketPayload {

    /** One config value, addressed the way configory addresses it: owning config id plus dotted path. */
    public record Entry(String configId, String path, ConfigType type, String value) {}

    public static final Type<ConfigSyncPacket> TYPE =
        new Type<>(com.logistics.LogisticsCore.resource("sync_config").toIdentifier());

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_CODEC = StreamCodec.of(
        (buf, entry) -> {
            buf.writeUtf(entry.configId());
            buf.writeUtf(entry.path());
            buf.writeEnum(entry.type());
            buf.writeUtf(entry.value());
        },
        buf -> new Entry(buf.readUtf(), buf.readUtf(), buf.readEnum(ConfigType.class), buf.readUtf()));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPacket> CODEC = StreamCodec.composite(
        ENTRY_CODEC.apply(ByteBufCodecs.list()), ConfigSyncPacket::entries, ConfigSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Snapshot every exposed value of every registered domain config. */
    public static ConfigSyncPacket current() {
        List<Entry> entries = new ArrayList<>();
        for (Config config : LogisticsConfigHost.domainConfigs()) {
            for (ConfigDefinition<?> definition : config.exposedDefinitions()) {
                String path = definition.path().fullPath();
                entries.add(new Entry(config.id(), path, definition.type(), encode(config.get(path), definition)));
            }
        }
        return new ConfigSyncPacket(List.copyOf(entries));
    }

    /**
     * Values travel as text. Numbers are written through their own {@code toString}, so a float or
     * double round-trips exactly rather than through a lossy widening.
     *
     * <p>Each read falls back to the definition's default, matching what {@code Config.get(ConfigKey)}
     * resolves to. A value absent from the file is therefore synced as the number the server is actually
     * running on, instead of failing the whole snapshot.
     */
    private static String encode(ConfigValue value, ConfigDefinition<?> definition) {
        Object fallback = definition.defaultValue();
        return switch (definition.type()) {
            case BOOLEAN -> Boolean.toString(value.asBoolean((Boolean) fallback));
            case INT -> Integer.toString(value.asInt((Integer) fallback));
            case LONG -> Long.toString(value.asLong((Long) fallback));
            case FLOAT -> Float.toString(value.asFloat((Float) fallback));
            case DOUBLE -> Double.toString(value.asDouble((Double) fallback));
            case STRING -> value.asString((String) fallback);
            case ENUM -> value.asString(fallback instanceof Enum<?> constant ? constant.name() : null);
        };
    }
}
