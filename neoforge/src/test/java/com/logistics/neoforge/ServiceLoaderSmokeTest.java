package com.logistics.neoforge;

import com.logistics.core.lib.block.BlockEntityTypeFactory;
import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.PlatformService;
import com.logistics.core.lib.platform.ResourceReloadRegistrar;
import com.logistics.core.lib.power.FuelHelper;
import com.logistics.neoforge.energy.NeoForgeEnergyCapabilityLookup;
import com.logistics.neoforge.platform.NeoForgeBlockEntityTypeFactory;
import com.logistics.neoforge.platform.NeoForgeCreativeTabRegistrar;
import com.logistics.neoforge.platform.NeoForgeResourceReloadRegistrar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceLoader smoke tests — mirrors {@code fabric/ServiceLoaderSmokeTest}.
 *
 * <p>Verifies that all META-INF/services/ registrations exist and the implementation
 * classes can be found and instantiated without a full NeoForge runtime bootstrap.
 * These tests would catch a missing services file that causes startup failure while
 * build CI passes.
 */
@DisplayName("ServiceLoader smoke tests (NeoForge)")
class ServiceLoaderSmokeTest {

    @Test
    @DisplayName("FuelHelper implementation is registered")
    void fuelHelper_isRegistered() {
        FuelHelper impl = ServiceLoader.load(FuelHelper.class).findFirst().orElseThrow(
                () -> new AssertionError("No FuelHelper found in META-INF/services/"));
        assertThat(impl).isInstanceOf(NeoForgeFuelHelper.class);
    }

    @Test
    @DisplayName("PlatformService implementation is registered")
    void platformService_isRegistered() {
        PlatformService impl = ServiceLoader.load(PlatformService.class).findFirst().orElseThrow(
                () -> new AssertionError("No PlatformService found in META-INF/services/"));
        assertThat(impl.getClass().getName()).startsWith("com.logistics.neoforge");
    }

    @Test
    @DisplayName("BlockEntityTypeFactory implementation is registered")
    void blockEntityTypeFactory_isRegistered() {
        BlockEntityTypeFactory impl = ServiceLoader.load(BlockEntityTypeFactory.class).findFirst().orElseThrow(
                () -> new AssertionError("No BlockEntityTypeFactory found in META-INF/services/"));
        assertThat(impl).isInstanceOf(NeoForgeBlockEntityTypeFactory.class);
    }

    @Test
    @DisplayName("EnergyCapabilityLookup implementation is registered")
    void energyCapabilityLookup_isRegistered() {
        EnergyCapabilityLookup impl = ServiceLoader.load(EnergyCapabilityLookup.class).findFirst().orElseThrow(
                () -> new AssertionError("No EnergyCapabilityLookup found in META-INF/services/"));
        assertThat(impl).isInstanceOf(NeoForgeEnergyCapabilityLookup.class);
    }

    @Test
    @DisplayName("CreativeTabRegistrar implementation is registered")
    void creativeTabRegistrar_isRegistered() {
        CreativeTabRegistrar impl = ServiceLoader.load(CreativeTabRegistrar.class).findFirst().orElseThrow(
                () -> new AssertionError("No CreativeTabRegistrar found in META-INF/services/"));
        assertThat(impl).isInstanceOf(NeoForgeCreativeTabRegistrar.class);
    }

    @Test
    @DisplayName("ResourceReloadRegistrar implementation is registered")
    void resourceReloadRegistrar_isRegistered() {
        ResourceReloadRegistrar impl = ServiceLoader.load(ResourceReloadRegistrar.class).findFirst().orElseThrow(
                () -> new AssertionError("No ResourceReloadRegistrar found in META-INF/services/"));
        assertThat(impl).isInstanceOf(NeoForgeResourceReloadRegistrar.class);
    }
}
