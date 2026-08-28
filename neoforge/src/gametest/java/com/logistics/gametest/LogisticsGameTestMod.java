package com.logistics.gametest;

import com.logistics.gametest.automation.KilnGameTestRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Entry point for the separate {@code logistics_gametest} mod (only present on the
 * {@code :neoforge:runGameTestServer} classpath, never in the shipped jar).
 *
 * <p>Each domain's {@code *GameTestRegistration} class declares its test functions as static
 * fields, so referencing them here forces that static initialization to run — and therefore
 * populate {@link GameTestFunctions#TEST_FUNCTION}'s pending entries — before the register call
 * below hands the DeferredRegister its event bus.
 */
@Mod("logistics_gametest")
public final class LogisticsGameTestMod {

    public LogisticsGameTestMod(IEventBus modBus) {
        KilnGameTestRegistration.bootstrap();

        GameTestFunctions.TEST_FUNCTION.register(modBus);
    }
}
