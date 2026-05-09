package com.logistics.fabric;

import com.logistics.core.lib.block.BlockEntityTypeFactory;
import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.platform.CreativeTabRegistrar;
import com.logistics.core.lib.platform.PlatformService;
import com.logistics.core.lib.platform.ResourceReloadRegistrar;
import com.logistics.core.lib.power.FuelHelper;
import com.logistics.fabric.energy.FabricEnergyCapabilityLookup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceLoader smoke tests — verifies that all META-INF/services/ registrations exist and
 * the implementation classes can be found and instantiated.
 *
 * <p>These tests would have caught the incident where a missing services file caused the mod
 * to fail at startup (ServiceLoader throws) while CI passed (nothing exercised the JVM).
 *
 * <p>No Minecraft bootstrap needed — all implementations have implicit no-arg constructors.
 */
@DisplayName("ServiceLoader smoke tests (Fabric)")
class ServiceLoaderSmokeTest {

    @Test
    @DisplayName("FuelHelper implementation is registered")
    void fuelHelper_isRegistered() {
        FuelHelper impl = ServiceLoader.load(FuelHelper.class).findFirst().orElseThrow(
                () -> new AssertionError("No FuelHelper found in META-INF/services/"));
        assertThat(impl).isInstanceOf(FabricFuelHelper.class);
    }

    @Test
    @DisplayName("PlatformService implementation is registered")
    void platformService_isRegistered() {
        PlatformService impl = ServiceLoader.load(PlatformService.class).findFirst().orElseThrow(
                () -> new AssertionError("No PlatformService found in META-INF/services/"));
        assertThat(impl).isInstanceOf(FabricPlatformService.class);
    }

    @Test
    @DisplayName("BlockEntityTypeFactory implementation is registered")
    void blockEntityTypeFactory_isRegistered() {
        BlockEntityTypeFactory impl = ServiceLoader.load(BlockEntityTypeFactory.class).findFirst().orElseThrow(
                () -> new AssertionError("No BlockEntityTypeFactory found in META-INF/services/"));
        assertThat(impl).isInstanceOf(FabricBlockEntityBuilder.class);
    }

    @Test
    @DisplayName("EnergyCapabilityLookup implementation is registered")
    void energyCapabilityLookup_isRegistered() {
        EnergyCapabilityLookup impl = ServiceLoader.load(EnergyCapabilityLookup.class).findFirst().orElseThrow(
                () -> new AssertionError("No EnergyCapabilityLookup found in META-INF/services/"));
        assertThat(impl).isInstanceOf(FabricEnergyCapabilityLookup.class);
    }

    @Test
    @DisplayName("CreativeTabRegistrar implementation is registered")
    void creativeTabRegistrar_isRegistered() {
        CreativeTabRegistrar impl = ServiceLoader.load(CreativeTabRegistrar.class).findFirst().orElseThrow(
                () -> new AssertionError("No CreativeTabRegistrar found in META-INF/services/"));
        assertThat(impl).isInstanceOf(FabricCreativeTabRegistrar.class);
    }

    @Test
    @DisplayName("ResourceReloadRegistrar implementation is registered")
    void resourceReloadRegistrar_isRegistered() {
        ResourceReloadRegistrar impl = ServiceLoader.load(ResourceReloadRegistrar.class).findFirst().orElseThrow(
                () -> new AssertionError("No ResourceReloadRegistrar found in META-INF/services/"));
        assertThat(impl).isInstanceOf(FabricResourceReloadRegistrar.class);
    }
}
