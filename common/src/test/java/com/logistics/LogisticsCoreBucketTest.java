package com.logistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A placeable fluid's bucket is registered per loader, so it is absent from the common registry.
 * {@code BuiltInRegistries.ITEM} is defaulted and answers a miss with {@code minecraft:air}, so the
 * absent bucket must be recognised by value, not by a null check.
 */
@DisplayName("LogisticsCore bucket resolution")
class LogisticsCoreBucketTest extends MinecraftTestEnvironment {

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    @Test
    @DisplayName("an unregistered bucket resolves to AIR rather than null")
    void unregisteredBucketResolvesToAir() {
        assertThat(LogisticsCore.BUCKET.forFluid("no_such_fluid")).isSameAs(Items.AIR);
    }

    @Test
    @DisplayName("a bucket the loader never registered is left out instead of listed as AIR")
    void placeableBucketsSkipUnregisteredEntries() {
        // Common registers no placeable-fluid buckets, so every one of them resolves to AIR here.
        assertThat(LogisticsCore.CUSTOM_FLUIDS)
            .anyMatch(LogisticsCore.FluidDef::placeable);

        assertThat(LogisticsCore.BUCKET.placeableBuckets()).doesNotContain(Items.AIR);
    }
}
