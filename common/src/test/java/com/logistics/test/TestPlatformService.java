package com.logistics.test;

import com.logistics.core.lib.platform.PlatformService;

import java.nio.file.Path;

/**
 * Minimal {@link PlatformService} for unit tests, discovered via
 * {@code META-INF/services} so {@code PlatformService.INSTANCE} resolves on the
 * test classpath without a loader-specific (Fabric/NeoForge) implementation.
 *
 * <p>Reports a development environment so dev-gated command nodes (e.g.
 * {@code /logistics diagnostics test}) are present and assertable in tests.
 */
public final class TestPlatformService implements PlatformService {
    @Override
    public Path configDir() {
        return Path.of(System.getProperty("java.io.tmpdir"), "logistics-test-config");
    }

    @Override
    public long fluidUnitsPerMillibucket() {
        return 1L;
    }

    @Override
    public boolean isLighterThanAir(net.minecraft.world.level.material.Fluid fluid) {
        return false;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return true;
    }
}
