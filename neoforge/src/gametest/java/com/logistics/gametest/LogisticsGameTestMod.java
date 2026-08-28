package com.logistics.gametest;

import net.neoforged.fml.common.Mod;

/**
 * Entry point for the separate {@code logistics_gametest} mod (only present on the
 * {@code :neoforge:runGameTestServer} classpath, never in the shipped jar).
 *
 * <p>Unlike newer MC versions, 1.21.1 has no data-driven {@code TEST_FUNCTION} registry to
 * populate, so this class has nothing to register: each {@code *GameTestRegistration} class
 * carries {@link net.neoforged.neoforge.gametest.GameTestHolder}, and NeoForge discovers them by
 * scanning this mod's annotations. The class exists because {@code javafml} requires a {@code @Mod}
 * entry point for the modid declared in {@code META-INF/neoforge.mods.toml}.
 *
 * <p>Two things in {@code fabric/src/gametest} have no NeoForge counterpart; everything else does:
 *
 * <ul>
 *   <li>{@code power.CableGameTest} — every test drives Fabric's Team Reborn transactional energy
 *       API directly (abort/commit semantics with no NeoForge equivalent).
 *   <li>{@code pipe.PipeFlowGameTest#testChestItemStorageReachable} — asserts on Fabric API's own
 *       vanilla-chest-to-ItemStorage adapter.
 * </ul>
 *
 * <p>Every test here declares its own {@code batch}, which is purely diagnostic: when a test fails,
 * MC 1.21.1's runner re-queues that batch forever instead of finishing, so the run wedges without
 * ever printing a failure. A per-class batch name means the last
 * {@code Running test batch '<name>'} line in the log names the offending class.
 */
@Mod("logistics_gametest")
public final class LogisticsGameTestMod {

    public LogisticsGameTestMod() {}
}
