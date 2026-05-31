package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.laserquarry.entity.ActiveQuarryRegistry;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ActiveQuarryRegistry")
class ActiveQuarryRegistryTest extends MinecraftTestEnvironment {

    private static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("test", "overworld"));
    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("test", "nether"));

    @BeforeEach
    @AfterEach
    void resetRegistryState() {
        ActiveQuarryRegistry.clear(OVERWORLD);
        ActiveQuarryRegistry.clear(NETHER);
    }

    @Test
    @DisplayName("returns empty list when no quarries are registered")
    void emptyWhenNothingRegistered() {
        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, 0L)).isEmpty();
    }

    @Test
    @DisplayName("returns registered positions for the given dimension")
    void returnsRegisteredPositions() {
        BlockPos a = new BlockPos(10, 64, 20);
        BlockPos b = new BlockPos(-5, 70, 100);
        ActiveQuarryRegistry.register(OVERWORLD, 0L, a);
        ActiveQuarryRegistry.register(OVERWORLD, 0L, b);

        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, 0L)).containsExactlyInAnyOrder(a, b);
    }

    @Test
    @DisplayName("isolates entries per dimension")
    void perDimensionIsolation() {
        BlockPos overworldPos = new BlockPos(0, 64, 0);
        BlockPos netherPos = new BlockPos(50, 32, 50);
        ActiveQuarryRegistry.register(OVERWORLD, 0L, overworldPos);
        ActiveQuarryRegistry.register(NETHER, 0L, netherPos);

        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, 0L)).containsExactly(overworldPos);
        assertThat(ActiveQuarryRegistry.getAll(NETHER, 0L)).containsExactly(netherPos);
    }

    @Test
    @DisplayName("unregister removes a single entry without affecting others")
    void unregisterRemovesOnlyTarget() {
        BlockPos a = new BlockPos(0, 64, 0);
        BlockPos b = new BlockPos(10, 64, 10);
        ActiveQuarryRegistry.register(OVERWORLD, 0L, a);
        ActiveQuarryRegistry.register(OVERWORLD, 0L, b);

        ActiveQuarryRegistry.unregister(OVERWORLD, a);

        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, 0L)).containsExactly(b);
    }

    @Test
    @DisplayName("retains entries when age equals the TTL exactly (eviction is strict-greater-than)")
    void retainsEntriesAtExactTTL() {
        BlockPos pos = new BlockPos(0, 64, 0);
        ActiveQuarryRegistry.register(OVERWORLD, 0L, pos);

        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, ActiveQuarryRegistry.REGISTRY_TTL_TICKS))
                .containsExactly(pos);
    }

    @Test
    @DisplayName("evicts entries older than the TTL on read")
    void evictsStaleEntries() {
        BlockPos fresh = new BlockPos(0, 64, 0);
        BlockPos stale = new BlockPos(100, 64, 100);
        ActiveQuarryRegistry.register(OVERWORLD, 0L, stale);
        // Fresh is registered well after stale, but still within the TTL of the read time.
        long freshRegisterTime = ActiveQuarryRegistry.REGISTRY_TTL_TICKS + 50L;
        ActiveQuarryRegistry.register(OVERWORLD, freshRegisterTime, fresh);

        long readTime = freshRegisterTime + (ActiveQuarryRegistry.REGISTRY_TTL_TICKS / 2);

        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, readTime)).containsExactly(fresh);
    }

    @Test
    @DisplayName("returns empty list and drops the dimension entry when all are stale")
    void dropsEmptyDimensionAfterEviction() {
        ActiveQuarryRegistry.register(OVERWORLD, 0L, new BlockPos(0, 64, 0));

        long readTime = ActiveQuarryRegistry.REGISTRY_TTL_TICKS + 1L;

        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, readTime)).isEmpty();
        // A subsequent read at game time 0 should also be empty (entry was fully removed).
        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, 0L)).isEmpty();
    }

    @Test
    @DisplayName("clear wipes all entries for the given dimension")
    void clearWipesDimension() {
        ActiveQuarryRegistry.register(OVERWORLD, 0L, new BlockPos(0, 64, 0));
        ActiveQuarryRegistry.register(NETHER, 0L, new BlockPos(0, 32, 0));

        ActiveQuarryRegistry.clear(OVERWORLD);

        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, 0L)).isEmpty();
        assertThat(ActiveQuarryRegistry.getAll(NETHER, 0L)).hasSize(1);
    }

    @Test
    @DisplayName("re-registering the same position updates its timestamp")
    void reregisterRefreshesTimestamp() {
        BlockPos pos = new BlockPos(0, 64, 0);
        ActiveQuarryRegistry.register(OVERWORLD, 0L, pos);

        // Re-register at a later game time
        ActiveQuarryRegistry.register(OVERWORLD, 1000L, pos);

        long readTime = 1000L + ActiveQuarryRegistry.REGISTRY_TTL_TICKS;
        assertThat(ActiveQuarryRegistry.getAll(OVERWORLD, readTime)).containsExactly(pos);
    }
}
