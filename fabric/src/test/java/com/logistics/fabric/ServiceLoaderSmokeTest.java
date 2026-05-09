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

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceLoader smoke tests — verifies that all META-INF/services/ registrations exist and
 * the implementation classes can be found and instantiated.
 *
 * <p>These tests would have caught the incident where a missing services file caused the mod
 * to fail at startup (ServiceLoader throws) while CI passed (nothing exercised the JVM).
 *
 * <p>No Minecraft bootstrap needed — all implementations have implicit no-arg constructors.
 *
 * <p>Implementation note: this branch uses {@code fabric-loader-junit}, which runs tests under
 * Knot's classloader. Knot isolates modules into separate classloader segments, causing
 * {@code ServiceLoader}'s {@code isAssignableFrom} check to fail when the service interface
 * and implementation are loaded by different segments. We therefore read the META-INF/services
 * resource directly and load the provider class by name — this validates the same invariants
 * (file exists, correct class is named, class is instantiable) without triggering the cross-
 * classloader type check. On mc/26.1, which doesn't use fabric-loader-junit, ServiceLoader
 * can be used directly.
 */
@DisplayName("ServiceLoader smoke tests (Fabric)")
class ServiceLoaderSmokeTest {

    /**
     * Verifies that a META-INF/services file exists for {@code serviceType}, names
     * {@code expectedImpl}, and that the named class can be loaded and instantiated.
     */
    private static void assertServiceRegistered(Class<?> serviceType, String expectedImpl)
            throws IOException, ReflectiveOperationException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL resource = cl.getResource("META-INF/services/" + serviceType.getName());
        assertThat(resource)
                .as("META-INF/services/%s must exist", serviceType.getName())
                .isNotNull();
        String registered = new String(resource.openStream().readAllBytes()).strip();
        assertThat(registered)
                .as("META-INF/services/%s must name %s", serviceType.getName(), expectedImpl)
                .contains(expectedImpl);
        Object instance = Class.forName(expectedImpl, true, cl)
                .getDeclaredConstructor()
                .newInstance();
        assertThat(instance).isNotNull();
    }

    @Test
    @DisplayName("FuelHelper implementation is registered")
    void fuelHelper_isRegistered() throws Exception {
        assertServiceRegistered(FuelHelper.class, FabricFuelHelper.class.getName());
    }

    @Test
    @DisplayName("PlatformService implementation is registered")
    void platformService_isRegistered() throws Exception {
        assertServiceRegistered(PlatformService.class, FabricPlatformService.class.getName());
    }

    @Test
    @DisplayName("BlockEntityTypeFactory implementation is registered")
    void blockEntityTypeFactory_isRegistered() throws Exception {
        assertServiceRegistered(BlockEntityTypeFactory.class, FabricBlockEntityBuilder.class.getName());
    }

    @Test
    @DisplayName("EnergyCapabilityLookup implementation is registered")
    void energyCapabilityLookup_isRegistered() throws Exception {
        assertServiceRegistered(EnergyCapabilityLookup.class, FabricEnergyCapabilityLookup.class.getName());
    }

    @Test
    @DisplayName("CreativeTabRegistrar implementation is registered")
    void creativeTabRegistrar_isRegistered() throws Exception {
        assertServiceRegistered(CreativeTabRegistrar.class, FabricCreativeTabRegistrar.class.getName());
    }

    @Test
    @DisplayName("ResourceReloadRegistrar implementation is registered")
    void resourceReloadRegistrar_isRegistered() throws Exception {
        assertServiceRegistered(ResourceReloadRegistrar.class, FabricResourceReloadRegistrar.class.getName());
    }
}
