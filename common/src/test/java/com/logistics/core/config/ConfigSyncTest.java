package com.logistics.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Server config sync")
class ConfigSyncTest {

    /** Static store shared with every other config test in this JVM, so never leave it installed. */
    @AfterEach
    void dropSyncedValues() {
        RemoteConfig.clear();
    }

    private static Config configOf(ConfigKey<?> key) {
        return ConfigRegistry.config(key.configId());
    }

    /** A packet carrying one overridden value, with everything else left at the server's real values. */
    private static ConfigSyncPacket packetOverriding(ConfigKey<?> key, String value) {
        List<ConfigSyncPacket.Entry> entries = ConfigSyncPacket.current().entries().stream()
            .map(entry -> entry.configId().equals(key.configId())
                    && entry.path().equals(key.path().fullPath())
                ? new ConfigSyncPacket.Entry(entry.configId(), entry.path(), entry.type(), value)
                : entry)
            .toList();
        return new ConfigSyncPacket(entries);
    }

    @Test
    @DisplayName("a client reads the server's value, not its own, once the sync has arrived")
    void syncedValueWins() {
        ConfigKey<Integer> key = LogisticsAutomation.CONFIG.QUARRY_AREA;
        int local = LogisticsConfigHost.get(key);

        RemoteConfig.install(packetOverriding(key, Integer.toString(local + 48)));

        assertThat(LogisticsConfigHost.get(key)).isEqualTo(local + 48);
        // The client's own file is untouched -- only the read is redirected.
        assertThat(configOf(key).get(key.path().fullPath()).asInt()).isEqualTo(local);
    }

    @Test
    @DisplayName("values fall back to the local config once the server's are dropped")
    void clearRestoresLocalValues() {
        ConfigKey<Integer> key = LogisticsAutomation.CONFIG.QUARRY_AREA;
        int local = LogisticsConfigHost.get(key);
        RemoteConfig.install(packetOverriding(key, Integer.toString(local + 48)));
        assertThat(LogisticsConfigHost.get(key)).isNotEqualTo(local);

        RemoteConfig.clear();

        assertThat(LogisticsConfigHost.get(key)).isEqualTo(local);
        assertThat(RemoteConfig.isActive()).isFalse();
    }

    @Test
    @DisplayName("a key the server never sent still reads from the local config")
    void unknownKeyFallsBackToLocal() {
        ConfigKey<Integer> key = LogisticsAutomation.CONFIG.QUARRY_AREA;
        int local = LogisticsConfigHost.get(key);

        // A server running an older version simply has no entry for this key.
        RemoteConfig.install(new ConfigSyncPacket(List.of()));

        assertThat(LogisticsConfigHost.get(key)).isEqualTo(local);
    }

    @Test
    @DisplayName("every value type survives the trip, floating point included")
    void everyTypeRoundTrips() {
        ConfigKey<Integer> intKey = LogisticsAutomation.CONFIG.QUARRY_AREA;
        ConfigKey<Long> longKey = LogisticsPower.CONFIG.MAGMATIC_OUTPUT;
        ConfigKey<Double> doubleKey = LogisticsPower.CONFIG.STIRLING_MIN_OUTPUT;
        ConfigKey<Float> floatKey = LogisticsPipe.CONFIG.PIPE_DRAG;

        List<ConfigSyncPacket.Entry> entries = List.of(
            entry(intKey, "37"), entry(longKey, "4242"), entry(doubleKey, "2.5"), entry(floatKey, "0.078125"));
        RemoteConfig.install(new ConfigSyncPacket(entries));

        assertThat(LogisticsConfigHost.get(intKey)).isEqualTo(37);
        assertThat(LogisticsConfigHost.get(longKey)).isEqualTo(4242L);
        assertThat(LogisticsConfigHost.get(doubleKey)).isEqualTo(2.5);
        assertThat(LogisticsConfigHost.get(floatKey)).isEqualTo(0.078125f);
    }

    @Test
    @DisplayName("the snapshot carries every key the client-side renderers and JEI read")
    void snapshotCoversTheClientReadKeys() {
        // Quarry frame geometry, pipe item motion, magmatic JEI figures -- the three areas read client-side.
        // Resolved before the snapshot, because a domain's config only joins the registry once its
        // CONFIG class initializes -- which in production every domain does during bootstrap.
        List<String> clientRead = List.of(
            pathOf(LogisticsAutomation.CONFIG.QUARRY_AREA),
            pathOf(LogisticsPipe.CONFIG.PIPE_MAX_SPEED),
            pathOf(LogisticsPipe.CONFIG.PIPE_MIN_SPEED),
            pathOf(LogisticsPipe.CONFIG.PIPE_DRAG),
            pathOf(LogisticsPower.CONFIG.MAGMATIC_OUTPUT),
            pathOf(LogisticsPower.CONFIG.MAGMATIC_BUFFER_CAPACITY),
            pathOf(LogisticsPower.CONFIG.MAGMATIC_TANK_CAPACITY),
            pathOf(LogisticsPower.CONFIG.MAGMATIC_BUCKET_BURN_TICKS));

        ConfigSyncPacket packet = ConfigSyncPacket.current();

        assertThat(packet.entries()).isNotEmpty();
        assertThat(packet.entries())
            .extracting(entry -> entry.configId() + ' ' + entry.path())
            .containsAll(clientRead);
    }

    @Test
    @DisplayName("the snapshot reports the server's current values, not the defaults")
    void snapshotReflectsEditedValues() {
        ConfigKey<Integer> key = LogisticsAutomation.CONFIG.QUARRY_AREA;
        int original = LogisticsConfigHost.get(key);
        try {
            assertThat(configOf(key).trySet(key, 48)).isTrue();

            assertThat(ConfigSyncPacket.current().entries())
                .filteredOn(entry -> (entry.configId() + ' ' + entry.path()).equals(pathOf(key)))
                .singleElement()
                .extracting(ConfigSyncPacket.Entry::value)
                .isEqualTo("48");
        } finally {
            configOf(key).trySet(key, original);
        }
    }

    private static ConfigSyncPacket.Entry entry(ConfigKey<?> key, String value) {
        return new ConfigSyncPacket.Entry(
            key.configId(), key.path().fullPath(), key.definition().type(), value);
    }

    private static String pathOf(ConfigKey<?> key) {
        return key.configId() + ' ' + key.path().fullPath();
    }
}
